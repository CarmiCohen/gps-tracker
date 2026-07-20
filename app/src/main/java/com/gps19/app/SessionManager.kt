package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionManager: Tracks session-level state and uptime metrics.
 * v9.4.00:
 * - Issue #102: Temporal Forensic Integrity. Standardized monotonic timestamp 
 *   parameter naming to 'nowRt'.
 */
@Singleton
class SessionManager @Inject constructor(
    private val repository: MainRepository,
    @ApplicationScope private val scope: CoroutineScope,
    private val logManager: LogManager,
    private val timeProvider: TimeProvider
) {
    var appStartTime: Long = timeProvider.currentTimeMillis()
        private set

    var lastGpsTs: Long = 0L
    var violationUptimeMs: Long = 0L
    private var totalUptimeMs: Long = 0L
    
    private var currentDropStartTs = 0L

    private val viewerPulseMap = mutableMapOf<String, Long>()
    private val trackerPulseMap = mutableMapOf<String, Long>()

    fun updateTick(nowRt: Long, lastTickTs: Long, isPeerAvailable: Boolean, isInViolation: Boolean) {
        val delta = nowRt - lastTickTs
        // R403: Use dynamic delta but fallback to standardized heartbeat constant
        val increment = if (lastTickTs > 0 && delta in 0L..3600000L) delta else TICK_INTERVAL_MS
        
        totalUptimeMs += increment
        if (isInViolation) {
            violationUptimeMs += increment
        }

        if (!isPeerAvailable && currentDropStartTs == 0L) {
            currentDropStartTs = nowRt
        } else if (isPeerAvailable && currentDropStartTs > 0L) {
            currentDropStartTs = 0L
        }
        
        cleanupOldPulses(nowRt)
    }

    fun notifyTamperCleared() {
        currentDropStartTs = 0L
    }

    fun onViewerPulse(id: String, ts: Long, isRealtime: Boolean): Boolean {
        val isNew = !viewerPulseMap.containsKey(id)
        viewerPulseMap[id] = if (isRealtime) ts else timeProvider.elapsedRealtime()
        return isNew
    }

    fun onTrackerPulse(id: String, ts: Long, isRealtime: Boolean): Boolean {
        val isNew = !trackerPulseMap.containsKey(id)
        trackerPulseMap[id] = if (isRealtime) ts else timeProvider.elapsedRealtime()
        return isNew
    }

    fun getViewerCount(): Int = viewerPulseMap.size
    fun getTrackerCount(): Int = trackerPulseMap.size

    fun getViolationPercentage(): Double {
        if (totalUptimeMs == 0L) return 0.0
        return (violationUptimeMs.toDouble() / totalUptimeMs.toDouble()) * 100.0
    }

    private fun cleanupOldPulses(nowRt: Long) {
        val itV = viewerPulseMap.entries.iterator()
        while (itV.hasNext()) {
            if (nowRt - itV.next().value > WATCH_TIMEOUT_MS) itV.remove()
        }
        val itT = trackerPulseMap.entries.iterator()
        while (itT.hasNext()) {
            if (nowRt - itT.next().value > WATCH_TIMEOUT_MS) itT.remove()
        }
    }

    fun reset() {
        appStartTime = timeProvider.currentTimeMillis()
        violationUptimeMs = 0L
        totalUptimeMs = 0L
        currentDropStartTs = 0L
        viewerPulseMap.clear()
        trackerPulseMap.clear()
    }
}
