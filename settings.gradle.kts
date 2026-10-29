pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "vulpes-gradle-plugin-build"

include("vulpes-format", "vulpes-codegen", "vulpes-gradle-plugin")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "OneLiteFeatherRepository"
            url = uri("https://repo.onelitefeather.dev/onelitefeather")
            if (System.getenv("CI") != null) {
                credentials {
                    username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                    password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                }
            } else {
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
            content { includeGroupByRegex("net\\.onelitefeather(\\..*)?") }
        }
    }

    versionCatalogs {
        create("libs") {
            version("adventure-key", "5.2.0")
            version("jackson", "2.22.4")
            version("javapoet", "0.22.0")
            version("minestom", "2026.10.07-26.2")
            version("jetbrains-annotations", "26.1.0")
            version("junit", "6.1.3")
            version("json-schema-validator", "3.0.8")
            version("shadow", "9.6.1")
            version("vulpes-base", "0.0.0")

            library("adventure.key", "net.kyori", "adventure-key").versionRef("adventure-key")
            library("javapoet", "com.palantir.javapoet", "javapoet").versionRef("javapoet")
            library("minestom", "net.minestom", "minestom").versionRef("minestom")
            library("vulpes.base", "net.onelitefeather.vulpes", "vulpes-base").versionRef("vulpes-base")
            library("jackson.bom", "com.fasterxml.jackson", "jackson-bom").versionRef("jackson")
            library("jackson.databind", "com.fasterxml.jackson.core", "jackson-databind").withoutVersion()
            library("junit.bom", "org.junit", "junit-bom").versionRef("junit")
            library("junit.jupiter", "org.junit.jupiter", "junit-jupiter").withoutVersion()
            library("junit.platform.launcher", "org.junit.platform", "junit-platform-launcher").withoutVersion()
            library("jetbrains.annotations", "org.jetbrains", "annotations").versionRef("jetbrains-annotations")
            library("json.schema.validator", "com.networknt", "json-schema-validator").versionRef("json-schema-validator")

            plugin("shadow", "com.gradleup.shadow").versionRef("shadow")
        }
    }
}
