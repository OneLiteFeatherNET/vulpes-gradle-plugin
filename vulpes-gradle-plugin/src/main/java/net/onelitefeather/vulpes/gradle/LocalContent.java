package net.onelitefeather.vulpes.gradle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.onelitefeather.vulpes.format.ContentFormatException;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.format.ContentRelease;

/**
 * Selects the local content directories that are merged over the data JARs of declared content projects. Uses no
 * Gradle types. The local files get the format checks only; the cross-entry rules run on the merged result.
 */
final class LocalContent {

    static final String RELEASE_FILE = "release.json";
    static final String SNAPSHOT_FILE = "snapshot.json";

    /** Same form as the version rule of {@code release-1.schema.json}: X.Y.Z, or X.Y.Z-SNAPSHOT. */
    private static final Pattern VERSION = Pattern.compile(
            "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-SNAPSHOT)?");

    private LocalContent() {
    }

    /**
     * A declared content project with a local directory, whose content is merged over its data JAR.
     *
     * @param project the content project name, equal to the directory name
     * @param directory the directory the files were read from
     * @param mavenVersion the version of the published data JAR the local content is merged over
     * @param content the local release and snapshot, checked for format only
     */
    record Served(String project, Path directory, String mavenVersion, ContentRelease content) {
    }

    /**
     * The outcome of a selection.
     *
     * @param served declared projects with a local directory, sorted by project name
     * @param ignored undeclared directories that hold content files, as {@code <root>/<name>}, sorted
     */
    record Resolution(List<Served> served, List<String> ignored) {

        /** Names of the projects served locally. */
        Set<String> servedProjects() {
            Set<String> projects = new TreeSet<>();
            served.forEach(s -> projects.add(s.project()));
            return projects;
        }
    }

    /**
     * Selects the local content.
     *
     * @param root the local content directory, {@code <projectDir>/<localContentDirectory>}
     * @param mavenVersions Maven version by declared content project; its keys are the declared projects
     * @param forbid whether a local override of a declared project is an error
     * @return the projects with a local directory and the ignored undeclared directories
     * @throws LocalContentException if a directory is partial, fails a format check, or is used while {@code forbid} is set
     */
    static Resolution resolve(Path root, Map<String, String> mavenVersions, boolean forbid) {
        List<Served> served = new ArrayList<>();
        for (String project : new TreeSet<>(mavenVersions.keySet())) {
            Path directory = root.resolve(project);
            boolean hasRelease = Files.isRegularFile(directory.resolve(RELEASE_FILE));
            boolean hasSnapshot = Files.isRegularFile(directory.resolve(SNAPSHOT_FILE));
            if (!hasRelease && !hasSnapshot) {
                continue;
            }
            String name = displayName(root, project);
            if (!hasRelease) {
                throw new LocalContentException(name + ": missing " + RELEASE_FILE + "; local content is not merged and the build does not fall back to the data JAR");
            }
            if (!hasSnapshot) {
                throw new LocalContentException(name + ": missing " + SNAPSHOT_FILE + "; local content is not merged and the build does not fall back to the data JAR");
            }
            served.add(new Served(project, directory, mavenVersions.get(project), read(name, project, directory)));
        }
        if (forbid && !served.isEmpty()) {
            List<String> names = served.stream()
                    .map(s -> s.project() + " (" + displayName(root, s.project()) + ")")
                    .toList();
            throw new LocalContentException("vulpes.local.forbid=true: content merged from local directories: "
                    + String.join(", ", names) + "; remove the local directories or unset vulpes.local.forbid");
        }
        return new Resolution(served, ignored(root, mavenVersions.keySet()));
    }

    private static ContentRelease read(String name, String project, Path directory) {
        byte[] release = bytes(name, directory.resolve(RELEASE_FILE));
        byte[] snapshot = bytes(name, directory.resolve(SNAPSHOT_FILE));
        ContentRelease content;
        try {
            content = ContentReader.readWithoutCrossEntryChecks(name, release, snapshot);
        } catch (ContentFormatException e) {
            throw new LocalContentException(e.getMessage(), e);
        }
        String declaredProject = content.release().project();
        if (!declaredProject.equals(project)) {
            throw new LocalContentException(name + ": " + RELEASE_FILE + " project '" + declaredProject
                    + "' does not match directory '" + project + "'");
        }
        String version = content.release().version();
        if (!VERSION.matcher(version).matches()) {
            throw new LocalContentException(name + ": " + RELEASE_FILE + " version '" + version
                    + "' is not X.Y.Z or X.Y.Z-SNAPSHOT");
        }
        return content;
    }

    private static byte[] bytes(String name, Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new LocalContentException(name + ": cannot read " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /** Undeclared directories that hold at least one of the two metadata files. */
    private static List<String> ignored(Path root, Set<String> declared) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        try (Stream<Path> children = Files.list(root)) {
            children.filter(Files::isDirectory)
                    .map(child -> child.getFileName().toString())
                    .filter(child -> !declared.contains(child))
                    .filter(child -> Files.isRegularFile(root.resolve(child).resolve(RELEASE_FILE))
                            || Files.isRegularFile(root.resolve(child).resolve(SNAPSHOT_FILE)))
                    .sorted()
                    .forEach(child -> names.add(displayName(root, child)));
        } catch (IOException e) {
            throw new LocalContentException("cannot list " + root + ": " + e.getMessage(), e);
        }
        return List.copyOf(names);
    }

    private static String displayName(Path root, String project) {
        return root.getFileName() + "/" + project;
    }
}
