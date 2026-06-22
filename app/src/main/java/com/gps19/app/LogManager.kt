package com.gps19.app

import com.gps19.core.engine.*
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

/**
 * LogManager: Centralizes logging logic, handling local storage and remote relay emission.
 * v8.9.19:
 * - Issue #223: Expanded logServiceEvent and submitToLogSink to support SNR and Vibration snapshots.
 * v8.9.17:
 * - Issue #214: Unified accuracy fallback logic to prioritize engine-calculated maxAccuracy 
 *   consistently when discrete fix accuracy is missing.
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
        accuracy: Float = 0f,
        snr: Float? = null,
        vibe: Float? = null
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

        // Issue 208/210/212/214: Authoritative Spatial Anchoring
        var finalLat = lat
        var finalLng = lng
        var finalAccuracy = accuracy
        
        val local = telemetry.localLocation.value
        val tracker = telemetry.trackerLocation.value
        
        // Determine the most authoritative telemetry source for this role
        val fallbackTelem = if (configManager.isTrackerMode) {
            if (local.lat != 0.0) local else tracker
        } else {
            if (tracker.lat != 0.0) tracker else local
        }

        if (finalLat == 0.0 && finalLng == 0.0) {
            // Auto-anchor to last known position
            if (fallbackTelem.lat != 0.0 && fallbackTelem.lng != 0.0) {
                finalLat = fallbackTelem.lat
                finalLng = fallbackTelem.lng
                if (finalAccuracy == 0f) {
                    // Issue #214: Consistently prioritize discrete accuracy, then maxAccuracy
                    finalAccuracy = if (fallbackTelem.accuracy > 0f) fallbackTelem.accuracy else fallbackTelem.maxAccuracy
                }
            }
        } else if (finalAccuracy == 0f) {
            // Coordinates provided but accuracy missing - use authoritative fallback
            finalAccuracy = if (fallbackTelem.accuracy > 0f) fallbackTelem.accuracy else fallbackTelem.maxAccuracy
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
            snrSnapshot = snr,
            vibeSnapshot = vibe
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
        accuracy: Float = 0f,
        snr: Float? = null,
        vibe: Float? = null
    ) {
        submitToLogSink(m, "system", important, isSpecial = isSpecial, specialColor = specialColor, lat = lat, lng = lng, accuracy = accuracy, snr = snr, vibe = vibe)
    }

    suspend fun getUnsyncedLogs(limit: Int) = logRepository.getUnsyncedLogs(limit)
    suspend fun markLogsAsSynced(ids: List<String>) = logRepository.markLogsAsSynced(ids)
}
