val catalog = libs

subprojects {
    apply(plugin = "java")

    group = "net.onelitefeather.vulpes"
    version = "0.0.0" // x-release-please-version

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
