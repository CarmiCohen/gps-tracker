package com.jd.core.shared

import org.junit.Assert.*
import org.junit.Test

/**
 * KinematicCalibrationTest: Validates Continuous Kinematic Calibration (GAP-REVOL39.0-G1 / R-ID 96).
 * Ensures dynamic IMU noise floor recalibration during high-acceleration vehicle transitions
 * and robust stationary detection under changing kinetic baselines.
 */
class KinematicCalibrationTest {

    @Test
    fun `Verify IMU noise floor scaling under high acceleration transitions`() {
        val noiseSamples = doubleArrayOf(0.02, 0.03, 0.025, 0.028, 0.031)

        val staticFloor = SensingEngine.calculateKinematicAdaptiveFloor(
            recentNoiseSamples = noiseSamples,
            accelerationMps2 = 0.0,
            velocityMps = 0.0
        )

        val highAccelFloor = SensingEngine.calculateKinematicAdaptiveFloor(
            recentNoiseSamples = noiseSamples,
            accelerationMps2 = 9.8, // 1G transition
            velocityMps = 15.0
        )

        assertTrue(
            "High acceleration transition MUST scale the IMU noise floor baseline upwards. static: $staticFloor, highAccel: $highAccelFloor",
            highAccelFloor > staticFloor
        )
    }

    @Test
    fun `Verify stationary rejection during active vehicle acceleration`() {
        val adaptiveFloor = 0.12

        // Stationary state when at rest with low vibration
        val atRest = SensingEngine.isStationary(
            vibration = 0.05,
            adaptiveFloor = adaptiveFloor,
            cpuLoad = 0.1,
            accelerationMps2 = 0.0,
            velocityMps = 0.0
        )
        assertTrue("At rest with low vibration MUST be stationary", atRest)

        // Active high-acceleration transition (3.0 m/s^2) MUST NOT be marked stationary
        val accelerating = SensingEngine.isStationary(
            vibration = 0.05,
            adaptiveFloor = adaptiveFloor,
            cpuLoad = 0.1,
            accelerationMps2 = 3.0,
            velocityMps = 0.0
        )
        assertFalse("High acceleration transition MUST NOT be classified as stationary", accelerating)
    }

    @Test
    fun `Verify stationary rejection during vehicle movement`() {
        val adaptiveFloor = 0.12

        val moving = SensingEngine.isStationary(
            vibration = 0.05,
            adaptiveFloor = adaptiveFloor,
            cpuLoad = 0.1,
            accelerationMps2 = 0.0,
            velocityMps = 10.0 // 10 m/s (36 km/h)
        )
        assertFalse("Vehicle moving above 0.5 m/s MUST NOT be classified as stationary", moving)
    }

    @Test
    fun `Verify auto tuned floor backward compatibility`() {
        val samples = doubleArrayOf(0.01, 0.02, 0.015)
        val autoTuned = SensingEngine.calculateAutoTunedFloor(samples)
        val adaptiveDefault = SensingEngine.calculateKinematicAdaptiveFloor(samples)

        assertEquals("Auto-tuned floor MUST match adaptive floor defaults", autoTuned, adaptiveDefault, 0.0001f)
    }
}
