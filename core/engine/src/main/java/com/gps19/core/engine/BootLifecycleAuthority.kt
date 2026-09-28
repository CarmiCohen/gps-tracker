package com.gps19.core.engine

/**
 * BootLifecycleAuthority: Central authority for monotonic clock recovery and 
 * boot session validation.
 */
interface BootLifecycleAuthority {
    /**
     * Validates if a saved boot ID matches the current system session.
     * Returns true if the session is identical, false if a reboot occurred.
     */
    fun isSessionValid(savedBootId: String): Boolean

    /**
     * Returns the current unique boot session identifier.
     */
    fun getCurrentBootId(): String

    /**
     * Recovers a monotonic timestamp (elapsedRealtime).
     * If the session is invalid (reboot occurred), returns 0.
     */
    fun recoverMonotonicTime(savedRt: Long, savedBootId: String): Long
}
