package com.jd.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifies Chapter 129.5: the Sensor never sounds or vibrates (Rule 14, R872). Only the Monitor may use these
 * classes. Gradle does not track the scanned files as inputs of this test, so run it with --rerun.
 */
class SensorStealthGuardTest {

    private val soundOrVibration = Regex("\\b(AudioTrack|MediaPlayer|ToneGenerator|SoundPool|Ringtone|RingtoneManager|Vibrator|VibratorManager|VibrationEffect|SirenPlayer)\\b")

    private fun sources(module: String) =
        File("../$module/src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.toList()

    @Test
    fun theSensorSourcesAreScanned() {
        assertTrue(sources("app-sensor").isNotEmpty())
    }

    @Test
    fun patternFindsTheMonitorSiren() {
        val monitorHits = sources("app-monitor").filter { file -> file.readLines().any(soundOrVibration::containsMatchIn) }

        assertTrue("The pattern must find the Monitor siren", monitorHits.any { it.name == "SirenPlayer.kt" })
    }

    @Test
    fun noSensorCodePlaysSoundOrVibrates() {
        val found = (sources("app-sensor") + sources("core-shared") + sources("core-network")).flatMap { file ->
            file.readLines().filter(soundOrVibration::containsMatchIn).map { "${file.name}: ${it.trim()}" }
        }

        assertEquals(emptyList<String>(), found)
    }
}
