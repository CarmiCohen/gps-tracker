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

    /** ISSUE-NOV27.1-O169: a still Sensor on the charger (every night) sends one row in 10 s, as on battery. */
    @Test
    fun aStillSensorSendsOneRowInTenSecondsChargingOrNot() {
        assertEquals(SensingEngine.SURVIVAL_INTERVAL_MS, SensingEngine.getOptimalIntervalMs(30f, batteryLevel = 100, isMoving = false))
        assertEquals(10_000L, SensingEngine.SURVIVAL_INTERVAL_MS)
        assertEquals(1_000L, SensingEngine.ROW_INTERVAL_MS)
    }

    @Test
    fun noConditionGivesARowFasterThanOneASecond() {
        for (temp in listOf(20f, 41f, 44f, 46f)) for (alarmed in listOf(false, true)) for (battery in listOf(5, 18, 100))
            for (moving in listOf(false, true)) {
                val interval = SensingEngine.getOptimalIntervalMs(temp, battery, moving, alarmed)
                assert(interval >= SensingEngine.ROW_INTERVAL_MS) { "$interval ms at $temp C, alarmed $alarmed, $battery %, moving $moving" }
            }
    }

    /** ISSUE-NOV25.25-O162: moving is the debounced [MovementTracker] state, no longer one fix over 0.5 m/s. */
    @Test
    fun movingGivesOneRowASecondAndStandingStillOneInTen() {
        assertEquals(1_000L, SensingEngine.getOptimalIntervalMs(30f, 80, isMoving = true))
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(30f, 80, isMoving = false))
    }

    @Test
    fun criticalBatteryAndHeatOverrideMovement() {
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(30f, 10, true))
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(46f, 100, true))
        assertEquals("Hysteresis holds the critical heat at 44 C", 10_000L, SensingEngine.getOptimalIntervalMs(44f, 100, true))
        assertEquals(1_000L, SensingEngine.getOptimalIntervalMs(42.5f, 100, true))
    }

    /** Chapter 249 (SIMP-NOV17.7-S1): a battery or temperature not known is not critical. */
    @Test
    fun unknownBatteryAndTemperatureAreNotCritical() {
        assertEquals(1_000L, SensingEngine.getOptimalIntervalMs(null, null, true))
        assertEquals(1_000L, SensingEngine.getOptimalIntervalMs(-1f, 80, true))
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(null, 10, true))
    }

    // ISSUE-NOV22.8-O136: an alarmed user may be chasing the thief; the trail must not wait 10 s between points.
    @Test
    fun anAlarmGivesAStationarySensorOneRowASecond() {
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(30f, 80, false, isAlarmed = false))
        assertEquals(1_000L, SensingEngine.getOptimalIntervalMs(30f, 80, false, isAlarmed = true))
    }

    @Test
    fun criticalBatteryAndHeatKeepTenSecondsDuringAnAlarm() {
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(30f, 10, true, isAlarmed = true))
        assertEquals(10_000L, SensingEngine.getOptimalIntervalMs(46f, 100, true, isAlarmed = true))
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
