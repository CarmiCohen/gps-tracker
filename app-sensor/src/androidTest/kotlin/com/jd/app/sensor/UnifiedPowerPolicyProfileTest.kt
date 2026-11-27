package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.SensingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UnifiedPowerPolicyProfileTest: Ported from OLD codebase.
 * Validates R497: Unified Power Policy and Doze-Mode deferral logic.
 * Ensures the system prioritizes survival over frequency in low-power states.
 */
@RunWith(AndroidJUnit4::class)
class UnifiedPowerPolicyProfileTest {

    @Test
    fun verifyPowerPolicyPriority() {
        // 1. Nominal State: High Battery, Moving
        val nominal = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            batteryLevel = 90,
            isMoving = true
        )
        assertEquals("Nominal state should request the fastest row clock (1 s, ISSUE-NOV22.5-O133)", 1000L, nominal)

        // 2. Low Battery State: Throttling engaged (R497)
        val lowBattery = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            batteryLevel = 18, // Below 20% threshold
            isMoving = true
        )
        assertTrue("Low battery MUST force at least 1s interval. Got: $lowBattery", lowBattery >= 1000L)

        // 3. Critical Power State: Survival mode
        val criticalBattery = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            batteryLevel = 10, // Critical threshold
            isMoving = true
        )
        assertEquals("Critical battery MUST force 10s survival interval", 10000L, criticalBattery)
    }

    /** ISSUE-NOV27.1-O169: outside power no longer raises the rate; a still Sensor gets 10 s, a warm moving one 1 s. */
    @Test
    fun verifyOutsidePowerGivesNoFastRows() {
        val stillWarm = SensingEngine.getOptimalIntervalMs(
            currentTemp = 41f,
            batteryLevel = 15,
            isMoving = false
        )
        assertEquals("A still Sensor sends one row in 10 s, charging or not", 10000L, stillWarm)
        val movingWarm = SensingEngine.getOptimalIntervalMs(
            currentTemp = 41f,
            batteryLevel = 15,
            isMoving = true
        )
        assertEquals("41C is below the critical heat: a moving Sensor keeps 1 s", 1000L, movingWarm)
    }
}
