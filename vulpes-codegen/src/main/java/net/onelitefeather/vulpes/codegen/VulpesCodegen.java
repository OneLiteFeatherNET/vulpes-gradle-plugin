package net.onelitefeather.vulpes.codegen;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import net.onelitefeather.vulpes.codegen.internal.Template;
import net.onelitefeather.vulpes.codegen.internal.Templates;
import net.onelitefeather.vulpes.format.ContentRelease;

/**
 * The entry point of the Vulpes code generator, used by the Gradle plugin and by the generator service. It turns a
 * content release, as read by {@code vulpes-format}, into Java sources.
 * <p>
 * Everything in other packages of this library is internal and may change in any release.
 */
public final class VulpesCodegen {

    private static final String VERSIONS_RESOURCE = "versions.properties";

    private VulpesCodegen() {
    }

    /**
     * Generates the Java sources of one content release.
     *
     * @return the sources, each with its package and class name
     * @throws CodegenException if the release cannot be generated, for example because it uses a name that is reserved
     */
    public static List<GeneratedSource> generate(ContentRelease content, CodegenOptions options) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(options, "options");
        return Templates.generate(template(options.template()), content, options.packageName());
    }

    /**
     * Writes the Java sources of one content release below the directory, in their package layout. Files are created
     * or overwritten; nothing else in the directory is touched.
     *
     * @throws CodegenException if the release cannot be generated
     * @throws UncheckedIOException if a file cannot be written
     */
    public static void write(ContentRelease content, CodegenOptions options, Path directory) {
        Objects.requireNonNull(directory, "directory");
        for (GeneratedSource source : generate(content, options)) {
            Path packageDirectory = directory.resolve(source.packageName().replace('.', '/'));
            Path file = packageDirectory.resolve(source.className() + ".java");
            try {
                Files.createDirectories(packageDirectory);
                Files.writeString(file, source.source(), UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("cannot write " + file, e);
            }
        }
    }

    /**
     * The libraries that the generated code of the template needs at runtime, in the version this generator was built
     * for.
     */
    public static List<Dependency> runtimeDependencies(CodegenTemplate template) {
        Objects.requireNonNull(template, "template");
        return template(template).runtimeDependencies();
    }

    private static Template template(CodegenTemplate template) {
        Properties versions = versions();
        return switch (template) {
            case MINESTOM -> Templates.minestom(require(versions, "vulpes-base"));
            case KEYS -> Templates.keys(require(versions, "adventure-key"));
        };
    }

    private static Properties versions() {
        try (InputStream in = VulpesCodegen.class.getResourceAsStream(VERSIONS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("vulpes-codegen has no " + VERSIONS_RESOURCE);
            }
            Properties versions = new Properties();
            versions.load(in);
            return versions;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + VERSIONS_RESOURCE, e);
        }
    }

    private static String require(Properties versions, String key) {
        return Objects.requireNonNull(versions.getProperty(key), () -> VERSIONS_RESOURCE + " has no " + key);
    }
}
