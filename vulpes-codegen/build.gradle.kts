import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
}

dependencies {
    api(project(":vulpes-format"))
    implementation(libs.javapoet)
    implementation(libs.evalex)

    testImplementation(testFixtures(project(":vulpes-format")))
    testImplementation(libs.vulpes.base)
    testImplementation(libs.minestom)
    testImplementation(libs.adventure.key)
    testImplementation(platform(libs.jackson.bom))
    testImplementation(libs.jackson.databind)
    testImplementation(gradleTestKit())
}

// Library versions the generator was built with, read at runtime to pick the runtime dependencies of a template.
val versionsFile = tasks.register<WriteProperties>("writeVersions") {
    destinationFile = layout.buildDirectory.file("generated/vulpes/versions.properties")
    property("vulpes-base", libs.versions.vulpes.base.get())
    property("adventure-key", libs.versions.adventure.key.get())
}

tasks.named<ProcessResources>("processResources") {
    from(versionsFile) {
        into("net/onelitefeather/vulpes/codegen")
    }
}

// minestom is built for Java 25: the tests compile and run on JDK 25 against a Java 25 classpath.
tasks.named<JavaCompile>("compileTestJava") {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    options.release = 25
}

tasks.test {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    // Rewrites the golden files instead of comparing with them: -Pvulpes.updateGolden=true
    systemProperty("vulpes.updateGolden", providers.gradleProperty("vulpes.updateGolden").getOrElse("false"))
}
