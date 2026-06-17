package com.gps19.app

import android.util.Log
import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionManager: Manages persistence counters like uptime, connection metrics, 
 * and viewer tracking.
 * v8.8.21: Migrated to TimeProvider for all timing logic to ensure system-wide consistency.
 */
@Singleton
class SessionManager @Inject constructor(
    private val repository: MainRepository,
    @ApplicationScope private val scope: CoroutineScope,
    private val logManager: LogManager,
    private val timeProvider: TimeProvider
) {
    var totalConnectedMs = 0L
    var sessionConnectedMs = 0L
    var uptimeMs = 0L
    var lastConnectionTs = 0L
    var lastDisconnectionTs = 0L
    var totalDropMs = 0L
    var maxDropMs = 0L
    var maxDropTs = 0L
    var lastGpsTs = 0L
    var appStartTime = 0L
    
    var violationUptimeMs = 0L
    
    var sessionFirstTrackerContactTs = 0L

    private var currentDropStartTs = 0L

    private val connectedViewers = ConcurrentHashMap<String, Long>()
    private var lastSaveTs = 0L

    private val sessionExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("GPS19_SESSION", "CRITICAL: Session persistence error: ${throwable.message}")
        logManager.logServiceEvent("CRITICAL: Session persistence error: ${throwable.message}", true)
    }

    init {
        loadFromRepository()
    }

    private fun loadFromRepository() {
        scope.launch(sessionExceptionHandler) {
            uptimeMs = repository.getLong(MainRepository.UPTIME_KEY, 0L)
            totalConnectedMs = repository.getLong(MainRepository.TOTAL_CONNECTED_KEY, 0L)
            lastConnectionTs = repository.getLong(MainRepository.LAST_CONNECTION_TS_KEY, 0L)
            lastDisconnectionTs = repository.getLong(MainRepository.LAST_DISCONNECTION_TS_KEY, 0L)
            totalDropMs = repository.getLong(MainRepository.TOTAL_DROP_KEY, 0L)
            maxDropMs = repository.getLong(MainRepository.MAX_DROP_KEY, 0L)
            maxDropTs = repository.getLong(MainRepository.MAX_DROP_TS_KEY, 0L)
            lastGpsTs = repository.getLong(MainRepository.LAST_GPS_TS_KEY, 0L)
            violationUptimeMs = repository.getLong(MainRepository.VIOLATION_UPTIME_MS_KEY, 0L)
            appStartTime = repository.getAppStartTime()

            if (appStartTime == 0L) {
                appStartTime = timeProvider.currentTimeMillis()
                repository.setAppStartTime(appStartTime)
            }
            updateRepositoryViewers()
        }
    }

    fun updateRepositoryViewers() {
        repository.updateConnectedViewers(connectedViewers.keys.toList())
    }

    /**
     * updateTick: Updates session durations.
     * v8.8.2: 'now' and 'lastTickTs' MUST be monotonic (SystemClock.elapsedRealtime()).
     */
    fun updateTick(now: Long, lastTickTs: Long, isPeerAvailable: Boolean, isInViolation: Boolean = false) {
        val delta = now - lastTickTs
        val increment = if (lastTickTs > 0 && delta in 0L..3600000L) delta else 1000L

        uptimeMs += increment

        if (isInViolation) {
            violationUptimeMs += increment
        }

        if (isPeerAvailable) {
            totalConnectedMs += increment
            sessionConnectedMs += increment
            
            if (currentDropStartTs > 0) {
                val dropDuration = now - currentDropStartTs
                totalDropMs += dropDuration
                if (dropDuration > maxDropMs) {
                    maxDropMs = dropDuration
                    maxDropTs = timeProvider.currentTimeMillis() // Store wall-clock for UI timestamping
                }
                currentDropStartTs = 0L
            }
        } else {
            sessionConnectedMs = 0L
            if (currentDropStartTs == 0L) {
                currentDropStartTs = now
            }
        }
    }

    fun getViolationPercentage(): Float {
        if (uptimeMs <= 0) return 0f
        return (violationUptimeMs.toFloat() / uptimeMs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * get...WithActive: Functions to query current metrics including ongoing drops.
     * 'now' MUST be monotonic time.
     */
    fun getTotalDropWithActive(now: Long): Long {
        if (currentDropStartTs == 0L) return totalDropMs
        return totalDropMs + (now - currentDropStartTs)
    }

    fun getMaxDropWithActive(now: Long): Long {
        if (currentDropStartTs == 0L) return maxDropMs
        val currentDrop = now - currentDropStartTs
        return maxOf(maxDropMs, currentDrop)
    }

    fun getMaxDropTsWithActive(now: Long): Long {
        if (currentDropStartTs == 0L) return maxDropTs
        val currentDrop = now - currentDropStartTs
        return if (currentDrop > maxDropMs) timeProvider.currentTimeMillis() else maxDropTs
    }

    fun notifyTamperCleared() {
        if (currentDropStartTs > 0) {
            val now = timeProvider.elapsedRealtime()
            val dropDuration = now - currentDropStartTs
            totalDropMs += dropDuration
            if (dropDuration > maxDropMs) {
                maxDropMs = dropDuration
                maxDropTs = timeProvider.currentTimeMillis()
            }
            currentDropStartTs = 0L
            persist(force = true)
        }
    }

    fun onViewerPulse(id: String, nowWall: Long, isTrackerMode: Boolean): Boolean {
        val trimmedId = id.trim()
        if (trimmedId.isEmpty()) return false
        
        val isNew = !connectedViewers.containsKey(trimmedId)
        if (isNew) {
            if (isTrackerMode && connectedViewers.isEmpty()) {
                lastConnectionTs = nowWall
                repository.saveLongSync(MainRepository.LAST_CONNECTION_TS_KEY, lastConnectionTs)
            }
        }
        connectedViewers[trimmedId] = nowWall
        updateRepositoryViewers()
        return isNew
    }

    fun onTrackerPulse(id: String, nowWall: Long, isTrackerMode: Boolean): Boolean {
        val trimmedId = id.trim()
        if (trimmedId.isEmpty()) return false
        
        val isNew = !connectedViewers.containsKey(trimmedId)
        if (isNew) {
            if (!isTrackerMode && sessionFirstTrackerContactTs == 0L) {
                sessionFirstTrackerContactTs = nowWall
            }
        }
        connectedViewers[trimmedId] = nowWall
        updateRepositoryViewers()
        return isNew
    }

    fun getViewerCount() = connectedViewers.size

    fun persist(force: Boolean = false) {
        val now = timeProvider.elapsedRealtime()
        if (force || now - lastSaveTs > 10000L) {
            scope.launch(sessionExceptionHandler) {
                repository.saveSessionMetricsBulk(
                    totalConnected = totalConnectedMs,
                    uptime = uptimeMs,
                    totalDrop = totalDropMs,
                    maxDrop = maxDropMs,
                    maxDropTs = maxDropTs,
                    lastGpsTs = lastGpsTs,
                    violationUptimeMs = violationUptimeMs
                )
                lastSaveTs = now
            }
        }
    }

    fun reset() {
        totalConnectedMs = 0L; sessionConnectedMs = 0L
        uptimeMs = 0L; lastConnectionTs = 0L; lastDisconnectionTs = 0L
        totalDropMs = 0L; maxDropMs = 0L; maxDropTs = 0L; lastGpsTs = 0L
        violationUptimeMs = 0L
        currentDropStartTs = 0L
        connectedViewers.clear()
        appStartTime = timeProvider.currentTimeMillis()
        sessionFirstTrackerContactTs = 0L
        updateRepositoryViewers()
    }
}
