package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * AppAlarmManager: Evaluates system health and manages siren states.
 * Sep.25.08:
 * - Issue #1329: Telemetry Mapping Convergence. Refactored syncEvaluationState 
 *   to use TelemetryMapper.mapSnapshotToHealth, eliminating manual mapping logic.
 * Sep.25.07:
 * - Issue #1329 Remediation: Fixed compilation errors in evaluateAlarms and 
 *   syncEvaluationState by aligning with the partitioned SystemEvaluationSnapshot structure.
 */
@Singleton
class AppAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val sessionManager: SessionManager,
    private val notificationManager: AppNotificationManager,
    private val timeProvider: TimeProvider,
    private val domainEventBus: DomainEventBus
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _isSirenRequired = MutableStateFlow(false)
    val isSirenRequired: StateFlow<Boolean> = _isSirenRequired.asStateFlow()

    private var lastAlarmsJson = "[]"
    private var currentSettings = AlertSettings()

    private val evaluationReport = SystemHealthReport()
    private val evaluationState = AlarmEvaluationState()
    
    private var isTrackerMode: Boolean = false
    private var currentRolePrefix: String = ""

    fun updateSettings(settings: AlertSettings) {
        this.currentSettings = settings
        updateSirenRequirement()
    }

    fun getSettings(): AlertSettings = currentSettings

    fun setPowerAlarmPending(pending: Boolean, rolePrefix: String = "") {
        if (evaluationState.powerAlarmPending != pending || this.currentRolePrefix != rolePrefix) {
            evaluationState.powerAlarmPending = pending
            this.currentRolePrefix = rolePrefix
            saveLogicState()
            updateSirenRequirement()
        }
    }

    fun hasUnresolvedAlarms(): Boolean {
        synchronized(evaluationState.activeAlarms) {
            return evaluationState.activeAlarms.values.any { !it.isResolved }
        }
    }

    fun shouldPlaySiren(silencedUntilRt: Long = 0L): Boolean {
        if (isTrackerMode) return false
        if (currentSettings.globalMute) return false
        if (!hasUnresolvedAlarms()) return false
        val nowRt = timeProvider.elapsedRealtime()
        
        if (evaluationState.lastSirenStopRt > 0L && nowRt - evaluationState.lastSirenStopRt < SIREN_RESUME_COOLDOWN_MS) return false
        if (nowRt < silencedUntilRt) return false
        return true
    }
    
    fun notifySirenManualStop() {
        evaluationState.lastSirenStopRt = timeProvider.elapsedRealtime()
        saveLogicState()
        updateSirenRequirement()
    }

    fun restoreState(json: String) {
        synchronized(evaluationState.activeAlarms) {
            evaluationState.activeAlarms.clear()
        }
        if (json.isEmpty() || json == "[]") {
            lastAlarmsJson = "[]"
            updateSirenRequirement()
            return
        }
        try {
            val array = JSONArray(json)
            synchronized(evaluationState.activeAlarms) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val type = obj.getString("type")
                    evaluationState.activeAlarms[type] = AlarmEvaluationState.ActiveAlarm(
                        type = type,
                        title = obj.optString("title", "Violation"),
                        subtitle = obj.optString("subtitle", ""),
                        isTriggered = obj.optBoolean("isTriggered", false),
                        firstTriggerTs = obj.optLong("firstTriggerTs", 0L),
                        firstTriggerRt = obj.optLong("firstTriggerRt", 0L),
                        lastLogTs = obj.optLong("lastLogTs", 0L),
                        lastLogRt = obj.optLong("lastLogRt", 0L),
                        isResolved = obj.optBoolean("isResolved", true)
                    )
                }
            }
            lastAlarmsJson = json
        } catch (e: Exception) {
            Timber.e(e, "Siren Persistence: Failed to restore alarm state")
        }
        updateSirenRequirement()
    }

    fun restoreLogicState(s: AppSettings, rolePrefix: String = "") {
        this.currentRolePrefix = rolePrefix
        this.isTrackerMode = (rolePrefix == "T_")
        
        if (rolePrefix.isEmpty()) {
            evaluationState.firstViolationTs = s.firstViolationTs
            evaluationState.firstViolationRt = s.firstViolationRt
            evaluationState.firstViolationWasJump = s.firstViolationWasJump
            evaluationState.distanceViolationCounter = s.distanceViolationCounter
            evaluationState.wasDistanceViolated = s.wasDistanceViolated
            evaluationState.powerAlarmPending = s.powerAlarmPending
            evaluationState.lastSirenStopRt = s.lastSirenStopRt
            evaluationState.lastGlobalTriggerRt = s.lastGlobalTriggerRt
            evaluationState.forensicReliabilityDegradationStartRt = s.forensicReliabilityDegradationStartRt
        } else {
            evaluationState.firstViolationTs = s.roleLongsMap.getOrDefault(rolePrefix + FIRST_VIOLATION_TS_KEY, 0L)
            evaluationState.firstViolationRt = s.roleLongsMap.getOrDefault(rolePrefix + FIRST_VIOLATION_RT_KEY, 0L)
            evaluationState.firstViolationWasJump = s.roleBoolsMap.getOrDefault(rolePrefix + FIRST_VIOLATION_WAS_JUMP_KEY, false)
            evaluationState.distanceViolationCounter = s.roleIntsMap.getOrDefault(rolePrefix + DISTANCE_VIOLATION_COUNTER_KEY, 0)
            evaluationState.wasDistanceViolated = s.roleBoolsMap.getOrDefault(rolePrefix + WAS_DISTANCE_VIOLATED_KEY, false)
            evaluationState.powerAlarmPending = s.roleBoolsMap.getOrDefault(rolePrefix + POWER_ALARM_PENDING_KEY, false)
            evaluationState.lastSirenStopRt = s.roleLongsMap.getOrDefault(rolePrefix + LAST_SIREN_STOP_RT_KEY, 0L)
            evaluationState.lastGlobalTriggerRt = s.roleLongsMap.getOrDefault(rolePrefix + LAST_GLOBAL_TRIGGER_RT_KEY, 0L)
            evaluationState.forensicReliabilityDegradationStartRt = s.roleLongsMap.getOrDefault(rolePrefix + FORENSIC_RELIABILITY_DEGRADATION_START_RT_KEY, 0L)
        }

        val savedBootId = s.roleStringsMap.getOrDefault(rolePrefix + "boot_id", "")
        val currentBootId = timeProvider.getBootId()
        if (savedBootId.isNotEmpty() && savedBootId != currentBootId) {
            evaluationState.firstViolationRt = 0L
            evaluationState.lastSirenStopRt = 0L
            evaluationState.lastGlobalTriggerRt = 0L
            evaluationState.forensicReliabilityDegradationStartRt = 0L
            saveLogicState()
        } else if (savedBootId.isEmpty()) {
            saveLogicState()
        }
        
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
                rolePrefix = currentRolePrefix
            )
            repository.saveString(currentRolePrefix + "boot_id", timeProvider.getBootId())
        }
    }

    fun evaluateAlarms(
        snapshot: SystemEvaluationSnapshot,
        serviceContext: AlarmServiceContext
    ) {
        this.isTrackerMode = serviceContext.isTrackerMode
        this.currentRolePrefix = serviceContext.rolePrefix
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
            onSpike = { message, duration ->
                domainEventBus.emit(DomainEvent.Alarm(AlarmEvent.LogEvent(
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
                )))
            },
            onTrigger = { eval ->
                val isSpecial = isSpecialType(eval.type)
                val specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null
                domainEventBus.emit(DomainEvent.Alarm(AlarmEvent.LogEvent(
                    type = eval.type,
                    message = "$versionTag ALARM TRIGGERED: ${eval.title}",
                    isImportant = true,
                    extremeValue = null,
                    logId = null,
                    durationMs = 0L,
                    isSpecial = isSpecial,
                    specialColor = specialColor,
                    lat = snapshot.kinetic.lat, lng = snapshot.kinetic.lng, accuracy = snapshot.kinetic.accuracy,
                    maxAccuracy = snapshot.kinetic.maxAccuracy, snr = snapshot.snrSnapshot, vibe = snapshot.vibeSnapshot
                )))
            },
            onResolve = { eval, durationMs ->
                val isSpecial = isSpecialType(eval.type)
                val specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null
                domainEventBus.emit(DomainEvent.Alarm(AlarmEvent.LogEvent(
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
                )))
            }
        )
        
        updateAlarmsJson()
        
        val stateChanged = evaluationState.wasDistanceViolated != oldWasViolated || 
                           evaluationState.distanceViolationCounter != oldCounter || 
                           evaluationState.firstViolationRt != oldRt || 
                           evaluationState.forensicReliabilityDegradationStartRt != oldStallRt ||
                           evaluationState.lastGlobalTriggerRt != oldGlobalTriggerRt
        if (stateChanged) {
            saveLogicState()
        }

        updateSirenRequirement()
    }

    private fun updateSirenRequirement() {
        _isSirenRequired.value = shouldPlaySiren()
    }

    private fun syncEvaluationState(
        snapshot: SystemEvaluationSnapshot,
        serviceContext: AlarmServiceContext
    ) {
        // Issue #1329: Centralized authority for health state synchronization.
        TelemetryMapper.mapSnapshotToHealth(snapshot, evaluationState.health)

        val cachedPoints = repository.getCachedHomePoints()
        for (i in cachedPoints.indices) {
            val p = cachedPoints[i]
            evaluationState.getOrCreateHomePoint(i).update(p.latitude, p.longitude)
        }
        evaluationState.truncateHomePoints(cachedPoints.size)

        evaluationState.update(
            now = serviceContext.now, 
            nowRt = serviceContext.nowRt, 
            serviceStartTime = serviceContext.serviceStartTs, 
            serviceStartRt = serviceContext.serviceStartRt,
            lastAlarmAckTs = repository.getLastAlarmAckTsSync(serviceContext.rolePrefix),
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

    fun dismissResolvedAlarms() {
        synchronized(evaluationState.activeAlarms) {
            val iterator = evaluationState.activeAlarms.entries.iterator()
            while (iterator.hasNext()) { if (iterator.next().value.isResolved) iterator.remove() }
        }
        updateAlarmsJson()
        updateSirenRequirement()
    }

    private fun updateAlarmsJson() {
        val jsonArray = JSONArray()
        synchronized(evaluationState.activeAlarms) {
            evaluationState.activeAlarms.values.forEach { eval ->
                val obj = JSONObject()
                obj.put("type", eval.type); obj.put("isTriggered", eval.isTriggered); obj.put("isResolved", eval.isResolved)
                obj.put("title", eval.title); obj.put("subtitle", eval.subtitle); obj.put("isSirenDisabled", currentSettings.globalMute)
                obj.put("firstTriggerTs", eval.firstTriggerTs)
                obj.put("firstTriggerRt", eval.firstTriggerRt)
                obj.put("lastLogTs", eval.lastLogTs)
                obj.put("lastLogRt", eval.lastLogRt)
                jsonArray.put(obj)
            }
        }
        val newJson = jsonArray.toString()
        if (newJson != lastAlarmsJson) { lastAlarmsJson = newJson; repository.saveAlarmsJsonSync(newJson, currentRolePrefix) }
    }

    private fun isSpecialType(type: String): Boolean {
        return when (type) {
            ALERT_ID_JUMP_ALERT, ALERT_ID_TRACKER_TAMPER, ALERT_ID_TRACKER_POWER,
            ALERT_ID_TRACKER_TILT, ALERT_ID_TRACKER_ACOUSTIC,
            ALERT_ID_TRACKER_GEOFENCE, ALERT_ID_TRACKER_LIFT, ALERT_ID_SYSTEM_STORAGE_LOW,
            ALERT_ID_SYSTEM_STORAGE_CRITICAL,
            ALERT_ID_SIGNAL_LOSS, ALERT_ID_GPS_STALL, ALERT_ID_TRACKER_TEMP,
            ALERT_ID_BATTERY_STEEP_DISCHARGE, ALERT_ID_HARDWARE_CONFIGURATION -> true
            else -> false
        }
    }

    fun getLastAlarmsJson(): String = lastAlarmsJson
    
    fun resetEvaluation() {
        synchronized(evaluationState.activeAlarms) { evaluationState.activeAlarms.clear() }
        lastAlarmsJson = "[]"; repository.saveAlarmsJsonSync("[]", currentRolePrefix)
        evaluationState.firstViolationTs = 0L; evaluationState.firstViolationRt = 0L; evaluationState.wasDistanceViolated = false; evaluationState.distanceViolationCounter = 0
        
        val nowRt = timeProvider.elapsedRealtime()
        if (nowRt - evaluationState.lastSirenStopRt > SIREN_RESUME_COOLDOWN_MS) {
            evaluationState.lastSirenStopRt = 0L
        }
        
        evaluationState.lastGlobalTriggerRt = 0L
        saveLogicState()
        updateSirenRequirement()
    }
}
