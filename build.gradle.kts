import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

val catalog = libs

subprojects {
    apply(plugin = "java")

    group = "net.onelitefeather.vulpes"
    version = "0.2.0" // x-release-please-version

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release = 17
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        add("testImplementation", platform(catalog.junit.bom))
        add("testImplementation", catalog.junit.jupiter)
        add("testRuntimeOnly", catalog.junit.platform.launcher)
    }
}

// The code generator libraries are published with the plugin: same version, same repositories, same POM metadata.
val publishedLibraries = setOf("vulpes-format", "vulpes-codegen")

subprojects {
    if (name !in publishedLibraries) {
        return@subprojects
    }
    val libraryName = name
    pluginManager.withPlugin("java-library") {
        apply(plugin = "maven-publish")
        // The test fixtures are not a library: they are kept out of the published module metadata.
        pluginManager.withPlugin("java-test-fixtures") {
            val java = components["java"] as AdhocComponentWithVariants
            java.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
            java.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
        }
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    pom {
                        name.set("Vulpes $libraryName")
                        description.set(
                            when (libraryName) {
                                "vulpes-format" -> "Reads Vulpes content releases: release.json and snapshot.json of a data JAR."
                                else -> "Generates Java sources from Vulpes content releases."
                            }
                        )
                        url.set("https://github.com/OneLiteFeatherNET/vulpes-gradle-plugin")
                        licenses {
                            license {
                                name.set("GNU Affero General Public License v3.0")
                                url.set("https://www.gnu.org/licenses/agpl-3.0.txt")
                            }
                        }
                        scm {
                            url.set("https://github.com/OneLiteFeatherNET/vulpes-gradle-plugin")
                            connection.set("scm:git:https://github.com/OneLiteFeatherNET/vulpes-gradle-plugin.git")
                            developerConnection.set("scm:git:ssh://git@github.com/OneLiteFeatherNET/vulpes-gradle-plugin.git")
                        }
                    }
                }
            }
            repositories {
                maven {
                    name = "OneLiteFeather"
                    credentials {
                        username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                        password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                    }
                    url = if (project.version.toString().endsWith("SNAPSHOT")) {
                        uri("https://repo.onelitefeather.dev/onelitefeather-snapshots")
                    } else {
                        uri("https://repo.onelitefeather.dev/onelitefeather-releases")
                    }
                }
            }
        }
    }
}
