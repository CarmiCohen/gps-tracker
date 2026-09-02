package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * SessionUseCase: Logic for managing tracking sessions, mode transitions, and resource cleanup.
 * Sep.02.66:
 * - Issue #241 RESOLVED: Mode-Selection Activation. Integrated IS_SYSTEM_ACTIVE_KEY 
 *   toggle into setAppMode to ensure atomic state transition during role selection (R-ID 241).
 * July.27.00:
 * - Architecture Audit: Updated to use centralized PreferenceKeys.
 */
class SessionUseCase @Inject constructor(
    private val repository: MainRepository,
    private val timeProvider: TimeProvider
) {
    /**
     * Sets the application mode and activates the system if a mode is selected.
     * Sep.02.66: Now sets IS_SYSTEM_ACTIVE_KEY to true for non-null modes (Issue #241).
     */
    suspend fun setAppMode(mode: String?): Long? {
        repository.setAppMode(mode)
        if (mode != null) {
            val appStartTime = timeProvider.currentTimeMillis()
            repository.saveLong(APP_START_TIME_KEY, appStartTime)
            repository.saveBoolean(IS_MANUAL_EXIT_KEY, false)
            // Ensure system is active upon mode selection to unblock workers (R-ID 241)
            repository.saveBoolean(IS_SYSTEM_ACTIVE_KEY, true)
            return appStartTime
        }
        return null
    }

    suspend fun setSystemActive(active: Boolean) {
        repository.saveBoolean(IS_SYSTEM_ACTIVE_KEY, active)
    }

    suspend fun stopTrackingSession() {
        withContext(Dispatchers.IO) {
            repository.setAppMode(null)
            repository.saveBoolean(IS_SYSTEM_ACTIVE_KEY, false)
            repository.resetStats()
            repository.clear()
        }
    }

    suspend fun resetStats(): Long {
        return withContext(Dispatchers.IO) {
            repository.resetStats()
            repository.sendCommand(UiCommand.StatsReset)
            val appStartTime = timeProvider.currentTimeMillis()
            repository.saveLong(APP_START_TIME_KEY, appStartTime)
            appStartTime
        }
    }
}
