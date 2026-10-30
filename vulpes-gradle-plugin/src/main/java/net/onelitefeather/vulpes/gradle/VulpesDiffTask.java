package net.onelitefeather.vulpes.gradle;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import net.onelitefeather.vulpes.format.ContentFormatException;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.format.ContentRelease;
import net.onelitefeather.vulpes.format.diff.ContentDiff;
import net.onelitefeather.vulpes.format.diff.DiffReport;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

/**
 * Compares the content data JARs with the same artifacts of the version given by {@code vulpes.diff.from}.
 */
@UntrackedTask(because = "Compares the releases on every run; the reports are cheap to write and never cached.")
public abstract class VulpesDiffTask extends DefaultTask {

    /** The Vulpes data JARs of the current versions, one per content project. */
    @Classpath
    public abstract ConfigurableFileCollection getDataJars();

    /** Artifact id by data JAR file name of the current versions. */
    @Input
    public abstract MapProperty<String, String> getArtifactIdsByFileName();

    /** The Vulpes data JARs of the base version, one per content project. */
    @Classpath
    public abstract ConfigurableFileCollection getBaseDataJars();

    /** Artifact id by data JAR file name of the base version. */
    @Input
    public abstract MapProperty<String, String> getBaseArtifactIdsByFileName();

    /** The version to compare with, from {@code vulpes.diff.from}. */
    @Input
    @Optional
    public abstract Property<String> getBaseVersion();

    /** The content project to compare, from {@code vulpes.diff.project}. Every project when absent. */
    @Input
    @Optional
    public abstract Property<String> getProjectFilter();

    /** Directory of the Markdown and JSON reports. */
    @Internal
    public abstract DirectoryProperty getReportDirectory();

    @TaskAction
    public void compare() {
        String baseVersion = getBaseVersion().getOrNull();
        if (baseVersion == null) {
            throw new GradleException("vulpesDiff needs the version to compare with: "
                    + "run it with -Pvulpes.diff.from=<version>");
        }
        Map<String, ContentRelease> current = read(getDataJars(), getArtifactIdsByFileName().get());
        Map<String, ContentRelease> base = read(getBaseDataJars(), getBaseArtifactIdsByFileName().get());
        String filter = getProjectFilter().getOrNull();
        Path reports = getReportDirectory().get().getAsFile().toPath();

        int compared = 0;
        for (Map.Entry<String, ContentRelease> entry : current.entrySet()) {
            String project = entry.getKey();
            if (filter != null && !filter.equals(project)) {
                continue;
            }
            ContentRelease before = base.get(project);
            if (before == null) {
                throw new GradleException(project + ": no data JAR of version " + baseVersion
                        + " in the vulpesDiffBase configuration");
            }
            DiffReport report = ContentDiff.compare(before, entry.getValue());
            String name = project + "-" + baseVersion + "-" + report.toVersion();
            Path markdown = reports.resolve(name + ".md");
            write(markdown, report.toMarkdown());
            write(reports.resolve(name + ".json"), report.toJson());
            getLogger().lifecycle("vulpes diff {} {} -> {}: {}, report {}", project, baseVersion,
                    report.toVersion(), summary(report), markdown);
            compared++;
        }
        if (compared == 0) {
            throw new GradleException("vulpesDiff found no content project to compare"
                    + (filter == null ? "" : " for vulpes.diff.project=" + filter));
        }
    }

    private static String summary(DiffReport report) {
        if (!report.hasChanges()) {
            return "no changes, no version bump required";
        }
        return "required version bump " + report.requiredBump();
    }

    private static Map<String, ContentRelease> read(ConfigurableFileCollection jars, Map<String, String> artifactIds) {
        Map<String, ContentRelease> releases = new TreeMap<>();
        for (File jar : jars.getFiles().stream().sorted(Comparator.comparing(File::getName)).toList()) {
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
            releases.put(artifactId, content);
        }
        return releases;
    }

    private static void write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + file, e);
        }
    }
}
