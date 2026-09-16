package com.gps19.core.engine

/**
 * Interface to provide device power states, such as Doze mode (idle mode),
 * allowing for deterministic testing of power-aware logic.
 * R-ID 338: Android Doze Awareness.
 */
interface PowerStateProvider {
    /**
     * Returns true if the device is currently in idle mode (Doze).
     * Equivalent to Android's PowerManager.isDeviceIdleMode().
     */
    fun isDeviceIdleMode(): Boolean
}
