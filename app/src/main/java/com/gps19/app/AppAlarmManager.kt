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
 * Oct.10.2:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to use property-based 
 *   TimeProvider and BootLifecycleAuthority APIs.
 * Oct.7.6:
 * - Issue #SIMP-1007-16: JNI FastPath Expansion. Updated AlarmEvent emission 
 *   to utilize the unified ForensicSnapshot container.
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
        
        return true
    }
    
    fun notifySirenManualStop() {
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
        val logicProto = s.roleLogicStatesMap[prefix]
        
        if (logicProto != null) {
            SettingsMapper.applyLogicStateFromProto(logicProto, evaluationState)
            
            if (!bootLifecycleAuthority.isSessionValid(logicProto.bootId)) {
                // Full Reboot: Monotonic clock reset, wipe RT dependent states
                evaluationState.firstViolationRt = 0L
                evaluationState.lastGlobalTriggerRt = 0L
                evaluationState.forensicReliabilityDegradationStartRt = 0L
                evaluationState.lastRelayOnlineRt = 0L
                evaluationState.lastRelayOfflineRt = 0L
                saveLogicState()
            } else {
                // Service Restart: Recover monotonic timestamps to preserve lockout
                evaluationState.lastGlobalTriggerRt = bootLifecycleAuthority.recoverMonotonicTime(evaluationState.lastGlobalTriggerRt, logicProto.bootId)
                
                // Centralized Lockout Recovery
                if (logicProto.lastSirenStopRt > 0) {
                    val absoluteStopRt = bootLifecycleAuthority.recoverMonotonicTime(logicProto.lastSirenStopRt, logicProto.bootId)
                    val nowRt = timeProvider.elapsedRealtime
                    if (nowRt - absoluteStopRt < SIREN_RESUME_COOLDOWN_MS) {
                        val remaining = SIREN_RESUME_COOLDOWN_MS - (nowRt - absoluteStopRt)
                        sirenLockoutUseCase.setSilence(remaining)
                    }
                }
            }
        } else {
            // Legacy/Fallback restoration path
            evaluationState.firstViolationTs = s.roleLongsMap.getOrDefault(prefix + FIRST_VIOLATION_TS_KEY, 0L)
            evaluationState.firstViolationRt = s.roleLongsMap.getOrDefault(prefix + FIRST_VIOLATION_RT_KEY, 0L)
            evaluationState.firstViolationWasJump = s.roleBoolsMap.getOrDefault(prefix + FIRST_VIOLATION_WAS_JUMP_KEY, false)
            evaluationState.distanceViolationCounter = s.roleIntsMap.getOrDefault(prefix + DISTANCE_VIOLATION_COUNTER_KEY, 0)
            evaluationState.wasDistanceViolated = s.roleBoolsMap.getOrDefault(prefix + WAS_DISTANCE_VIOLATED_KEY, false)
            evaluationState.powerAlarmPending = s.roleBoolsMap.getOrDefault(prefix + POWER_ALARM_PENDING_KEY, false)
            evaluationState.lastGlobalTriggerRt = s.roleLongsMap.getOrDefault(prefix + LAST_GLOBAL_TRIGGER_RT_KEY, 0L)
            evaluationState.forensicReliabilityDegradationStartRt = s.roleLongsMap.getOrDefault(prefix + FORENSIC_RELIABILITY_DEGRADATION_START_RT_KEY, 0L)
        }
        
        syncActiveAlarmsFlow()
        updateSirenRequirement()
    }

    private fun saveLogicState() {
        scope.launch {
            // Heuristic for manual stop recovery: save relative offset from silence timeout
            evaluationState.lastSirenStopRt = if (sirenLockoutUseCase.isLockedOut()) {
                timeProvider.elapsedRealtime - (SIREN_RESUME_COOLDOWN_MS / 2) 
            } else 0L
            
            evaluationState.bootId = bootLifecycleAuthority.currentBootId
            repository.saveLogicState(evaluationState, currentRole)
        }
    }

    fun evaluateAlarms(
        update: LocationUpdate,
        serviceContext: AlarmServiceContext
    ) {
        this.isTrackerMode = serviceContext.isTrackerMode
        this.currentRole = serviceContext.role
        val versionTag = "[${BuildConfig.VERSION_NAME}]"
        
        syncEvaluationState(update, serviceContext)

        val oldWasViolated = evaluationState.wasDistanceViolated
        val oldCounter = evaluationState.distanceViolationCounter
        val oldRt = evaluationState.firstViolationRt
        val oldStallRt = evaluationState.forensicReliabilityDegradationStartRt
        val oldGlobalTriggerRt = evaluationState.lastGlobalTriggerRt

        MainAlarmLogic.detectViolations(
            state = evaluationState,
            timeProvider = timeProvider,
            report = evaluationReport,
            isLockedOut = sirenLockoutUseCase.isLockedOut(),
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
                    lat = update.kinetic.lat, lng = update.kinetic.lng, accuracy = update.kinetic.accuracy,
                    maxAccuracy = update.kinetic.maxAccuracy, forensic = update.integrity.forensic.copy()
                ))
            },
            onTrigger = { eval: AlarmEvaluationState.ActiveAlarm ->
                val isSpecial = isSpecialType(eval.type)
                val specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null
                
                domainEventBus.emit(AlarmEvent.LogEvent(
                    type = eval.type,
                    message = "$versionTag ALARM TRIGGERED: ${eval.title}",
                    isImportant = true,
                    extremeValue = null,
                    logId = null,
                    durationMs = 0L,
                    isSpecial = isSpecial,
                    specialColor = specialColor,
                    lat = update.kinetic.lat, lng = update.kinetic.lng, accuracy = update.kinetic.accuracy,
                    maxAccuracy = update.kinetic.maxAccuracy, forensic = update.integrity.forensic.copy()
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
                    lat = update.kinetic.lat, lng = update.kinetic.lng, accuracy = update.kinetic.accuracy,
                    maxAccuracy = update.kinetic.maxAccuracy, forensic = update.integrity.forensic.copy()
                ))
            },
            onTriggerMuted = { eval: AlarmEvaluationState.ActiveAlarm ->
                val isSpecial = isSpecialType(eval.type)
                val specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null
                domainEventBus.emit(AlarmEvent.LogEvent(
                    type = eval.type,
                    message = "$versionTag ALARM (MUTED): ${eval.title}",
                    isImportant = false,
                    extremeValue = null,
                    logId = null,
                    durationMs = 0L,
                    isSpecial = isSpecial,
                    specialColor = specialColor,
                    lat = update.kinetic.lat, lng = update.kinetic.lng, accuracy = update.kinetic.accuracy,
                    maxAccuracy = update.kinetic.maxAccuracy, forensic = update.integrity.forensic.copy()
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
        update: LocationUpdate,
        serviceContext: AlarmServiceContext
    ) {
        TelemetryMapper.mapSnapshotToHealth(update, evaluationState.health)

        val cachedPoints = repository.getCachedHomePoints()
        for (i in cachedPoints.indices) {
            val p = cachedPoints[i]
            evaluationState.getOrCreateHomePoint(i).update(p.latitude, p.longitude)
        }
        evaluationState.getOrCreateHomePoint(cachedPoints.size)

        // Issue #1410: Use remote acknowledgment if provided in the update 
        val targetAlarmAckTs = if (update.lastAlarmAckTs > 0) {
            update.lastAlarmAckTs 
        } else {
            repository.getLastAlarmAckTsSync(serviceContext.role)
        }

        evaluationState.update(
            now = serviceContext.now, 
            nowRt = serviceContext.nowRt, 
            serviceStartTime = serviceContext.serviceStartTs, 
            serviceStartRt = serviceContext.serviceStartRt,
            lastAlarmAckTs = targetAlarmAckTs,
            violationStartTs = update.violationStartTs,
            appStartTime = serviceContext.appStartTime,
            isRelayConnected = serviceContext.isRelayConnected, 
            isTrackerConnected = serviceContext.isTrackerConnected,
            discoveryPhase = serviceContext.discoveryPhase ?: when {
                serviceContext.nowRt - serviceContext.serviceStartRt < BOOTSTRAP_PHASE_MS -> DiscoveryPhase.BOOTSTRAP
                serviceContext.nowRt - serviceContext.serviceStartRt < BOOTSTRAP_PHASE_MS + DISCOVERY_PHASE_MS -> DiscoveryPhase.DISCOVERING
                else -> DiscoveryPhase.MONITORING
            },
            trackerLat = update.kinetic.lat, 
            trackerLng = update.kinetic.lng, 
            trackerGpsAccuracy = update.kinetic.accuracy,
            maxTrackerAccuracy = update.kinetic.maxAccuracy, 
            lastGpsPacketTs = update.kinetic.gpsTs, 
            lastGpsPacketRt = 0L, 
            trackerLastValidFixTs = 0L,
            trackerLastValidFixRt = update.lastValidFixRt,
            trackerSpeed = update.kinetic.speed, 
            jumpTier = update.jumpTier, 
            isAdaptiveJump = update.isAdaptiveJump, 
            trackerBattery = update.integrity.battery, 
            trackerTemp = update.atmospheric.temp,
            wasDistanceViolated = evaluationState.wasDistanceViolated, 
            distanceViolationCounter = evaluationState.distanceViolationCounter,
            firstViolationTs = evaluationState.firstViolationTs, 
            firstViolationRt = evaluationState.firstViolationRt,
            firstViolationWasJump = evaluationState.firstViolationWasJump, 
            maxDistance = serviceContext.maxDistanceAuthority, 
            distToHomeAuthority = serviceContext.distToHomeAuthority, 
            isGpsGap = update.integrity.isLocationPending && update.integrity.locationPendingReason == LocationPendingReason.GPS_GAP, 
            trackerBaroAltEma = update.atmospheric.baroAlt,
            isTrackerMode = serviceContext.isTrackerMode, 
            capabilities = serviceContext.capabilities,
            vibrationSensitivity = currentSettings.vibrationSensitivity,
            tiltSensitivity = currentSettings.tiltSensitivity,
            powerAlarmPending = evaluationState.powerAlarmPending,
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
        
        evaluationState.lastGlobalTriggerRt = 0L
        saveLogicState()
        syncActiveAlarmsFlow()
        updateSirenRequirement()
    }
}
