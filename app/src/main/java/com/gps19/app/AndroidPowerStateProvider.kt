package com.gps19.app

import android.content.Context
import android.os.PowerManager
import com.gps19.core.engine.PowerStateProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [PowerStateProvider] using PowerManager.
 * Sep.16.06:
 * - Issue #1050/1052 Test Suite Hardening: Abstracted Doze state for deterministic testing.
 */
@Singleton
class AndroidPowerStateProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : PowerStateProvider {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    override fun isDeviceIdleMode(): Boolean {
        return powerManager.isDeviceIdleMode
    }
}
