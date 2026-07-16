package com.gps19.app

import android.os.SystemClock
import com.gps19.core.engine.TimeProvider

/**
 * Android-specific implementation of [TimeProvider] using SystemClock.
 * v9.5.0: Hilt removed. Manual DI.
 */
class AndroidTimeProvider : TimeProvider {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
