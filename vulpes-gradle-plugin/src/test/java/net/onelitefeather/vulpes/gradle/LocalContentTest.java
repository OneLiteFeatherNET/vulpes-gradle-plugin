package net.onelitefeather.vulpes.gradle;

import static net.onelitefeather.vulpes.format.fixtures.DataJarFixtures.releaseJson;
import static net.onelitefeather.vulpes.format.fixtures.DataJarFixtures.snapshotJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.onelitefeather.vulpes.format.Release;
import net.onelitefeather.vulpes.format.fixtures.DataJarFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LocalContentTest {

    private static final String CYGNUS = "cygnus";
    private static final String MAVEN_VERSION = "1.4.0";
    private static final String COORDINATE = "net.onelitefeather.vulpes.content:cygnus:" + MAVEN_VERSION;
    private static final String SNAPSHOT_COORDINATE = "net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT";
    private static final Map<String, String> DECLARED_CYGNUS = Map.of(CYGNUS, COORDINATE);
    private static final Map<String, String> SNAPSHOT_DECLARED = Map.of(CYGNUS, SNAPSHOT_COORDINATE);
    private static final Map<String, String> NO_REASONS = Map.of();
    private static final Map<String, String> NOT_FOUND = Map.of(CYGNUS, "not found");

    @Test
    void selectsDirectoryWithBothFiles(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, releaseJson(), snapshotJson());

        LocalContent.Resolution resolution = LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false);

        assertEquals(List.of(CYGNUS), servedProjects(resolution), "served projects");
        assertEquals(MAVEN_VERSION, resolution.served().get(0).mavenVersion(), "Maven version it replaces");
    }

    @Test
    void keepsDataJarWhenDirectoryIsAbsent(@TempDir Path dir) {
        LocalContent.Resolution resolution = LocalContent.resolve(root(dir), DECLARED_CYGNUS, NO_REASONS, false);

        assertTrue(resolution.served().isEmpty(), "no local directory, so nothing is served locally");
        assertTrue(resolution.ignored().isEmpty(), "no undeclared directory, so nothing is ignored");
    }

    @Test
    void keepsDataJarWhenDirectoryHoldsNoMetadataFile(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        Files.createDirectories(root.resolve(CYGNUS));
        Files.writeString(root.resolve(CYGNUS).resolve("notes.txt"), "not content");

        LocalContent.Resolution resolution = LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false);

        assertTrue(resolution.served().isEmpty(), "a directory without release.json or snapshot.json is not content");
    }

    @Test
    void failsNamingMissingSnapshot(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        Files.createDirectories(root.resolve(CYGNUS));
        Files.write(root.resolve(CYGNUS).resolve("release.json"), releaseJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false));

        assertTrue(e.getMessage().contains("vulpes/cygnus: missing snapshot.json"),
                "message must name the directory and the missing file:\n" + e.getMessage());
    }

    @Test
    void failsNamingMissingRelease(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        Files.createDirectories(root.resolve(CYGNUS));
        Files.write(root.resolve(CYGNUS).resolve("snapshot.json"), snapshotJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false));

        assertTrue(e.getMessage().contains("vulpes/cygnus: missing release.json"),
                "message must name the directory and the missing file:\n" + e.getMessage());
    }

    @Test
    void failsWhenReleaseProjectDiffersFromDirectory(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, DataJarFixtures.modifyRelease(releaseJson(), release -> release.put("project", "manis")),
                snapshotJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false));

        assertTrue(e.getMessage().contains("vulpes/cygnus") && e.getMessage().contains("'manis'")
                        && e.getMessage().contains("'cygnus'"),
                "message must name the directory and both project values:\n" + e.getMessage());
    }

    @Test
    void failsForUnsupportedReleaseFormatNamingSupportedFormats(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, DataJarFixtures.modifyRelease(releaseJson(), release -> release.put("format", 3)),
                snapshotJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false));

        assertTrue(e.getMessage().contains("vulpes/cygnus") && e.getMessage().contains("supported: 1, 2"),
                "message must name the directory and the supported formats:\n" + e.getMessage());
    }

    @Test
    void failsWhenSnapshotDoesNotMatchRevision(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, releaseJson(), DataJarFixtures.modifySnapshot(releaseJson(), snapshotJson(),
                snapshot -> snapshot.put("namespace", "manis")).snapshotJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false));

        assertTrue(e.getMessage().contains("revision does not match"),
                "the reader's revision check must apply to local files:\n" + e.getMessage());
    }

    @Test
    void acceptsSnapshotVersion(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, DataJarFixtures.modifyRelease(releaseJson(), release -> release.put("version", "1.4.1-SNAPSHOT")),
                snapshotJson());

        LocalContent.Resolution resolution = LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false);

        assertEquals("1.4.1-SNAPSHOT", resolution.served().get(0).content().release().version(), "local version");
    }

    @Test
    void failsForVersionThatIsNotMavenForm(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, DataJarFixtures.modifyRelease(releaseJson(), release -> release.put("version", "1.4.1-dev.3")),
                snapshotJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false));

        assertTrue(e.getMessage().contains("vulpes/cygnus") && e.getMessage().contains("1.4.1-dev.3"),
                "message must name the directory and the version:\n" + e.getMessage());
    }

    @Test
    void readsBaseOfFormatTwo(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, DataJarFixtures.modifyRelease(releaseJson(), release -> {
            release.put("format", 2);
            release.putObject("base").put("repository", "https://github.com/OneLiteFeatherNET/vulpes-base")
                    .put("branch", "main").put("commit", "0123456789abcdef0123456789abcdef01234567");
        }), snapshotJson());

        Release.Base base = LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false).served().get(0).content().release().base();

        assertEquals("main", base.branch(), "base branch of the local release");
    }

    @Test
    void ignoresUndeclaredDirectoryWithWarning(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, "manis", DataJarFixtures.modifyRelease(releaseJson(), release -> release.put("project", "manis")),
                snapshotJson());

        LocalContent.Resolution resolution = LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false);

        assertTrue(resolution.served().isEmpty(), "an undeclared directory adds no content");
        assertEquals(List.of("vulpes/manis"), resolution.ignored(), "undeclared directory reported as ignored");
    }

    @Test
    void undeclaredDirectoryDoesNotFailUnderForbid(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, "manis", DataJarFixtures.modifyRelease(releaseJson(), release -> release.put("project", "manis")),
                snapshotJson());

        LocalContent.Resolution resolution = LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, true);

        assertEquals(List.of("vulpes/manis"), resolution.ignored(), "warned, not failed, under forbid");
    }

    @Test
    void forbidFailsNamingServedProject(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, releaseJson(), snapshotJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, true));

        assertTrue(e.getMessage().contains("cygnus") && e.getMessage().contains("vulpes/cygnus"),
                "message must name the served project and its directory:\n" + e.getMessage());
    }

    @Test
    void forbidSucceedsWithoutLocalContent(@TempDir Path dir) {
        LocalContent.Resolution resolution = LocalContent.resolve(root(dir), DECLARED_CYGNUS, NO_REASONS, true);

        assertTrue(resolution.served().isEmpty(), "nothing is served, so forbid has nothing to reject");
    }

    @Test
    void mergedProjectHasNoUnresolvedReason(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, releaseJson(), snapshotJson());

        LocalContent.Served served = LocalContent.resolve(root, DECLARED_CYGNUS, NO_REASONS, false).served().get(0);

        assertNull(served.unresolvedReason(), "a resolved declared project is merged, not local-only");
    }

    @Test
    void selectsUnresolvedProjectWithBothFilesAsLocalOnly(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, releaseJson(), snapshotJson());

        LocalContent.Served served = LocalContent.resolve(root, SNAPSHOT_DECLARED, NOT_FOUND, false).served().get(0);

        assertEquals("not found", served.unresolvedReason(), "the unresolved declared project is local-only");
    }

    @Test
    void localOnlyProjectKeepsDeclaredVersion(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, releaseJson(), snapshotJson());

        LocalContent.Served served = LocalContent.resolve(root, SNAPSHOT_DECLARED, NOT_FOUND, false).served().get(0);

        assertEquals("0.1.0-SNAPSHOT", served.mavenVersion(), "the declared version of the local-only project");
    }

    @Test
    void failsNamingUnresolvedProjectWithoutDirectory(@TempDir Path dir) {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root(dir), SNAPSHOT_DECLARED, NOT_FOUND, false));

        assertTrue(e.getMessage().contains(SNAPSHOT_COORDINATE) && e.getMessage().contains("not found")
                        && e.getMessage().contains("vulpes/cygnus"),
                "message must name the coordinate, the reason and the expected directory:\n" + e.getMessage());
    }

    @Test
    void failsNamingPartialDirectoryOfUnresolvedProject(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        Files.createDirectories(root.resolve(CYGNUS));
        Files.write(root.resolve(CYGNUS).resolve("release.json"), releaseJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, SNAPSHOT_DECLARED, NOT_FOUND, false));

        assertTrue(e.getMessage().contains("vulpes/cygnus: missing snapshot.json"),
                "a partial directory fails for a local-only project too:\n" + e.getMessage());
    }

    @Test
    void failsForUnsupportedFormatInLocalOnlyProject(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, DataJarFixtures.modifyRelease(releaseJson(), release -> release.put("format", 3)),
                snapshotJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, SNAPSHOT_DECLARED, NOT_FOUND, false));

        assertTrue(e.getMessage().contains("vulpes/cygnus") && e.getMessage().contains("supported: 1, 2"),
                "a format error fails a local-only project and names the directory:\n" + e.getMessage());
    }

    @Test
    void failsForVersionThatIsNotMavenFormInLocalOnlyProject(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, DataJarFixtures.modifyRelease(releaseJson(), release -> release.put("version", "1.4.1-dev.3")),
                snapshotJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, SNAPSHOT_DECLARED, NOT_FOUND, false));

        assertTrue(e.getMessage().contains("vulpes/cygnus") && e.getMessage().contains("1.4.1-dev.3"),
                "a local-only project with a version that is not Maven form fails:\n" + e.getMessage());
    }

    @Test
    void forbidNamesLocalOnlyProjectWithCoordinateAndReason(@TempDir Path dir) throws IOException {
        Path root = root(dir);
        writeLocal(root, CYGNUS, releaseJson(), snapshotJson());

        LocalContentException e = assertThrows(LocalContentException.class,
                () -> LocalContent.resolve(root, SNAPSHOT_DECLARED, NOT_FOUND, true));

        assertTrue(e.getMessage().contains("cygnus") && e.getMessage().contains(SNAPSHOT_COORDINATE)
                        && e.getMessage().contains("not found"),
                "forbid must name the local-only project, its coordinate and the reason:\n" + e.getMessage());
    }

    @Test
    void shortensNotFoundFailureToNotFound() {
        String message = "Could not find net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT.\n"
                + "Searched in the following locations:\n  file:/repository/";

        assertEquals("not found", LocalContent.shortReason(message), "a missing module is reported as not found");
    }

    @Test
    void shortensAuthenticationFailureToDeepestCause() {
        String message = "Could not resolve net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT.\n"
                + "  Required by:\n      project :\n"
                + "   > Could not get resource 'https://repo.example/cygnus.pom'.\n"
                + "      > Authentication failed for 'https://repo.example/'.";

        assertEquals("Authentication failed for 'https://repo.example/'", LocalContent.shortReason(message),
                "the cause of an authentication failure is the reason, on one line");
    }

    @Test
    void shortensOfflineFailureToItsOnlyLine() {
        String message = "No cached version of net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT available for offline mode.";

        assertEquals("No cached version of net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT available for offline mode",
                LocalContent.shortReason(message), "a single-line failure keeps its line without the final period");
    }

    @Test
    void blankFailureMessageIsNotResolved() {
        assertEquals("not resolved", LocalContent.shortReason((String) null), "a failure without a message");
    }

    @Test
    void shortensNotFoundCauseToNotFound() {
        Throwable failure = new RuntimeException("Could not resolve g:a:1.",
                new RuntimeException("Could not find g:a:1.\nSearched in the following locations:\n  https://repo/"));

        assertEquals("not found", LocalContent.shortReason(failure),
                "a missing module named by the cause is reported as not found");
    }

    @Test
    void shortensAuthenticationCauseToItsLine() {
        Throwable failure = new RuntimeException("Could not resolve g:a:1.",
                new RuntimeException("Could not GET 'https://repo/x'. Received status code 401 from server: Unauthorized"));

        assertEquals("Could not GET 'https://repo/x'. Received status code 401 from server: Unauthorized",
                LocalContent.shortReason(failure),
                "the cause of an authentication failure is the reason, not the resolve line");
    }

    @Test
    void throwableWithoutCauseIsShortenedLikeItsMessage() {
        String message = "Could not resolve g:a:1.";

        assertEquals(LocalContent.shortReason(message), LocalContent.shortReason(new RuntimeException(message)),
                "a failure without a cause keeps the reason of its message");
    }

    @Test
    void nullThrowableIsNotResolved() {
        assertEquals("not resolved", LocalContent.shortReason((Throwable) null), "a missing failure");
    }

    @Test
    void causeCycleEndsAtTheFailureMessage() {
        Throwable failure = new RuntimeException("Could not resolve g:a:1.") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        assertEquals("Could not resolve g:a:1", LocalContent.shortReason(failure),
                "a cause that points to itself must not loop");
    }

    private static Path root(Path dir) {
        try {
            return Files.createDirectories(dir.resolve("vulpes"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeLocal(Path root, String project, byte[] release, byte[] snapshot) throws IOException {
        Path directory = Files.createDirectories(root.resolve(project));
        Files.write(directory.resolve("release.json"), release);
        Files.write(directory.resolve("snapshot.json"), snapshot);
    }

    private static List<String> servedProjects(LocalContent.Resolution resolution) {
        return resolution.served().stream().map(LocalContent.Served::project).toList();
    }
}
