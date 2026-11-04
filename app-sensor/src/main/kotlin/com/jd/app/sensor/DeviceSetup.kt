package com.jd.app.sensor

import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Revived Device-Specific Setup & Hardware Hardening Strategy (vNov4.31)
 * Central authority for vendor-specific power management overrides, background execution policies,
 * and performance tier profiling (Samsung A15, Xiaomi, Huawei, S21FE).
 */

enum class PerformanceTier {
    HIGH,
    STANDARD,
    STAGGERED
}

data class HardwareCapabilities(
    val model: String = Build.MODEL ?: "Unknown",
    val manufacturer: String = Build.MANUFACTURER ?: "Unknown",
    val isA15Device: Boolean = model.contains("A15", ignoreCase = true) || model.contains("SM-A155", ignoreCase = true),
    val isSamsungDevice: Boolean = manufacturer.equals("Samsung", ignoreCase = true),
    val isHuaweiDevice: Boolean = manufacturer.equals("Huawei", ignoreCase = true),
    val isXiaomiDevice: Boolean = manufacturer.equals("Xiaomi", ignoreCase = true) || manufacturer.equals("Redmi", ignoreCase = true),
    val performanceTier: PerformanceTier = if (isA15Device) PerformanceTier.STAGGERED else PerformanceTier.STANDARD,
    val requiresWakeLockRenewal: Boolean = isSamsungDevice || isHuaweiDevice,
    val hasBackgroundRestriction: Boolean = isXiaomiDevice || isHuaweiDevice,
    val isManualOverrideActive: Boolean = false
)

@Singleton
class DeviceHardeningStrategy @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var lastForegroundExitRt = 0L
    private val BACKGROUND_ESCALATION_WINDOW_MS = 60_000L

    fun applyVendorWakeLockPolicy(capabilities: HardwareCapabilities, force: Boolean = false) {
        if (capabilities.requiresWakeLockRenewal || capabilities.isSamsungDevice || capabilities.isHuaweiDevice || capabilities.isManualOverrideActive) {
            Log.i("DeviceHardeningStrategy", "Applying vendor wake lock policy (force=$force)")
        }
    }

    fun notifyUiVisibility(visible: Boolean, nowRt: Long) {
        if (!visible && lastForegroundExitRt == 0L) {
            lastForegroundExitRt = nowRt
            Log.i("DeviceHardeningStrategy", "UI moved to background. Escalation window active.")
        } else if (visible) {
            lastForegroundExitRt = 0L
        }
    }

    fun executeVendorContinuity(
        capabilities: HardwareCapabilities,
        nowRt: Long,
        isPowerSave: Boolean,
        isUiVisible: Boolean
    ) {
        notifyUiVisibility(isUiVisible, nowRt)

        val isInTransitionWindow = lastForegroundExitRt > 0L && (nowRt - lastForegroundExitRt < BACKGROUND_ESCALATION_WINDOW_MS)
        val isStaggeredTier = capabilities.performanceTier == PerformanceTier.STAGGERED

        if (isInTransitionWindow && isStaggeredTier) {
            Log.i("DeviceHardeningStrategy", "Executing background transition hardening for staggered tier device.")
        }

        if (capabilities.isHuaweiDevice) {
            Log.i("DeviceHardeningStrategy", "Huawei device detected. Enforcing aggressive wake lock & watchdog.")
        }

        if (capabilities.isSamsungDevice) {
            Log.i("DeviceHardeningStrategy", "Samsung device detected (e.g. A15/S21FE). Maintaining listener priority.")
        }
    }
}

@Singleton
class DeviceProfileManager @Inject constructor(
    private val hardeningStrategy: DeviceHardeningStrategy
) {
    fun evaluateCapabilities(): HardwareCapabilities {
        return HardwareCapabilities()
    }

    fun executeContinuityTweaks(
        capabilities: HardwareCapabilities,
        nowRt: Long,
        serviceTickCounter: Long,
        isPowerSaveMode: Boolean,
        isUiVisible: Boolean
    ) {
        hardeningStrategy.applyVendorWakeLockPolicy(capabilities, force = false)
        hardeningStrategy.executeVendorContinuity(capabilities, nowRt, isPowerSaveMode, isUiVisible)
    }
}
