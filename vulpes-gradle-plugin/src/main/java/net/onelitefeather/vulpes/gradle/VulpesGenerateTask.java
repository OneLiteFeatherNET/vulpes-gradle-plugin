package net.onelitefeather.vulpes.gradle;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.onelitefeather.vulpes.format.ContentFormatException;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.format.ContentRelease;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 * Writes the release constants of every Vulpes data JAR as Java sources.
 */
@CacheableTask
public abstract class VulpesGenerateTask extends DefaultTask {

    /** The Vulpes data JARs, one per content project. */
    @Classpath
    public abstract ConfigurableFileCollection getDataJars();

    /** Artifact id by data JAR file name. */
    @Input
    public abstract MapProperty<String, String> getArtifactIdsByFileName();

    /** Package of the only content project, from {@code vulpes.packageName}. */
    @Input
    @Optional
    public abstract Property<String> getPackageName();

    /** Package by content project name, from {@code vulpes.project(name).packageName}. */
    @Input
    public abstract MapProperty<String, String> getPackages();

    /** Version of the plugin, so that a new plugin regenerates the sources. */
    @Input
    public abstract Property<String> getPluginVersion();

    /** Directory of the generated sources. It is emptied on every run. */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void generate() {
        Path output = getOutputDirectory().get().getAsFile().toPath();
        clear(output);

        List<ContentRelease> releases = readReleases();
        List<String> projects = new ArrayList<>();
        releases.forEach(content -> projects.add(content.release().project()));
        Map<String, String> packages;
        try {
            packages = PackageResolver.resolve(projects, getPackageName().getOrNull(), getPackages().get());
        } catch (IllegalArgumentException e) {
            throw new GradleException(e.getMessage(), e);
        }

        for (ContentRelease content : releases) {
            String project = content.release().project();
            String source = ReleaseClassWriter.write(packages.get(project), content.release());
            write(output.resolve(packages.get(project).replace('.', '/')),
                    ReleaseClassWriter.className(project) + ".java", source);
        }
    }

    private List<ContentRelease> readReleases() {
        Map<String, String> artifactIds = getArtifactIdsByFileName().get();
        List<ContentRelease> releases = new ArrayList<>();
        for (File jar : getDataJars().getFiles().stream().sorted(Comparator.comparing(File::getName)).toList()) {
            String artifactId = artifactIds.get(jar.getName());
            if (artifactId == null) {
                throw new GradleException(jar.getName() + ": no artifact id is known for this data JAR");
            }
            ContentRelease content;
            try {
                content = ContentReader.read(jar.toPath());
            } catch (ContentFormatException e) {
                throw new GradleException(e.getMessage(), e);
            }
            String project = content.release().project();
            if (!project.equals(artifactId)) {
                throw new GradleException(jar.getName() + ": release.json project '" + project
                        + "' does not match artifact '" + artifactId + "'");
            }
            releases.add(content);
        }
        return releases;
    }

    private static void clear(Path directory) {
        try {
            if (Files.exists(directory)) {
                try (Stream<Path> paths = Files.walk(directory)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                        Files.delete(path);
                    }
                }
            }
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot clear " + directory, e);
        }
    }

    private static void write(Path directory, String fileName, String source) {
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(fileName), source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + directory.resolve(fileName), e);
        }
    }
}
