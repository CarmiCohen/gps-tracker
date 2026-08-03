package com.gps19.app

import com.gps19.core.engine.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * LogManager: Centralizes logging logic, handling local storage and remote relay emission.
 * Aug.03.37:
 * - Issue #669: Forensic Audit: Database I/O Contention. Added logForensicTrace 
 *   to utilize MappedByteBuffer spill-buffer for high-frequency diagnostics.
 * July.28.24:
 * - Issue #621: Alignment of isImportant naming for Boolean consistency.
 */
@Singleton
class LogManager @Inject constructor(
    private val logRepository: LogRepository,
    private val telemetry: TelemetryRepository,
    private val connectivitySuiteProvider: Provider<ConnectivitySuite>,
    private val configManager: ConfigManager,
    private val timeProvider: TimeProvider
) {
    private var sessionStartTs = 0L
    private val isLoggingInProgress = AtomicBoolean(false)

    private val connectivitySuite: ConnectivitySuite by lazy(LazyThreadSafetyMode.PUBLICATION) {
        connectivitySuiteProvider.get()
    }

    fun startNewSession() {
        sessionStartTs = timeProvider.currentTimeMillis()
    }

    /**
     * logForensicTrace: Routes high-frequency traces to the off-heap spill-buffer.
     * Bypasses standard log batching and SQLite hot-path to prevent Davey stalls.
     */
    fun logForensicTrace(message: String, lat: Double = 0.0, lng: Double = 0.0, accuracy: Double = 0.0) {
        val now = timeProvider.currentTimeMillis()
        val log = LogEntry(
            localId = UUID.randomUUID().toString(),
            timestamp = now,
            message = message,
            type = "FORENSIC_TRACE",
            isImportant = false,
            id = configManager.deviceId,
            viewerId = configManager.viewerId,
            role = if (configManager.isTrackerMode) "tracker" else "viewer",
            lat = lat,
            lng = lng,
            accuracy = accuracy
        )
        logRepository.addLog(log)
    }

    fun submitToLogSink(
        message: String, 
        type: String, 
        isImportant: Boolean = true, 
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
        if (!isLoggingInProgress.compareAndSet(false, true)) return
        
        try {
            val now = timeProvider.currentTimeMillis()
            val health = telemetry.systemHealth.value
            
            if (type == "hidden") return
            val isSuppressedByStorage = health.isStorageCritical && !isSpecial
            if (isSuppressedByStorage) return
            if (health.isStorageLow && !isImportant && !isSpecial) return
            if (type == "system" && !isImportant && (now - sessionStartTs < LOG_MUZZLE_STARTUP_MS)) {
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
                isImportant = isImportant,
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
            
            val suite = connectivitySuite
            val isConnected = suite.isConnected()
            
            val data = log.toJSONObject().apply {
                put("ver", BuildConfig.VERSION_NAME)
            }
            
            if (isConnected) {
                val priority = if (isImportant || isSpecial) SignalingPriority.HIGH else SignalingPriority.NORMAL
                suite.emit("log_update", data, priority)
            }
            
            logRepository.addLog(log, initiallySynced = isConnected)
        } finally {
            isLoggingInProgress.set(false)
        }
    }

    fun logServiceEvent(
        m: String, 
        isImportant: Boolean = true, 
        isSpecial: Boolean = false, 
        specialColor: Int? = null,
        lat: Double = 0.0,
        lng: Double = 0.0,
        accuracy: Double = 0.0,
        maxAccuracy: Double = 0.0,
        snr: Double? = null,
        vibe: Double? = null
    ) {
        submitToLogSink(m, "system", isImportant = isImportant, isSpecial = isSpecial, specialColor = specialColor, lat = lat, lng = lng, accuracy = accuracy, maxAccuracy = maxAccuracy, snr = snr, vibe = vibe)
    }

    fun logWatchdogPulse(set: Boolean, skipped: Int) {
        if (set) {
            submitToLogSink("Watchdog: Alarm set (skipped=$skipped)", "watchdog_stats", isImportant = false)
        }
    }

    suspend fun getUnsyncedLogs(limit: Int) = logRepository.getUnsyncedLogs(limit)
    suspend fun markLogsAsSynced(ids: List<String>) = logRepository.markLogsAsSynced(ids)
}
