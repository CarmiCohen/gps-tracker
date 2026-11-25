package com.jd.core.ui

import android.location.Location

private const val MPS_TO_KMH = 3.6f
private const val MOVING_ABOVE_KMH = 1.5f

/**
 * How long the Monitor's own fix still tells its speed. Longer than [FIX_CURRENT_MS]: the Monitor's 10 m filter sends
 * no fix once it stops. The Sensor's HUD uses [FIX_CURRENT_MS], the age at which its telemetry stops sending the fix.
 */
const val MONITOR_SPEED_VALID_MS = 35_000L

/** One position fix from this device. [accuracyMeters] is 0 and [speedMps] null when not reported. */
data class FixReading(
    val timeMs: Long,
    val accuracyMeters: Float,
    val speedMps: Float?
)

fun Location.toFixReading() = FixReading(
    timeMs = time,
    accuracyMeters = if (hasAccuracy()) accuracy else 0f,
    speedMps = if (hasSpeed()) speed else null
)

fun metersPerSecondToKmh(speedMps: Float): Float = speedMps * MPS_TO_KMH

fun motionState(speedKmh: Float?): String = when {
    speedKmh == null -> NO_VALUE
    speedKmh > MOVING_ABOVE_KMH -> "MOVING"
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
        trackerState = motionState(speedKmh),
        speedKmh = speedKmh,
        batteryLevel = battery.level,
        isCharging = battery.isCharging,
        tempCelsius = battery.tempCelsius,
        accuracyMeters = fix?.accuracyMeters ?: 0f,
        telemetryAgeSec = fixAgeMs?.div(1000) ?: -1L
    )
}
