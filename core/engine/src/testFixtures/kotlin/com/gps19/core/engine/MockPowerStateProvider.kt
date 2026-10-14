package com.gps19.core.engine

/**
 * MockPowerStateProvider: Unified power state mock for test suites.
 * SIMP-OCT14.5-S1: Centralized implementation to ensure consistency.
 */
class MockPowerStateProvider(
    private val delegate: PowerStateProvider? = null,
    initialIdle: Boolean = false
) : PowerStateProvider {
    var useDelegate: Boolean = delegate != null
    var isIdle: Boolean = initialIdle

    override val isDeviceIdleMode: Boolean
        get() = if (useDelegate && delegate != null) delegate.isDeviceIdleMode else isIdle
}
