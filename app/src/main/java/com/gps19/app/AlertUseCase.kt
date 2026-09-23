package com.gps19.app

import com.gps19.core.engine.TimeProvider
import javax.inject.Inject

/**
 * AlertUseCase: Handles logic for alarm dismissal and siren control.
 * Sep.23.70:
 * - Issue #1230 REMEDIATION: Implemented role-based namespace isolation using 
 *   ConfigManager context to prevent cross-role alarm state leakage (R-ID 453).
 */
class AlertUseCase @Inject constructor(
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val logManager: LogManager,
    private val configManager: ConfigManager
) {
    private fun getRolePrefix(): String = if (configManager.isTrackerMode) "T_" else "V_"

    suspend fun dismissAlarms(): Long {
        val now = timeProvider.currentTimeMillis()
        repository.saveLong(getRolePrefix() + LAST_ALARM_ACK_TS_KEY, now)
        repository.sendCommand(UiCommand.StopSiren("User Dismissed"))
        logManager.submitToLogSink("USER ACTION: Alerts acknowledged", "user", isImportant = true)
        return now
    }

    suspend fun stopSiren(causes: String?): Long {
        val now = timeProvider.currentTimeMillis()
        repository.saveLong(getRolePrefix() + LAST_ALARM_ACK_TS_KEY, now)
        repository.sendCommand(UiCommand.StopSiren(causes))
        logManager.submitToLogSink("USER ACTION: Siren stopped ${causes ?: ""}", "user", isImportant = true)
        return now
    }
}
