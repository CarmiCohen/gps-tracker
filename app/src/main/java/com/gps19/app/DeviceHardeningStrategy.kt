package com.gps19.app

import android.content.Context
import com.gps19.core.engine.HardwareCapabilities
import com.gps19.core.engine.PerformanceTier
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeviceHardeningStrategy: Central authority for vendor-specific power management overrides 
 * and unified background execution policies (Samsung, Xiaomi, Huawei).
 * Sep.24.00:
 * - Issue #1232 REMEDIATION: Converted cosmetic stubs into functional hardening logic. 
 *   Implemented Samsung/Huawei-specific WakeLock escalation and Watchdog re-alignment (R-ID 421).
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
            // Huawei requires aggressive Watchdog scheduling and forced WakeLock renewal to prevent deep sleep freezing.
            systemMonitor.acquireWakeLock(force = true)
            systemMonitor.scheduleWatchdogAlarm(force = false)
        }
        
        if (capabilities.isSamsungDevice) {
            // Samsung A15/S21FE requires periodic WakeLock renewal regardless of activity to maintain location listener priority.
            systemMonitor.renewWakeLock()
        }
        
        if (capabilities.hasBackgroundRestriction || capabilities.performanceTier == PerformanceTier.STAGGERED) {
            // For Xiaomi or restricted devices, ensure the watchdog grid is alive to prevent heuristic termination.
            if (!isPowerSave) {
                systemMonitor.scheduleWatchdogAlarm(force = false)
            }
        }
    }
}
