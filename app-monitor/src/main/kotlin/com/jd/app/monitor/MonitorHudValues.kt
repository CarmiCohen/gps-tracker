package com.jd.app.monitor

import com.jd.core.ui.BatteryReading
import com.jd.core.ui.HudState
import com.jd.core.ui.NO_VALUE

private const val MPS_TO_KMH = 3.6f
private const val MOVING_ABOVE_KMH = 1.5f

// Position updates need 10m of travel, so a stopped Monitor gets no fix that would report its new speed.
private const val SPEED_VALID_MS = 35_000L

/** One position reading from the Monitor device. [accuracyMeters] is 0 and [speedMps] null when not reported. */
data class LocalFix(
    val position: MapPosition,
    val timeMs: Long,
    val accuracyMeters: Float,
    val speedMps: Float?
)

fun metersPerSecondToKmh(speedMps: Float): Float = speedMps * MPS_TO_KMH

fun motionState(speedKmh: Float?): String = when {
    speedKmh == null -> NO_VALUE
    speedKmh > MOVING_ABOVE_KMH -> "MOVING"
    else -> "STATIONARY"
}

/** Fills this HUD line with the Monitor device's own readings. */
fun HudState.withLocalReadings(fix: LocalFix?, battery: BatteryReading, nowMs: Long): HudState {
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
