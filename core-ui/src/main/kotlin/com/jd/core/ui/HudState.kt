package com.jd.core.ui

import androidx.compose.runtime.Immutable
import java.util.Locale

/** Shown in place of any HUD value that is not known. */
const val NO_VALUE = "--"

/** Marks the battery value in the HUD telemetry line, in place of the old `B:` label (ISSUE-NOV17.7-O106). */
const val HUD_BATTERY_ICON = "🔋" // 🔋

/** Marks the temperature value in the HUD telemetry line, in place of the old `T:` label (ISSUE-NOV17.7-O106). */
const val HUD_TEMPERATURE_ICON = "🌡️" // 🌡️ (with the emoji presentation selector)

/**
 * HudState: Master state data model for HUD across Monitor and Sensor applications.
 * Adheres to Requirement R960 and HUD LED Specifications (v9.3.6).
 * A null reading means the value is not known, so the HUD never shows an invented number.
 */
@Immutable
data class HudState(
    val appMode: String = "monitor", // "monitor" (Viewer) or "sensor" (Tracker)
    val selfId: String = "--", // the ID of the phone this HUD describes (ISSUE-NOV17.7-O105)
    val isRelayConnected: Boolean = false,  // this phone's relay connection (relayStatusText)
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
    val speedText: String
        get() = "${speedKmh?.let { String.format(Locale.US, "%.1f", it) } ?: NO_VALUE} km/h"

    /**
     * The ID of the phone this HUD describes, alone (Nov19.3; was `This phone: V` / `Sensor: T`, ISSUE-NOV17.7-O105):
     * the line's color already tells which phone it is.
     */
    val idText: String
        get() = selfId.take(6).uppercase()

    /** Uses the same state as the SRV LED, so the two can never disagree. */
    val relayStatusText: String
        get() = "Relay Status: ${if (isRelayConnected) "Connected" else "Disconnected"}"

    /**
     * Battery and temperature are marked by an icon, not a `B:` or `T:` label, and the temperature has no `C`
     * (ISSUE-NOV17.7-O106): `🔋42%⚡|🌡️34°|A:±5.0m|AGE:0s`.
     */
    val telemetryText: String
        get() {
            val battery = batteryLevel?.let { "$it%${if (isCharging) "⚡" else ""}" } ?: NO_VALUE
            val temperature = tempCelsius?.let { String.format(Locale.US, "%.0f°", it) } ?: NO_VALUE
            val accuracy = if (accuracyMeters > 0) String.format(Locale.US, "±%.1fm", accuracyMeters) else NO_VALUE
            val age = if (telemetryAgeSec >= 0) "${telemetryAgeSec}s" else NO_VALUE
            return "$HUD_BATTERY_ICON$battery|$HUD_TEMPERATURE_ICON$temperature|A:$accuracy|AGE:$age"
        }
}
