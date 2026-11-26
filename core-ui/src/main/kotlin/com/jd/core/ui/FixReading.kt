package com.jd.core.ui

import android.location.Location

private const val MPS_TO_KMH = 3.6f

/**
 * How long the Monitor's own fix still tells its speed. Longer than [FIX_CURRENT_MS]: the Monitor's 10 m filter sends
 * no fix once it stops. The Sensor's HUD uses [FIX_CURRENT_MS], the age at which its telemetry stops sending the fix.
 */
const val MONITOR_SPEED_VALID_MS = 35_000L

/**
 * One position fix from this device. [accuracyMeters] is 0 and [speedMps] null when not reported. [isMoving] is the
 * device's debounced movement up to this fix ([com.jd.core.shared.MovementTracker], ISSUE-NOV25.25-O162).
 */
data class FixReading(
    val timeMs: Long,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val isMoving: Boolean = false
)

fun Location.toFixReading(isMoving: Boolean = false) = FixReading(
    timeMs = time,
    accuracyMeters = if (hasAccuracy()) accuracy else 0f,
    speedMps = if (hasSpeed()) speed else null,
    isMoving = isMoving
)

fun metersPerSecondToKmh(speedMps: Float): Float = speedMps * MPS_TO_KMH

/**
 * The HUD's STATIONARY / MOVING: [isMoving] from a [com.jd.core.shared.MovementTracker] (ISSUE-NOV25.25-O162; one fix's
 * speed over 1.5 km/h before), no value while no current speed is known.
 */
fun motionState(speedKmh: Float?, isMoving: Boolean): String = when {
    speedKmh == null -> NO_VALUE
    isMoving -> "MOVING"
    else -> "STATIONARY"
}

/**
 * Fills this HUD line with the device's own readings. A missing [fix] shows no position values; a fix [speedValidMs]
 * old or older keeps its accuracy and age but no longer shows a speed.
 */
fun HudState.withLocalReadings(fix: FixReading?, battery: BatteryReading, nowMs: Long, speedValidMs: Long): HudState {
    val fixAgeMs = fix?.let { (nowMs - it.timeMs).coerceAtLeast(0L) }
    val speedKmh = fix?.speedMps
        ?.takeIf { fixAgeMs != null && fixAgeMs < speedValidMs }
        ?.let(::metersPerSecondToKmh)
    return copy(
        trackerState = motionState(speedKmh, fix?.isMoving == true),
        speedKmh = speedKmh,
        batteryLevel = battery.level,
        isCharging = battery.isCharging,
        tempCelsius = battery.tempCelsius,
        accuracyMeters = fix?.accuracyMeters ?: 0f,
        telemetryAgeSec = fixAgeMs?.div(1000) ?: -1L
    )
}
