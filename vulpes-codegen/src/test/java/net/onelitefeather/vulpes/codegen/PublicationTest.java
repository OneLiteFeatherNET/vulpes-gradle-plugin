package net.onelitefeather.vulpes.codegen;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Publishes the two code generator libraries into a file repository of the temporary directory and reads the POMs.
 * The build is run offline, so only the dependencies that the build already resolved are needed.
 */
final class PublicationTest {

    private static final String GROUP = "net.onelitefeather.vulpes";
    private static final Path REPOSITORY_ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void publishesVulpesCodegenUnderTheVersionOfThePlugin(@TempDir Path dir) throws IOException {
        Pom codegen = publish(dir).pom("vulpes-codegen");

        assertEquals(GROUP, codegen.groupId(), "groupId of vulpes-codegen");
        assertEquals("vulpes-codegen", codegen.artifactId(), "artifactId of vulpes-codegen");
        assertFalse(codegen.version().isBlank(), "vulpes-codegen must have a version");
        assertFalse("development".equals(codegen.version()), "vulpes-codegen must carry the plugin version");
    }

    @Test
    void publishesVulpesFormatUnderTheSameVersion(@TempDir Path dir) throws IOException {
        Repository repository = publish(dir);

        assertEquals(repository.pom("vulpes-codegen").version(), repository.pom("vulpes-format").version(),
                "vulpes-format and vulpes-codegen must share one version");
    }

    @Test
    void codegenDependsOnFormatWithCompileScope(@TempDir Path dir) throws IOException {
        Pom codegen = publish(dir).pom("vulpes-codegen");

        assertEquals("compile", codegen.scopeOf(GROUP, "vulpes-format"), "scope of vulpes-format in vulpes-codegen");
    }

    @Test
    void codegenListsJavaPoetAndEvalExAsRuntimeWithResolvedVersions(@TempDir Path dir) throws IOException {
        Pom codegen = publish(dir).pom("vulpes-codegen");

        assertEquals("runtime", codegen.scopeOf("com.palantir.javapoet", "javapoet"), "scope of javapoet");
        assertEquals("runtime", codegen.scopeOf("com.ezylang", "EvalEx"), "scope of EvalEx");
        assertFalse(codegen.versionOf("com.palantir.javapoet", "javapoet").isBlank(), "javapoet version");
        assertFalse(codegen.versionOf("com.ezylang", "EvalEx").isBlank(), "EvalEx version");
    }

    @Test
    void publishedPomsContainNoTestOrCompileOnlyDependencies(@TempDir Path dir) throws IOException {
        Repository repository = publish(dir);

        for (String artifactId : List.of("vulpes-format", "vulpes-codegen")) {
            for (PomDependency dependency : repository.pom(artifactId).dependencies()) {
                assertFalse(List.of("test", "provided").contains(dependency.scope()),
                        artifactId + " must not publish " + dependency + " with a test or provided scope");
                assertFalse(dependency.artifactId().startsWith("junit"), artifactId + " must not publish " + dependency);
                assertFalse(dependency.artifactId().equals("minestom")
                        || dependency.artifactId().equals("vulpes-base")
                        || dependency.artifactId().equals("adventure-key")
                        || dependency.artifactId().equals("jetbrains-annotations")
                        || dependency.artifactId().equals("annotations"),
                        artifactId + " must not publish test-only or compile-only " + dependency);
            }
        }
    }

    @Test
    void formatPublishesJacksonAsRuntimeWithResolvedVersion(@TempDir Path dir) throws IOException {
        Pom format = publish(dir).pom("vulpes-format");

        assertEquals("runtime", format.scopeOf("com.fasterxml.jackson.core", "jackson-databind"),
                "scope of jackson-databind");
        assertFalse(format.versionOf("com.fasterxml.jackson.core", "jackson-databind").isBlank(),
                "jackson-databind must carry its resolved version");
    }

    @Test
    void publicationHasNoTestFixturesArtifact(@TempDir Path dir) throws IOException {
        Repository repository = publish(dir);

        assertFalse(Files.exists(repository.artifactDirectory("vulpes-format").resolve("vulpes-format-"
                + repository.pom("vulpes-format").version() + "-test-fixtures.jar")),
                "test fixtures of vulpes-format must not be published");
        assertFalse(repository.moduleOf("vulpes-format").contains("test-fixtures"),
                "module metadata of vulpes-format must not reference test fixtures");
    }

    /** Publishes both libraries into a repository of the given directory and returns the repository. */
    private static Repository publish(Path dir) throws IOException {
        Path repository = Files.createDirectories(dir.resolve("repository"));
        Path init = dir.resolve("publish-to-test-repository.init.gradle.kts");
        Files.writeString(init, """
                allprojects {
                    plugins.withId("maven-publish") {
                        extensions.configure(org.gradle.api.publish.PublishingExtension::class.java) {
                            repositories {
                                maven {
                                    name = "TestRepository"
                                    url = uri("%s")
                                }
                            }
                        }
                    }
                }
                """.formatted(repository.toUri()), UTF_8);

        GradleRunner.create()
                .withProjectDir(REPOSITORY_ROOT.toFile())
                .withTestKitDir(gradleUserHome().toFile())
                .withArguments(
                        ":vulpes-format:publishAllPublicationsToTestRepository",
                        ":vulpes-codegen:publishAllPublicationsToTestRepository",
                        "--offline",
                        "--init-script", init.toString())
                .build();
        return new Repository(repository);
    }

    /** The Gradle user home of this build, whose caches hold the dependencies that an offline build needs. */
    private static Path gradleUserHome() {
        String configured = System.getenv("GRADLE_USER_HOME");
        return configured != null ? Path.of(configured) : Path.of(System.getProperty("user.home"), ".gradle");
    }

    /** A file repository written by a Maven publication. */
    private record Repository(Path root) {

        Path artifactDirectory(String artifactId) {
            Path artifacts = root.resolve(GROUP.replace('.', '/')).resolve(artifactId);
            try (Stream<Path> versions = Files.list(artifacts)) {
                List<Path> directories = versions.filter(Files::isDirectory).toList();
                assertEquals(1, directories.size(), "exactly one published version of " + artifactId);
                return directories.get(0);
            } catch (IOException e) {
                throw new UncheckedIOException("cannot list " + artifacts, e);
            }
        }

        Pom pom(String artifactId) throws IOException {
            Path directory = artifactDirectory(artifactId);
            String version = directory.getFileName().toString();
            Path file = directory.resolve(artifactId + "-" + version + ".pom");
            return Pom.parse(Files.readString(file, UTF_8));
        }

        String moduleOf(String artifactId) throws IOException {
            Path directory = artifactDirectory(artifactId);
            String version = directory.getFileName().toString();
            return Files.readString(directory.resolve(artifactId + "-" + version + ".module"), UTF_8);
        }
    }

    private record PomDependency(String groupId, String artifactId, String version, String scope) {

        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version + " (" + scope + ")";
        }
    }

    /** The coordinates and dependencies of one POM. */
    private record Pom(String groupId, String artifactId, String version, List<PomDependency> dependencies) {

        static Pom parse(String xml) {
            try {
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(xml)));
                Element project = document.getDocumentElement();
                List<PomDependency> dependencies = new ArrayList<>();
                NodeList nodes = project.getElementsByTagName("dependency");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element dependency = (Element) nodes.item(i);
                    dependencies.add(new PomDependency(text(dependency, "groupId"), text(dependency, "artifactId"),
                            text(dependency, "version"), text(dependency, "scope")));
                }
                return new Pom(text(project, "groupId"), text(project, "artifactId"), text(project, "version"),
                        List.copyOf(dependencies));
            } catch (Exception e) {
                throw new IllegalStateException("cannot parse POM:\n" + xml, e);
            }
        }

        String scopeOf(String group, String artifact) {
            return find(group, artifact).scope();
        }

        String versionOf(String group, String artifact) {
            return find(group, artifact).version();
        }

        private PomDependency find(String group, String artifact) {
            for (PomDependency dependency : dependencies) {
                if (dependency.groupId().equals(group) && dependency.artifactId().equals(artifact)) {
                    return dependency;
                }
            }
            throw new AssertionError("no dependency " + group + ":" + artifact + " in " + artifactId + " " + version
                    + ": " + dependencies);
        }

        private static String text(Element parent, String tag) {
            NodeList children = parent.getElementsByTagName(tag);
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i).getParentNode() == parent) {
                    return children.item(i).getTextContent().trim();
                }
            }
            return "";
        }
    }
}
