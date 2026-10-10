package com.gps19.app

import com.gps19.core.engine.AppRole
import com.gps19.core.engine.TimeProvider
import javax.inject.Inject

/**
 * AlertUseCase: Handles logic for alarm dismissal and siren control.
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to use property-based 
 *   TimeProvider API.
 */
class AlertUseCase @Inject constructor(
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val logManager: LogManager,
    private val configManager: ConfigManager
) {
    private fun getAlarmRole(): AppRole = if (configManager.isTrackerMode) AppRole.TRACKER else AppRole.VIEWER_REMOTE

    suspend fun dismissAlarms(): Long {
        val now = timeProvider.currentTimeMillis
        repository.saveLong(getAlarmRole(), LAST_ALARM_ACK_TS_KEY, now)
        repository.sendCommand(UiCommand.StopSiren("User Dismissed"))
        logManager.submitToLogSink("USER ACTION: Alerts acknowledged", "user", isImportant = true)
        return now
    }

    suspend fun stopSiren(causes: String?): Long {
        val now = timeProvider.currentTimeMillis
        repository.saveLong(getAlarmRole(), LAST_ALARM_ACK_TS_KEY, now)
        repository.sendCommand(UiCommand.StopSiren(causes))
        logManager.submitToLogSink("USER ACTION: Siren stopped ${causes ?: ""}", "user", isImportant = true)
        return now
    }
}
