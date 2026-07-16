package com.gps19.app

import com.gps19.core.engine.*
import java.util.UUID

/**
 * LogManager: Centralizes logging logic, handling local storage and remote relay emission.
 * v9.5.0: Issue #513 - Flatten Service Architecture (ConnectivitySuite).
 */
class LogManager(
    private val logRepository: LogRepository,
    private val telemetry: TelemetryRepository,
    private val connectivitySuite: () -> ConnectivitySuite,
    private val configManager: ConfigManager,
    private val timeProvider: TimeProvider
) {
    private var sessionStartTs = 0L

    fun startNewSession() {
        sessionStartTs = timeProvider.currentTimeMillis()
    }

    fun submitToLogSink(
        message: String, 
        type: String, 
        important: Boolean = true, 
        extremeValue: Double? = null, 
        localId: String? = null,
        durationMs: Long = 0L,
        isSpecial: Boolean = false,
        specialColor: Int? = null,
        lat: Double = 0.0,
        lng: Double = 0.0,
        accuracy: Double = 0.0,
        maxAccuracy: Double = 0.0,
        snr: Double? = null,
        vibe: Double? = null
    ) {
        val now = timeProvider.currentTimeMillis()
        val integrity = telemetry.integrityState.value
        
        if (type == "hidden") return

        val isSuppressedByStorage = integrity.isStorageCritical && !isSpecial
        if (isSuppressedByStorage) return

        if (integrity.isStorageLow && !important && !isSpecial) return

        if (type == "system" && !important && (now - sessionStartTs < LOG_MUZZLE_STARTUP_MS)) {
            return
        }

        var finalLat = lat
        var finalLng = lng
        var finalAccuracy = accuracy
        var finalMaxAccuracy = maxAccuracy
        var finalSnr = snr
        var finalVibe = vibe
        
        val local = telemetry.localLocation.value
        val tracker = telemetry.trackerLocation.value
        
        val fallbackTelem = if (configManager.isTrackerMode) {
            if (local.lat != 0.0) local else tracker
        } else {
            if (tracker.lat != 0.0) tracker else local
        }

        if (finalLat == 0.0 && finalLng == 0.0) {
            if (fallbackTelem.lat != 0.0 && fallbackTelem.lng != 0.0) {
                finalLat = fallbackTelem.lat
                finalLng = fallbackTelem.lng
                finalAccuracy = fallbackTelem.accuracy
                finalMaxAccuracy = fallbackTelem.maxAccuracy
            }
        } else {
            if (finalAccuracy == 0.0 && finalLat == fallbackTelem.lat) {
                finalAccuracy = fallbackTelem.accuracy
            }
            if (finalMaxAccuracy == 0.0 && finalLat == fallbackTelem.lat) {
                finalMaxAccuracy = fallbackTelem.maxAccuracy
            }
        }

        if (finalVibe == null && (fallbackTelem.vibration ?: 0.0) > 0.0) {
            finalVibe = fallbackTelem.vibration
        }

        val log = LogEntry(
            localId = localId ?: UUID.randomUUID().toString(),
            timestamp = now,
            message = message,
            type = type,
            isImportant = important,
            id = configManager.deviceId,
            viewerId = configManager.viewerId,
            extremeValue = extremeValue,
            durationMs = durationMs,
            isSpecial = isSpecial,
            specialColor = specialColor,
            role = if (configManager.isTrackerMode) "tracker" else "viewer",
            lat = finalLat,
            lng = finalLng,
            accuracy = finalAccuracy,
            maxAccuracy = finalMaxAccuracy,
            snrSnapshot = finalSnr,
            vibeSnapshot = finalVibe
        )
        
        val suite = connectivitySuite()
        val isConnected = suite.isConnected()
        
        val data = log.toJSONObject().apply {
            put("ver", BuildConfig.VERSION_NAME)
        }
        
        if (isConnected) {
            suite.emit("log_update", data)
        }
        
        logRepository.addLog(log, initiallySynced = isConnected)
    }

    fun logServiceEvent(
        m: String, 
        important: Boolean = true, 
        isSpecial: Boolean = false, 
        specialColor: Int? = null,
        lat: Double = 0.0,
        lng: Double = 0.0,
        accuracy: Double = 0.0,
        maxAccuracy: Double = 0.0,
        snr: Double? = null,
        vibe: Double? = null
    ) {
        submitToLogSink(m, "system", important, isSpecial = isSpecial, specialColor = specialColor, lat = lat, lng = lng, accuracy = accuracy, maxAccuracy = maxAccuracy, snr = snr, vibe = vibe)
    }

    fun logWatchdogPulse(set: Boolean, skipped: Int) {
        if (set) {
            submitToLogSink("Watchdog: Alarm set (skipped=$skipped)", "watchdog_stats", important = false)
        }
    }

    suspend fun getUnsyncedLogs(limit: Int) = logRepository.getUnsyncedLogs(limit)
    suspend fun markLogsAsSynced(ids: List<String>) = logRepository.markLogsAsSynced(ids)
}
