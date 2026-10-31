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
