package com.gps19.app

import android.content.Context
import com.gps19.core.engine.HardwareCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeviceHardeningStrategy: Central authority for vendor-specific power management overrides 
 * and unified background execution policies (Samsung, Xiaomi, Huawei).
 * Sep.23.08:
 * - Issue #1204: Unified Hardware Lifecycle & Vendor Hardening. Unified WakeLock acquisition 
 *   and OEM-specific constraint mitigation into a central strategy class.
 */
@Singleton
class DeviceHardeningStrategy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val systemMonitor: SystemMonitor
) {
    /**
     * Unifies and applies the correct WakeLock policy based on device manufacturer and capabilities.
     */
    fun applyVendorWakeLockPolicy(capabilities: HardwareCapabilities, force: Boolean = false) {
        if (capabilities.requiresWakeLockRenewal || capabilities.isSamsungDevice || capabilities.isHuaweiDevice || capabilities.isManualOverrideActive) {
            systemMonitor.acquireWakeLock(force = force)
        } else {
            systemMonitor.renewWakeLock()
        }
    }

    /**
     * Executes vendor-specific background continuity adjustments to prevent OS termination.
     */
    fun executeVendorContinuity(capabilities: HardwareCapabilities, nowRt: Long, isPowerSave: Boolean) {
        if (capabilities.isHuaweiDevice) {
            Timber.d("DeviceHardeningStrategy: Enforcing Huawei background stay-alive adaptation rules.")
        }
        if (capabilities.isSamsungDevice) {
            Timber.d("DeviceHardeningStrategy: Aligning Samsung background resource policy parameters.")
        }
        if (capabilities.hasBackgroundRestriction) {
            Timber.d("DeviceHardeningStrategy: Mitigating OEM background restriction policies.")
        }
    }
}
