package com.jd.core.ui

import android.location.Location

private const val MPS_TO_KMH = 3.6f
private const val MOVING_ABOVE_KMH = 1.5f

// A fix this old no longer tells the current speed: the Monitor's 10m filter sends no fix once it stops,
// and the Sensor gets none while its GPS has no signal.
private const val SPEED_VALID_MS = 35_000L

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

/** Fills this HUD line with the device's own readings. A missing [fix] shows no position values. */
fun HudState.withLocalReadings(fix: FixReading?, battery: BatteryReading, nowMs: Long): HudState {
    val fixAgeMs = fix?.let { (nowMs - it.timeMs).coerceAtLeast(0L) }
    val speedKmh = fix?.speedMps
        ?.takeIf { fixAgeMs != null && fixAgeMs < SPEED_VALID_MS }
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
