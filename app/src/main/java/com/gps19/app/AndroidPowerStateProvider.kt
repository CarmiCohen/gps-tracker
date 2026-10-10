package com.gps19.app

import android.content.Context
import android.os.PowerManager
import com.gps19.core.engine.PowerStateProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [PowerStateProvider] using PowerManager.
 * Oct.10.2:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Migrated isDeviceIdleMode to property.
 */
@Singleton
class AndroidPowerStateProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : PowerStateProvider {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    override val isDeviceIdleMode: Boolean
        get() = powerManager.isDeviceIdleMode
}
