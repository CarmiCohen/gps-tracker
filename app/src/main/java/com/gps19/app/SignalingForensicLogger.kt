package com.gps19.app

import androidx.annotation.VisibleForTesting
import com.gps19.core.engine.TimeProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * SignalingForensicLogger: Decouples signaling-specific forensic logging 
 * and throttling from the main ConnectivitySuite.
 * Sep.26.11:
 * - Issue #1343: Signaling Lifecycle Probes. Added throttled logging for 
 *   transmission failures and interface handovers (R-ID 334).
 * - Issue #1343: Fixed throttling logic to ensure the first event after 
 *   initialization (or reset) is always logged.
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
    private var lastTxFailureLogTs = 0L
    private var lastHandoverLogTs = 0L

    /**
     * logDrop: Throttled logging for packet rejection events (R-ID 332).
     */
    fun logDrop(tag: String, reason: String?, id: String, viewerId: String, mode: String, ownD: String, ownV: String, extra: String? = null) {
        val nowRt = timeProvider.elapsedRealtime()
        if (lastDropLogTs == 0L || nowRt - lastDropLogTs > 10000L) {
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
        if (lastHighRttLogTs == 0L || nowRt - lastHighRttLogTs > 10000L) {
            lastHighRttLogTs = nowRt
            logManagerProvider.get().submitToLogSink("High latency spike detected: RTT=$rtt ms (Threshold: $threshold)", "high_latency", isImportant = false)
        }
    }

    /**
     * logTransmissionFailure: Throttled audit of outbound signaling failures.
     */
    fun logTransmissionFailure(reason: String, mode: String, deviceId: String, viewerId: String) {
        val nowRt = timeProvider.elapsedRealtime()
        if (lastTxFailureLogTs == 0L || nowRt - lastTxFailureLogTs > 15000L) {
            lastTxFailureLogTs = nowRt
            val logMsg = "Forensic TX Failure: $reason (Mode: $mode, D:$deviceId, V:$viewerId)"
            Timber.w(logMsg)
            logManagerProvider.get().logForensicTrace(logMsg)
        }
    }

    /**
     * logHandover: Records network interface transitions in the forensic trace.
     */
    fun logHandover(status: String, interfaceName: String?) {
        val nowRt = timeProvider.elapsedRealtime()
        if (lastHandoverLogTs == 0L || nowRt - lastHandoverLogTs > 5000L) {
            lastHandoverLogTs = nowRt
            val msg = "Forensic Handover: $status (${interfaceName ?: "unknown"})"
            Timber.i(msg)
            logManagerProvider.get().logForensicTrace(msg)
        }
    }

    @VisibleForTesting
    fun resetThrottling() {
        lastDropLogTs = 0L
        lastHighRttLogTs = 0L
        lastTxFailureLogTs = 0L
        lastHandoverLogTs = 0L
    }
}
