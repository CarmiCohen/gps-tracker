package com.gps19.core.engine

/**
 * TimeProvider: Interface for temporal authority.
 */
interface TimeProvider {
    fun currentTimeMillis(): Long
    /**
     * Equivalent to Android's SystemClock.elapsedRealtime().
     * Returns milliseconds since boot, including time spent in sleep.
     */
    fun elapsedRealtime(): Long

    /**
     * Returns a unique ID for the current boot session.
     * Used for monotonic latch validation across reboots.
     */
    fun getBootId(): String = "default_boot"
}
