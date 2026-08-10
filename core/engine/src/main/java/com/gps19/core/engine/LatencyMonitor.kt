package com.gps19.core.engine

import java.util.concurrent.atomic.AtomicLong

/**
 * LatencyMonitor: Unified framework for tracking execution durations 
 * of critical operations (JNI, DB, I/O).
 * Aug.10.26:
 * - Issue #131: Forensic Performance Audit. Added rolling max latency tracking 
 *   to support forensic trend analysis on budget hardware (A15).
 * July.29.22:
 * - Issue #623: Structural: Latency Monitor Metric Cleanup. Standardized spike 
 *   reporting and removed deprecated measure() API.
 */
object LatencyMonitor {

    private val maxIoLatency = AtomicLong(0)

    /**
     * AuditType: Classification of the operation being monitored for R623 compliance.
     */
    enum class AuditType(val label: String) {
        PERFORMANCE("Forensic Performance Audit"),
        IO("Forensic I/O Audit")
    }

    /**
     * Standardized spike reporting helper to ensure consistent naming conventions.
     * Reduces boilerplate by constructing the forensic message internally.
     * Invokes [onSpike] with both a formatted [message] and the raw [duration].
     */
    inline fun <T> measureAndAudit(
        timeProvider: TimeProvider,
        thresholdMs: Long,
        operation: String,
        type: AuditType,
        onSpike: (message: String, duration: Long) -> Unit,
        block: () -> T
    ): T {
        val start = timeProvider.elapsedRealtime()
        val result = block()
        val duration = timeProvider.elapsedRealtime() - start
        
        if (type == AuditType.IO) {
            updateMaxIo(duration)
        }

        if (duration > thresholdMs) {
            onSpike("${type.label}: $operation spike (${duration}ms > ${thresholdMs}ms)", duration)
        }
        return result
    }

    fun updateMaxIo(duration: Long) {
        var currentMax: Long
        do {
            currentMax = maxIoLatency.get()
            if (duration <= currentMax) break
        } while (!maxIoLatency.compareAndSet(currentMax, duration))
    }

    /**
     * Returns the maximum IO latency recorded since the last consume call.
     */
    fun consumeMaxIoLatency(): Long {
        return maxIoLatency.getAndSet(0)
    }
}
