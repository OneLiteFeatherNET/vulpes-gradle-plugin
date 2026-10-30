package net.onelitefeather.vulpes.format.diff;

import static net.onelitefeather.vulpes.format.diff.DiffFixtures.IRON_SWORD;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.OLD_SWORD;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.STEEL_SWORD;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.SWORD;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.content;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.maxHealth;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.item;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.onelitefeather.vulpes.format.ExpressionParameter;
import net.onelitefeather.vulpes.format.ExpressionType;
import org.junit.jupiter.api.Test;

final class DiffReportTest {

    @Test
    void markdownStatesNoChangesForEqualReleases() {
        DiffReport report = ContentDiff.compare(
                content("1.4.0", snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of())),
                content("1.5.0", snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of())));

        String markdown = report.toMarkdown();

        assertTrue(markdown.contains("No changes."), "markdown of equal releases:\n" + markdown);
    }

    @Test
    void markdownListsRenamedItemWithItsNewKey() {
        String markdown = renamedReport().toMarkdown();

        assertTrue(markdown.contains("- `" + OLD_SWORD + "` -> `" + STEEL_SWORD + "`"),
                "markdown must list the rename:\n" + markdown);
    }

    @Test
    void markdownNamesRequiredBump() {
        String markdown = renamedReport().toMarkdown();

        assertTrue(markdown.contains("Required version bump: MINOR"), "markdown must name the bump:\n" + markdown);
    }

    @Test
    void markdownListsChangedExpressionInExpressionsSection() {
        List<ExpressionParameter> level = List.of(new ExpressionParameter("level", ExpressionType.NUMBER));
        String markdown = ContentDiff.compare(
                content("1.4.0", snapshot(List.of(), List.of(), List.of(), List.of(maxHealth(level, ExpressionType.NUMBER, "20 + level")))),
                content("1.5.0", snapshot(List.of(), List.of(), List.of(), List.of(maxHealth(level, ExpressionType.NUMBER, "30 + level")))))
                .toMarkdown();

        assertTrue(markdown.contains("## Expressions\n"), "markdown must have an expressions section:\n" + markdown);
        assertTrue(markdown.contains("- `maxHealth`: expression"), "markdown must list the changed source:\n" + markdown);
    }

    @Test
    void jsonHasSortedObjectKeys() throws IOException {
        JsonNode json = new ObjectMapper().readTree(renamedReport().toJson());

        assertEquals(List.of("advancements", "attributes", "expressionFunctions", "expressions", "fonts", "fromVersion", "items",
                        "packs", "project", "requiredBump", "sounds", "toVersion"), names(json.fieldNames()),
                "root keys of the report");
        assertEquals(List.of("added", "changed", "removed", "renamed"), names(json.get("items").fieldNames()),
                "keys of the items section");
    }

    @Test
    void jsonStatesRequiredBump() throws IOException {
        JsonNode json = new ObjectMapper().readTree(renamedReport().toJson());

        assertEquals("MINOR", json.get("requiredBump").asText(), "requiredBump in json");
    }

    private static DiffReport renamedReport() {
        return ContentDiff.compare(
                content("1.4.0", snapshot(List.of(item(SWORD, OLD_SWORD, List.of())), List.of(), List.of())),
                content("1.5.0", snapshot(List.of(item(SWORD, STEEL_SWORD, List.of(OLD_SWORD))), List.of(), List.of())));
    }

    private static List<String> names(Iterator<String> fieldNames) {
        List<String> names = new ArrayList<>();
        fieldNames.forEachRemaining(names::add);
        return names;
    }
}
