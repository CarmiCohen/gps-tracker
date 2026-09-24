package com.gps19.app

import android.os.SystemClock
import com.gps19.core.engine.TimeProvider
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [TimeProvider] using SystemClock.
 * July.21.00:
 * - Hilt Hardening: Added @Inject constructor for dependency graph inclusion.
 */
@Singleton
class AndroidTimeProvider @Inject constructor() : TimeProvider {
    private val lazyBootId: String by lazy {
        try {
            val file = File("/proc/sys/kernel/random/boot_id")
            if (file.exists()) {
                file.readText().trim()
            } else {
                UUID.randomUUID().toString()
            }
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }

    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun getBootId(): String = lazyBootId
}
