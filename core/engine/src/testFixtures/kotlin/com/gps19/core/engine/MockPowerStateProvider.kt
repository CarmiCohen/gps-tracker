package com.gps19.core.engine

/**
 * MockPowerStateProvider: Unified power state mock for test suites.
 * Oct.18.22:
 * - ISSUE-OCT18.22-F1: Incomplete Battery Saver Signaling Policy. Added 
 *   isPowerSaveMode to match interface expansion.
 * Oct.18.16:
 * - Issue #SIMP-OCT18.16-P1: Doze Mode White-listing. Added isIgnoringBatteryOptimizations 
 *   to match interface expansion.
 */
class MockPowerStateProvider(
    private val delegate: PowerStateProvider? = null,
    initialIdle: Boolean = false,
    initialPowerSave: Boolean = false,
    initialWhitelisted: Boolean = false
) : PowerStateProvider {
    var useDelegate: Boolean = delegate != null
    var isIdle: Boolean = initialIdle
    var isPowerSave: Boolean = initialPowerSave
    var isWhitelisted: Boolean = initialWhitelisted

    override val isDeviceIdleMode: Boolean
        get() = if (useDelegate && delegate != null) delegate.isDeviceIdleMode else isIdle

    override val isPowerSaveMode: Boolean
        get() = if (useDelegate && delegate != null) delegate.isPowerSaveMode else isPowerSave

    override val isIgnoringBatteryOptimizations: Boolean
        get() = if (useDelegate && delegate != null) delegate.isIgnoringBatteryOptimizations else isWhitelisted
}
