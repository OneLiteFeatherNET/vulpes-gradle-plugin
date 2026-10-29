package net.onelitefeather.vulpes.format;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.onelitefeather.vulpes.format.fixtures.DataJarFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DataJarFixturesTest {

    private static final String PROJECT = "cygnus";
    private static final String VERSION = "1.4.0";

    @Test
    void writtenDataJarIsReadByContentReader(@TempDir Path dir) {
        Path jar = DataJarFixtures.writeDataJar(dir.resolve("cygnus-1.4.0.jar"),
                DataJarFixtures.releaseJson(), DataJarFixtures.snapshotJson());

        ContentRelease content = ContentReader.read(jar);

        assertEquals(PROJECT, content.release().project(), "project of the written data JAR");
    }

    @Test
    void writtenDataJarIsByteIdenticalForSameInput(@TempDir Path dir) throws IOException {
        Path first = DataJarFixtures.writeDataJar(dir.resolve("first.jar"),
                DataJarFixtures.releaseJson(), DataJarFixtures.snapshotJson());
        Path second = DataJarFixtures.writeDataJar(dir.resolve("second.jar"),
                DataJarFixtures.releaseJson(), DataJarFixtures.snapshotJson());

        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second),
                "data JARs written from the same input must be byte-identical");
    }

    @Test
    void mavenModuleIsLaidOutInRepositoryStructure(@TempDir Path repository) {
        DataJarFixtures.writeMavenModule(repository, PROJECT, VERSION,
                DataJarFixtures.releaseJson(), DataJarFixtures.snapshotJson());

        Path module = repository.resolve("net/onelitefeather/vulpes/content/cygnus/1.4.0");

        assertTrue(Files.isRegularFile(module.resolve("cygnus-1.4.0.jar")), "data JAR in the module directory");
        assertTrue(Files.isRegularFile(module.resolve("cygnus-1.4.0.pom")), "POM in the module directory");
    }

    @Test
    void modifiedReleaseJsonChangesRequestedField() {
        byte[] modified = DataJarFixtures.modifyRelease(DataJarFixtures.releaseJson(),
                release -> release.put("version", "1.5.0"));

        String json = new String(modified, UTF_8);

        assertTrue(json.contains("\"version\":\"1.5.0\""), "requested field must be changed: " + json);
    }

    @Test
    void modifiedReleaseJsonIsCompactWithSortedKeys() {
        byte[] modified = DataJarFixtures.modifyRelease(DataJarFixtures.releaseJson(),
                release -> release.put("version", "1.5.0"));

        String json = new String(modified, UTF_8);

        assertFalse(json.contains("\n"), "output must not contain line breaks: " + json);
        assertTrue(json.indexOf("\"format\"") < json.indexOf("\"project\""), "keys must be sorted: " + json);
    }
}
