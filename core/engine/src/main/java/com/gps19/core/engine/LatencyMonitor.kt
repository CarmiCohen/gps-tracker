package com.gps19.core.engine

/**
 * LatencyMonitor: Unified framework for tracking execution durations 
 * of critical operations (JNI, DB, I/O).
 * July.25.11:
 * - Issue #590: Created to detect "silent jitter" in high-frequency paths.
 */
object LatencyMonitor {
    /**
     * Measures the execution time of [block] and invokes [onSpike] if the duration 
     * exceeds [thresholdMs].
     */
    inline fun <T> measure(
        timeProvider: TimeProvider,
        thresholdMs: Long,
        onSpike: (duration: Long) -> Unit,
        block: () -> T
    ): T {
        val start = timeProvider.elapsedRealtime()
        val result = block()
        val duration = timeProvider.elapsedRealtime() - start
        
        if (duration > thresholdMs) {
            onSpike(duration)
        }
        return result
    }
}
