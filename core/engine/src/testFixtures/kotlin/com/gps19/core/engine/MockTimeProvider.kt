package com.gps19.core.engine

/**
 * MockTimeProvider: Unified temporal mock for test suites.
 * SIMP-OCT14.2-S1: Centralized implementation to avoid duplication.
 */
class MockTimeProvider(
    initialTime: Long = 1700000000000L,
    initialRt: Long = 100000L,
    private val autoAdvanceRt: Long = 0L
) : TimeProvider {
    private var _currentTimeMillis: Long = initialTime
    private var _elapsedRealtime: Long = initialRt

    override val currentTimeMillis: Long get() = _currentTimeMillis
    override val elapsedRealtime: Long 
        get() {
            val current = _elapsedRealtime
            if (autoAdvanceRt > 0) {
                advance(autoAdvanceRt)
            }
            return current
        }

    /**
     * Advances both wall-clock and realtime clocks by [ms].
     */
    fun advance(ms: Long) {
        _currentTimeMillis += ms
        _elapsedRealtime += ms
    }
    
    /**
     * Explicitly sets the current wall-clock time.
     */
    fun setCurrentTime(ms: Long) {
        _currentTimeMillis = ms
    }

    /**
     * Explicitly sets the elapsed realtime.
     */
    fun setElapsedRealtime(ms: Long) {
        _elapsedRealtime = ms
    }
}
