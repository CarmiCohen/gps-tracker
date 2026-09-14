package com.gps19.app

import com.gps19.core.engine.TimeProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * SignalingForensicLogger: Decouples signaling-specific forensic logging 
 * and throttling from the main ConnectivitySuite.
 * Sep.14.47:
 * - Forensic Decoupling (#1039): Extracted drop and latency logging to 
 *   reduce ConnectivitySuite complexity (R-ID 333).
 */
@Singleton
class SignalingForensicLogger @Inject constructor(
    private val logManagerProvider: Provider<LogManager>,
    private val timeProvider: TimeProvider
) {
    private var lastDropLogTs = 0L
    private var lastHighRttLogTs = 0L

    /**
     * logDrop: Throttled logging for packet rejection events (R-ID 332).
     */
    fun logDrop(tag: String, reason: String?, id: String, viewerId: String, mode: String, ownD: String, ownV: String, extra: String? = null) {
        val nowRt = timeProvider.elapsedRealtime()
        if (nowRt - lastDropLogTs > 10000L) {
            lastDropLogTs = nowRt
            val logMsg = "Forensic drop [$tag]: reason=$reason id=$id viewerId=$viewerId${if (extra != null) " type=$extra" else ""}"
            val timberMsg = "$logMsg mode=$mode (ownD=$ownD, ownV=$ownV)"
            
            Timber.w(timberMsg)
            logManagerProvider.get().submitToLogSink(logMsg, "signaling_drop", isImportant = false)
        }
    }

    /**
     * logHighLatency: Throttled logging for RTT spikes.
     */
    fun logHighLatency(rtt: Long, threshold: Int) {
        val nowRt = timeProvider.elapsedRealtime()
        if (nowRt - lastHighRttLogTs > 10000L) {
            lastHighRttLogTs = nowRt
            logManagerProvider.get().submitToLogSink("High latency spike detected: RTT=$rtt ms (Threshold: $threshold)", "high_latency", isImportant = false)
        }
    }
}
