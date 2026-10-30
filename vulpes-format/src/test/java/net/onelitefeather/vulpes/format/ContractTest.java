package net.onelitefeather.vulpes.format;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.onelitefeather.vulpes.format.TestFixtures.bytes;
import static net.onelitefeather.vulpes.format.TestFixtures.sha256Hex;
import static net.onelitefeather.vulpes.format.TestFixtures.tree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ContractTest {

    private static final String SCHEMA_DIR = "/net/onelitefeather/vulpes/format/";
    private static final String SNAPSHOT_FIXTURE = "/fixtures/snapshot.json";
    private static final String RELEASE_FIXTURE = "/fixtures/release.json";

    @Test
    void snapshotFixtureMatchesSnapshotSchema() {
        assertFixtureMatchesSchema("snapshot-1.schema.json", SNAPSHOT_FIXTURE);
    }

    @Test
    void releaseFixtureMatchesReleaseSchema() {
        assertFixtureMatchesSchema("release-1.schema.json", RELEASE_FIXTURE);
    }

    @Test
    void releaseSnapshotVersionMatchesReleaseSchema() {
        assertEquals(List.of(), releaseViolations("1.5.0-SNAPSHOT"),
                "a -SNAPSHOT version must validate against release-1.schema.json");
    }

    @Test
    void releasePreviewVersionIsRejected() {
        List<String> violations = releaseViolations("1.5.0-dev.3");
        assertEquals(1, violations.size(), "a -dev.N version must be rejected once: " + violations);
        assertTrue(violations.get(0).startsWith("/version"), "the violation must point at the version: " + violations);
    }

    @Test
    void releaseRevisionIsSha256OfSnapshotBytes() {
        String revision = tree(bytes(RELEASE_FIXTURE)).get("revision").asText();
        assertEquals("sha256:" + sha256Hex(bytes(SNAPSHOT_FIXTURE)), revision,
                "release.json revision must be the sha256 of the exact snapshot.json bytes");
    }

    @Test
    void itemsAreSortedByKey() {
        assertSortedBy("items", "key");
    }

    @Test
    void attributesAreSortedByKey() {
        assertSortedBy("attributes", "key");
    }

    @Test
    void fontsAreSortedByKey() {
        assertSortedBy("fonts", "key");
    }

    @Test
    void soundEventsAreSortedByKey() {
        assertSortedBy("sounds", "key");
    }

    @Test
    void advancementsAreSortedByKey() {
        assertSortedBy("advancements", "key");
    }

    @Test
    void expressionFunctionsAreSortedByName() {
        assertSortedBy("expressionFunctions", "name");
    }

    @Test
    void assetsAreSortedByPath() {
        assertSortedBy("assets", "path");
    }

    @Test
    void soundEntriesAreSortedByName() {
        List<String> names = new ArrayList<>();
        tree(bytes(SNAPSHOT_FIXTURE)).get("sounds")
                .forEach(event -> event.get("sounds").forEach(entry -> names.add(entry.get("name").asText())));
        assertSorted("sounds[].sounds[].name", names);
    }

    @Test
    void previousKeysAreSortedInEveryEntity() {
        JsonNode snapshot = tree(bytes(SNAPSHOT_FIXTURE));
        for (String list : List.of("items", "attributes", "fonts", "sounds", "advancements")) {
            snapshot.get(list).forEach(entity -> assertSortedArray(list + "[].previousKeys", entity.get("previousKeys")));
        }
        snapshot.get("expressionFunctions")
                .forEach(function -> assertSortedArray("expressionFunctions[].previousNames", function.get("previousNames")));
    }

    @Test
    void packResolutionsAreAscending() {
        List<Integer> resolutions = new ArrayList<>();
        tree(bytes(SNAPSHOT_FIXTURE)).get("pack").get("resolutions").forEach(value -> resolutions.add(value.asInt()));
        assertEquals(resolutions.stream().sorted().toList(), resolutions, "pack.resolutions must be ascending");
    }

    @Test
    void releasePacksAreSortedById() {
        List<String> ids = new ArrayList<>();
        tree(bytes(RELEASE_FIXTURE)).get("packs").forEach(pack -> ids.add(pack.get("id").asText()));
        assertSorted("packs[].id", ids);
    }

    private static void assertFixtureMatchesSchema(String schemaName, String fixture) {
        assertEquals(List.of(), violations(schemaName, new String(bytes(fixture), UTF_8)),
                fixture + " must validate against " + schemaName);
    }

    private static List<String> releaseViolations(String version) {
        ObjectNode release = (ObjectNode) tree(bytes(RELEASE_FIXTURE));
        release.put("version", version);
        return violations("release-1.schema.json", release.toString());
    }

    private static List<String> violations(String schemaName, String json) {
        Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(new ByteArrayInputStream(bytes(SCHEMA_DIR + schemaName)), InputFormat.JSON);
        return schema.validate(json, InputFormat.JSON).stream()
                .map(error -> error.getInstanceLocation() + ": " + error.getMessage())
                .toList();
    }

    private static void assertSortedBy(String list, String field) {
        List<String> values = new ArrayList<>();
        tree(bytes(SNAPSHOT_FIXTURE)).get(list).forEach(entity -> values.add(entity.get(field).asText()));
        assertSorted(list + "[]." + field, values);
    }

    private static void assertSortedArray(String what, JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        assertSorted(what, values);
    }

    private static void assertSorted(String what, List<String> values) {
        assertEquals(values.stream().sorted().toList(), values, what + " must be sorted");
    }
}
