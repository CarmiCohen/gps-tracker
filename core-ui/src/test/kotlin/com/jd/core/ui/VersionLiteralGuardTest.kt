package com.jd.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifies Chapter 126.1: no production code writes a release version into a string (ISSUE-NOV5.0-O6).
 * Comments may still name the version that changed them. Gradle does not track these files as
 * inputs of this test, so run it with --rerun.
 */
class VersionLiteralGuardTest {

    private val modules = listOf("app-monitor", "app-sensor", "core-network", "core-shared", "core-ui")
    private val versionInString = Regex("\"[^\"]*(Nov|Revol)\\d+\\.\\d+[^\"]*\"")

    private fun sources(module: String) =
        File("../$module/src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.toList()

    @Test
    fun everyModuleIsScanned() {
        modules.forEach { assertTrue("No sources found in $it", sources(it).isNotEmpty()) }
    }

    @Test
    fun patternFindsAVersionInAStringAndNotInAComment() {
        assertTrue(versionInString.containsMatchIn("""log("Version: Nov1.7 (Final)")"""))
        assertTrue(versionInString.containsMatchIn("""versionName ?: "Nov4.27""""))
        assertTrue(!versionInString.containsMatchIn(" * Updated Nov1.7: Fixed a regression."))
    }

    @Test
    fun noProductionStringHoldsAVersion() {
        val found = modules.flatMap(::sources).flatMap { file ->
            file.readLines().filter(versionInString::containsMatchIn).map { "${file.name}: ${it.trim()}" }
        }

        assertEquals(emptyList<String>(), found)
    }
}
