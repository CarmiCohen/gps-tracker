package com.gps19.app

import com.gps19.core.engine.TimeProvider
import javax.inject.Inject

/**
 * AlertUseCase: Handles logic for alarm dismissal and siren control.
 * July.27.00:
 * - Architecture Audit: Updated to use centralized PreferenceKeys.
 */
class AlertUseCase @Inject constructor(
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val logManager: LogManager
) {
    suspend fun dismissAlarms(): Long {
        val now = timeProvider.currentTimeMillis()
        repository.saveLong(LAST_ALARM_ACK_TS_KEY, now)
        repository.sendCommand(UiCommand.StopSiren("User Dismissed"))
        logManager.submitToLogSink("USER ACTION: Alerts acknowledged", "user", isImportant = true)
        return now
    }

    suspend fun stopSiren(causes: String?): Long {
        val now = timeProvider.currentTimeMillis()
        repository.saveLong(LAST_ALARM_ACK_TS_KEY, now)
        repository.sendCommand(UiCommand.StopSiren(causes))
        logManager.submitToLogSink("USER ACTION: Siren stopped ${causes ?: ""}", "user", isImportant = true)
        return now
    }
}
