package net.onelitefeather.vulpes.format.fixtures;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds content data JARs and Maven repositories from the format 1 contract fixtures.
 */
public final class DataJarFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long ENTRY_TIME = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
    private static final String GROUP_PATH = "net/onelitefeather/vulpes/content";
    private static final String GROUP_ID = "net.onelitefeather.vulpes.content";
    private static final String RELEASE_ENTRY = "META-INF/vulpes/release.json";
    private static final String SNAPSHOT_ENTRY = "META-INF/vulpes/snapshot.json";

    private DataJarFixtures() {
    }

    /** The {@code release.json} contract fixture of project {@code cygnus}. */
    public static byte[] releaseJson() {
        return resource("/fixtures/release.json");
    }

    /** The {@code snapshot.json} contract fixture of project {@code cygnus}. */
    public static byte[] snapshotJson() {
        return resource("/fixtures/snapshot.json");
    }

    /**
     * Writes a data JAR with both metadata entries. The output is byte-identical for identical input.
     */
    public static Path writeDataJar(Path file, byte[] releaseJson, byte[] snapshotJson) {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            putEntry(zip, RELEASE_ENTRY, releaseJson);
            putEntry(zip, SNAPSHOT_ENTRY, snapshotJson);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }

    /**
     * Writes a data JAR and a minimal POM for {@code net.onelitefeather.vulpes.content:<artifactId>:<version>}
     * into a Maven repository directory.
     */
    public static void writeMavenModule(Path repository, String artifactId, String version, byte[] releaseJson,
                                        byte[] snapshotJson) {
        Path module = repository.resolve(GROUP_PATH).resolve(artifactId).resolve(version);
        try {
            Files.createDirectories(module);
            writeDataJar(module.resolve(artifactId + "-" + version + ".jar"), releaseJson, snapshotJson);
            Files.writeString(module.resolve(artifactId + "-" + version + ".pom"), pom(artifactId, version), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Changes a {@code release.json} and returns it as compact JSON with sorted object keys.
     */
    public static byte[] modifyRelease(byte[] releaseJson, Consumer<ObjectNode> change) {
        try {
            ObjectNode release = (ObjectNode) MAPPER.readTree(releaseJson);
            change.accept(release);
            return MAPPER.writeValueAsBytes(sortKeys(release));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Changes a {@code snapshot.json} and returns it as compact JSON with sorted object keys, together with a
     * {@code release.json} whose {@code revision} is the SHA-256 of the new snapshot bytes.
     */
    public static Metadata modifySnapshot(byte[] releaseJson, byte[] snapshotJson, Consumer<ObjectNode> change) {
        try {
            ObjectNode snapshot = (ObjectNode) MAPPER.readTree(snapshotJson);
            change.accept(snapshot);
            byte[] modifiedSnapshot = MAPPER.writeValueAsBytes(sortKeys(snapshot));
            byte[] modifiedRelease = modifyRelease(releaseJson,
                    release -> release.put("revision", "sha256:" + sha256Hex(modifiedSnapshot)));
            return new Metadata(modifiedRelease, modifiedSnapshot);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The two metadata files of one data JAR. */
    public record Metadata(byte[] releaseJson, byte[] snapshotJson) {
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void putEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(ENTRY_TIME);
        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }

    private static String pom(String artifactId, String version) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    <packaging>jar</packaging>
                </project>
                """.formatted(GROUP_ID, artifactId, version);
    }

    private static JsonNode sortKeys(JsonNode node) {
        if (node.isObject()) {
            Map<String, JsonNode> fields = new TreeMap<>();
            ((ObjectNode) node).properties().forEach(field -> fields.put(field.getKey(), field.getValue()));
            ObjectNode sorted = MAPPER.createObjectNode();
            fields.forEach((name, value) -> sorted.set(name, sortKeys(value)));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode sorted = MAPPER.createArrayNode();
            node.forEach(element -> sorted.add(sortKeys(element)));
            return sorted;
        }
        return node;
    }

    private static byte[] resource(String name) {
        try (InputStream in = DataJarFixtures.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("missing test fixture " + name);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
