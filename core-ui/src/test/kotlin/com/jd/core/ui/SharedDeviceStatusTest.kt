package com.jd.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * SIMP-NOV6.1-S1: the internet and location-provider checks live once, in core-ui's `PhoneStateReader.kt`; no app
 * keeps its own copy. Gradle does not track the scanned files as inputs of this test, so run it with --rerun.
 */
class SharedDeviceStatusTest {

    private val ownCopy = Regex("\\bfun\\s+(isNetworkAvailable|isLocationProviderEnabled)\\b|NET_CAPABILITY_INTERNET")

    private fun sources(module: String) =
        File("../$module/src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.toList()

    private fun hits(files: List<File>) = files.flatMap { file ->
        file.readLines().filter(ownCopy::containsMatchIn).map { "${file.name}: ${it.trim()}" }
    }

    @Test
    fun theAppSourcesAreScanned() {
        assertTrue(sources("app-sensor").isNotEmpty())
        assertTrue(sources("app-monitor").isNotEmpty())
    }

    @Test
    fun patternFindsTheSharedChecks() {
        val shared = hits(sources("core-ui")).filter { it.startsWith("PhoneStateReader.kt") }

        assertEquals("Both shared functions and the one internet check", 3, shared.size)
    }

    @Test
    fun noAppKeepsItsOwnCopy() {
        assertEquals(emptyList<String>(), hits(sources("app-sensor") + sources("app-monitor")))
    }
}
