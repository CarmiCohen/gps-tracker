package com.gps19.app

import com.gps19.core.engine.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlertUseCase: Handles logic for alarm dismissal and siren control.
 * Extracted from MainViewModel to resolve Issue 115 (Architectural Bloat).
 */
@Singleton
class AlertUseCase @Inject constructor(
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val logManager: LogManager
) {
    suspend fun dismissAlarms(): Long {
        val now = timeProvider.currentTimeMillis()
        repository.saveLong(MainRepository.LAST_ALARM_ACK_TS_KEY, now)
        repository.sendCommand(UiCommand.StopSiren("User Dismissed"))
        logManager.submitToLogSink("USER ACTION: Alerts acknowledged", "user", important = true)
        return now
    }

    suspend fun stopSiren(causes: String?): Long {
        val now = timeProvider.currentTimeMillis()
        repository.saveLong(MainRepository.LAST_ALARM_ACK_TS_KEY, now)
        repository.sendCommand(UiCommand.StopSiren(causes))
        logManager.submitToLogSink("USER ACTION: Siren stopped ${causes ?: ""}", "user", important = true)
        return now
    }
}
