package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionLifecycleCoordinator: Centralized authority for resetting session-specific state.
 * Issue #1165: Unified Session Lifecycle Management. Ensures all hardware peaks, 
 * temporal lockouts, and vitality markers are zeroed atomically upon session restart.
 */
@Singleton
class SessionLifecycleCoordinator @Inject constructor(
    private val timeProvider: TimeProvider,
    private val hardwareSuite: HardwareSuite,
    private val forensicAuditor: ForensicAuditor,
    private val sessionManager: SessionManager,
    private val integrityMonitor: IntegrityMonitor,
    private val alarmManager: AppAlarmManager,
    private val logManager: LogManager,
    private val forensicUseCase: ServiceForensicUseCase
) {

    /**
     * resetSession: Performs a coordinated reset of all session-related components.
     * @param roleTag "T" for Tracker, "V" for Viewer.
     * @param processors List of LocationProcessors to reset.
     * @param onReset Callback for service-specific local state cleanup.
     */
    fun resetSession(
        roleTag: String,
        processors: List<LocationProcessor>,
        onReset: () -> Unit
    ) {
        Timber.i("SessionLifecycleCoordinator: Initiating atomic session reset for role [$roleTag].")

        // 1. Reset Global Singletons
        alarmManager.resetEvaluation()
        sessionManager.reset()
        integrityMonitor.resetStats()
        forensicUseCase.resetLatches()
        
        // 2. Reset Role-Specific Shared Hardware State
        forensicAuditor.reset(roleTag)
        hardwareSuite.resetBaseline(roleTag)

        // 3. Reset Engine State
        processors.forEach { it.resetStats() }

        // 4. Service-Specific Local State Cleanup
        onReset()

        logManager.logServiceEvent(m = "Session Terminated [$roleTag]", isImportant = false)
    }
}
