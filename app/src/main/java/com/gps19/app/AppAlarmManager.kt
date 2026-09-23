package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * AlarmEvent: Reactive event container for alarm state changes and logging.
 */
sealed class AlarmEvent {
    data class LogEvent(
        val type: String, val message: String, val isImportant: Boolean, 
        val extremeValue: Double?, val logId: String?, val durationMs: Long, 
        val isSpecial: Boolean, val specialColor: Int?, 
        val lat: Double, val lng: Double, val accuracy: Double, 
        val maxAccuracy: Double, val snr: Double?, val vibe: Double?
    ) : AlarmEvent()
}

/**
 * AppAlarmManager: Evaluates system health and manages siren states.
 * Sep.23.60:
 * - Issue #1270/1280 RESOLVED: Integrated siren trigger orchestration into the 
 *   alarm evaluation loop. Background services now physically activate the 
 *   AudioSynthesizer when violations are active and stealth rules allow (R-ID 418).
 * Sep.23.06:
 * - Issue #1164 RESOLVED: Enhanced alarm history serialization by embedding firstTriggerTs,
 *   firstTriggerRt, lastLogTs, and lastLogRt inside JSON persistence to preserve alarm duration
 *   and debounce tracking across process lifetimes.
 * Sep.22.50:
 * - Issue #1164 RESOLVED: Implemented persistence for logic state (geofence debounce,
 *   power latches, and siren timers) to ensure reliability across process restarts (R-ID 417).
 */
@Singleton
class AppAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val sessionManager: SessionManager,
    private val notificationManager: AppNotificationManager,
    private val timeProvider: TimeProvider,
    private val audioSynthesizer: AudioSynthesizer
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _alarmEvents = MutableSharedFlow<AlarmEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val alarmEvents: SharedFlow<AlarmEvent> = _alarmEvents.asSharedFlow()

    private val activeAlarms = mutableMapOf<String, AlarmEvaluation>()
    private var lastAlarmsJson = "[]"
    private var currentSettings = AlertSettings()

    // Persistent flyweights for zero-churn evaluation
    private val evaluationReport = SystemHealthReport()
    private val evaluationState = AlarmEvaluationState()

    private var firstViolationTs: Long = 0L
    private var firstViolationRt: Long = 0L
    private var firstViolationWasJump: Boolean = false
    private var distanceViolationCounter: Int = 0
    private var wasDistanceViolated: Boolean = false
    private var powerAlarmPending: Boolean = false
    
    private var lastSirenStopRt: Long = 0L
    private var lastGlobalTriggerRt: Long = 0L
    private var isTrackerMode: Boolean = false

    fun updateSettings(settings: AlertSettings) {
        this.currentSettings = settings
    }

    fun getSettings(): AlertSettings = currentSettings

    fun setPowerAlarmPending(pending: Boolean) {
        if (this.powerAlarmPending != pending) {
            this.powerAlarmPending = pending
            saveLogicState()
        }
    }

    fun hasUnresolvedAlarms(): Boolean {
        synchronized(activeAlarms) {
            return activeAlarms.values.any { !it.isResolved }
        }
    }

    fun getUnresolvedAlarmTypes(): Set<String> {
        synchronized(activeAlarms) {
            return activeAlarms.filterValues { !it.isResolved }.keys.toSet()
        }
    }

    fun getUnresolvedAlarmsSummary(): String {
        synchronized(activeAlarms) {
            return activeAlarms.values.filter { !it.isResolved }.joinToString(", ") { it.title }
        }
    }

    fun shouldPlaySiren(): Boolean {
        if (isTrackerMode) return false
        if (currentSettings.globalMute) return false
        if (!hasUnresolvedAlarms()) return false
        val nowRt = timeProvider.elapsedRealtime()
        
        if (lastSirenStopRt > 0L && nowRt - lastSirenStopRt < SIREN_RESUME_COOLDOWN_MS) return false
        if (nowRt < audioSynthesizer.getSilencedUntilRt()) return false
        return true
    }
    
    fun notifySirenManualStop() {
        lastSirenStopRt = timeProvider.elapsedRealtime()
        saveLogicState()
    }

    fun restoreState(json: String) {
        if (json.isEmpty() || json == "[]") return
        try {
            val array = JSONArray(json)
            synchronized(activeAlarms) {
                activeAlarms.clear()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val type = obj.getString("type")
                    activeAlarms[type] = AlarmEvaluation(
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
    }

    /**
     * restoreLogicState: Restores geofence debounce and power latches from AppSettings.
     * v9.3.5 (Issue #1164): Ensures logic stability across process restarts.
     */
    fun restoreLogicState(s: AppSettings) {
        firstViolationTs = s.firstViolationTs
        firstViolationRt = s.firstViolationRt
        firstViolationWasJump = s.firstViolationWasJump
        distanceViolationCounter = s.distanceViolationCounter
        wasDistanceViolated = s.wasDistanceViolated
        powerAlarmPending = s.powerAlarmPending
        lastSirenStopRt = s.lastSirenStopRt
        lastGlobalTriggerRt = s.lastGlobalTriggerRt
        
        evaluationState.firstViolationTs = firstViolationTs
        evaluationState.firstViolationRt = firstViolationRt
        evaluationState.firstViolationWasJump = firstViolationWasJump
        evaluationState.wasDistanceViolated = wasDistanceViolated
        evaluationState.distanceViolationCounter = distanceViolationCounter
        evaluationState.forensicReliabilityDegradationStartRt = s.forensicReliabilityDegradationStartRt
        
        Timber.i("Logic State Restored: GeoCounter: $distanceViolationCounter, WasViolated: $wasDistanceViolated")
    }

    private fun saveLogicState() {
        scope.launch {
            repository.saveLogicState(
                firstViolationTs = firstViolationTs,
                firstViolationRt = firstViolationRt,
                firstViolationWasJump = firstViolationWasJump,
                distanceViolationCounter = distanceViolationCounter,
                wasDistanceViolated = wasDistanceViolated,
                powerAlarmPending = powerAlarmPending,
                lastSirenStopRt = lastSirenStopRt,
                lastGlobalTriggerRt = lastGlobalTriggerRt,
                forensicReliabilityDegradationStartRt = evaluationState.forensicReliabilityDegradationStartRt
            )
        }
    }

    fun evaluateAlarms(
        telemetry: AlarmTelemetrySnapshot,
        serviceContext: AlarmServiceContext
    ) {
        this.isTrackerMode = serviceContext.isTrackerMode
        val versionTag = "[${BuildConfig.VERSION_NAME}]"
        
        syncEvaluationState(telemetry, serviceContext)

        val report = MainAlarmLogic.detectViolations(
            state = evaluationState,
            timeProvider = timeProvider,
            report = evaluationReport,
            onSpike = { message, duration ->
                _alarmEvents.tryEmit(AlarmEvent.LogEvent(
                    type = ALERT_ID_PERFORMANCE_SPIKE,
                    message = "$versionTag $message",
                    isImportant = false,
                    extremeValue = duration.toDouble(),
                    logId = null,
                    durationMs = duration,
                    isSpecial = true,
                    specialColor = FORENSIC_PINK_COLOR,
                    lat = telemetry.lat, lng = telemetry.lng, accuracy = telemetry.accuracy,
                    maxAccuracy = telemetry.maxAccuracy, snr = telemetry.snrSnapshot, vibe = telemetry.vibeSnapshot
                ))
            }
        )
        
        processViolationReport(report, serviceContext.now, serviceContext.nowRt, versionTag, telemetry.lat, telemetry.lng, telemetry.accuracy, telemetry.maxAccuracy, telemetry.snrSnapshot, telemetry.vibeSnapshot)

        // Issue #1270/1280: Integrated Siren Orchestration
        val needsSiren = shouldPlaySiren()
        val isCurrentlyPlaying = audioSynthesizer.isPlaying()

        if (needsSiren && !isCurrentlyPlaying) {
            audioSynthesizer.playSiren(
                timeProvider = timeProvider,
                isTrackerMode = isTrackerMode,
                vibrate = true,
                force = true
            )
        } else if (!needsSiren && isCurrentlyPlaying) {
            // Stop if no alarms or if stealth/mute rules apply
            if (!hasUnresolvedAlarms() || isTrackerMode || currentSettings.globalMute) {
                audioSynthesizer.stopSiren(timeProvider = timeProvider)
            }
        }
    }

    private fun syncEvaluationState(
        telemetry: AlarmTelemetrySnapshot,
        serviceContext: AlarmServiceContext
    ) {
        evaluationState.health.update(
            signalLoss = telemetry.isSignalLoss, 
            gpsStalled = telemetry.isGpsStalling, 
            gpsHardwareLock = telemetry.isGpsHardwareLock, 
            localInternetLoss = telemetry.localInternetLoss,
            isHardwareOnline = telemetry.isHardwareOnline, 
            batteryLevel = telemetry.battery, 
            batteryTemp = telemetry.temp,
            isCharging = false, // Derived from currentMa in evaluatePhysical
            currentMa = telemetry.currentMa, 
            status = telemetry.status, 
            isJammer = telemetry.isJammer,
            isTamperDetected = telemetry.isTamperDetected,
            tiltDegrees = telemetry.tiltDegrees, 
            acousticDb = telemetry.acousticDb, 
            baroAlt = telemetry.baroAlt, 
            lux = telemetry.lux, 
            isNear = telemetry.isNear, 
            luxBaseline = telemetry.luxBaseline, 
            acousticFloorDb = telemetry.acousticFloorDb, 
            adaptiveVibrationFloor = telemetry.adaptiveVibrationFloor, 
            peakVibrationShock = telemetry.peakVibrationShock,
            isPowerTamper = telemetry.isPowerTamper, 
            isLocationPending = telemetry.isLocationPending,
            locationPendingReason = telemetry.locationPendingReason, 
            isPowerSaveMode = telemetry.isPowerSaveMode,
            standbyBucket = telemetry.standbyBucket, 
            netInterface = telemetry.netInterface,
            isStorageLow = telemetry.isStorageLow, 
            isStorageCritical = telemetry.isStorageCritical,
            isBatterySteepDischarge = telemetry.isBatterySteepDischarge, 
            isCoolingModeActive = telemetry.isCoolingModeActive,
            vibration = telemetry.vibeSnapshot ?: 0.0, 
            cpuLoad = telemetry.cpuLoad, 
            ioWait = telemetry.ioWait, 
            maxIoLatency = telemetry.maxIoLatency, 
            isSilentFailure = telemetry.isSilentFailure, 
            isMaliAnomaly = telemetry.isMaliAnomaly, 
            isUltraLongStationary = telemetry.isUltraLongStationary,
            isBatteryLow = telemetry.isBatteryLow, 
            isBatteryCritical = telemetry.isBatteryCritical,
            tamperNote = telemetry.tamperNote
        )

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
            lastAlarmAckTs = repository.getLastAlarmAckTsSync(), 
            appStartTime = serviceContext.appStartTime,
            isRelayConnected = serviceContext.isRelayConnected, 
            isTrackerConnected = serviceContext.isTrackerConnected,
            discoveryPhase = serviceContext.discoveryPhase ?: when {
                serviceContext.nowRt - serviceContext.serviceStartRt < BOOTSTRAP_PHASE_MS -> DiscoveryPhase.BOOTSTRAP
                serviceContext.nowRt - serviceContext.serviceStartRt < BOOTSTRAP_PHASE_MS + DISCOVERY_PHASE_MS -> DiscoveryPhase.DISCOVERING
                else -> DiscoveryPhase.MONITORING
            },
            trackerLat = telemetry.lat, 
            trackerLng = telemetry.lng, 
            trackerGpsAccuracy = telemetry.accuracy,
            maxTrackerAccuracy = telemetry.maxAccuracy, 
            lastGpsPacketTs = telemetry.gpsTs, 
            lastGpsPacketRt = 0L, // Handled internally by evaluateGeofence via nowRt
            trackerLastValidFixTs = 0L, // Deprecated in favor of RT
            trackerLastValidFixRt = telemetry.lastValidFixRt,
            trackerSpeed = telemetry.speed, 
            jumpTier = telemetry.jumpTier, 
            isAdaptiveJump = telemetry.isAdaptiveJump, 
            trackerBattery = telemetry.battery, 
            trackerTemp = telemetry.temp,
            wasDistanceViolated = wasDistanceViolated, 
            distanceViolationCounter = distanceViolationCounter,
            firstViolationTs = firstViolationTs, 
            firstViolationRt = firstViolationRt,
            firstViolationWasJump = firstViolationWasJump, 
            maxDistance = serviceContext.maxDistanceAuthority, 
            distToHomeAuthority = serviceContext.distToHomeAuthority, 
            isGpsGap = telemetry.isGpsGap, 
            trackerBaroAltEma = telemetry.baroAltEma,
            isTrackerMode = serviceContext.isTrackerMode, 
            capabilities = serviceContext.capabilities,
            vibrationSensitivity = currentSettings.vibrationSensitivity,
            tiltSensitivity = currentSettings.tiltSensitivity
        )
    }

    private fun processViolationReport(
        report: SystemHealthReport,
        now: Long,
        nowRt: Long,
        versionTag: String,
        lat: Double,
        lng: Double,
        accuracy: Double,
        maxAccuracy: Double,
        snr: Double?,
        vibe: Double?
    ) {
        val oldWasViolated = wasDistanceViolated
        val oldCounter = distanceViolationCounter
        val oldRt = firstViolationRt
        val oldStallRt = evaluationState.forensicReliabilityDegradationStartRt
        
        wasDistanceViolated = evaluationState.wasDistanceViolated
        distanceViolationCounter = evaluationState.distanceViolationCounter
        firstViolationTs = evaluationState.firstViolationTs
        firstViolationRt = evaluationState.firstViolationRt
        firstViolationWasJump = evaluationState.firstViolationWasJump

        val newAlarms = mutableMapOf<String, AlarmEvaluation>()
        var triggerOccurredInThisCycle = false
        
        report.reports.forEach { violation ->
            val type = violation.type
            val enabled = isAlarmEnabled(type)
            val isSpecial = isSpecialType(type)
            val specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null
            val eval = synchronized(activeAlarms) { activeAlarms[type] } ?: AlarmEvaluation(type, violation.title)
            
            if (violation.conditionMet && enabled) {
                if (!eval.isTriggered || eval.isResolved) {
                    if ((nowRt - lastGlobalTriggerRt) >= ALERT_TRIGGER_GRACE_PERIOD_MS) {
                        eval.isTriggered = true; eval.firstTriggerTs = now; eval.firstTriggerRt = nowRt; eval.isResolved = false
                        triggerOccurredInThisCycle = true
                        _alarmEvents.tryEmit(AlarmEvent.LogEvent(type, "$versionTag ALARM TRIGGERED: ${violation.title}", true, violation.extremeValue, null, 0L, isSpecial, specialColor, lat, lng, accuracy, maxAccuracy, snr, vibe))
                        if (nowRt - lastSirenStopRt < SIREN_RESUME_COOLDOWN_MS) lastSirenStopRt = 0L 
                    }
                }
                eval.lastLogTs = now; eval.lastLogRt = nowRt; eval.title = violation.title; eval.subtitle = violation.subtitle
                newAlarms[type] = eval
            } else if (eval.isTriggered) {
                if (!eval.isResolved) {
                    eval.isResolved = true
                    val durationMs = if (eval.firstTriggerRt > 0) nowRt - eval.firstTriggerRt else now - eval.firstTriggerTs
                    _alarmEvents.tryEmit(AlarmEvent.LogEvent(type, "$versionTag ALARM RESOLVED: ${violation.title}", false, violation.extremeValue, null, durationMs, isSpecial, specialColor, lat, lng, accuracy, maxAccuracy, snr, vibe))
                }
                newAlarms[type] = eval
            }
        }

        if (triggerOccurredInThisCycle) {
            lastGlobalTriggerRt = nowRt
        }
        
        synchronized(activeAlarms) { activeAlarms.clear(); activeAlarms.putAll(newAlarms) }
        updateAlarmsJson()
        
        val stateChanged = wasDistanceViolated != oldWasViolated || distanceViolationCounter != oldCounter || firstViolationRt != oldRt || triggerOccurredInThisCycle || evaluationState.forensicReliabilityDegradationStartRt != oldStallRt
        if (stateChanged) {
            saveLogicState()
        }
    }

    fun dismissResolvedAlarms() {
        synchronized(activeAlarms) {
            val iterator = activeAlarms.entries.iterator()
            while (iterator.hasNext()) { if (iterator.next().value.isResolved) iterator.remove() }
        }
        updateAlarmsJson()
    }

    private fun updateAlarmsJson() {
        val jsonArray = JSONArray()
        synchronized(activeAlarms) {
            activeAlarms.values.forEach { eval ->
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
        if (newJson != lastAlarmsJson) { lastAlarmsJson = newJson; repository.saveAlarmsJsonSync(newJson) }
    }

    private fun isAlarmEnabled(type: String): Boolean {
        return when (type) {
            ALERT_ID_LOCAL_INTERNET -> currentSettings.localInternet
            ALERT_ID_RELAY_OFFLINE -> currentSettings.relayConnection
            ALERT_ID_TRACKER_OFFLINE -> currentSettings.serverConnection
            ALERT_ID_SIGNAL_LOSS -> currentSettings.signalLoss
            ALERT_ID_JUMP_ALERT -> currentSettings.jammerDetection
            ALERT_ID_TRACKER_GEOFENCE -> currentSettings.distance
            ALERT_ID_GPS_STALL -> currentSettings.gpsStalling
            ALERT_ID_TRACKER_GAP -> currentSettings.longTimeGap
            ALERT_ID_TRACKER_POWER -> currentSettings.power
            ALERT_ID_TRACKER_BATTERY -> currentSettings.lowBattery
            ALERT_ID_BATTERY_STEEP_DISCHARGE -> currentSettings.batteryHealth
            ALERT_ID_TRACKER_TEMP -> currentSettings.highTemperature
            ALERT_ID_TRACKER_TAMPER -> currentSettings.tamperAlert
            ALERT_ID_TRACKER_TILT -> currentSettings.tiltAlert
            ALERT_ID_TRACKER_ACOUSTIC -> currentSettings.acousticAlert
            ALERT_ID_TRACKER_LIFT -> currentSettings.liftAlert
            ALERT_ID_SYSTEM_STORAGE_LOW -> currentSettings.systemStorageLow
            ALERT_ID_SYSTEM_STORAGE_CRITICAL -> true
            ALERT_ID_HARDWARE_CONFIGURATION -> true
            else -> true
        }
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
        synchronized(activeAlarms) { activeAlarms.clear() }
        lastAlarmsJson = "[]"; repository.saveAlarmsJsonSync("[]")
        wasDistanceViolated = false; distanceViolationCounter = 0; firstViolationTs = 0L; firstViolationRt = 0L
        evaluationState.firstViolationTs = 0L; evaluationState.firstViolationRt = 0L; evaluationState.wasDistanceViolated = false; evaluationState.distanceViolationCounter = 0
        
        val nowRt = timeProvider.elapsedRealtime()
        if (nowRt - lastSirenStopRt > SIREN_RESUME_COOLDOWN_MS) {
            lastSirenStopRt = 0L
        }
        
        lastGlobalTriggerRt = 0L
        saveLogicState()
    }

    private data class AlarmEvaluation(
        val type: String, var title: String, var subtitle: String = "",
        var isTriggered: Boolean = false, var firstTriggerTs: Long = 0L, var firstTriggerRt: Long = 0L,
        var lastLogTs: Long = 0L, var lastLogRt: Long = 0L, var isResolved: Boolean = true
    )
}
