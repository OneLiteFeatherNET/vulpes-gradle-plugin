plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":vulpes-format"))
}

gradlePlugin {
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
