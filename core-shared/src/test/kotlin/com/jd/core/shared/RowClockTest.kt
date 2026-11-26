package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The Sensor's row clock never sends more than one row a second (ISSUE-NOV22.5-O133). */
class RowClockTest {

    @Before
    fun setup() {
        SensingEngine.resetThermalState()
    }

    @Test
    fun aPluggedInSensorSendsOneRowASecondNotTen() {
        // The A15 on USB, standing still: it sent 10 rows a second until Nov22.8.
        assertEquals(SensingEngine.ROW_INTERVAL_MS, SensingEngine.getOptimalIntervalMs(30f, isCharging = true, batteryLevel = 100, isMoving = false))
        assertEquals(1_000L, SensingEngine.ROW_INTERVAL_MS)
    }

    @Test
    fun noConditionGivesARowFasterThanOneASecond() {
        for (temp in listOf(20f, 41f, 44f, 46f)) for (charging in listOf(false, true)) for (battery in listOf(5, 18, 100))
            for (moving in listOf(false, true)) {
                val interval = SensingEngine.getOptimalIntervalMs(temp, charging, battery, moving)
                assert(interval >= SensingEngine.ROW_INTERVAL_MS) { "$interval ms at $temp C, charging $charging, $battery %, moving $moving" }
            }
    }

    /** ISSUE-NOV25.25-O162: moving is the debounced [MovementTracker] state, no longer one fix over 0.5 m/s. */
    @Test
    fun movingGivesOneRowASecondAndStandingStillOneInTen() {
        assertEquals(1_000L, SensingEngine.getOptimalIntervalMs(30f, false, 80, isMoving = true))
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(30f, false, 80, isMoving = false))
    }

    @Test
    fun criticalBatteryAndHeatOverrideMovementAndOutsidePower() {
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(30f, true, 10, true))
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(46f, true, 100, true))
        assertEquals("Hysteresis holds the critical heat at 44 C", 10_000L, SensingEngine.getOptimalIntervalMs(44f, true, 100, true))
        assertEquals(1_000L, SensingEngine.getOptimalIntervalMs(42.5f, true, 100, true))
    }

    /** Chapter 249 (SIMP-NOV17.7-S1): a battery or temperature not known is not critical; -1 was a critical battery until Nov23.1. */
    @Test
    fun unknownBatteryAndTemperatureAreNotCritical() {
        assertEquals(1_000L, SensingEngine.getOptimalIntervalMs(null, false, null, true))
        assertEquals(1_000L, SensingEngine.getOptimalIntervalMs(-1f, true, 80, false))
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(null, false, 10, true))
    }

    // ISSUE-NOV22.8-O136: an alarmed user may be chasing the thief; the trail must not wait 10 s between points.
    @Test
    fun anAlarmGivesAStationarySensorOnBatteryOneRowASecond() {
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(30f, false, 80, false, isAlarmed = false))
        assertEquals(1_000L, SensingEngine.getOptimalIntervalMs(30f, false, 80, false, isAlarmed = true))
    }

    @Test
    fun criticalBatteryAndHeatKeepTenSecondsDuringAnAlarm() {
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(30f, false, 10, true, isAlarmed = true))
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(46f, false, 100, true, isAlarmed = true))
    }

    @Test
    fun theAlarmWindowLastsFiveMinutesAfterTheLastAlarm() {
        val alarmMs = 1_000_000L
        assertEquals(300_000L, SensingEngine.ALARM_WINDOW_MS)
        assertEquals("No alarm yet", false, SensingEngine.isInAlarmWindow(null, alarmMs))
        assertEquals(true, SensingEngine.isInAlarmWindow(alarmMs, alarmMs))
        assertEquals(true, SensingEngine.isInAlarmWindow(alarmMs, alarmMs + 299_999L))
        assertEquals(false, SensingEngine.isInAlarmWindow(alarmMs, alarmMs + 300_000L))
        val nextAlarmMs = alarmMs + 200_000L // A new alarm, of any type, restarts the window.
        assertEquals(true, SensingEngine.isInAlarmWindow(nextAlarmMs, alarmMs + 450_000L))
    }
}
