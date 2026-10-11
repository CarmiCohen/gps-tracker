package com.gps19.core.engine

import java.util.concurrent.atomic.AtomicLong

/**
 * LatencyMonitor: Unified framework for tracking execution durations 
 * of critical operations (JNI, DB, I/O).
 * Oct.10.11:
 * - Issue #SIMP-1011-6: Mali Forensic Audit. Added maxJniLatency tracking 
 *   to identify native bridge stalls correlated with GPU anomalies.
 * Oct.10.2:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Migrated to TimeProvider 
 *   property-based API.
 * Aug.10.26:
 * - Issue #131: Forensic Performance Audit. Added rolling max latency tracking 
 *   to support forensic trend analysis on budget hardware (A15).
 */
object LatencyMonitor {

    private val maxIoLatency = AtomicLong(0)
    private val maxJniLatency = AtomicLong(0)

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
        val start = timeProvider.elapsedRealtime
        val result = block()
        val duration = timeProvider.elapsedRealtime - start
        
        if (type == AuditType.IO) {
            updateMaxIo(duration)
        } else if (type == AuditType.PERFORMANCE && operation.contains("Native", ignoreCase = true)) {
            updateMaxJni(duration)
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

    fun updateMaxJni(duration: Long) {
        var currentMax: Long
        do {
            currentMax = maxJniLatency.get()
            if (duration <= currentMax) break
        } while (!maxJniLatency.compareAndSet(currentMax, duration))
    }

    /**
     * Returns the maximum IO latency recorded since the last consume call.
     */
    fun consumeMaxIoLatency(): Long {
        return maxIoLatency.getAndSet(0)
    }

    /**
     * Returns the maximum JNI latency recorded since the last consume call.
     */
    fun consumeMaxJniLatency(): Long {
        return maxJniLatency.getAndSet(0)
    }
}
