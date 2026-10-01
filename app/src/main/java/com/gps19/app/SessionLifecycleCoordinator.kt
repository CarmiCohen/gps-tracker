package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionLifecycleCoordinator: Centralized authority for resetting session-specific state.
 * Oct.1.1:
 * - Issue #1407: Unified Storage Authority. Migrated resetSession to use AppRole 
 *   enum, eliminating fragile string-based "T"/"V" tags (R-ID 568).
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
     * @param role Target AppRole for the reset.
     * @param processors List of LocationProcessors to reset.
     * @param onReset Callback for service-specific local state cleanup.
     */
    fun resetSession(
        role: AppRole,
        processors: List<LocationProcessor>,
        onReset: () -> Unit
    ) {
        Timber.i("SessionLifecycleCoordinator: Initiating atomic session reset for role [${role.name}].")

        // 1. Reset Global Singletons
        alarmManager.resetEvaluation(role)
        sessionManager.reset()
        integrityMonitor.resetStats()
        forensicUseCase.resetLatches()
        
        // 2. Reset Role-Specific Shared Hardware State
        forensicAuditor.reset(role)
        hardwareSuite.resetBaseline(role.prefix.removeSuffix("_"))

        // 3. Reset Engine State
        processors.forEach { it.resetStats() }

        // 4. Service-Specific Local State Cleanup
        onReset()

        logManager.logServiceEvent(m = "Session Terminated [${role.name}]", isImportant = false)
    }
}
