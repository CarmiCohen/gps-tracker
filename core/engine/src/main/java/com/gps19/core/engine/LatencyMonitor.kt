package com.gps19.core.engine

/**
 * LatencyMonitor: Unified framework for tracking execution durations 
 * of critical operations (JNI, DB, I/O).
 * July.29.22:
 * - Issue #623: Structural: Latency Monitor Metric Cleanup. Finalized migration.
 *   Standardized spike reporting and removed deprecated measure() API.
 */
object LatencyMonitor {

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
        
        if (duration > thresholdMs) {
            onSpike("${type.label}: $operation spike (${duration}ms > ${thresholdMs}ms)", duration)
        }
        return result
    }
}
