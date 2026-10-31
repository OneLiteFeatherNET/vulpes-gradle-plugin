import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.shadow)
}

// Bundled into the plugin JAR by shadow, but never published: the POM and the module metadata do not list it.
val bundled = configurations.create("bundled") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations {
    compileOnly { extendsFrom(bundled) }
    testImplementation { extendsFrom(bundled) }
    runtimeClasspath { extendsFrom(bundled) }

    // The shadow JAR is the published artifact: it replaces the plain JAR in the java variants.
    listOf("apiElements", "runtimeElements").forEach { variant ->
        named(variant) {
            outgoing.artifacts.clear()
            outgoing.artifact(tasks.named("shadowJar"))
        }
    }
}

shadow.addShadowVariantIntoJavaComponent.set(false)

dependencies {
    bundled(project(":vulpes-codegen"))

    testImplementation(testFixtures(project(":vulpes-format")))
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

// Library versions the plugin was built and tested with, read at runtime to pick the runtime dependencies of a template.
val versionsFile = tasks.register<WriteProperties>("writeVersions") {
    destinationFile = layout.buildDirectory.file("generated/vulpes/versions.properties")
    property("vulpes-base", libs.versions.vulpes.base.get())
    property("adventure-key", libs.versions.adventure.key.get())
}

tasks.named<ProcessResources>("processResources") {
    from(versionsFile) {
        into("net/onelitefeather/vulpes/gradle")
    }
}

tasks.named<Jar>("jar") {
    archiveClassifier = "plain"
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier = ""
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()
    relocate("com.fasterxml.jackson", "net.onelitefeather.vulpes.gradle.internal.jackson")
    relocate("com.palantir.javapoet", "net.onelitefeather.vulpes.gradle.internal.javapoet")
    relocate("com.ezylang.evalex", "net.onelitefeather.vulpes.gradle.internal.evalex")
}

testing {
    suites {
        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter(libs.versions.junit.get())
            dependencies {
                implementation(testFixtures(project(":vulpes-format")))
                implementation(libs.adventure.key)
                implementation(platform(libs.jackson.bom))
                implementation(libs.jackson.databind)
                implementation(gradleTestKit())
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

// adventure-key is built for Java 21; the TestKit tests put a copy of it into their repositories.
tasks.named<JavaCompile>("compileFunctionalTestJava") {
    options.release = 21
}

gradlePlugin {
    testSourceSets(sourceSets["functionalTest"])
    plugins {
        create("vulpes") {
            id = "net.onelitefeather.vulpes"
            implementationClass = "net.onelitefeather.vulpes.gradle.VulpesPlugin"
            displayName = "Vulpes"
            description = "Generates Java code from Vulpes content releases"
        }
    }
}

publishing {
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
