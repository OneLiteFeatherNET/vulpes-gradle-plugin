plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)

    compileOnly(libs.jetbrains.annotations)

    testImplementation(libs.json.schema.validator)
}
