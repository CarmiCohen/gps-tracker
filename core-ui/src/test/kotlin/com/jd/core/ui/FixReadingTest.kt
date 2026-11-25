package com.jd.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies Chapter 122.1, 122.3, 122.4, and 128.1: a HUD line shows the device's own readings.
 * Both the Monitor local line and the Sensor HUD use [withLocalReadings] (ISSUE-NOV5.0-O2, ISSUE-NOV6.1-O27).
 */
class FixReadingTest {

    private val now = 1_000_000L
    private val battery = BatteryReading(level = 42, isCharging = true, tempCelsius = 33.5f)

    private fun fix(ageMs: Long = 0L, accuracy: Float = 8f, speedMps: Float? = 10f) =
        FixReading(now - ageMs, accuracy, speedMps)

    @Test
    fun usesBatteryAndFixValues() {
        val state = HudState().withLocalReadings(fix(ageMs = 4_000L), battery, now, MONITOR_SPEED_VALID_MS)

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
        val state = HudState().withLocalReadings(null, battery, now, MONITOR_SPEED_VALID_MS)

        assertEquals("42%⚡ 34° -- --", state.telemetryText)
        assertEquals("-- km/h", state.speedText)
        assertNull(state.speedKmh)
        assertEquals(NO_VALUE, state.trackerState)
    }

    @Test
    fun unknownBatteryShowsNoNumber() {
        val state = HudState().withLocalReadings(null, BatteryReading(), now, MONITOR_SPEED_VALID_MS)

        assertEquals("-- -- -- --", state.telemetryText)
        assertFalse(state.isCharging)
    }

    @Test
    fun oldFixKeepsItsAccuracyAndAgeButNotItsSpeed() {
        val state = HudState().withLocalReadings(fix(ageMs = 35_000L), battery, now, MONITOR_SPEED_VALID_MS)

        assertNull(state.speedKmh)
        assertEquals(NO_VALUE, state.trackerState)
        assertEquals(8f, state.accuracyMeters, 0.001f)
        assertEquals(35L, state.telemetryAgeSec)
    }

    /** SIMP-NOV7.21-S1: the Sensor HUD drops the speed at the age its telemetry stops sending the fix. */
    @Test
    fun theSensorLimitDropsTheSpeedWithTheTelemetry() {
        val current = HudState().withLocalReadings(fix(ageMs = FIX_CURRENT_MS - 1), battery, now, FIX_CURRENT_MS)
        val stale = HudState().withLocalReadings(fix(ageMs = FIX_CURRENT_MS), battery, now, FIX_CURRENT_MS)

        assertEquals(36f, current.speedKmh!!, 0.001f)
        assertNull(stale.speedKmh)
        assertEquals(NO_VALUE, stale.trackerState)
        assertEquals(15L, stale.telemetryAgeSec)
    }

    @Test
    fun fixWithoutSpeedShowsNoSpeed() {
        val state = HudState().withLocalReadings(fix(speedMps = null), battery, now, MONITOR_SPEED_VALID_MS)

        assertNull(state.speedKmh)
        assertEquals(NO_VALUE, state.trackerState)
    }

    @Test
    fun fixWithoutAccuracyShowsNoAccuracy() {
        val state = HudState().withLocalReadings(fix(accuracy = 0f), battery, now, MONITOR_SPEED_VALID_MS)

        assertEquals("42%⚡ 34° -- 0s", state.telemetryText)
    }

    @Test
    fun fixAheadOfTheClockHasZeroAge() {
        val state = HudState().withLocalReadings(fix(ageMs = -5_000L), battery, now, MONITOR_SPEED_VALID_MS)

        assertEquals(0L, state.telemetryAgeSec)
    }

    @Test
    fun keepsTheConnectionFieldsOfTheLine() {
        val line = HudState(selfId = "T", isRelayConnected = true, isPeerActive = true, isSpeedDimmed = true)

        val state = line.withLocalReadings(fix(), battery, now, MONITOR_SPEED_VALID_MS)

        assertTrue(state.isSpeedDimmed) // SIMP-NOV19.5-S1; was the appMode string
        assertEquals("T", state.selfId)
        assertTrue(state.isRelayConnected)
        assertTrue(state.isPeerActive)
    }

    @Test
    fun motionStateFollowsTheThreshold() {
        assertEquals(NO_VALUE, motionState(null))
        assertEquals("STATIONARY", motionState(0f))
        assertEquals("STATIONARY", motionState(1.5f))
        assertEquals("MOVING", motionState(1.6f))
    }
}
