package com.jd.core.shared

import kotlin.math.*

/**
 * SensingEngine: 100% Kotlin implementation of high-frequency sensor logic.
 * Corrected Revol29.4: Aligned with R-ID 16 floor (1Hz) for low battery survival.
 * Updated Nov1.3: Integrated Velocity-Based Adaptive Sampling (Revol34.0 / R-ID 82).
 */
object SensingEngine {

    private const val THERMAL_CRITICAL_CELSIUS = 45.0
    private const val GRAVITY_EARTH = 9.80665

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
     * Aligned with Revol34.0: 10Hz (moving), 1Hz (slow), 0.1Hz (stationary).
     */
    fun getOptimalIntervalMs(
        currentTemp: Float, 
        isCharging: Boolean, 
        batteryLevel: Int,
        velocityMps: Float = 0f
    ): Long {
        if (currentTemp >= THERMAL_CRITICAL_CELSIUS) return 1000L // Thermal Protection
        
        val baseInterval = when {
            velocityMps > 5.0f -> 100L   // 10Hz: Moving (> 18 km/h)
            velocityMps > 0.5f -> 1000L  // 1Hz: Slow/Walking
            else -> 10000L               // 0.1Hz: Stationary
        }

        return when {
            isCharging -> min(100L, baseInterval) // Max performance when charging
            batteryLevel < 15 -> baseInterval.coerceAtLeast(1000L) // Survival Mode floor
            else -> baseInterval
        }
    }

    /**
     * Auto-Tune IMU (Point 78): Calculates dynamic vibration floor based on 
     * specific device hardware noise.
     */
    fun calculateAutoTunedFloor(recentNoiseSamples: DoubleArray): Float {
        if (recentNoiseSamples.isEmpty()) return 0.12f
        val mean = recentNoiseSamples.average()
        val variance = recentNoiseSamples.map { (it - mean).pow(2.0) }.average()
        val standardDeviation = sqrt(variance)
        // Set floor to Mean + 3 Sigma (99.7% confidence interval)
        return (mean + (standardDeviation * 3.0)).coerceIn(0.05, 0.5).toFloat()
    }

    fun isStationary(vibration: Double, adaptiveFloor: Double, cpuLoad: Double): Boolean {
        val loadFactor = if (cpuLoad > 0.85) 2.0 else 1.0
        val dynamicGate = (adaptiveFloor * 1.5 * loadFactor).coerceIn(0.05, 0.24)
        return vibration < dynamicGate
    }
}
