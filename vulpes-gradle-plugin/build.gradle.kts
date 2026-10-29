plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":vulpes-format"))

    testImplementation(testFixtures(project(":vulpes-format")))
}

testing {
    suites {
        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter(libs.versions.junit.get())
            dependencies {
                implementation(testFixtures(project(":vulpes-format")))
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
