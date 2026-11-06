package com.jd.core.ui

import androidx.compose.runtime.Immutable
import java.util.Locale

/** Shown in place of any HUD value that is not known. */
const val NO_VALUE = "--"

/**
 * HudState: Master state data model for HUD across Monitor and Sensor applications.
 * Adheres to Requirement R960 and HUD LED Specifications (v9.3.6).
 * A null reading means the value is not known, so the HUD never shows an invented number.
 */
@Immutable
data class HudState(
    val appMode: String = "monitor", // "monitor" (Viewer) or "sensor" (Tracker)
    val selfId: String = "--",
    val targetId: String = "--",
    val isInternet: Boolean = false,        // INT LED
    val isRelayConnected: Boolean = false,  // SRV LED
    val isLocalGpsActive: Boolean = false,  // GPS LED
    val isPeerActive: Boolean = false,      // TRK LED (Viewer) or VWR LED (Tracker)
    val isDataHealthy: Boolean = false,    // DAT LED (true if internet, relay, and peer active)
    val watchdogOk: Boolean = true,         // WDG LED
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

    val telemetryText: String
        get() {
            val battery = batteryLevel?.let { "$it%${if (isCharging) "⚡" else ""}" } ?: NO_VALUE
            val temperature = tempCelsius?.let { String.format(Locale.US, "%.0f°C", it) } ?: NO_VALUE
            val accuracy = if (accuracyMeters > 0) String.format(Locale.US, "±%.1fm", accuracyMeters) else NO_VALUE
            val age = if (telemetryAgeSec >= 0) "${telemetryAgeSec}s" else NO_VALUE
            return "B:$battery|T:$temperature|A:$accuracy|AGE:$age"
        }
}
