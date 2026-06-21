package com.gps19.app

import com.gps19.core.engine.*
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

/**
 * LogManager: Centralizes logging logic, handling local storage and remote relay emission.
 * v8.9.10:
 * - Issue 208: Implemented coordinate auto-population. Logs now fallback to the last known 
 *   telemetry position if not explicitly provided, ensuring all events are spatially anchored.
 * v8.9.9:
 * - Issue 208: Fixed Duplicate Log Sync. Logs emitted in real-time are now marked as 'synced'.
 */
@Singleton
class LogManager @Inject constructor(
    private val logRepository: LogRepository,
    private val telemetry: TelemetryRepository,
    private val networkManager: Lazy<AppNetworkManager>,
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
        lng: Double = 0.0
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

        // Issue 208: Auto-anchor logs to the last known location if not provided
        var finalLat = lat
        var finalLng = lng
        if (finalLat == 0.0 && finalLng == 0.0) {
            val lastLoc = if (configManager.isTrackerMode) {
                telemetry.localLocation.value
            } else {
                telemetry.trackerLocation.value
            }
            if (lastLoc.lat != 0.0 && lastLoc.lng != 0.0) {
                finalLat = lastLoc.lat
                finalLng = lastLoc.lng
            }
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
            lng = finalLng
        )
        
        val net = networkManager.get()
        val isConnected = net.isConnected()
        
        val data = log.toJSONObject().apply {
            put("ver", BuildConfig.VERSION_NAME)
        }
        
        if (isConnected) {
            net.emit("log_update", data)
        }
        
        logRepository.addLog(log, initiallySynced = isConnected)
    }

    fun logServiceEvent(m: String, important: Boolean = true, isSpecial: Boolean = false, specialColor: Int? = null, lat: Double = 0.0, lng: Double = 0.0) {
        submitToLogSink(m, "system", important, isSpecial = isSpecial, specialColor = specialColor, lat = lat, lng = lng)
    }

    suspend fun getUnsyncedLogs(limit: Int) = logRepository.getUnsyncedLogs(limit)
    suspend fun markLogsAsSynced(ids: List<String>) = logRepository.markLogsAsSynced(ids)
}
