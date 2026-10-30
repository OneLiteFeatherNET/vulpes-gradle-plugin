package net.onelitefeather.vulpes.codegen;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GeneratedSourcesCompileTest {

    @Test
    void minestomSourcesCompileAgainstVulpesBase(@TempDir Path output) {
        List<GeneratedSource> sources = Templates.generate(Templates.minestom("0.0.0"), TestContents.fixture(),
                TestContents.PACKAGE);

        String diagnostics = compile(sources, classPath(entry -> true), output);

        assertTrue(diagnostics.isEmpty(), "minestom sources must compile without diagnostics:\n" + diagnostics);
    }

    @Test
    void keysSourcesCompileWithAdventureKeyOnly(@TempDir Path output) {
        List<GeneratedSource> sources = Templates.generate(Templates.keys("5.2.0"), TestContents.fixture(),
                TestContents.PACKAGE);

        String diagnostics = compile(sources, classPath(entry -> entry.contains("adventure-key")), output);

        assertTrue(diagnostics.isEmpty(), "keys sources must compile with adventure-key only:\n" + diagnostics);
    }

    /**
     * Compiles the sources in memory into {@code output} and returns the diagnostics, empty when it compiled.
     */
    static String compile(List<GeneratedSource> sources, String classPath, Path output) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("a system Java compiler is required to check generated code");
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<JavaFileObject> units = new ArrayList<>();
        for (GeneratedSource source : sources) {
            units.add(new InMemorySource(source.packageName() + "." + source.className(), source.source()));
        }
        List<String> options = List.of("-proc:none", "-encoding", "UTF-8", "-classpath", classPath, "-d",
                output.toString());
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, UTF_8)) {
            boolean ok = compiler.getTask(null, files, diagnostics, options, null, units).call();
            if (ok) {
                return "";
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String text = diagnostics.getDiagnostics().stream().map(Object::toString).collect(Collectors.joining("\n"));
        return text.isEmpty() ? "compilation failed without diagnostics" : text;
    }

    /**
     * The class path of this test JVM, filtered by the given predicate.
     */
    static String classPath(java.util.function.Predicate<String> keep) {
        return Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator))
                .filter(keep)
                .collect(Collectors.joining(java.io.File.pathSeparator));
    }

    private static final class InMemorySource extends SimpleJavaFileObject {

        private final String source;

        InMemorySource(String qualifiedName, String source) {
            super(URI.create("string:///" + qualifiedName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
