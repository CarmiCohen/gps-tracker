package com.gps19.core.ui

/**
 * HudViewState: Minimalist state for the Sensor Admin HUD.
 * Decouples the UI from the sensing models (Point 5).
 */
data class HudViewState(
    val trackerId: String = "--",
    val monitorId: String = "--",
    val isGpsActive: Boolean = false,
    val isRelayConnected: Boolean = false,
    val isPowerActive: Boolean = false,
    val satsUsed: Int = 0,
    val satsView: Int = 0,
    val accuracyMeters: Float = 0f,
    val isMuzzled: Boolean = false
)
