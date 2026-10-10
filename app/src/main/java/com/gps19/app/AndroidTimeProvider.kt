package com.gps19.app

import android.os.SystemClock
import com.gps19.core.engine.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [TimeProvider] using SystemClock.
 * Oct.10.2:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Migrated methods to properties.
 */
@Singleton
class AndroidTimeProvider @Inject constructor() : TimeProvider {
    override val elapsedRealtime: Long get() = SystemClock.elapsedRealtime()
    override val currentTimeMillis: Long get() = System.currentTimeMillis()
}
