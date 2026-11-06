package com.jd.app.monitor

import com.jd.core.ui.BatteryReading
import com.jd.core.ui.HudState
import com.jd.core.ui.NO_VALUE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies Chapter 122.1, 122.3, and 122.4: the local HUD line shows the Monitor's own readings. */
class MonitorHudValuesTest {

    private val now = 1_000_000L
    private val battery = BatteryReading(level = 42, isCharging = true, tempCelsius = 33.5f)

    private fun fix(ageMs: Long = 0L, accuracy: Float = 8f, speedMps: Float? = 10f) =
        LocalFix(MapPosition(32.0, 34.0), now - ageMs, accuracy, speedMps)

    @Test
    fun usesBatteryAndFixValues() {
        val state = HudState().withLocalReadings(fix(ageMs = 4_000L), battery, now)

        assertEquals(42, state.batteryLevel)
        assertTrue(state.isCharging)
        assertEquals(33.5f, state.tempCelsius!!, 0.001f)
        assertEquals(8f, state.accuracyMeters, 0.001f)
        assertEquals(36f, state.speedKmh!!, 0.001f)
        assertEquals(4L, state.telemetryAgeSec)
        assertEquals("MOVING", state.trackerState)
    }

    @Test
    fun noFixShowsNoPositionValues() {
        val state = HudState().withLocalReadings(null, battery, now)

        assertEquals("B:42%⚡|T:34°C|A:--|AGE:--", state.telemetryText)
        assertNull(state.speedKmh)
        assertEquals(NO_VALUE, state.trackerState)
    }

    @Test
    fun unknownBatteryShowsNoNumber() {
        val state = HudState().withLocalReadings(null, BatteryReading(), now)

        assertEquals("B:--|T:--|A:--|AGE:--", state.telemetryText)
        assertFalse(state.isCharging)
    }

    @Test
    fun oldFixKeepsItsAccuracyAndAgeButNotItsSpeed() {
        val state = HudState().withLocalReadings(fix(ageMs = 35_000L), battery, now)

        assertNull(state.speedKmh)
        assertEquals(NO_VALUE, state.trackerState)
        assertEquals(8f, state.accuracyMeters, 0.001f)
        assertEquals(35L, state.telemetryAgeSec)
    }

    @Test
    fun fixWithoutSpeedShowsNoSpeed() {
        val state = HudState().withLocalReadings(fix(speedMps = null), battery, now)

        assertNull(state.speedKmh)
        assertEquals(NO_VALUE, state.trackerState)
    }

    @Test
    fun fixAheadOfTheClockHasZeroAge() {
        val state = HudState().withLocalReadings(fix(ageMs = -5_000L), battery, now)

        assertEquals(0L, state.telemetryAgeSec)
    }

    @Test
    fun motionStateFollowsTheThreshold() {
        assertEquals(NO_VALUE, motionState(null))
        assertEquals("STATIONARY", motionState(0f))
        assertEquals("STATIONARY", motionState(1.5f))
        assertEquals("MOVING", motionState(1.6f))
    }
}
