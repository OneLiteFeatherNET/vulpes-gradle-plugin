package net.onelitefeather.vulpes.codegen;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Compares the generated sources of the contract fixture with the checked-in golden files. Run with
 * {@code -Pvulpes.updateGolden=true} to rewrite the golden files instead.
 */
final class GoldenSourcesTest {

    private static final Path GOLDEN = Path.of("src/test/resources/golden");

    @Test
    void minestomSourcesMatchGoldenFiles() {
        verify("minestom", Templates.generate(Templates.minestom("0.0.0"), TestContents.fixture(), TestContents.PACKAGE));
    }

    @Test
    void keysSourcesMatchGoldenFiles() {
        verify("keys", Templates.generate(Templates.keys("5.2.0"), TestContents.fixture(), TestContents.PACKAGE));
    }

    private static void verify(String template, List<GeneratedSource> sources) {
        Path directory = GOLDEN.resolve(template);
        if (Boolean.getBoolean("vulpes.updateGolden")) {
            write(directory, sources);
            return;
        }
        Set<String> expected = names(directory);
        Set<String> actual = sources.stream().map(source -> source.className() + ".java").collect(Collectors.toSet());
        assertEquals(actual, expected, "golden files of template " + template + " must match the generated classes");
        for (GeneratedSource source : sources) {
            String golden = read(directory.resolve(source.className() + ".java"));
            assertEquals(golden, source.source(), "generated " + source.className() + " must match its golden file");
        }
    }

    private static void write(Path directory, List<GeneratedSource> sources) {
        try {
            Files.createDirectories(directory);
            for (GeneratedSource source : sources) {
                Files.writeString(directory.resolve(source.className() + ".java"), source.source(), UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Set<String> names(Path directory) {
        assertTrue(Files.isDirectory(directory), "golden directory must exist: " + directory.toAbsolutePath());
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(file -> file.getFileName().toString()).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("missing golden file " + file.toAbsolutePath(), e);
        }
    }
}
