package com.jd.core.ui

import androidx.compose.runtime.Immutable

/**
 * HudState: Master state data model for HUD across Monitor and Sensor applications.
 * Adheres to Requirement R960 and HUD LED Specifications (v9.3.6).
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
    val trackerState: String = "STATIONARY",// "MOVING", "STATIONARY", "PARKING"
    val speedKmh: Float = 0f,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val tempCelsius: Float = 30f,
    val satsUsed: Int = 0,
    val satsView: Int = 0,
    val accuracyMeters: Float = 0f,
    val telemetryAgeSec: Long = -1L,
    val sequenceId: Long = 0L
)
