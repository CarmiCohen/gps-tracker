package com.gps19.core.engine

/**
 * LatencyMonitor: Unified framework for tracking execution durations 
 * of critical operations (JNI, DB, I/O).
 * July.29.01:
 * - Issue #623: Structural: Latency Monitor Metric Cleanup. Standardized 
 *   spike reporting and removed call-site boilerplate.
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
