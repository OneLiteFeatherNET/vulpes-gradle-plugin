package net.onelitefeather.vulpes.gradle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
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
 * Selects the local content directories of the declared content projects. A directory is merged over the data JAR of
 * its project, or, when that data JAR does not resolve, used alone as a local-only project. Uses no Gradle types.
 * The local files get the format checks; the cross-entry rules run on the merged result, or on a local-only snapshot.
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
     * A declared content project with a local directory. Its content is merged over its data JAR, or, when the data JAR
     * did not resolve, it is the content of a local-only project.
     *
     * @param project the content project name, equal to the directory name
     * @param directory the directory the files were read from
     * @param coordinate the declared coordinate {@code group:module:version}
     * @param mavenVersion the version of the declared coordinate: the version of the published data JAR the local
     *                     content is merged over, or the declared version of a local-only project
     * @param content the local release and snapshot, checked for format
     * @param unresolvedReason why the data JAR did not resolve, or {@code null} when it resolved and the content is merged
     * @param missingAssets references of a local-only project to resource locations without an asset; empty when the
     *                      content is merged, which checks the merged result instead
     */
    record Served(String project, Path directory, String coordinate, String mavenVersion, ContentRelease content,
                  String unresolvedReason, List<ContentMerge.MissingAsset> missingAssets) {

        /** Whether the local directory is the only content of the project. */
        boolean localOnly() {
            return unresolvedReason != null;
        }
    }

    /**
     * The outcome of a selection.
     *
     * @param served declared projects with a local directory, sorted by project name
     * @param ignored undeclared directories that hold content files, as {@code <root>/<name>}, sorted
     */
    record Resolution(List<Served> served, List<String> ignored) {
    }

    /**
     * Selects the local content.
     *
     * @param root the local content directory, {@code <projectDir>/<localContentDirectory>}
     * @param coordinates declared coordinate {@code group:module:version} by declared content project; its keys are the
     *                    declared projects
     * @param unresolved the reason by declared project whose data JAR did not resolve; a project absent here resolved
     * @param forbid whether a local override of a declared project is an error
     * @return the projects with a local directory and the ignored undeclared directories
     * @throws LocalContentException if a directory is partial, fails a check, is used while {@code forbid} is set, or an
     *                               unresolved project has no local directory
     */
    static Resolution resolve(Path root, Map<String, String> coordinates, Map<String, String> unresolved,
                              boolean forbid) {
        List<Served> served = new ArrayList<>();
        for (String project : new TreeSet<>(coordinates.keySet())) {
            Path directory = root.resolve(project);
            boolean hasRelease = Files.isRegularFile(directory.resolve(RELEASE_FILE));
            boolean hasSnapshot = Files.isRegularFile(directory.resolve(SNAPSHOT_FILE));
            String reason = unresolved.get(project);
            if (!hasRelease && !hasSnapshot) {
                if (reason != null) {
                    throw new LocalContentException(coordinates.get(project) + ": not resolved (" + reason
                            + "); no local directory " + displayName(root, project) + " holds " + RELEASE_FILE
                            + " and " + SNAPSHOT_FILE);
                }
                continue;
            }
            String name = displayName(root, project);
            if (!hasRelease) {
                throw new LocalContentException(name + ": missing " + RELEASE_FILE + "; local content is not merged and the build does not fall back to the data JAR");
            }
            if (!hasSnapshot) {
                throw new LocalContentException(name + ": missing " + SNAPSHOT_FILE + "; local content is not merged and the build does not fall back to the data JAR");
            }
            String coordinate = coordinates.get(project);
            ContentRelease content = read(name, project, directory);
            List<ContentMerge.MissingAsset> missingAssets = reason != null
                    ? ContentMerge.checkLocalOnly(project, content) : List.of();
            served.add(new Served(project, directory, coordinate, versionOf(coordinate), content, reason,
                    missingAssets));
        }
        if (forbid && !served.isEmpty()) {
            List<String> names = served.stream()
                    .map(s -> s.project() + " (" + displayName(root, s.project())
                            + (s.localOnly() ? ", local only: " + s.coordinate() + " not resolved ("
                            + s.unresolvedReason() + ")" : "") + ")")
                    .toList();
            throw new LocalContentException("vulpes.local.forbid=true: content from local directories: "
                    + String.join(", ", names) + "; remove the local directories or unset vulpes.local.forbid");
        }
        return new Resolution(served, ignored(root, coordinates.keySet()));
    }

    /**
     * Shortens the message of a failed resolution to one line for the notice. A missing module is {@code not found};
     * otherwise the deepest cause line ({@code > ...}) is used, or the first line when there is no cause.
     *
     * @param message the message of the resolution failure, may be {@code null}
     * @return the reason, without a final period
     */
    static String shortReason(String message) {
        if (message == null || message.isBlank()) {
            return "not resolved";
        }
        String[] lines = message.strip().split("\\R");
        if (lines[0].startsWith("Could not find ")) {
            return "not found";
        }
        String line = lines[0].strip();
        for (String candidate : lines) {
            String stripped = candidate.strip();
            if (stripped.startsWith("> ")) {
                line = stripped.substring(2).strip();
            }
        }
        return line.endsWith(".") ? line.substring(0, line.length() - 1) : line;
    }

    /**
     * Shortens a failed resolution to one line for the notice. Gradle reports the reason of an unresolved module in
     * the cause of a {@code Could not resolve ...} failure, so the whole cause chain is searched for a missing module
     * first and the deepest message is used otherwise.
     *
     * @param failure the failure of the resolution, may be {@code null}
     * @return the reason, without a final period
     */
    static String shortReason(Throwable failure) {
        String reason = null;
        Set<Throwable> visited = new HashSet<>();
        for (Throwable current = failure; current != null && visited.add(current); current = current.getCause()) {
            String message = current.getMessage();
            if (message == null || message.isBlank()) {
                continue;
            }
            if (message.strip().startsWith("Could not find ")) {
                return "not found";
            }
            reason = message;
        }
        return reason == null ? "not resolved" : shortReason(reason);
    }

    private static String versionOf(String coordinate) {
        return coordinate.substring(coordinate.lastIndexOf(':') + 1);
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
