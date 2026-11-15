package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 188 (ISSUE-NOV10.8-O83): the Sensor's switches and limits are set on the Monitor. A change to them gets a new
 * revision, so the Sensor takes it; a change to the Monitor's own alarms does not; a switched-off kind sends no alert.
 */
class SensorBehaviourTest {

    @Test
    fun `the Monitor's switches and sliders make the Sensor's settings`() {
        val settings = AlertSettings(
            tiltAlert = false, tamperAlert = true, acousticAlert = false, liftAlert = true,
            tiltSensitivity = 0.3f, vibrationSensitivity = 0.7f, noiseSensitivity = 0.9f, sensorRevision = 42L
        )
        assertEquals(
            SensorBehaviour(
                tiltAlert = false, vibrationAlert = true, noiseAlert = false, liftAlert = true,
                tiltSensitivity = 0.3f, vibrationSensitivity = 0.7f, noiseSensitivity = 0.9f, revision = 42L
            ),
            settings.sensorBehaviour()
        )
    }

    @Test
    fun `a change to the Sensor's part gets a new revision, other changes keep it`() {
        val stored = AlertSettings(sensorRevision = 1_000L)
        assertEquals(5_000L, stored.copy(tiltSensitivity = 0.9f).revisedAfter(stored, nowMs = 5_000L).sensorRevision)
        assertEquals(5_000L, stored.copy(acousticAlert = false).revisedAfter(stored, nowMs = 5_000L).sensorRevision)
        assertEquals(
            "A Monitor-only alarm switch is not sent again",
            1_000L, stored.copy(lowBattery = false, globalMute = true).revisedAfter(stored, nowMs = 5_000L).sensorRevision
        )
        assertEquals(
            "A clock that went back still gives a newer revision",
            1_001L, stored.copy(liftAlert = false).revisedAfter(stored, nowMs = 10L).sensorRevision
        )
        assertEquals(
            "A stale revision in the edited copy is replaced by the stored one",
            1_000L, stored.copy(sensorRevision = 3L).revisedAfter(stored, nowMs = 5_000L).sensorRevision
        )
    }

    @Test
    fun `a switched-off kind sends no alert, other rows always pass`() {
        val behaviour = SensorBehaviour(tiltAlert = false, vibrationAlert = false, noiseAlert = false, liftAlert = false)
        listOf(TILT_ALERT_STATUS, VIBRATION_ALERT_STATUS, ACOUSTIC_ALERT_STATUS, LIFT_ALERT_STATUS).forEach {
            assertFalse(it, behaviour.allows(it))
            assertTrue(it, SensorBehaviour().allows(it))
        }
        assertTrue(behaviour.allows(GEOFENCE_BREACH_STATUS))
        assertTrue(behaviour.allows("VALID"))
    }

    @Test
    fun `the noise slider sets the jump over the background, 30 dB in the middle`() {
        assertEquals(45.0, noiseJumpForSensitivity(0.1f), 1e-6)
        assertEquals(ACOUSTIC_JUMP_DB, noiseJumpForSensitivity(DEFAULT_NOISE_SENSITIVITY), 1e-6)
        assertEquals(15.0, noiseJumpForSensitivity(1.0f), 1e-6)
        assertEquals("Out of range is clamped", 45.0, noiseJumpForSensitivity(0f), 1e-6)
        assertEquals("30 dB", noiseLimitLabel(0.5f))
    }

    @Test
    fun `a slider change applies to a listening detector at once`() {
        val detector = AcousticDetector(warmUpMs = 0L)
        detector.onLevel(-60.0, 0L)
        assertFalse("20 dB is below the default 30 dB jump", detector.onLevel(-40.0, 100L))
        detector.jumpDb = 15.0
        assertTrue(detector.onLevel(-40.0, 200L))
    }
}
