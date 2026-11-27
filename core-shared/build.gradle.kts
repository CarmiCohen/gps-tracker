plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("ru.vyarus.animalsniffer") version "2.0.1"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // SIMP-NOV6.0-S1: the Android API of the apps' minSdk (26), see the animalsniffer block below.
    signature("net.sf.androidscents.signature:android-api-level-26:8.0.0_r2@signature")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

// SIMP-NOV6.0-S1 (ISSUE-NOV6.0-O25): this module compiles against the JDK, and lint runs no API check on a JVM
// module, so a JDK call that Android 26 lacks (O25: MappedByteBuffer.force(int, int), position(int)) built cleanly.
// animalsnifferMain checks the compiled main classes against the API 26 signature and fails on any such call.
// The jar (used by every app build) and the unit tests depend on it. AndroidMinSdkSignatureTest keeps the
// signature's level equal to the minSdk of every module using this one.
animalsniffer {
    sourceSets = listOf(project.sourceSets.main.get())
}
val apiCheck = tasks.named("animalsnifferMain")
tasks.jar { dependsOn(apiCheck) }

// TrackingDocumentIntegrityTest (ISSUE-NOV8.2-O46), VersionSourceTest (SIMP-NOV7.0-S1), BaselineIndexTest
// (ISSUE-NOV8.5-O47) and AndroidMinSdkSignatureTest (SIMP-NOV6.0-S1) read these files and the git tags, so a change
// to one, or a moved tag, must re-run the tests.
tasks.test {
    dependsOn(apiCheck)
    inputs.files(
        rootProject.file("DOCS/issues.md"),
        rootProject.file("STATUS/RESOLUTION_ARCHIVE.md"),
        rootProject.file("STATUS/SOT_MASTER_REQUIREMENTS.md"),
        rootProject.file("STATUS/BASELINE/INDEX.md"),
        rootProject.file("DOCS/issue_details.md"),
        rootProject.file("DOCS/operational_notes.md"),
        rootProject.file(".git/packed-refs"),
        rootProject.fileTree(".git/refs/tags"),
        rootProject.file("gradle.properties"),
        rootProject.file("app-monitor/build.gradle.kts"),
        rootProject.file("app-sensor/build.gradle.kts"),
        rootProject.file("core-network/build.gradle.kts"),
        rootProject.file("core-ui/build.gradle.kts"),
        rootProject.file("core-shared/build.gradle.kts")
    ).withPropertyName("trackingDocuments").withPathSensitivity(PathSensitivity.RELATIVE)
}
