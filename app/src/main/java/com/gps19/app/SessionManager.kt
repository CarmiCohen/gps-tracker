package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionManager: Tracks session-level state and uptime metrics.
 * Oct.4.5:
 * - Issue #1425: Unified Clock Authority. Added appStartRt for monotonic 
 *   duration checks (e.g. startup settling delay).
 * - Rename currentDropStartTs to currentDropStartRt for source clarity.
 */
@Singleton
class SessionManager @Inject constructor(
    private val repository: MainRepository,
    private val timeProvider: TimeProvider
) {
    // appStartTime remains Wall Clock for absolute audit logs and UI display.
    var appStartTime: Long = timeProvider.currentTimeMillis()
        private set

    // appStartRt is the monotonic reference for duration logic (R-ID 1425).
    var appStartRt: Long = timeProvider.elapsedRealtime()
        private set

    var lastGpsTs: Long = 0L
    var violationUptimeMs: Long = 0L
    private var totalUptimeMs: Long = 0L
    
    private var currentDropStartRt = 0L
    
    var isInViolation: Boolean = false
        private set

    private val viewerPulseMap = mutableMapOf<String, Long>()
    private val trackerPulseMap = mutableMapOf<String, Long>()

    fun updateTick(nowRt: Long, lastTickRt: Long, isPeerAvailable: Boolean, isInViolation: Boolean) {
        this.isInViolation = isInViolation
        val delta = nowRt - lastTickRt
        // R403: Use dynamic delta but fallback to standardized heartbeat constant
        val increment = if (lastTickRt > 0 && delta in 0L..3600000L) delta else TICK_INTERVAL_MS
        
        totalUptimeMs += increment
        if (isInViolation) {
            violationUptimeMs += increment
        }

        if (!isPeerAvailable && currentDropStartRt == 0L) {
            currentDropStartRt = nowRt
        } else if (isPeerAvailable && currentDropStartRt > 0L) {
            currentDropStartRt = 0L
        }
        
        cleanupOldPulses(nowRt)
    }

    fun notifyTamperCleared() {
        currentDropStartRt = 0L
    }

    fun onViewerPulse(id: String, nowRt: Long): Boolean {
        val isNew = !viewerPulseMap.containsKey(id)
        viewerPulseMap[id] = nowRt
        return isNew
    }

    fun onTrackerPulse(id: String, nowRt: Long): Boolean {
        val isNew = !trackerPulseMap.containsKey(id)
        trackerPulseMap[id] = nowRt
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
        appStartRt = timeProvider.elapsedRealtime()
        violationUptimeMs = 0L
        totalUptimeMs = 0L
        currentDropStartRt = 0L
        isInViolation = false
        viewerPulseMap.clear()
        trackerPulseMap.clear()
    }
}
