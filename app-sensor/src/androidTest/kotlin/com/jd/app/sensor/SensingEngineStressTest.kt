package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.SensingEngine
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SensingEngineStressTest: Automates high-load behavioral requirements (R709/R133).
 * Verifies that the SensingEngine correctly forces load-shedding when 
 * hardware constraints are met.
 */
@RunWith(AndroidJUnit4::class)
class SensingEngineStressTest {

    /**
     * verifyLoadSheddingPriority: Ensures that hardware stress (Thermal/Battery) 
     * always overrides performance-seeking (Velocity) intervals.
     */
    @Test
    fun verifyLoadSheddingPriority() {
        // High Speed (10m/s) normally requests 100ms (10Hz)
        // But Critical Thermal (46C) must force 10,000ms (0.1Hz)
        val interval = SensingEngine.getOptimalIntervalMs(
            currentTemp = 46f,
            isCharging = false,
            batteryLevel = 100,
            velocityMps = 10f
        )
        
        assertTrue(
            "R709: Critical thermal MUST override velocity-based intervals. Got: $interval",
            interval >= 10000L
        )
    }

    /**
     * verifyBatterySurvivalMode: Verifies the 0.1Hz survival gate for critical battery.
     */
    @Test
    fun verifyBatterySurvivalMode() {
        val interval = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            isCharging = false,
            batteryLevel = 10, // Critical threshold
            velocityMps = 5f
        )
        
        assertTrue(
            "R-ID 615: Critical battery survival gate must be 10s. Got: $interval",
            interval >= 10000L
        )
    }
}
