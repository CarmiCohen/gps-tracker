package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppAlarmManager: Evaluates system health and manages siren states.
 * Oct.1.8:
 * - Issue #1410: Viewer Persistence. Added calculation of violationStartTs 
 *   (earliest trigger) and propagation of lastAlarmAckTs to ensure 
 *   idempotent alarm evaluation across peers and re-installs (R-ID 575).
 * Oct.1.6:
 * - Issue #1409: Connectivity Logic Hardening. Removed SIGNAL_LOSS and GPS_STALL 
 *   from special types to ensure connectivity alerts are notification-only 
 *   and do not trigger sirens or Red-Screen promotion (R-ID 572).
 * - Issue #1410: Standardized Manual Silence. Updated notifySirenManualStop 
 *   to use SILENCE_TIMEOUT_MS (5m) instead of cooldown to ensure persistent 
 *   muting on user action (R-ID 575).
 */
@Singleton
class AppAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val sessionManager: SessionManager,
    private val notificationManager: AppNotificationManager,
    private val timeProvider: TimeProvider,
    private val bootLifecycleAuthority: BootLifecycleAuthority,
    private val domainEventBus: DomainEventBus,
    private val sirenLockoutUseCase: SirenLockoutUseCase
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _isSirenRequired = MutableStateFlow(false)
    val isSirenRequired: StateFlow<Boolean> = _isSirenRequired.asStateFlow()

    private val _activeAlarmsFlow = MutableStateFlow<List<AlarmInfo>>(emptyList())
    val activeAlarmsFlow: StateFlow<List<AlarmInfo>> = _activeAlarmsFlow.asStateFlow()

    private var currentSettings = AlertSettings()

    private val evaluationReport = SystemHealthReport()
    private val evaluationState = AlarmEvaluationState()
    
    private var isTrackerMode: Boolean = false
    private var currentRole: AppRole = AppRole.TRACKER

    init {
        // React to lockout changes to refresh siren requirement
        sirenLockoutUseCase.silencedUntilRt
            .onEach { updateSirenRequirement() }
            .launchIn(scope)
    }

    fun updateSettings(settings: AlertSettings) {
        this.currentSettings = settings
        updateSirenRequirement()
    }

    fun getSettings(): AlertSettings = currentSettings

    fun setPowerAlarmPending(pending: Boolean, role: AppRole? = null) {
        val targetRole = role ?: this.currentRole
        
        if (evaluationState.powerAlarmPending != pending || this.currentRole != targetRole) {
            evaluationState.powerAlarmPending = pending
            this.currentRole = targetRole
            saveLogicState()
            updateSirenRequirement()
        }
    }

    fun hasUnresolvedAlarms(): Boolean {
        synchronized(evaluationState.activeAlarms) {
            return evaluationState.activeAlarms.values.any { !it.isResolved }
        }
    }

    fun getActiveAlarmSummary(): String {
        synchronized(evaluationState.activeAlarms) {
            return evaluationState.activeAlarms.values
                .filter { !it.isResolved }
                .joinToString(", ") { it.title }
        }
    }

    /**
     * Issue #1410: Calculates the timestamp of the earliest currently active 
     * and unresolved violation.
     */
    fun getEarliestViolationTs(): Long {
        synchronized(evaluationState.activeAlarms) {
            return evaluationState.activeAlarms.values
                .filter { !it.isResolved }
                .map { it.firstTriggerTs }
                .minOrNull() ?: 0L
        }
    }

    fun shouldPlaySiren(): Boolean {
        if (isTrackerMode) return false
        if (currentSettings.globalMute) return false
        
        synchronized(evaluationState.activeAlarms) {
            val hasSpecialUnresolved = evaluationState.activeAlarms.values.any { 
                !it.isResolved && isSpecialType(it.type) 
            }
            if (!hasSpecialUnresolved) return false
        }
        
        // Centralized Lockout Check
        if (sirenLockoutUseCase.isLockedOut()) return false
        
        // Additional engine-level safety (e.g. recent manual stop)
        val nowRt = timeProvider.elapsedRealtime()
        if (evaluationState.lastSirenStopRt > 0L && nowRt - evaluationState.lastSirenStopRt < SIREN_RESUME_COOLDOWN_MS) return false
        
        return true
    }
    
    fun notifySirenManualStop() {
        evaluationState.lastSirenStopRt = timeProvider.elapsedRealtime()
        // Issue #1410: Standardize on SILENCE_TIMEOUT_MS (5m) for manual user intervention
        sirenLockoutUseCase.setSilence(SILENCE_TIMEOUT_MS)
        saveLogicState()
        updateSirenRequirement()
    }

    fun restoreState(alarms: List<AlarmEvaluationState.ActiveAlarm>) {
        synchronized(evaluationState.activeAlarms) {
            evaluationState.activeAlarms.clear()
            alarms.forEach { evaluationState.activeAlarms[it.type] = it }
        }
        syncActiveAlarmsFlow()
        updateSirenRequirement()
    }

    fun restoreLogicState(s: AppSettings, role: AppRole) {
        this.currentRole = role
        this.isTrackerMode = (currentRole == AppRole.TRACKER)
        
        val prefix = currentRole.prefix
        evaluationState.firstViolationTs = s.roleLongsMap.getOrDefault(prefix + FIRST_VIOLATION_TS_KEY, 0L)
        evaluationState.firstViolationRt = s.roleLongsMap.getOrDefault(prefix + FIRST_VIOLATION_RT_KEY, 0L)
        evaluationState.firstViolationWasJump = s.roleBoolsMap.getOrDefault(prefix + FIRST_VIOLATION_WAS_JUMP_KEY, false)
        evaluationState.distanceViolationCounter = s.roleIntsMap.getOrDefault(prefix + DISTANCE_VIOLATION_COUNTER_KEY, 0)
        evaluationState.wasDistanceViolated = s.roleBoolsMap.getOrDefault(prefix + WAS_DISTANCE_VIOLATED_KEY, false)
        evaluationState.powerAlarmPending = s.roleBoolsMap.getOrDefault(prefix + POWER_ALARM_PENDING_KEY, false)
        evaluationState.lastSirenStopRt = s.roleLongsMap.getOrDefault(prefix + LAST_SIREN_STOP_RT_KEY, 0L)
        evaluationState.lastGlobalTriggerRt = s.roleLongsMap.getOrDefault(prefix + LAST_GLOBAL_TRIGGER_RT_KEY, 0L)
        evaluationState.forensicReliabilityDegradationStartRt = s.roleLongsMap.getOrDefault(prefix + FORENSIC_RELIABILITY_DEGRADATION_START_RT_KEY, 0L)

        val savedBootId = s.roleStringsMap.getOrDefault(prefix + "boot_id", "")
        if (!bootLifecycleAuthority.isSessionValid(savedBootId)) {
            // Full Reboot: Monotonic clock reset, wipe RT dependent states
            evaluationState.firstViolationRt = 0L
            evaluationState.lastSirenStopRt = 0L
            evaluationState.lastGlobalTriggerRt = 0L
            evaluationState.forensicReliabilityDegradationStartRt = 0L
            saveLogicState()
        } else {
            // Service Restart: Recover monotonic timestamps to preserve lockout
            evaluationState.lastSirenStopRt = bootLifecycleAuthority.recoverMonotonicTime(evaluationState.lastSirenStopRt, savedBootId)
            evaluationState.lastGlobalTriggerRt = bootLifecycleAuthority.recoverMonotonicTime(evaluationState.lastGlobalTriggerRt, savedBootId)
            
            // Sync the centralized lockout use case with the recovered state
            val nowRt = timeProvider.elapsedRealtime()
            if (evaluationState.lastSirenStopRt > 0 && nowRt - evaluationState.lastSirenStopRt < SIREN_RESUME_COOLDOWN_MS) {
                val remaining = SIREN_RESUME_COOLDOWN_MS - (nowRt - evaluationState.lastSirenStopRt)
                sirenLockoutUseCase.setSilence(remaining)
            }
        }
        
        syncActiveAlarmsFlow()
        updateSirenRequirement()
    }

    private fun saveLogicState() {
        scope.launch {
            repository.saveLogicState(
                firstViolationTs = evaluationState.firstViolationTs,
                firstViolationRt = evaluationState.firstViolationRt,
                firstViolationWasJump = evaluationState.firstViolationWasJump,
                distanceViolationCounter = evaluationState.distanceViolationCounter,
                wasDistanceViolated = evaluationState.wasDistanceViolated,
                powerAlarmPending = evaluationState.powerAlarmPending,
                lastSirenStopRt = evaluationState.lastSirenStopRt,
                lastGlobalTriggerRt = evaluationState.lastGlobalTriggerRt,
                forensicReliabilityDegradationStartRt = evaluationState.forensicReliabilityDegradationStartRt,
                role = currentRole
            )
            repository.saveString(currentRole, "boot_id", bootLifecycleAuthority.getCurrentBootId())
        }
    }

    fun evaluateAlarms(
        snapshot: SystemEvaluationSnapshot,
        serviceContext: AlarmServiceContext
    ) {
        this.isTrackerMode = serviceContext.isTrackerMode
        this.currentRole = serviceContext.role
        val versionTag = "[${BuildConfig.VERSION_NAME}]"
        
        syncEvaluationState(snapshot, serviceContext)

        val oldWasViolated = evaluationState.wasDistanceViolated
        val oldCounter = evaluationState.distanceViolationCounter
        val oldRt = evaluationState.firstViolationRt
        val oldStallRt = evaluationState.forensicReliabilityDegradationStartRt
        val oldGlobalTriggerRt = evaluationState.lastGlobalTriggerRt

        MainAlarmLogic.detectViolations(
            state = evaluationState,
            timeProvider = timeProvider,
            report = evaluationReport,
            versionTag = versionTag,
            onSpike = { message: String, duration: Long ->
                domainEventBus.emit(AlarmEvent.LogEvent(
                    type = ALERT_ID_PERFORMANCE_SPIKE,
                    message = "$versionTag $message",
                    isImportant = false,
                    extremeValue = duration.toDouble(),
                    logId = null,
                    durationMs = duration,
                    isSpecial = true,
                    specialColor = FORENSIC_PINK_COLOR,
                    lat = snapshot.kinetic.lat, lng = snapshot.kinetic.lng, accuracy = snapshot.kinetic.accuracy,
                    maxAccuracy = snapshot.kinetic.maxAccuracy, snr = snapshot.snrSnapshot, vibe = snapshot.vibeSnapshot
                ))
            },
            onTrigger = { eval: AlarmEvaluationState.ActiveAlarm ->
                val isSpecial = isSpecialType(eval.type)
                val specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null
                
                // Visual feedback for triggers, noting if they are currently muted
                val isMuted = sirenLockoutUseCase.isLockedOut() || (evaluationState.lastSirenStopRt > 0L && timeProvider.elapsedRealtime() - evaluationState.lastSirenStopRt < SIREN_RESUME_COOLDOWN_MS)
                val statusTag = if (isMuted && isSpecial) "(MUTED)" else "TRIGGERED"

                domainEventBus.emit(AlarmEvent.LogEvent(
                    type = eval.type,
                    message = "$versionTag ALARM $statusTag: ${eval.title}",
                    isImportant = !isMuted,
                    extremeValue = null,
                    logId = null,
                    durationMs = 0L,
                    isSpecial = isSpecial,
                    specialColor = specialColor,
                    lat = snapshot.kinetic.lat, lng = snapshot.kinetic.lng, accuracy = snapshot.kinetic.accuracy,
                    maxAccuracy = snapshot.kinetic.maxAccuracy, snr = snapshot.snrSnapshot, vibe = snapshot.vibeSnapshot
                ))
            },
            onResolve = { eval: AlarmEvaluationState.ActiveAlarm, durationMs: Long ->
                val isSpecial = isSpecialType(eval.type)
                val specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null
                domainEventBus.emit(AlarmEvent.LogEvent(
                    type = eval.type,
                    message = "$versionTag ALARM RESOLVED: ${eval.title}",
                    isImportant = false,
                    extremeValue = null,
                    logId = null,
                    durationMs = durationMs,
                    isSpecial = isSpecial,
                    specialColor = specialColor,
                    lat = snapshot.kinetic.lat, lng = snapshot.kinetic.lng, accuracy = snapshot.kinetic.accuracy,
                    maxAccuracy = snapshot.kinetic.maxAccuracy, snr = snapshot.snrSnapshot, vibe = snapshot.vibeSnapshot
                ))
            }
        )
        
        persistActiveAlarms()
        
        val stateChanged = evaluationState.wasDistanceViolated != oldWasViolated || 
                           evaluationState.distanceViolationCounter != oldCounter || 
                           evaluationState.firstViolationRt != oldRt || 
                           evaluationState.forensicReliabilityDegradationStartRt != oldStallRt ||
                           evaluationState.lastGlobalTriggerRt != oldGlobalTriggerRt
        if (stateChanged) {
            saveLogicState()
        }

        syncActiveAlarmsFlow()
        updateSirenRequirement()
    }

    private fun updateSirenRequirement() {
        _isSirenRequired.value = shouldPlaySiren()
    }

    private fun syncEvaluationState(
        snapshot: SystemEvaluationSnapshot,
        serviceContext: AlarmServiceContext
    ) {
        TelemetryMapper.mapSnapshotToHealth(snapshot, evaluationState.health)

        val cachedPoints = repository.getCachedHomePoints()
        for (i in cachedPoints.indices) {
            val p = cachedPoints[i]
            evaluationState.getOrCreateHomePoint(i).update(p.latitude, p.longitude)
        }
        evaluationState.truncateHomePoints(cachedPoints.size)

        // Issue #1410: Use remote acknowledgment if provided in the snapshot 
        // (carried over peer telemetry), otherwise use local authority.
        val targetAlarmAckTs = if (snapshot.lastAlarmAckTs > 0) {
            snapshot.lastAlarmAckTs 
        } else {
            repository.getLastAlarmAckTsSync(serviceContext.role)
        }

        evaluationState.update(
            now = serviceContext.now, 
            nowRt = serviceContext.nowRt, 
            serviceStartTime = serviceContext.serviceStartTs, 
            serviceStartRt = serviceContext.serviceStartRt,
            lastAlarmAckTs = targetAlarmAckTs,
            violationStartTs = snapshot.violationStartTs,
            appStartTime = serviceContext.appStartTime,
            isRelayConnected = serviceContext.isRelayConnected, 
            isTrackerConnected = serviceContext.isTrackerConnected,
            discoveryPhase = serviceContext.discoveryPhase ?: when {
                serviceContext.nowRt - serviceContext.serviceStartRt < BOOTSTRAP_PHASE_MS -> DiscoveryPhase.BOOTSTRAP
                serviceContext.nowRt - serviceContext.serviceStartRt < BOOTSTRAP_PHASE_MS + DISCOVERY_PHASE_MS -> DiscoveryPhase.DISCOVERING
                else -> DiscoveryPhase.MONITORING
            },
            trackerLat = snapshot.kinetic.lat, 
            trackerLng = snapshot.kinetic.lng, 
            trackerGpsAccuracy = snapshot.kinetic.accuracy,
            maxTrackerAccuracy = snapshot.kinetic.maxAccuracy, 
            lastGpsPacketTs = snapshot.kinetic.gpsTs, 
            lastGpsPacketRt = 0L, 
            trackerLastValidFixTs = 0L,
            trackerLastValidFixRt = snapshot.lastValidFixRt,
            trackerSpeed = snapshot.kinetic.speed, 
            jumpTier = snapshot.jumpTier, 
            isAdaptiveJump = snapshot.isAdaptiveJump, 
            trackerBattery = snapshot.integrity.battery, 
            trackerTemp = snapshot.atmospheric.temp,
            wasDistanceViolated = evaluationState.wasDistanceViolated, 
            distanceViolationCounter = evaluationState.distanceViolationCounter,
            firstViolationTs = evaluationState.firstViolationTs, 
            firstViolationRt = evaluationState.firstViolationRt,
            firstViolationWasJump = evaluationState.firstViolationWasJump, 
            maxDistance = serviceContext.maxDistanceAuthority, 
            distToHomeAuthority = serviceContext.distToHomeAuthority, 
            isGpsGap = snapshot.integrity.isLocationPending && snapshot.integrity.locationPendingReason == LocationPendingReason.GPS_GAP, 
            trackerBaroAltEma = snapshot.atmospheric.baroAlt,
            isTrackerMode = serviceContext.isTrackerMode, 
            capabilities = serviceContext.capabilities,
            vibrationSensitivity = currentSettings.vibrationSensitivity,
            tiltSensitivity = currentSettings.tiltSensitivity,
            powerAlarmPending = evaluationState.powerAlarmPending,
            lastSirenStopRt = evaluationState.lastSirenStopRt,
            lastGlobalTriggerRt = evaluationState.lastGlobalTriggerRt
        )
    }

    private fun syncActiveAlarmsFlow() {
        val list = synchronized(evaluationState.activeAlarms) {
            evaluationState.activeAlarms.values.map { 
                AlarmInfo(
                    title = it.title, 
                    subtitle = it.subtitle, 
                    type = it.type, 
                    isResolved = it.isResolved,
                    isSirenDisabled = !isSpecialType(it.type)
                )
            }
        }
        _activeAlarmsFlow.value = list
    }

    fun dismissResolvedAlarms() {
        synchronized(evaluationState.activeAlarms) {
            val iterator = evaluationState.activeAlarms.entries.iterator()
            while (iterator.hasNext()) { if (iterator.next().value.isResolved) iterator.remove() }
        }
        persistActiveAlarms()
        syncActiveAlarmsFlow()
        updateSirenRequirement()
    }

    private fun persistActiveAlarms() {
        val alarms = synchronized(evaluationState.activeAlarms) {
            evaluationState.activeAlarms.values.toList()
        }
        scope.launch {
            repository.saveActiveAlarms(alarms, currentRole)
        }
    }

    /**
     * Issue #1409: Hardened Special Types.
     * Connectivity alerts (Signal Loss, GPS Stall) are notification-only.
     */
    private fun isSpecialType(type: String): Boolean {
        return when (type) {
            ALERT_ID_JUMP_ALERT, ALERT_ID_TRACKER_TAMPER, ALERT_ID_TRACKER_POWER,
            ALERT_ID_TRACKER_TILT, ALERT_ID_TRACKER_ACOUSTIC,
            ALERT_ID_TRACKER_GEOFENCE, ALERT_ID_TRACKER_LIFT, ALERT_ID_SYSTEM_STORAGE_LOW,
            ALERT_ID_SYSTEM_STORAGE_CRITICAL,
            ALERT_ID_TRACKER_TEMP,
            ALERT_ID_BATTERY_STEEP_DISCHARGE, ALERT_ID_HARDWARE_CONFIGURATION -> true
            else -> false
        }
    }

    fun resetEvaluation(role: AppRole? = null) {
        if (role != null) {
            this.currentRole = role
        }
        synchronized(evaluationState.activeAlarms) { evaluationState.activeAlarms.clear() }
        persistActiveAlarms()
        evaluationState.firstViolationTs = 0L; evaluationState.firstViolationRt = 0L; evaluationState.wasDistanceViolated = false; evaluationState.distanceViolationCounter = 0
        
        val nowRt = timeProvider.elapsedRealtime()
        if (nowRt - evaluationState.lastSirenStopRt > SIREN_RESUME_COOLDOWN_MS) {
            evaluationState.lastSirenStopRt = 0L
        }
        
        evaluationState.lastGlobalTriggerRt = 0L
        saveLogicState()
        syncActiveAlarmsFlow()
        updateSirenRequirement()
    }
}
