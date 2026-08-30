package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * LogManager: Centralizes logging logic, handling local storage and remote relay emission.
 * Aug.30.13:
 * - Issue #779 Hardening: Integrated ForensicSanitizer into submitToLogSink 
 *   to ensure all persisted and emitted logs are scrubbed of forensic 
 *   metadata at the edge of the pipeline (R779).
 * Aug.22.05:
 * - Audit Chapter 12.3: Hardened startup muzzle logic to allow isSpecial logs 
 *   to bypass the suppression window.
 */
@Singleton
class LogManager @Inject constructor(
    private val logRepository: LogRepository,
    private val forensicSpillBufferProvider: Provider<ForensicSpillBuffer>,
    private val telemetry: TelemetryRepository,
    private val connectivitySuiteProvider: Provider<ConnectivitySuite>,
    private val configManager: ConfigManager,
    private val timeProvider: TimeProvider
) {
    private var sessionStartTs = 0L
    private val isOverflowLogged = AtomicBoolean(false)

    private val connectivitySuite: ConnectivitySuite by lazy(LazyThreadSafetyMode.PUBLICATION) {
        connectivitySuiteProvider.get()
    }

    fun startNewSession() {
        sessionStartTs = timeProvider.currentTimeMillis()
        isOverflowLogged.set(false)
    }

    /**
     * setForensicStallSimulation: Proxies simulation state to LogRepository (R196-V).
     */
    fun setForensicStallSimulation(active: Boolean) {
        logRepository.setForensicStallSimulation(active)
    }

    /**
     * logForensicTrace: Routes high-frequency traces to the off-heap spill-buffer.
     */
    fun logForensicTrace(message: String, lat: Double = 0.0, lng: Double = 0.0, accuracy: Double = 0.0) {
        val buffer = forensicSpillBufferProvider.get()
        val now = timeProvider.currentTimeMillis()
        
        // R779: Scrub metadata at the edge
        val sanitizedMsg = ForensicSanitizer.sanitizeMessage(message)
        val finalMsg = ForensicSanitizer.scrubHardwareInfo(sanitizedMsg, false)

        val log = LogEntry(
            localId = "", 
            timestamp = now,
            message = finalMsg,
            type = "FORENSIC_TRACE",
            isImportant = false,
            id = configManager.deviceId,
            viewerId = configManager.viewerId,
            role = if (configManager.isTrackerMode) "tracker" else "viewer",
            lat = lat,
            lng = lng,
            accuracy = accuracy
        )
        
        if (!buffer.writeTrace(log)) {
            handleOverflow()
        } else {
            if (isOverflowLogged.get() && buffer.getFillLevel() < 0.5) {
                isOverflowLogged.set(false)
            }
        }
    }

    /**
     * logForensicTraceOptimized: Zero-allocation path for 100Hz sampling.
     * Note: Optimized traces use primitive fields; message is fixed "FORENSIC_TRACE" 
     * which doesn't require sanitization.
     */
    fun logForensicTraceOptimized(
        timestamp: Long, lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double,
        vibe: Double, snr: Double, batteryLevel: Int, isCharging: Boolean, batteryTemp: Double
    ) {
        val buffer = forensicSpillBufferProvider.get()
        if (!buffer.writeTraceOptimized(
            timestamp, lat, lng, accuracy, maxAccuracy, vibe, snr, 
            batteryTemp, batteryLevel, isCharging
        )) {
            handleOverflow()
        } else {
            if (isOverflowLogged.get() && buffer.getFillLevel() < 0.5) {
                isOverflowLogged.set(false)
            }
        }
    }

    fun isForensicBufferUnderPressure(): Boolean = forensicSpillBufferProvider.get().isHighPressure()

    private fun handleOverflow() {
        if (isOverflowLogged.compareAndSet(false, true)) {
            Timber.w("Forensic Spill-Buffer Overflow! Dropping new traces until space is cleared.")
            submitToLogSink(
                message = "FORENSIC AUDIT: Spill-buffer overflow detected. Sampling inhibited to protect un-persisted data.",
                type = ALERT_ID_FORENSIC_OVERFLOW,
                isImportant = true,
                isSpecial = true,
                specialColor = FORENSIC_PINK_COLOR
            )
        }
    }

    /**
     * submitToLogSink: Unified entry for all standard system logs.
     */
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
        val now = timeProvider.currentTimeMillis()
        val health = telemetry.systemHealth.value
        
        if (health.isStorageCritical && !isSpecial) return
        if (health.isStorageLow && !isImportant && !isSpecial) return
        
        if (type == "system" && !isImportant && !isSpecial && (now - sessionStartTs < LOG_MUZZLE_STARTUP_MS)) {
            return
        }

        // R779: Forensic scrubbing of paths and hardware identifiers at the entry point.
        val sanitizedMsg = ForensicSanitizer.sanitizeMessage(message)
        val finalMsg = ForensicSanitizer.scrubHardwareInfo(sanitizedMsg, isSpecial)

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
            localId = localId ?: "", 
            timestamp = now,
            message = finalMsg,
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
            vibeSnapshot = finalVibe,
            tempSnapshot = health.batteryTemp,
            battSnapshot = health.batteryLevel,
            chargingSnapshot = health.isCharging
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
