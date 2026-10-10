package com.gps19.core.engine

/**
 * TimeProvider: Interface for temporal authority.
 * Oct.10.2:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Migrated methods to properties 
 *   to ensure consistent state access and Hilt visibility.
 */
interface TimeProvider {
    /**
     * Returns the current wall-clock time in milliseconds.
     */
    val currentTimeMillis: Long

    /**
     * Equivalent to Android's SystemClock.elapsedRealtime().
     * Returns milliseconds since boot, including time spent in sleep.
     */
    val elapsedRealtime: Long
}
