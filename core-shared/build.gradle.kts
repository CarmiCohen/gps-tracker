plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

// TrackingDocumentIntegrityTest (ISSUE-NOV8.2-O46) and VersionSourceTest (SIMP-NOV7.0-S1) read these files, so a change to one must re-run the tests.
tasks.test {
    inputs.files(
        rootProject.file("DOCS/issues.md"),
        rootProject.file("STATUS/RESOLUTION_ARCHIVE.md"),
        rootProject.file("STATUS/SOT_MASTER_REQUIREMENTS.md"),
        rootProject.file("gradle.properties"),
        rootProject.file("app-monitor/build.gradle.kts"),
        rootProject.file("app-sensor/build.gradle.kts")
    ).withPropertyName("trackingDocuments").withPathSensitivity(PathSensitivity.RELATIVE)
}
