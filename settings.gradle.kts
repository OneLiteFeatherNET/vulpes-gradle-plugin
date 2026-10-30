pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "vulpes-gradle-plugin-build"

include("vulpes-format", "vulpes-gradle-plugin")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            version("jackson", "2.22.4")
            version("jetbrains-annotations", "26.1.0")
            version("junit", "6.1.3")
            version("json-schema-validator", "3.0.8")
            version("shadow", "9.6.1")

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
