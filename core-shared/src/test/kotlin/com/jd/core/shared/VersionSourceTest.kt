package com.jd.core.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Chapter 153 (SIMP-NOV7.0-S1 / Version Nov8.10): Single Version Source verification test.
 *
 * Verifies that appVersionName and appVersionCode are centralized in `gradle.properties`
 * and referenced dynamically in both `app-monitor/build.gradle.kts` and `app-sensor/build.gradle.kts`,
 * preventing version code drift or duplicate hardcoded version declarations across build scripts.
 */
class VersionSourceTest {

    private val gradleProperties by lazy { File("../gradle.properties").readText(Charsets.UTF_8) }
    private val monitorBuildGradle by lazy { File("../app-monitor/build.gradle.kts").readText(Charsets.UTF_8) }
    private val sensorBuildGradle by lazy { File("../app-sensor/build.gradle.kts").readText(Charsets.UTF_8) }

    @Test
    fun gradlePropertiesContainsCentralizedVersionProperties() {
        assertTrue("appVersionName=" in gradleProperties, "gradle.properties must define appVersionName")
        assertTrue("appVersionCode=" in gradleProperties, "gradle.properties must define appVersionCode")

        val versionName = gradleProperties.lines().firstOrNull { it.startsWith("appVersionName=") }?.substringAfter("=")
        val versionCode = gradleProperties.lines().firstOrNull { it.startsWith("appVersionCode=") }?.substringAfter("=")

        // The format is checked, not a fixed value: a test pinned to one version had to be edited each release,
        // and Nov8.12 shipped without a version bump (ISSUE-NOV8.13-O53).
        assertTrue(versionName != null && Regex("""[A-Z][a-z]+\d+\.\d+(-[A-Za-z]\w*)?""").matches(versionName), "appVersionName must look like Nov8.13 or Nov8.100-Documentation, was $versionName")
        assertTrue((versionCode?.toIntOrNull() ?: 0) >= 66, "appVersionCode must be a number of at least 66 (Nov8.100-Documentation), was $versionCode")
    }

    @Test
    fun monitorBuildScriptUsesCentralizedVersionProperties() {
        assertTrue(
            "providers.gradleProperty(\"appVersionCode\")" in monitorBuildGradle,
            "app-monitor/build.gradle.kts must dynamically read appVersionCode from gradle.properties"
        )
        assertTrue(
            "providers.gradleProperty(\"appVersionName\")" in monitorBuildGradle,
            "app-monitor/build.gradle.kts must dynamically read appVersionName from gradle.properties"
        )
    }

    @Test
    fun sensorBuildScriptUsesCentralizedVersionProperties() {
        assertTrue(
            "providers.gradleProperty(\"appVersionCode\")" in sensorBuildGradle,
            "app-sensor/build.gradle.kts must dynamically read appVersionCode from gradle.properties"
        )
        assertTrue(
            "providers.gradleProperty(\"appVersionName\")" in sensorBuildGradle,
            "app-sensor/build.gradle.kts must dynamically read appVersionName from gradle.properties"
        )
    }
}
