package com.gps19.core.engine

/**
 * MockPowerStateProvider: Unified power state mock for test suites.
 * Oct.18.16:
 * - Issue #SIMP-OCT18.16-P1: Doze Mode White-listing. Added isIgnoringBatteryOptimizations 
 *   to match interface expansion.
 * SIMP-OCT14.5-S1: Centralized implementation to ensure consistency.
 */
class MockPowerStateProvider(
    private val delegate: PowerStateProvider? = null,
    initialIdle: Boolean = false,
    initialWhitelisted: Boolean = false
) : PowerStateProvider {
    var useDelegate: Boolean = delegate != null
    var isIdle: Boolean = initialIdle
    var isWhitelisted: Boolean = initialWhitelisted

    override val isDeviceIdleMode: Boolean
        get() = if (useDelegate && delegate != null) delegate.isDeviceIdleMode else isIdle

    override val isIgnoringBatteryOptimizations: Boolean
        get() = if (useDelegate && delegate != null) delegate.isIgnoringBatteryOptimizations else isWhitelisted
}
