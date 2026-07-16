package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SessionUseCase: Logic for managing tracking sessions, mode transitions, and resource cleanup.
 * v9.5.0:
 * - Issue #503: Hilt Removal.
 */
class SessionUseCase(
    private val repository: MainRepository,
    private val timeProvider: TimeProvider
) {
    suspend fun setAppMode(mode: String?): Long? {
        repository.setAppMode(mode)
        if (mode != null) {
            val appStartTime = timeProvider.currentTimeMillis()
            repository.saveLong(MainRepository.APP_START_TIME_KEY, appStartTime)
            repository.saveBoolean(MainRepository.IS_MANUAL_EXIT_KEY, false)
            return appStartTime
        }
        return null
    }

    suspend fun stopTrackingSession() {
        withContext(Dispatchers.IO) {
            repository.setAppMode(null)
            repository.resetStats()
            repository.clear()
        }
    }

    suspend fun resetStats(): Long {
        return withContext(Dispatchers.IO) {
            repository.resetStats()
            repository.sendCommand(UiCommand.StatsReset)
            val appStartTime = timeProvider.currentTimeMillis()
            repository.saveLong(MainRepository.APP_START_TIME_KEY, appStartTime)
            appStartTime
        }
    }
}
