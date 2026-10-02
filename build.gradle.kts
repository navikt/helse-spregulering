plugins {
    alias(libs.plugins.sykepenger.deployable)
}

sykepengerDeployable {
    mainClass = "MainKt"
}

dependencies {
    implementation(libs.rapidsAndRivers)
    api(libs.flyway.database.postgresql)
    implementation(libs.hikariCP)
    implementation(libs.postgresql)
    implementation(libs.kotliquery)

    testImplementation(libs.tbdLibs.postgresTestdatabaser)
    testImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testImplementation(libs.mockk)
}
