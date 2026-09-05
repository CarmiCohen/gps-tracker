package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionManager: Tracks session-level state and uptime metrics.
 * Sep.05.27:
 * - Issue #918 RESOLVED: Pulse Source Consistency. Standardized onViewerPulse 
 *   and onTrackerPulse to strictly use monotonic nowRt to prevent HUD 
 *   staleness logic failures (R-ID 257).
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor and @Singleton.
 */
@Singleton
class SessionManager @Inject constructor(
    private val repository: MainRepository,
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

    fun updateTick(nowRt: Long, lastTickRt: Long, isPeerAvailable: Boolean, isInViolation: Boolean) {
        val delta = nowRt - lastTickRt
        // R403: Use dynamic delta but fallback to standardized heartbeat constant
        val increment = if (lastTickRt > 0 && delta in 0L..3600000L) delta else TICK_INTERVAL_MS
        
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

    /**
     * Issue #918: Standardized to monotonic nowRt to ensure HUD staleness accuracy.
     */
    fun onViewerPulse(id: String, nowRt: Long): Boolean {
        val isNew = !viewerPulseMap.containsKey(id)
        viewerPulseMap[id] = nowRt
        return isNew
    }

    /**
     * Issue #918: Standardized to monotonic nowRt to ensure HUD staleness accuracy.
     */
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
        violationUptimeMs = 0L
        totalUptimeMs = 0L
        currentDropStartTs = 0L
        viewerPulseMap.clear()
        trackerPulseMap.clear()
    }
}
