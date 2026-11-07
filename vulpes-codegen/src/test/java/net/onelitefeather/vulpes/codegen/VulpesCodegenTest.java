package net.onelitefeather.vulpes.codegen;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.format.ContentRelease;
import net.onelitefeather.vulpes.format.fixtures.DataJarFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class VulpesCodegenTest {

    private static final Path GOLDEN = Path.of("src/test/resources/golden");
    private static final String PACKAGE = "net.onelitefeather.vulpes.content.cygnus";

    @Test
    void generateReturnsTheMinestomGoldenSources() {
        assertGoldenSources("minestom", VulpesCodegen.generate(fixture(), new CodegenOptions(PACKAGE,
                CodegenTemplate.MINESTOM)));
    }

    @Test
    void generateReturnsTheKeysGoldenSources() {
        assertGoldenSources("keys", VulpesCodegen.generate(fixture(), new CodegenOptions(PACKAGE,
                CodegenTemplate.KEYS)));
    }

    @Test
    void writeCreatesTheSourcesInPackageLayoutAndNoOtherFile(@TempDir Path directory) {
        CodegenOptions options = new CodegenOptions(PACKAGE, CodegenTemplate.KEYS);
        List<GeneratedSource> sources = VulpesCodegen.generate(fixture(), options);

        VulpesCodegen.write(fixture(), options, directory);

        Set<String> expected = sources.stream()
                .map(source -> PACKAGE.replace('.', '/') + "/" + source.className() + ".java")
                .collect(Collectors.toSet());
        assertEquals(expected, relativeFiles(directory), "files written below the output directory");
        for (GeneratedSource source : sources) {
            assertEquals(source.source(), read(directory.resolve(PACKAGE.replace('.', '/'))
                    .resolve(source.className() + ".java")), source.className() + " must be written as generated");
        }
    }

    @Test
    void generateFailsWithCodegenExceptionNamingTheProjectForReservedFontNamespace() {
        ContentRelease reserved = withReservedFont();

        CodegenException exception = assertThrows(CodegenException.class,
                () -> VulpesCodegen.generate(reserved, new CodegenOptions(PACKAGE, CodegenTemplate.MINESTOM)));

        assertTrue(exception.getMessage().contains("'cygnus'"),
                "message must name the project cygnus: " + exception.getMessage());
    }

    @Test
    void runtimeDependencyOfMinestomIsVulpesBase() {
        assertEquals(List.of(new Dependency("net.onelitefeather.vulpes", "vulpes-base", "0.6.0")),
                VulpesCodegen.runtimeDependencies(CodegenTemplate.MINESTOM), "runtime dependencies of minestom");
    }

    @Test
    void runtimeDependencyOfKeysIsAdventureKey() {
        assertEquals(List.of(new Dependency("net.kyori", "adventure-key", "5.2.0")),
                VulpesCodegen.runtimeDependencies(CodegenTemplate.KEYS), "runtime dependencies of keys");
    }

    private static ContentRelease fixture() {
        return ContentReader.read("fixture", DataJarFixtures.releaseJson(), DataJarFixtures.snapshotJson());
    }

    /** The contract fixture with a font in the reserved namespace {@code space}. */
    private static ContentRelease withReservedFont() {
        DataJarFixtures.Metadata reserved = DataJarFixtures.modifySnapshot(DataJarFixtures.releaseJson(),
                DataJarFixtures.snapshotJson(), snapshot -> {
                    ObjectNode font = ((ArrayNode) snapshot.get("fonts")).addObject();
                    font.put("id", "3c8d9e0f-1a2b-4c3d-8e4f-5a6b7c8d9e0f");
                    font.put("key", "cygnus:space_icons");
                    font.put("font", "space:default");
                    font.put("type", "bitmap");
                    font.put("file", "cygnus:font/space_icons.png");
                    font.put("ascent", 7);
                    font.put("height", 8);
                    font.putArray("chars");
                    font.putArray("previousKeys");
                });
        return ContentReader.read("reserved", reserved.releaseJson(), reserved.snapshotJson());
    }

    private static void assertGoldenSources(String template, List<GeneratedSource> sources) {
        Path directory = GOLDEN.resolve(template);
        Set<String> expected = listNames(directory);
        Set<String> actual = sources.stream().map(source -> source.className() + ".java").collect(Collectors.toSet());
        assertEquals(expected, actual, "generated classes must match the golden files of " + template);
        for (GeneratedSource source : sources) {
            assertEquals(read(directory.resolve(source.className() + ".java")), source.source(),
                    "generated " + source.className() + " must match its golden file");
        }
    }

    private static Set<String> listNames(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(file -> file.getFileName().toString()).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list " + directory.toAbsolutePath(), e);
        }
    }

    private static Set<String> relativeFiles(Path directory) {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> directory.relativize(file).toString().replace('\\', '/'))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot walk " + directory, e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file.toAbsolutePath(), e);
        }
    }
}
