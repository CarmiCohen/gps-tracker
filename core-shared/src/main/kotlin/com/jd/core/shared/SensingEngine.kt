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

    const val THERMAL_CRITICAL_CELSIUS = 45.0
    const val THERMAL_HYSTERESIS_CELSIUS = 2.0
    /** The fastest row clock: one telemetry row a second (ISSUE-NOV22.5-O133). */
    const val ROW_INTERVAL_MS = 1_000L
    /** The slow row clock: stationary, critical battery or critical heat. */
    const val SURVIVAL_INTERVAL_MS = 10_000L
    private const val SURVIVAL_BATTERY_PERCENT = 15
    private const val MOVING_MPS = 0.5f
    /** How long after its last alarm a Sensor keeps one row a second (ISSUE-NOV22.8-O136, user decision 2026-11-22). */
    const val ALARM_WINDOW_MS = 5 * 60_000L

    /** Whether the last alarm, at monotonic [lastAlarmMs] (null: none yet), is less than [ALARM_WINDOW_MS] before [nowMs]. */
    fun isInAlarmWindow(lastAlarmMs: Long?, nowMs: Long): Boolean =
        lastAlarmMs != null && nowMs - lastAlarmMs < ALARM_WINDOW_MS
    
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
     * The interval of the Sensor's sensing loop, which is also its row clock: every tick makes one telemetry row.
     * The fastest rate is one row a second (ISSUE-NOV22.5-O133): a location fix comes at most once a second, so faster
     * rows repeated the same position and used up the relay's bandwidth. Motion, noise and light are detected on the
     * sensors' own events, and a queued alarm preempts the loop, so a slower row clock delays no alarm.
     * Moving, on outside power, or within the alarm window ([isAlarmed], ISSUE-NOV22.8-O136): [ROW_INTERVAL_MS].
     * Stationary, critical battery (< 15 %) or critical heat (with hysteresis, R133): [SURVIVAL_INTERVAL_MS]; heat and
     * battery override movement, outside power and an alarm (R-ID 83, user decision 2026-11-22).
     */
    @Synchronized
    fun getOptimalIntervalMs(
        currentTemp: Float,
        isCharging: Boolean,
        batteryLevel: Int,
        velocityMps: Float = 0f,
        isAlarmed: Boolean = false
    ): Long {
        val isCriticalHeat = currentTemp >= THERMAL_CRITICAL_CELSIUS ||
            (activeThermalThrottlingMs == SURVIVAL_INTERVAL_MS && currentTemp >= THERMAL_CRITICAL_CELSIUS - THERMAL_HYSTERESIS_CELSIUS)
        activeThermalThrottlingMs = if (isCriticalHeat) SURVIVAL_INTERVAL_MS else ROW_INTERVAL_MS
        return when {
            isCriticalHeat || batteryLevel < SURVIVAL_BATTERY_PERCENT -> SURVIVAL_INTERVAL_MS
            isAlarmed || isCharging || velocityMps > MOVING_MPS -> ROW_INTERVAL_MS
            else -> SURVIVAL_INTERVAL_MS
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
