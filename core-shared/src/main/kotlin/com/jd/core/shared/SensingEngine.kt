package com.jd.core.shared

import kotlin.math.*

/** The Sensor's row clock (movement, alarm, battery and heat throttling) and world-space motion math. */
object SensingEngine {

    const val THERMAL_CRITICAL_CELSIUS = 45.0
    const val THERMAL_HYSTERESIS_CELSIUS = 2.0
    /** The fastest row clock: one telemetry row a second (ISSUE-NOV22.5-O133). */
    const val ROW_INTERVAL_MS = 1_000L
    /** The slow row clock: stationary, critical battery or critical heat. */
    const val SURVIVAL_INTERVAL_MS = 10_000L
    private const val SURVIVAL_BATTERY_PERCENT = 15
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
     * Moving ([isMoving] by [MovementTracker], ISSUE-NOV25.25-O162) or within the alarm window ([isAlarmed],
     * ISSUE-NOV22.8-O136): [ROW_INTERVAL_MS].
     * Stationary, critical battery (< 15 %) or critical heat (with hysteresis, R133): [SURVIVAL_INTERVAL_MS]; heat and
     * battery override movement and an alarm (R-ID 83).
     * Outside power does not count (ISSUE-NOV27.1-O169): a still Sensor on the charger would only repeat its position.
     * A temperature or battery level not known (null) is neither critical (SIMP-NOV17.7-S1).
     */
    @Synchronized
    fun getOptimalIntervalMs(
        currentTemp: Float?,
        batteryLevel: Int?,
        isMoving: Boolean = false,
        isAlarmed: Boolean = false
    ): Long {
        val isCriticalHeat = currentTemp != null && (currentTemp >= THERMAL_CRITICAL_CELSIUS ||
            (activeThermalThrottlingMs == SURVIVAL_INTERVAL_MS && currentTemp >= THERMAL_CRITICAL_CELSIUS - THERMAL_HYSTERESIS_CELSIUS))
        activeThermalThrottlingMs = if (isCriticalHeat) SURVIVAL_INTERVAL_MS else ROW_INTERVAL_MS
        val isCriticalBattery = batteryLevel != null && batteryLevel < SURVIVAL_BATTERY_PERCENT
        return when {
            isCriticalHeat || isCriticalBattery -> SURVIVAL_INTERVAL_MS
            isAlarmed || isMoving -> ROW_INTERVAL_MS
            else -> SURVIVAL_INTERVAL_MS
        }
    }
}
