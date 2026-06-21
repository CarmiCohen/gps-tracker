package com.gps19.app

import com.gps19.core.engine.*
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

/**
 * LogManager: Centralizes logging logic, handling local storage and remote relay emission.
 * v8.9.14:
 * - Issue #212: Hardened accuracy fallback logic in submitToLogSink to ensure consistency.
 * v8.9.12:
 * - Issue #210: Standardized logServiceEvent by removing redundant coordinate parameters.
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
        lng: Double = 0.0,
        accuracy: Float = 0f
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

        // Issue 208/210/212: Auto-anchor logs to the last known location if not provided
        var finalLat = lat
        var finalLng = lng
        var finalAccuracy = accuracy
        
        if (finalLat == 0.0 && finalLng == 0.0) {
            val local = telemetry.localLocation.value
            val tracker = telemetry.trackerLocation.value
            
            val fallback = if (configManager.isTrackerMode) {
                if (local.lat != 0.0) local else tracker
            } else {
                if (tracker.lat != 0.0) tracker else local
            }
            
            if (fallback.lat != 0.0 && fallback.lng != 0.0) {
                finalLat = fallback.lat
                finalLng = fallback.lng
                if (finalAccuracy == 0f) {
                    finalAccuracy = if (fallback.accuracy > 0f) fallback.accuracy else fallback.maxAccuracy
                }
            }
        } else if (finalAccuracy == 0f) {
            // Coordinate provided but accuracy missing - attempt telemetry lookup
            val local = telemetry.localLocation.value
            val tracker = telemetry.trackerLocation.value
            val fallback = if (configManager.isTrackerMode) local else tracker
            if (fallback.accuracy > 0f) finalAccuracy = fallback.accuracy
            else if (fallback.maxAccuracy > 0f) finalAccuracy = fallback.maxAccuracy
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
            accuracy = finalAccuracy
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

    fun logServiceEvent(
        m: String, 
        important: Boolean = true, 
        isSpecial: Boolean = false, 
        specialColor: Int? = null,
        lat: Double = 0.0,
        lng: Double = 0.0,
        accuracy: Float = 0f
    ) {
        submitToLogSink(m, "system", important, isSpecial = isSpecial, specialColor = specialColor, lat = lat, lng = lng, accuracy = accuracy)
    }

    suspend fun getUnsyncedLogs(limit: Int) = logRepository.getUnsyncedLogs(limit)
    suspend fun markLogsAsSynced(ids: List<String>) = logRepository.markLogsAsSynced(ids)
}
