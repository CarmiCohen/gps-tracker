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
        // 1. Nominal: Moving fast, good battery, cool temp -> 10Hz
        val nominal = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            isCharging = false,
            batteryLevel = 80,
            velocityMps = 10f
        )
        assertEquals(100L, nominal)

        // 2. Battery Warning: Moving fast, low battery (18%), cool temp -> 1Hz (Throttled)
        val lowBattery = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            isCharging = false,
            batteryLevel = 18,
            velocityMps = 10f
        )
        assertEquals(1000L, lowBattery)

        // 3. Thermal Warning: Moving fast, good battery, warm temp (41C) -> 1Hz (Throttled)
        val thermalWarning = SensingEngine.getOptimalIntervalMs(
            currentTemp = 41f,
            isCharging = false,
            batteryLevel = 80,
            velocityMps = 10f
        )
        assertEquals(1000L, thermalWarning)

        // 4. Critical Survival: Stationary, battery critical (10%) -> 0.1Hz
        val criticalSurvival = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            isCharging = false,
            batteryLevel = 10,
            velocityMps = 0f
        )
        assertEquals(10000L, criticalSurvival)

        // 5. Critical Thermal: Moving fast, battery full, hot temp (46C) -> 0.1Hz (Critical Throttled)
        val criticalThermal = SensingEngine.getOptimalIntervalMs(
            currentTemp = 46f,
            isCharging = false,
            batteryLevel = 100,
            velocityMps = 10f
        )
        assertEquals(10000L, criticalThermal)

        // 6. Charging Recovery: Warm (41C), Charging -> 1Hz (Thermal cap still applies)
        val chargingWarm = SensingEngine.getOptimalIntervalMs(
            currentTemp = 41f,
            isCharging = true,
            batteryLevel = 80,
            velocityMps = 10f
        )
        assertEquals(1000L, chargingWarm)
    }
}
