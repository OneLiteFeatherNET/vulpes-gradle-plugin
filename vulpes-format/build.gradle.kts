plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)

    testImplementation(libs.json.schema.validator)
}
