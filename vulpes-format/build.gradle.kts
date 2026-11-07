plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    // A pinned version instead of the BOM: the POM must carry the resolved version and no BOM import.
    implementation("com.fasterxml.jackson.core:jackson-databind:${libs.versions.jackson.get()}")

    compileOnly(libs.jetbrains.annotations)

    testFixturesImplementation(platform(libs.jackson.bom))
    testFixturesImplementation(libs.jackson.databind)

    testImplementation(libs.json.schema.validator)
}
