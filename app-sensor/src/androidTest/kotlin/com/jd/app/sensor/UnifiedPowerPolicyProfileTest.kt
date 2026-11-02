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
            isCharging = false,
            batteryLevel = 90,
            velocityMps = 10f
        )
        assertEquals("Nominal state should request high frequency (100ms)", 100L, nominal)

        // 2. Low Battery State: Throttling engaged (R497)
        val lowBattery = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            isCharging = false,
            batteryLevel = 18, // Below 20% threshold
            velocityMps = 10f
        )
        assertTrue("Low battery MUST force at least 1s interval. Got: $lowBattery", lowBattery >= 1000L)

        // 3. Critical Power State: Survival mode
        val criticalBattery = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            isCharging = false,
            batteryLevel = 10, // Critical threshold
            velocityMps = 10f
        )
        assertEquals("Critical battery MUST force 10s survival interval", 10000L, criticalBattery)
    }

    @Test
    fun verifyChargingOverride() {
        // Charging should allow higher frequency even at lower battery, 
        // but thermal limits still apply.
        val chargingWarm = SensingEngine.getOptimalIntervalMs(
            currentTemp = 41f,
            isCharging = true,
            batteryLevel = 15,
            velocityMps = 5f
        )
        // Thermal (41C) forces 1s backoff regardless of charging status
        assertEquals("Thermal limit MUST override charging benefits", 1000L, chargingWarm)
    }
}
