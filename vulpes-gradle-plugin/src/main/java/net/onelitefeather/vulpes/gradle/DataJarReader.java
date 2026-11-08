package net.onelitefeather.vulpes.gradle;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.onelitefeather.vulpes.format.ContentFormatException;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.format.ContentRelease;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;

/**
 * Reads the Vulpes data JARs of a task's classpath, in file name order, and merges the local content over them.
 */
final class DataJarReader {

    private DataJarReader() {
    }

    /**
     * The content of the declared projects.
     *
     * @param releases one release per declared project, sorted by project name; a project with local content has its
     *                 merged release, or its local release when it is local-only
     * @param overrides the declared projects merged with a local directory, sorted by project name
     * @param localOnly the declared projects that use their local directory alone, sorted by project name
     * @param ignored undeclared directories that were ignored, as {@code <root>/<name>}
     */
    record Content(List<ContentRelease> releases, List<Override> overrides, List<LocalContent.Served> localOnly,
                   List<String> ignored) {
    }

    /**
     * A declared project whose published content was merged with its local directory.
     *
     * @param project the content project name
     * @param directory the local directory
     * @param publishedVersion the version of the published data JAR the local content was merged over
     * @param merged the merged content and the counts of replaced and added entries
     */
    record Override(String project, Path directory, String publishedVersion, ContentMerge.Merged merged) {
    }

    /**
     * Reads the content of every declared project. A project with a local directory holding {@code release.json} and
     * {@code snapshot.json} is merged over its data JAR, or used alone when its data JAR did not resolve.
     *
     * @param localRoot the local content directory, {@code <projectDir>/<localContentDirectory>}
     * @param jars the data JARs that resolved
     * @param artifactIds artifact id by data JAR file name
     * @param coordinates declared coordinate {@code group:module:version} by declared content project
     * @param unresolved reason by declared content project whose data JAR did not resolve
     * @param forbid whether a declared project with a local directory fails the build
     * @return the releases, the overrides, the local-only projects and the ignored directories
     * @throws GradleException if local content or a data JAR cannot be used
     */
    static Content readAll(Path localRoot, FileCollection jars, Map<String, String> artifactIds,
                           Map<String, String> coordinates, Map<String, String> unresolved, boolean forbid) {
        LocalContent.Resolution local;
        try {
            local = LocalContent.resolve(localRoot, coordinates, unresolved, forbid);
        } catch (LocalContentException e) {
            throw new GradleException(e.getMessage(), e);
        }
        Map<String, ContentRelease> published = new TreeMap<>();
        for (ContentRelease content : read(jars, artifactIds)) {
            published.put(content.release().project(), content);
        }
        Set<String> servedProjects = new TreeSet<>();
        local.served().forEach(served -> servedProjects.add(served.project()));
        List<ContentRelease> releases = new ArrayList<>();
        published.forEach((project, content) -> {
            if (!servedProjects.contains(project)) {
                releases.add(content);
            }
        });
        List<Override> overrides = new ArrayList<>();
        List<LocalContent.Served> localOnly = new ArrayList<>();
        for (LocalContent.Served served : local.served()) {
            if (served.localOnly()) {
                releases.add(served.content());
                localOnly.add(served);
                continue;
            }
            ContentRelease base = published.get(served.project());
            if (base == null) {
                throw new GradleException(served.project() + ": no data JAR of this content project to merge "
                        + served.directory() + " over");
            }
            ContentMerge.Merged result;
            try {
                result = ContentMerge.merge(served.project(), base, served.content());
            } catch (LocalContentException e) {
                throw new GradleException(e.getMessage(), e);
            }
            releases.add(result.content());
            overrides.add(new Override(served.project(), served.directory(), served.mavenVersion(), result));
        }
        // A declared project whose POM resolved but whose data JAR did not is absent from the lenient view. Without a
        // local directory nothing else provides it, so it must fail rather than be skipped.
        for (String project : coordinates.keySet()) {
            if (!published.containsKey(project) && !servedProjects.contains(project)) {
                throw new GradleException(coordinates.get(project) + ": data JAR not resolved");
            }
        }
        releases.sort(Comparator.comparing((ContentRelease content) -> content.release().project()));
        return new Content(releases, overrides, localOnly, local.ignored());
    }

    /**
     * Reads every data JAR and checks that its release belongs to the artifact the JAR was resolved from.
     *
     * @param jars the data JARs
     * @param artifactIds artifact id by data JAR file name
     * @return the releases, sorted by the file name of their JAR
     * @throws GradleException if a JAR has no artifact id, cannot be read or does not match its artifact
     */
    static List<ContentRelease> read(FileCollection jars, Map<String, String> artifactIds) {
        List<ContentRelease> releases = new ArrayList<>();
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
            releases.add(content);
        }
        return releases;
    }
}
