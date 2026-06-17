package com.gps19.core.engine

/**
 * Interface to provide time values to the engine, allowing for deterministic
 * testing and removing Android framework dependencies.
 */
interface TimeProvider {
    /**
     * Returns milliseconds since boot, including time spent in sleep.
     * Equivalent to Android's SystemClock.elapsedRealtime().
     */
    fun elapsedRealtime(): Long

    /**
     * Returns the current wall-clock time in milliseconds.
     * Equivalent to System.currentTimeMillis().
     */
    fun currentTimeMillis(): Long
}
