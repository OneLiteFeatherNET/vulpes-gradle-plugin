plugins {
    `java-library`
}

dependencies {
    api(project(":vulpes-format"))

    testImplementation(testFixtures(project(":vulpes-format")))
}
