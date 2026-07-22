package com.gps19.app

import android.os.SystemClock
import com.gps19.core.engine.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [TimeProvider] using SystemClock.
 * July.21.00:
 * - Hilt Hardening: Added @Inject constructor for dependency graph inclusion.
 */
@Singleton
class AndroidTimeProvider @Inject constructor() : TimeProvider {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
