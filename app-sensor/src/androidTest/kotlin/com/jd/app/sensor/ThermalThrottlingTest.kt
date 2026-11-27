package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.SensingEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ThermalThrottlingTest: Verifies multi-stage throttling logic (Revol35.0).
 * Ensures that battery and thermal constraints correctly override velocity-based intervals.
 */
@RunWith(AndroidJUnit4::class)
class ThermalThrottlingTest {

    @Test
    fun testThrottlingPriority() {
        // 1. Nominal: Moving fast, good battery, cool temp -> 1Hz, the fastest row clock (ISSUE-NOV22.5-O133)
        val nominal = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            batteryLevel = 80,
            isMoving = true
        )
        assertEquals(1000L, nominal)

        // 2. Battery Warning: Moving fast, low battery (18%), cool temp -> 1Hz (Throttled)
        val lowBattery = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            batteryLevel = 18,
            isMoving = true
        )
        assertEquals(1000L, lowBattery)

        // 3. Thermal Warning: Moving fast, good battery, warm temp (41C) -> 1Hz (Throttled)
        val thermalWarning = SensingEngine.getOptimalIntervalMs(
            currentTemp = 41f,
            batteryLevel = 80,
            isMoving = true
        )
        assertEquals(1000L, thermalWarning)

        // 4. Critical Survival: Stationary, battery critical (10%) -> 0.1Hz
        val criticalSurvival = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            batteryLevel = 10,
            isMoving = false
        )
        assertEquals(10000L, criticalSurvival)

        // 5. Critical Thermal: Moving fast, battery full, hot temp (46C) -> 0.1Hz (Critical Throttled)
        val criticalThermal = SensingEngine.getOptimalIntervalMs(
            currentTemp = 46f,
            batteryLevel = 100,
            isMoving = true
        )
        assertEquals(10000L, criticalThermal)

        // 6. Still and warm (41C), on the charger or not -> 0.1Hz: outside power gives no fast rows (ISSUE-NOV27.1-O169)
        val stillWarm = SensingEngine.getOptimalIntervalMs(
            currentTemp = 41f,
            batteryLevel = 80,
            isMoving = false
        )
        assertEquals(10000L, stillWarm)
    }
}
