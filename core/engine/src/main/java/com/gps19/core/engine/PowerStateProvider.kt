package com.gps19.core.engine

/**
 * Interface to provide device power states, such as Doze mode (idle mode),
 * allowing for deterministic testing of power-aware logic.
 * Oct.18.16:
 * - Issue #SIMP-OCT18.16-P1: Doze Mode White-listing. Added isIgnoringBatteryOptimizations 
 *   to allow signaling bypass during Doze mode for persistent sessions.
 * Oct.10.2:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Migrated isDeviceIdleMode to property.
 */
interface PowerStateProvider {
    /**
     * Returns true if the device is currently in idle mode (Doze).
     * Equivalent to Android's PowerManager.isDeviceIdleMode().
     */
    val isDeviceIdleMode: Boolean

    /**
     * Returns true if the application is white-listed to ignore battery optimizations.
     * Equivalent to PowerManager.isIgnoringBatteryOptimizations().
     */
    val isIgnoringBatteryOptimizations: Boolean
}
