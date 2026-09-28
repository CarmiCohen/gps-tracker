package com.gps19.app

import com.gps19.core.engine.BootLifecycleAuthority
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [BootLifecycleAuthority].
 * Centralizes monotonic clock recovery and session validation.
 */
@Singleton
class AndroidBootLifecycleAuthority @Inject constructor() : BootLifecycleAuthority {

    private val bootId: String by lazy {
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

    override fun isSessionValid(savedBootId: String): Boolean {
        if (savedBootId.isEmpty()) return true // Fresh state, not invalid yet
        return savedBootId == bootId
    }

    override fun getCurrentBootId(): String = bootId

    override fun recoverMonotonicTime(savedRt: Long, savedBootId: String): Long {
        if (savedRt <= 0L) return 0L
        return if (isSessionValid(savedBootId)) {
            savedRt
        } else {
            0L // Boot session changed, monotonic time is no longer valid
        }
    }
}
