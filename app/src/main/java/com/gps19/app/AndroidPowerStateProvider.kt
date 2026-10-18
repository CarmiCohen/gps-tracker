package com.gps19.app

import android.content.Context
import android.os.PowerManager
import com.gps19.core.engine.PowerStateProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [PowerStateProvider] using PowerManager.
 * Oct.18.22:
 * - ISSUE-OCT18.22-F1: Incomplete Battery Saver Signaling Policy. Implemented 
 *   isPowerSaveMode for Battery Saver awareness.
 * Oct.18.16:
 * - Issue #SIMP-OCT18.16-P1: Doze Mode White-listing. Implemented 
 *   isIgnoringBatteryOptimizations check for high-persistence signaling.
 */
@Singleton
class AndroidPowerStateProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : PowerStateProvider {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    override val isDeviceIdleMode: Boolean
        get() = powerManager.isDeviceIdleMode

    override val isPowerSaveMode: Boolean
        get() = powerManager.isPowerSaveMode

    override val isIgnoringBatteryOptimizations: Boolean
        get() = powerManager.isIgnoringBatteryOptimizations(GpsApplication.PACKAGE_NAME)
}
