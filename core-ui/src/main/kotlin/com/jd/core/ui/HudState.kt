package com.jd.core.ui

import androidx.compose.runtime.Immutable
import java.util.Locale

/** Shown in place of any HUD value that is not known. */
const val NO_VALUE = "--"

/** The speed unit, drawn in a tiny font after the number (Nov19.6). */
const val SPEED_UNIT = "km/h"

/** From this accuracy on, the HUD shows whole meters, `±13m` instead of `±12.5m` (Nov19.6). */
const val WHOLE_METER_ACCURACY = 9.95f

/**
 * HudState: Master state data model for HUD across Monitor and Sensor applications.
 * Adheres to Requirement R960 and HUD LED Specifications (v9.3.6).
 * A null reading means the value is not known, so the HUD never shows an invented number.
 */
@Immutable
data class HudState(
    val appMode: String = "monitor", // "monitor" (Viewer) or "sensor" (Tracker)
    val selfId: String = "--", // the ID of the phone this HUD describes (ISSUE-NOV17.7-O105)
    val isRelayConnected: Boolean = false,  // this phone's relay connection
    val isPeerActive: Boolean = false,      // the other app is heard (the Monitor's map center and speed color)
    val leds: List<HudLed> = emptyList(),   // the LEDs this line shows, green, red or gray (Nov19.5, HudLeds.kt)
    val trackerState: String = NO_VALUE,    // "MOVING", "STATIONARY", "PARKING", or NO_VALUE
    val speedKmh: Float? = null,
    val batteryLevel: Int? = null,
    val isCharging: Boolean = false,
    val tempCelsius: Float? = null,
    val satsUsed: Int = 0,
    val satsView: Int = 0,
    val accuracyMeters: Float = 0f,
    val telemetryAgeSec: Long = -1L,
    val sequenceId: Long = 0L
) {
    /** The speed number alone; [HudStatusBar] draws [SPEED_UNIT] after it in a tiny font. */
    val speedNumberText: String
        get() = speedKmh?.let { String.format(Locale.US, "%.1f", it) } ?: NO_VALUE

    val speedText: String
        get() = "$speedNumberText $SPEED_UNIT"

    /**
     * The ID of the phone this HUD describes, alone (Nov19.3; was `This phone: V` / `Sensor: T`, ISSUE-NOV17.7-O105):
     * the line's color already tells which phone it is.
     */
    val idText: String
        get() = selfId.take(6).uppercase()

    /**
     * No icons and no labels (Nov19.6; icons since ISSUE-NOV17.7-O106): the `%`, `°`, `±` and `s` tell the values
     * apart, `42%⚡|34°|±5.0m|0s`. An accuracy of [WHOLE_METER_ACCURACY] or more drops its decimal: `±13m`.
     */
    val telemetryText: String
        get() {
            val battery = batteryLevel?.let { "$it%${if (isCharging) "⚡" else ""}" } ?: NO_VALUE
            val temperature = tempCelsius?.let { String.format(Locale.US, "%.0f°", it) } ?: NO_VALUE
            val accuracy = when {
                accuracyMeters <= 0f -> NO_VALUE
                accuracyMeters >= WHOLE_METER_ACCURACY -> String.format(Locale.US, "±%.0fm", accuracyMeters)
                else -> String.format(Locale.US, "±%.1fm", accuracyMeters)
            }
            val age = if (telemetryAgeSec >= 0) "${telemetryAgeSec}s" else NO_VALUE
            return "$battery|$temperature|$accuracy|$age"
        }
}
