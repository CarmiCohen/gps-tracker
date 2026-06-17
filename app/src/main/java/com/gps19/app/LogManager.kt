package com.gps19.app

import com.gps19.core.engine.TimeProvider
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

/**
 * LogManager: Centralizes logging logic, handling both local repository 
 * storage and remote relay emission.
 * v8.8.22:
 * - Aligned with SoT: now uses global LOG_MUZZLE_STARTUP_MS constant.
 * v8.8.23: Standardized all thresholds with Requirements SoT.
 * v8.8.25: Timing Standardization - Migrated to TimeProvider.
 * v8.8.26: Issue 114 - Forensic vid propagation in submitToLogSink.
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
        specialColor: Int? = null
    ) {
        val now = timeProvider.currentTimeMillis()
        val integrity = telemetry.integrityState.value
        
        if (type == "hidden") return

        // R892/STORAGE: Critical Gate Audit - Ensure isSpecial logs bypass storage suppression
        val isSuppressedByStorage = integrity.isStorageCritical && !isSpecial
        if (isSuppressedByStorage) return

        if (integrity.isStorageLow && !important && !isSpecial) return

        if (type == "system" && !important && (now - sessionStartTs < LOG_MUZZLE_STARTUP_MS)) {
            return
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
            specialColor = specialColor
        )
        
        val data = log.toJSONObject().apply {
            put("ver", BuildConfig.VERSION_NAME)
        }
        networkManager.get().emit("log_update", data)
        
        // Final local persistence gate (already validated by isSuppressedByStorage)
        logRepository.addLog(log)
    }

    fun logServiceEvent(m: String, important: Boolean = true, isSpecial: Boolean = false, specialColor: Int? = null) {
        submitToLogSink(m, "system", important, isSpecial = isSpecial, specialColor = specialColor)
    }
}
