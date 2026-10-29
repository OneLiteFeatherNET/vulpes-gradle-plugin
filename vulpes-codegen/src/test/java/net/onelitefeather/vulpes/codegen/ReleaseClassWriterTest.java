package net.onelitefeather.vulpes.codegen;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.format.Pack;
import net.onelitefeather.vulpes.format.Provenance;
import net.onelitefeather.vulpes.format.Release;
import net.onelitefeather.vulpes.format.fixtures.DataJarFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ReleaseClassWriterTest {

    private static final String PACKAGE = "net.onelitefeather.vulpes.content.cygnus";
    private static final String REVISION = "sha256:046d3f84a66ba0940f6686d2679df8ac03d02c0350e0312e79feac5c9318b0b4";
    private static final String PACK_16X_URL =
            "https://cdn.onelitefeather.net/packs/cygnus/3b1e5c0d9a8f7e6d5c4b3a291807f6e5d4c3b2a1.zip";
    private static final String PACK_32X_URL =
            "https://cdn.onelitefeather.net/packs/cygnus/a07d1e2f3a4b5c6d7e8f90a1b2c3d4e5f6a7b8c9.zip";

    @Test
    void declaresFinalClassNamedAfterProjectInPackage() {
        String source = ReleaseClassWriter.write(PACKAGE, fixtureRelease());

        assertTrue(source.contains("package net.onelitefeather.vulpes.content.cygnus;\n"), "package line:\n" + source);
        assertTrue(source.contains("public final class CygnusRelease {\n"), "class declaration:\n" + source);
    }

    @Test
    void declaresProjectVersionAndRevisionConstants() {
        String source = ReleaseClassWriter.write(PACKAGE, fixtureRelease());

        assertTrue(source.contains("    public static final String PROJECT = \"cygnus\";\n"), "PROJECT:\n" + source);
        assertTrue(source.contains("    public static final String VERSION = \"1.4.0\";\n"), "VERSION:\n" + source);
        assertTrue(source.contains("    public static final String REVISION = \"" + REVISION + "\";\n"),
                "REVISION:\n" + source);
    }

    @Test
    void declaresPackConstantsWithIdResolutionUrlSha1AndSize() {
        String source = ReleaseClassWriter.write(PACKAGE, fixtureRelease());

        assertTrue(source.contains("    public static final Pack PACK_16X = new Pack(\"16x\", 16, \"" + PACK_16X_URL
                + "\", \"3b1e5c0d9a8f7e6d5c4b3a291807f6e5d4c3b2a1\", 123456L);\n"), "PACK_16X:\n" + source);
        assertTrue(source.contains("    public static final Pack PACK_32X = new Pack(\"32x\", 32, \"" + PACK_32X_URL
                + "\", \"a07d1e2f3a4b5c6d7e8f90a1b2c3d4e5f6a7b8c9\", 234567L);\n"), "PACK_32X:\n" + source);
    }

    @Test
    void listsPacksInContractOrder() {
        String source = ReleaseClassWriter.write(PACKAGE, fixtureRelease());

        assertTrue(source.contains("    public static final List<Pack> PACKS = List.of(PACK_16X, PACK_32X);\n"),
                "PACKS in contract order:\n" + source);
    }

    @Test
    void emptyPackListYieldsEmptyPacksConstant() {
        String source = ReleaseClassWriter.write(PACKAGE, releaseWithPacks(List.of()));

        assertTrue(source.contains("    public static final List<Pack> PACKS = List.of();\n"),
                "empty PACKS:\n" + source);
        assertFalse(source.contains("PACK_"), "no pack constants without packs:\n" + source);
    }

    @Test
    void omitsProvenanceValues() {
        String source = ReleaseClassWriter.write(PACKAGE, fixtureRelease());

        assertFalse(source.contains("stelaris"), "createdBy must not be generated:\n" + source);
        assertFalse(source.contains("2026"), "creation time must not be generated:\n" + source);
    }

    @Test
    void escapesQuotesAndBackslashesInStringLiterals() {
        Pack pack = new Pack("16x", 16, "https://x/\"a\\b", "3b1e5c0d9a8f7e6d5c4b3a291807f6e5d4c3b2a1", 1L);

        String source = ReleaseClassWriter.write(PACKAGE, releaseWithPacks(List.of(pack)));

        assertTrue(source.contains("\"https://x/\\\"a\\\\b\""), "escaped url literal:\n" + source);
    }

    @Test
    void escapesControlCharactersAsNamedOrUnicodeEscape() {
        Pack pack = new Pack("16x", 16, "https://x/\u0001\n", "3b1e5c0d9a8f7e6d5c4b3a291807f6e5d4c3b2a1", 1L);

        String source = ReleaseClassWriter.write(PACKAGE, releaseWithPacks(List.of(pack)));

        assertTrue(source.contains("\"https://x/\\u0001\\n\""), "escaped control characters:\n" + source);
    }

    @Test
    void generatedSourceCompiles(@TempDir Path output) {
        String source = ReleaseClassWriter.write(PACKAGE, fixtureRelease());

        assertCompiles(PACKAGE, "CygnusRelease", source, output);
    }

    @Test
    void generatedSourceWithoutPacksCompiles(@TempDir Path output) {
        String source = ReleaseClassWriter.write(PACKAGE, releaseWithPacks(List.of()));

        assertCompiles(PACKAGE, "CygnusRelease", source, output);
    }

    private static Release fixtureRelease() {
        return ContentReader.read("fixture", DataJarFixtures.releaseJson(), DataJarFixtures.snapshotJson()).release();
    }

    private static Release releaseWithPacks(List<Pack> packs) {
        return new Release(1, "cygnus", UUID.fromString("5b0c7a52-3c1e-4d7a-9f2e-0a1b2c3d4e5f"), "1.4.0", REVISION,
                packs, new Provenance("2.0.0", "stelaris", Instant.parse("2026-10-29T10:00:00Z")));
    }

    private static void assertCompiles(String packageName, String className, String source, Path output) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "a system Java compiler is required to check generated code");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, UTF_8)) {
            files.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(output));
            boolean compiled = compiler.getTask(null, files, diagnostics, List.of("-proc:none"), null,
                    List.of(new InMemorySource(className, source))).call();

            assertTrue(compiled && diagnostics.getDiagnostics().isEmpty(),
                    "generated source must compile without diagnostics: " + diagnostics.getDiagnostics());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Path classFile = output.resolve(packageName.replace('.', '/')).resolve(className + ".class");
        assertTrue(Files.isRegularFile(classFile), "compiled class file must exist: " + classFile);
    }

    private static final class InMemorySource extends SimpleJavaFileObject {

        private final String source;

        InMemorySource(String className, String source) {
            super(URI.create("string:///" + className + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
