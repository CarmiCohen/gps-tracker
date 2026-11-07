package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies Chapter 129.4: the siren follows the Siren & Audio settings, and each wave type has its own sound. */
class SirenSoundTest {

    @Test
    fun defaultsPlayAtForcedMaximumOnTheAlarmStreamWithVibration() {
        val sound = AlertSettings().sirenSound("Siren")

        assertEquals(1f, sound.gain, 0f)
        assertTrue(sound.useAlarmStream)
        assertTrue(sound.forceMaxVolume)
        assertTrue(sound.vibrate)
    }

    @Test
    fun customVolumeAppliesOnlyWithoutForcedMaximum() {
        val custom = AlertSettings(useMaxVolume = false, useCustomVolume = true, alarmVolume = 0.3f)

        assertEquals(0.3f, custom.sirenSound("Siren").gain, 0.0001f)
        assertEquals(1f, custom.copy(useMaxVolume = true).sirenSound("Siren").gain, 0f)
        assertEquals(1f, custom.copy(useCustomVolume = false).sirenSound("Siren").gain, 0f)
    }

    @Test
    fun withoutOverridingSilenceTheSirenFollowsTheNotificationStream() {
        val quiet = AlertSettings(overrideSilence = false, useMaxVolume = false, vibrationEnabled = false)
        val sound = quiet.sirenSound("Pulse")

        assertFalse(sound.useAlarmStream)
        assertFalse(sound.forceMaxVolume)
        assertFalse(sound.vibrate)
        assertEquals("Pulse", sound.waveType)
    }

    @Test
    fun eachWaveTypeHasItsOwnLoop() {
        val siren = sirenWave("Siren")
        val chimes = sirenWave("Chimes")
        val pulse = sirenWave("Pulse")

        assertEquals(44_100, siren.size)
        assertEquals(17_640, chimes.size)
        assertEquals(11_024, pulse.size)
        assertTrue(siren.any { it > 20_000 })
        assertTrue("Pulse is silent in its second half", pulse.drop(pulse.size / 2).all { it.toInt() == 0 })
        assertTrue(pulse.take(pulse.size / 2).any { it > 20_000 })
    }

    @Test
    fun anUnknownWaveTypePlaysTheSiren() {
        assertTrue(sirenWave("Unknown").contentEquals(sirenWave("Siren")))
    }
}
