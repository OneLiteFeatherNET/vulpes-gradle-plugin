plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)

    compileOnly(libs.jetbrains.annotations)

    testFixturesImplementation(platform(libs.jackson.bom))
    testFixturesImplementation(libs.jackson.databind)

    testImplementation(libs.json.schema.validator)
}
