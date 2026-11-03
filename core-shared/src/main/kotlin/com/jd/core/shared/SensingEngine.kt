package com.jd.core.shared

import kotlin.math.*

/**
 * SensingEngine: 100% Kotlin implementation of high-frequency sensor logic.
 * Corrected Revol29.4: Aligned with R-ID 16 floor (1Hz) for low battery survival.
 * Updated Nov1.3: Integrated Velocity-Based Adaptive Sampling (Revol34.0 / R-ID 82).
 * Updated Nov1.4: Integrated Multi-Stage Thermal & Battery Throttling (Revol35.0 / R-ID 83).
 * Updated Nov2.25: Added Thermal Hysteresis (R133 / R-ID 88) to prevent interval oscillation.
 * Updated Nov3.1: Continuous Kinematic Calibration (GAP-REVOL39.0-G1 / R-ID 96).
 */
object SensingEngine {

    const val THERMAL_WARNING_CELSIUS = 40.0
    const val THERMAL_CRITICAL_CELSIUS = 45.0
    const val THERMAL_HYSTERESIS_CELSIUS = 2.0
    
    private const val GRAVITY_EARTH = 9.80665
    
    private var activeThermalThrottlingMs = 0L

    /**
     * Resets the internal thermal state. Primarily used for unit testing.
     */
    @Synchronized
    fun resetThermalState() {
        activeThermalThrottlingMs = 0L
    }

    /**
     * Rotates raw accelerometer data into World Space using the Gravity Vector.
     * Ensures vertical/horizontal components are accurate regardless of orientation.
     * @return [horizontal_magnitude, vertical_component]
     */
    fun rotateToWorldSpace(
        ax: Double, ay: Double, az: Double, 
        gx: Double, gy: Double, gz: Double
    ): DoubleArray {
        val gMag = sqrt(gx * gx + gy * gy + gz * gz)
        if (gMag < 0.1) return doubleArrayOf(0.0, 0.0)

        // 1. Vertical Component: Projection of acceleration onto the gravity vector
        val dotProduct = (ax * gx + ay * gy + az * gz)
        val verticalComponent = dotProduct / gMag

        // 2. Horizontal Magnitude: Remaining acceleration vector length
        val totalAccelMag = sqrt(ax * ax + ay * ay + az * az)
        val horizontalMag = sqrt(max(0.0, totalAccelMag * totalAccelMag - verticalComponent * verticalComponent))

        // Normalize to Gs
        return doubleArrayOf(horizontalMag / GRAVITY_EARTH, verticalComponent / GRAVITY_EARTH)
    }

    /**
     * Determines the optimal sensing interval based on movement, heat, and power source.
     * Aligned with Revol35.0: Multi-stage throttling (10Hz moving, 1Hz slow/warn, 0.1Hz stationary/crit).
     * Updated Nov2.25: Implemented Hysteresis for boundary stability (R133).
     */
    @Synchronized
    fun getOptimalIntervalMs(
        currentTemp: Float, 
        isCharging: Boolean, 
        batteryLevel: Int,
        velocityMps: Float = 0f
    ): Long {
        // 1. Thermal Throttling with Hysteresis (Highest Priority)
        val thermalCap = when {
            currentTemp >= THERMAL_CRITICAL_CELSIUS -> 10000L
            activeThermalThrottlingMs == 10000L && currentTemp >= (THERMAL_CRITICAL_CELSIUS - THERMAL_HYSTERESIS_CELSIUS) -> 10000L
            currentTemp >= THERMAL_WARNING_CELSIUS -> 1000L
            activeThermalThrottlingMs == 1000L && currentTemp >= (THERMAL_WARNING_CELSIUS - THERMAL_HYSTERESIS_CELSIUS) -> 1000L
            else -> 100L
        }
        activeThermalThrottlingMs = thermalCap
        
        // 2. Battery Throttling
        val batteryCap = when {
            batteryLevel < 15 -> 10000L // 0.1Hz: Survival Mode
            batteryLevel < 20 -> 1000L  // 1Hz: Low Battery
            else -> 100L                // 10Hz: Nominal
        }

        // 3. Velocity-Based Base Interval
        val baseInterval = when {
            velocityMps > 5.0f -> 100L   // 10Hz: Moving
            velocityMps > 0.5f -> 1000L  // 1Hz: Slow
            else -> 10000L               // 0.1Hz: Stationary
        }

        // Combine constraints: The longest interval wins (throttling)
        val combinedInterval = max(baseInterval, max(thermalCap, batteryCap))

        // Special case for charging: allows performance mode if thermal is safe
        // Charging respects thermal caps and hysteresis
        return if (isCharging && thermalCap == 100L) {
            min(100L, combinedInterval)
        } else {
            combinedInterval
        }
    }

    /**
     * Auto-Tune IMU (Point 78): Calculates dynamic vibration floor based on 
     * specific device hardware noise.
     */
    fun calculateAutoTunedFloor(recentNoiseSamples: DoubleArray): Float {
        return calculateKinematicAdaptiveFloor(recentNoiseSamples, accelerationMps2 = 0.0, velocityMps = 0.0)
    }

    /**
     * Continuous Kinematic Calibration (GAP-REVOL39.0-G1 / R-ID 96):
     * Calculates dynamic IMU vibration floor continuously recalibrated for high-acceleration vehicle transitions.
     */
    fun calculateKinematicAdaptiveFloor(
        recentNoiseSamples: DoubleArray,
        accelerationMps2: Double = 0.0,
        velocityMps: Double = 0.0
    ): Float {
        val baseFloor = if (recentNoiseSamples.isEmpty()) {
            0.12f
        } else {
            val mean = recentNoiseSamples.average()
            val variance = recentNoiseSamples.map { (it - mean).pow(2.0) }.average()
            val standardDeviation = sqrt(variance)
            (mean + (standardDeviation * 3.0)).toFloat()
        }

        val accelFactor = (1.0 + (abs(accelerationMps2) / GRAVITY_EARTH)).coerceIn(1.0, 3.0)
        val velocityFactor = if (velocityMps > 5.0) 1.25 else 1.0

        return (baseFloor * accelFactor * velocityFactor).coerceIn(0.05, 1.5).toFloat()
    }

    /**
     * Hardened Stationary Detection (GAP-REVOL39.0-G1 / R-ID 96):
     * Evaluates stationary state under changing kinetic baselines and high acceleration vehicle transitions.
     */
    fun isStationary(
        vibration: Double,
        adaptiveFloor: Double,
        cpuLoad: Double = 0.0,
        accelerationMps2: Double = 0.0,
        velocityMps: Double = 0.0
    ): Boolean {
        // Active vehicle motion (>0.5 m/s) or high acceleration transition (>2.0 m/s^2) cannot be stationary
        if (velocityMps > 0.5 || abs(accelerationMps2) > 2.0) return false

        val loadFactor = if (cpuLoad > 0.85) 2.0 else 1.0
        val dynamicGate = (adaptiveFloor * 1.5 * loadFactor).coerceIn(0.05, 0.50)
        return vibration < dynamicGate
    }
}
