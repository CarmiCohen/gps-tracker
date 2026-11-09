package com.jd.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Chapter 161 (ISSUE-NOV5.2-O11 Group 4): only the Sensor listens. The Monitor never opens a microphone and does not
 * declare one (user decision, 2026-11-08; `DOCS/03_MONITOR_REQUIREMENTS.md`). Gradle does not track the scanned files
 * as inputs of this test, so run it with --rerun.
 */
class MonitorMicrophoneGuardTest {

    private val microphoneUse = Regex("\\b(AudioRecord|MediaRecorder|SpeechRecognizer|RECORD_AUDIO|FOREGROUND_SERVICE_MICROPHONE|FOREGROUND_SERVICE_TYPE_MICROPHONE)\\b")

    private fun files(module: String) =
        File("../$module/src/main").walkTopDown().filter { it.extension == "kt" || it.name == "AndroidManifest.xml" }.toList()

    private fun hits(module: String) = files(module).flatMap { file ->
        file.readLines().filter(microphoneUse::containsMatchIn).map { "${file.name}: ${it.trim()}" }
    }

    @Test
    fun theMonitorNeverUsesAMicrophone() {
        assertTrue(files("app-monitor").isNotEmpty())
        assertEquals(emptyList<String>(), hits("app-monitor"))
    }

    @Test
    fun theSensorDeclaresAndUsesItsMicrophone() {
        val sensorHits = hits("app-sensor")
        assertTrue("The pattern must find the Sensor's RECORD_AUDIO", sensorHits.any { it.startsWith("AndroidManifest.xml") && "RECORD_AUDIO" in it })
        assertTrue("The pattern must find the Sensor's AudioRecord", sensorHits.any { it.startsWith("AcousticMonitor.kt") && "AudioRecord" in it })
        val manifest = File("../app-sensor/src/main/AndroidManifest.xml").readText()
        assertTrue("SensorService must declare the microphone service type", Regex("foregroundServiceType=\"[^\"]*microphone").containsMatchIn(manifest))
    }
}
