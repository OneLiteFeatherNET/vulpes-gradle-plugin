package net.onelitefeather.vulpes.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import net.onelitefeather.vulpes.format.fixtures.DataJarFixtures;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class VulpesPluginFunctionalTest {

    private static final String CYGNUS = "cygnus";
    private static final String VERSION = "1.4.0";
    private static final String GENERATED = "build/generated/sources/vulpes/java/main/";
    private static final String CYGNUS_SOURCE = GENERATED + "net/onelitefeather/vulpes/content/cygnus/CygnusRelease.java";
    private static final String CYGNUS_CLASS = "build/classes/java/main/net/onelitefeather/vulpes/content/cygnus/CygnusRelease.class";

    @Test
    void generatesReleaseClassSourceForDataJar(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.build("dependencies { vulpes(\"net.onelitefeather.vulpes.content:cygnus:1.4.0\") }");

        workspace.runner("compileJava").build();

        String source = Files.readString(workspace.file(CYGNUS_SOURCE), StandardCharsets.UTF_8);
        assertTrue(source.contains("public final class CygnusRelease {"), "generated class:\n" + source);
    }

    @Test
    void compilesGeneratedReleaseClass(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.build("dependencies { vulpes(\"net.onelitefeather.vulpes.content:cygnus:1.4.0\") }");

        BuildResult result = workspace.runner("compileJava").build();

        assertEquals(TaskOutcome.SUCCESS, outcome(result, ":compileJava"), "compileJava outcome");
        assertTrue(Files.isRegularFile(workspace.file(CYGNUS_CLASS)), "compiled CygnusRelease.class");
    }

    @Test
    void runsGenerationBeforeCompilation(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.build("dependencies { vulpes(\"net.onelitefeather.vulpes.content:cygnus:1.4.0\") }");

        BuildResult result = workspace.runner("compileJava").build();

        assertTrue(indexOf(result, ":vulpesGenerate") < indexOf(result, ":compileJava"),
                "vulpesGenerate must run before compileJava: " + tasks(result));
    }

    @Test
    void secondGenerationRunIsUpToDate(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.build("dependencies { vulpes(\"net.onelitefeather.vulpes.content:cygnus:1.4.0\") }");
        workspace.runner("vulpesGenerate").build();

        BuildResult result = workspace.runner("vulpesGenerate").build();

        assertEquals(TaskOutcome.UP_TO_DATE, outcome(result, ":vulpesGenerate"), "second run outcome");
    }

    @Test
    void secondRunWithConfigurationCacheReusesCacheEntry(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.build("dependencies { vulpes(\"net.onelitefeather.vulpes.content:cygnus:1.4.0\") }");
        workspace.runner("--configuration-cache", "vulpesGenerate").build();

        BuildResult result = workspace.runner("--configuration-cache", "vulpesGenerate").build();

        assertTrue(result.getOutput().contains("Configuration cache entry reused"),
                "second run must reuse the configuration cache:\n" + result.getOutput());
    }

    @Test
    void restoresOutputFromBuildCacheAfterClean(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.build("dependencies { vulpes(\"net.onelitefeather.vulpes.content:cygnus:1.4.0\") }");
        workspace.runner("--build-cache", "vulpesGenerate").build();
        workspace.runner("clean").build();

        BuildResult result = workspace.runner("--build-cache", "vulpesGenerate").build();

        assertEquals(TaskOutcome.FROM_CACHE, outcome(result, ":vulpesGenerate"), "outcome after clean");
    }

    @Test
    void failsForUnsupportedReleaseFormat(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.modifyRelease(DataJarFixtures.releaseJson(),
                release -> release.put("format", 2)));
        workspace.build("dependencies { vulpes(\"net.onelitefeather.vulpes.content:cygnus:1.4.0\") }");

        BuildResult result = workspace.runner("vulpesGenerate").buildAndFail();

        assertTrue(result.getOutput().contains("release.json: unsupported format 2, supported: 1"),
                "message must name release.json, the format 2 and the supported format 1:\n" + result.getOutput());
    }

    @Test
    void failsWhenReleaseProjectDiffersFromArtifactId(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.modifyRelease(DataJarFixtures.releaseJson(),
                release -> release.put("project", "manis")));
        workspace.build("dependencies { vulpes(\"net.onelitefeather.vulpes.content:cygnus:1.4.0\") }");

        BuildResult result = workspace.runner("vulpesGenerate").buildAndFail();

        assertTrue(result.getOutput().contains("release.json project 'manis' does not match artifact 'cygnus'"),
                "message must name both project values:\n" + result.getOutput());
    }

    @Test
    void failsForSeveralProjectsWithPackageName(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.publish("manis", VERSION, DataJarFixtures.modifyRelease(DataJarFixtures.releaseJson(),
                release -> release.put("project", "manis")));
        workspace.build("""
                dependencies {
                    vulpes("net.onelitefeather.vulpes.content:cygnus:1.4.0")
                    vulpes("net.onelitefeather.vulpes.content:manis:1.4.0")
                }

                vulpes {
                    packageName = "x.y"
                }
                """);

        BuildResult result = workspace.runner("vulpesGenerate").buildAndFail();

        assertTrue(result.getOutput().contains("vulpes.project(\"<name>\").packageName"),
                "message must name the per-project setting:\n" + result.getOutput());
    }

    @Test
    void generatesEachProjectIntoItsConfiguredPackage(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.publish("manis", VERSION, DataJarFixtures.modifyRelease(DataJarFixtures.releaseJson(),
                release -> release.put("project", "manis")));
        workspace.build("""
                dependencies {
                    vulpes("net.onelitefeather.vulpes.content:cygnus:1.4.0")
                    vulpes("net.onelitefeather.vulpes.content:manis:1.4.0")
                }

                vulpes {
                    project("cygnus") {
                        packageName = "a.cygnus"
                    }
                    project("manis") {
                        packageName = "a.manis"
                    }
                }
                """);

        workspace.runner("vulpesGenerate").build();

        assertTrue(Files.isRegularFile(workspace.file(GENERATED + "a/cygnus/CygnusRelease.java")),
                "cygnus in package a.cygnus");
        assertTrue(Files.isRegularFile(workspace.file(GENERATED + "a/manis/ManisRelease.java")),
                "manis in package a.manis");
    }

    @Test
    void generatesSingleProjectIntoPackageName(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.build("""
                dependencies {
                    vulpes("net.onelitefeather.vulpes.content:cygnus:1.4.0")
                }

                vulpes {
                    packageName = "net.onelitefeather.cygnus.content"
                }
                """);

        workspace.runner("vulpesGenerate").build();

        assertTrue(Files.isRegularFile(workspace.file(GENERATED + "net/onelitefeather/cygnus/content/CygnusRelease.java")),
                "CygnusRelease in package net.onelitefeather.cygnus.content");
    }

    @Test
    void removesConstantsOfPacksThatNoLongerExist(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.publish(CYGNUS, "1.5.0", DataJarFixtures.modifyRelease(DataJarFixtures.releaseJson(), release -> {
            release.put("version", "1.5.0");
            ((ArrayNode) release.get("packs")).remove(1);
        }));
        workspace.build("dependencies { vulpes(\"net.onelitefeather.vulpes.content:cygnus:1.4.0\") }");
        workspace.runner("vulpesGenerate").build();
        assertTrue(Files.readString(workspace.file(CYGNUS_SOURCE)).contains("PACK_32X"),
                "precondition: version 1.4.0 has pack 32x");

        workspace.build("dependencies { vulpes(\"net.onelitefeather.vulpes.content:cygnus:1.5.0\") }");
        workspace.runner("vulpesGenerate").build();

        assertFalse(Files.readString(workspace.file(CYGNUS_SOURCE)).contains("PACK_32X"),
                "version 1.5.0 has no pack 32x, so no constant for it may remain");
    }

    @Test
    void failsWithoutJavaPlugin(@TempDir Path dir) throws IOException {
        Workspace workspace = new Workspace(dir);
        workspace.publish(CYGNUS, VERSION, DataJarFixtures.releaseJson());
        workspace.write("""
                plugins {
                    id("net.onelitefeather.vulpes")
                }

                repositories {
                    maven {
                        url = uri("%s")
                    }
                }

                dependencies {
                    vulpes("net.onelitefeather.vulpes.content:cygnus:1.4.0")
                }
                """.formatted(workspace.repositoryUri()));

        BuildResult result = workspace.runner("vulpesGenerate").buildAndFail();

        assertTrue(result.getOutput().contains("requires the 'java' plugin"),
                "message must name the missing java plugin:\n" + result.getOutput());
    }

    private static TaskOutcome outcome(BuildResult result, String path) {
        BuildTask task = result.task(path);
        assertNotNull(task, path + " must have run: " + tasks(result));
        return task.getOutcome();
    }

    private static int indexOf(BuildResult result, String path) {
        List<BuildTask> tasks = result.getTasks();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getPath().equals(path)) {
                return i;
            }
        }
        throw new AssertionError(path + " did not run: " + tasks(result));
    }

    private static String tasks(BuildResult result) {
        return result.getTasks().stream().map(task -> task.getPath() + "=" + task.getOutcome())
                .toList().toString();
    }

    /**
     * A build with a settings file, a build cache and a file repository, all inside one temporary directory.
     */
    private static final class Workspace {

        private final Path project;
        private final Path repository;
        private final Path buildCache;

        Workspace(Path root) throws IOException {
            this.project = Files.createDirectories(root.resolve("project"));
            this.repository = Files.createDirectories(root.resolve("repository"));
            this.buildCache = root.resolve("build-cache");
            Files.writeString(project.resolve("settings.gradle.kts"), """
                    rootProject.name = "game"

                    buildCache {
                        local {
                            directory = file("%s")
                        }
                    }
                    """.formatted(buildCache.toString().replace("\\", "\\\\")), StandardCharsets.UTF_8);
        }

        void publish(String artifactId, String version, byte[] releaseJson) {
            DataJarFixtures.writeMavenModule(repository, artifactId, version, releaseJson,
                    DataJarFixtures.snapshotJson());
        }

        String repositoryUri() {
            return repository.toUri().toString();
        }

        /**
         * Writes the build script with the java and vulpes plugins and the file repository, followed by the body.
         */
        void build(String body) throws IOException {
            write("""
                    plugins {
                        java
                        id("net.onelitefeather.vulpes")
                    }

                    repositories {
                        maven {
                            url = uri("%s")
                        }
                    }

                    %s
                    """.formatted(repositoryUri(), body));
        }

        void write(String buildScript) throws IOException {
            Files.writeString(project.resolve("build.gradle.kts"), buildScript, StandardCharsets.UTF_8);
        }

        Path file(String relative) {
            return project.resolve(relative);
        }

        GradleRunner runner(String... arguments) {
            List<String> all = Arrays.asList(arguments);
            return GradleRunner.create()
                    .withProjectDir(project.toFile())
                    .withArguments(all)
                    .withPluginClasspath();
        }
    }
}
