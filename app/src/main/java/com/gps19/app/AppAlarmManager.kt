package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 * July.29.01:
 * - Issue #623: Structural: Latency Monitor Metric Cleanup. Standardized spike 
 *   reporting and utilized pre-formatted forensic messages from measureAndAudit.
 * July.28.22:
 * - Issue #617: Global SharedFlow Audit. Hardened _alarmEvents.
 */
@Singleton
class AppAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val sessionManager: SessionManager,
    private val notificationManager: AppNotificationManager,
    private val timeProvider: TimeProvider
) {
    private val _alarmEvents = MutableSharedFlow<AlarmEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val alarmEvents: SharedFlow<AlarmEvent> = _alarmEvents.asSharedFlow()

    private val activeAlarms = mutableMapOf<String, AlarmEvaluation>()
    private var lastAlarmsJson = "[]"
    private var currentSettings = AlertSettings()

    private var firstViolationTs: Long = 0L
    private var firstViolationRt: Long = 0L
    private var firstViolationWasJump: Boolean = false
    private var distanceViolationCounter: Int = 0
    private var wasDistanceViolated: Boolean = false
    private var powerAlarmPending: Boolean = false
    
    private var lastSirenStopTs: Long = 0L
    private var lastGlobalTriggerTs: Long = 0L
    private var isTrackerMode: Boolean = false

    fun updateSettings(settings: AlertSettings) {
        this.currentSettings = settings
    }

    fun getSettings(): AlertSettings = currentSettings

    fun setPowerAlarmPending(pending: Boolean) {
        this.powerAlarmPending = pending
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
        if (nowRt - lastSirenStopTs < SIREN_RESUME_COOLDOWN_MS) return false
        if (nowRt < AudioSynthesizer.getSilencedUntilRt()) return false
        return true
    }
    
    fun notifySirenManualStop() {
        lastSirenStopTs = timeProvider.elapsedRealtime()
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
                        isResolved = obj.optBoolean("isResolved", true)
                    )
                }
            }
            lastAlarmsJson = json
        } catch (e: Exception) {
            Timber.e(e, "Siren Persistence: Failed to restore alarm state")
        }
    }

    fun evaluateAlarms(
        now: Long, nowRt: Long, serviceStartTs: Long, serviceStartRt: Long, appStartTime: Long,
        isTrackerMode: Boolean, isRelayConnected: Boolean, isTrackerConnected: Boolean,
        status: SentinelStatus, isJammer: Boolean = false, jumpTier: Int = 0,
        trackerLat: Double, trackerLng: Double, trackerAccuracy: Double, maxTrackerAccuracy: Double,
        trackerLastGpsTs: Long, trackerLastGpsRt: Long = 0L, trackerLastValidFixTs: Long = 0L,
        trackerLastValidFixRt: Long = 0L, trackerSpeed: Double, trackerBattery: Int,
        trackerTemp: Double, isHardwareOnline: Boolean, isLocalInternetLoss: Boolean,
        isSignalLoss: Boolean, isGpsStalling: Boolean, isUiVisible: Boolean,
        distToHomeAuthority: Double?, maxDistanceAuthority: Double, isGpsGap: Boolean,
        isTamperDetected: Boolean, isPowerTamper: Boolean, trackerTiltDegrees: Double,
        trackerAcousticDb: Double, trackerBaroAlt: Double, trackerBaroAltEma: Double = 0.0, 
        trackerLux: Double, isNear: Boolean, luxBaseline: Double, acousticFloorDb: Double,
        adaptiveVibrationFloor: Double, peakVibrationShock: Double, trackerCurrentMa: Int,
        isPowerSaveMode: Boolean = false, standbyBucket: Int = -1, netInterface: String = "UNKNOWN",
        isStorageLow: Boolean = false, isStorageCritical: Boolean = false,
        isBatterySteepDischarge: Boolean = false, isCoolingModeActive: Boolean = false,
        discoveryPhase: DiscoveryPhase? = null, capabilities: HardwareCapabilities = HardwareCapabilities(),
        isLocationPending: Boolean = false, locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
        snrSnapshot: Double? = null, vibeSnapshot: Double? = null
    ) {
        this.isTrackerMode = isTrackerMode
        val versionTag = "[${BuildConfig.VERSION_NAME}]"
        
        val evaluationState = AlarmEvaluationState(
            now = now, nowRt = nowRt, serviceStartTime = serviceStartTs, serviceStartRt = serviceStartRt,
            lastAlarmAckTs = repository.getLastAlarmAckTsSync(), appStartTime = appStartTime,
            isRelayConnected = isRelayConnected, isTrackerConnected = isTrackerConnected,
            discoveryPhase = discoveryPhase ?: when {
                nowRt - serviceStartRt < BOOTSTRAP_PHASE_MS -> DiscoveryPhase.BOOTSTRAP
                nowRt - serviceStartRt < BOOTSTRAP_PHASE_MS + DISCOVERY_PHASE_MS -> DiscoveryPhase.DISCOVERING
                else -> DiscoveryPhase.MONITORING
            },
            trackerLat = trackerLat, trackerLng = trackerLng,
            homePoints = repository.getCachedHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) },
            maxDistance = maxDistanceAuthority, trackerGpsAccuracy = trackerAccuracy, maxTrackerAccuracy = maxTrackerAccuracy,
            lastGpsPacketTs = trackerLastGpsTs, lastGpsPacketRt = trackerLastGpsRt,
            trackerLastValidFixTs = trackerLastValidFixTs, trackerLastValidFixRt = trackerLastValidFixRt,
            trackerSpeed = trackerSpeed, jumpTier = jumpTier, trackerBattery = trackerBattery, trackerTemp = trackerTemp,
            wasDistanceViolated = wasDistanceViolated, distanceViolationCounter = distanceViolationCounter,
            firstViolationTs = firstViolationTs, firstViolationRt = firstViolationRt,
            firstViolationWasJump = firstViolationWasJump, distToHomeAuthority = distToHomeAuthority,
            isGpsGap = isGpsGap, trackerBaroAltEma = trackerBaroAltEma, isTrackerMode = isTrackerMode,
            health = SystemHealthState(
                isHardwareOnline = isHardwareOnline, localInternetLoss = isLocalInternetLoss, signalLoss = isSignalLoss,
                gpsStalled = isGpsStalling, status = status, isJammer = isJammer, batteryLevel = trackerBattery,
                batteryTemp = trackerTemp, isTamperDetected = isTamperDetected, tiltDegrees = trackerTiltDegrees,
                acousticDb = trackerAcousticDb, baroAlt = trackerBaroAlt, lux = trackerLux, isNear = isNear,
                luxBaseline = luxBaseline, acousticFloorDb = acousticFloorDb, adaptiveVibrationFloor = adaptiveVibrationFloor,
                peakVibrationShock = peakVibrationShock, currentMa = trackerCurrentMa, isPowerTamper = isPowerTamper,
                isLocationPending = isLocationPending, locationPendingReason = locationPendingReason,
                isPowerSaveMode = isPowerSaveMode, standbyBucket = standbyBucket, netInterface = netInterface,
                isStorageLow = isStorageLow, isStorageCritical = isStorageCritical,
                isBatterySteepDischarge = isBatterySteepDischarge, isCoolingModeActive = isCoolingModeActive
            ),
            capabilities = capabilities
        )

        val report = MainAlarmLogic.detectViolations(
            state = evaluationState,
            timeProvider = timeProvider,
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
                    lat = trackerLat, lng = trackerLng, accuracy = trackerAccuracy,
                    maxAccuracy = maxTrackerAccuracy, snr = snrSnapshot, vibe = vibeSnapshot
                ))
            }
        )
        
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
                    if ((nowRt - lastGlobalTriggerTs) >= ALERT_TRIGGER_GRACE_PERIOD_MS) {
                        eval.isTriggered = true; eval.firstTriggerTs = now; eval.firstTriggerRt = nowRt; eval.isResolved = false
                        triggerOccurredInThisCycle = true
                        _alarmEvents.tryEmit(AlarmEvent.LogEvent(type, "$versionTag ALARM TRIGGERED: ${violation.title}", true, violation.extremeValue, null, 0L, isSpecial, specialColor, trackerLat, trackerLng, trackerAccuracy, maxTrackerAccuracy, snrSnapshot, vibeSnapshot))
                        if (nowRt - lastSirenStopTs < SIREN_RESUME_COOLDOWN_MS) lastSirenStopTs = 0L 
                    }
                }
                eval.lastLogTs = now; eval.lastLogRt = nowRt; eval.title = violation.title; eval.subtitle = violation.subtitle
                newAlarms[type] = eval
            } else if (eval.isTriggered) {
                if (!eval.isResolved) {
                    eval.isResolved = true
                    val durationMs = if (eval.firstTriggerRt > 0) nowRt - eval.firstTriggerRt else now - eval.firstTriggerTs
                    _alarmEvents.tryEmit(AlarmEvent.LogEvent(type, "$versionTag ALARM RESOLVED: ${violation.title}", false, violation.extremeValue, null, durationMs, isSpecial, specialColor, trackerLat, trackerLng, trackerAccuracy, maxTrackerAccuracy, snrSnapshot, vibeSnapshot))
                }
                newAlarms[type] = eval
            }
        }

        if (triggerOccurredInThisCycle) lastGlobalTriggerTs = nowRt
        synchronized(activeAlarms) { activeAlarms.clear(); activeAlarms.putAll(newAlarms) }
        updateAlarmsJson()
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
        lastSirenStopTs = 0L; lastGlobalTriggerTs = 0L
    }

    private data class AlarmEvaluation(
        val type: String, var title: String, var subtitle: String = "",
        var isTriggered: Boolean = false, var firstTriggerTs: Long = 0L, var firstTriggerRt: Long = 0L,
        var lastLogTs: Long = 0L, var lastLogRt: Long = 0L, var isResolved: Boolean = true
    )
}
