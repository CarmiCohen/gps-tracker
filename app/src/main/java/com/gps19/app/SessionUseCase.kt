package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * SessionUseCase: Logic for managing tracking sessions, mode transitions, and resource cleanup.
 * Sep.07.81:
 * - HUD LED Specification Compliance (R975): Integrated repository.clear() into 
 *   setAppMode to ensure telemetry state is reset during mode transitions, 
 *   preventing "ghost" peer status on single-device switches.
 * Sep.02.66:
 * - Issue #241 RESOLVED: Mode-Selection Activation. Integrated IS_SYSTEM_ACTIVE_KEY 
 *   toggle into setAppMode to ensure atomic state transition during role selection (R-ID 241).
 */
class SessionUseCase @Inject constructor(
    private val repository: MainRepository,
    private val timeProvider: TimeProvider
) {
    /**
     * Sets the application mode and activates the system if a mode is selected.
     * Sep.07.81: Now calls repository.clear() to reset peer activity during role change.
     */
    suspend fun setAppMode(mode: String?): Long? {
        repository.setAppMode(mode)
        if (mode != null) {
            // R975: Clear shared telemetry state to prevent stale peer activity from previous role
            repository.clear()

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
