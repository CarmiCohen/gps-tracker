package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * AppAlarmManager: Evaluates system health and manages siren states.
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor and @Singleton.
 * July.20.07:
 * - Issue #102: Temporal Forensic Integrity. Propagated monotonic 'rt' timestamps 
 *   to AlarmEvaluationState for skew-immune alarm evaluation.
 */
@Singleton
class AppAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val sessionManager: SessionManager,
    private val notificationManager: AppNotificationManager,
    private val timeProvider: TimeProvider
) {
    interface Listener {
        fun onLogEvent(
            type: String, message: String, important: Boolean, extremeValue: Double?, 
            logId: String?, durationMs: Long, isSpecial: Boolean, specialColor: Int?, 
            lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double, 
            snr: Double?, vibe: Double?
        )
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    private val activeAlarms = mutableMapOf<String, AlarmEvaluation>()
    private var lastAlarmsJson = "[]"
    private var currentSettings = AlertSettings()

    private var firstViolationTs: Long = 0L
    private var firstViolationRt: Long = 0L // Monotonic
    private var firstViolationWasJump: Boolean = false
    private var distanceViolationCounter: Int = 0
    private var wasDistanceViolated: Boolean = false
    private var powerAlarmPending: Boolean = false
    
    private var lastSirenStopTs: Long = 0L
    private var lastGlobalTriggerTs: Long = 0L

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
        if (currentSettings.globalMute) return false
        if (!hasUnresolvedAlarms()) return false
        
        val now = timeProvider.elapsedRealtime()
        if (now - lastSirenStopTs < SIREN_RESUME_COOLDOWN_MS) {
            return false
        }
        
        return true
    }
    
    fun notifySirenManualStop() {
        lastSirenStopTs = timeProvider.elapsedRealtime()
    }

    fun evaluateAlarms(
        now: Long,
        nowRt: Long, // Monotonic
        serviceStartTs: Long,
        serviceStartRt: Long, // Monotonic
        appStartTime: Long,
        isTrackerMode: Boolean,
        isRelayConnected: Boolean,
        isTrackerConnected: Boolean,
        status: SentinelStatus,
        isJammer: Boolean = false,
        jumpTier: Int = 0,
        trackerLat: Double,
        trackerLng: Double,
        trackerAccuracy: Double,
        maxTrackerAccuracy: Double,
        trackerLastGpsTs: Long,
        trackerLastGpsRt: Long = 0L,
        trackerLastValidFixTs: Long = 0L,
        trackerLastValidFixRt: Long = 0L,
        trackerSpeed: Double,
        trackerBattery: Int,
        trackerTemp: Double,
        isHardwareOnline: Boolean,
        isLocalInternetLoss: Boolean,
        isSignalLoss: Boolean,
        isGpsStalling: Boolean,
        isUiVisible: Boolean,
        distToHomeAuthority: Double?,
        maxDistanceAuthority: Double,
        isGpsGap: Boolean,
        isTamperDetected: Boolean,
        isPowerTamper: Boolean,
        trackerTiltDegrees: Double,
        trackerAcousticDb: Double,
        trackerBaroAlt: Double,
        trackerBaroAltEma: Double = 0.0, 
        trackerLux: Double,
        isNear: Boolean,
        luxBaseline: Double,
        acousticFloorDb: Double,
        adaptiveVibrationFloor: Double,
        peakVibrationShock: Double,
        trackerCurrentMa: Int,
        isPowerSaveMode: Boolean = false,
        standbyBucket: Int = -1,
        netInterface: String = "UNKNOWN",
        isStorageLow: Boolean = false,
        isStorageCritical: Boolean = false,
        isBatterySteepDischarge: Boolean = false,
        isCoolingModeActive: Boolean = false,
        discoveryPhase: DiscoveryPhase? = null,
        capabilities: HardwareCapabilities = HardwareCapabilities(),
        isLocationPending: Boolean = false,
        locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
        snrSnapshot: Double? = null,
        vibeSnapshot: Double? = null
    ) {
        val versionTag = "[${BuildConfig.VERSION_NAME}]"
        
        val lastAlarmAckTs = repository.getLastAlarmAckTsSync()
        val evaluationState = AlarmEvaluationState(
            now = now,
            nowRt = nowRt,
            serviceStartTime = serviceStartTs,
            serviceStartRt = serviceStartRt,
            lastAlarmAckTs = lastAlarmAckTs,
            appStartTime = appStartTime,
            isRelayConnected = isRelayConnected,
            isTrackerConnected = isTrackerConnected,
            discoveryPhase = discoveryPhase ?: when {
                nowRt - serviceStartRt < BOOTSTRAP_PHASE_MS -> DiscoveryPhase.BOOTSTRAP
                nowRt - serviceStartRt < BOOTSTRAP_PHASE_MS + DISCOVERY_PHASE_MS -> DiscoveryPhase.DISCOVERING
                else -> DiscoveryPhase.MONITORING
            },
            trackerLat = trackerLat,
            trackerLng = trackerLng,
            homePoints = repository.getCachedHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) },
            maxDistance = maxDistanceAuthority,
            trackerGpsAccuracy = trackerAccuracy,
            maxTrackerAccuracy = maxTrackerAccuracy,
            lastGpsPacketTs = trackerLastGpsTs,
            lastGpsPacketRt = trackerLastGpsRt,
            trackerLastValidFixTs = trackerLastValidFixTs,
            trackerLastValidFixRt = trackerLastValidFixRt,
            trackerSpeed = trackerSpeed,
            jumpTier = jumpTier,
            trackerBattery = trackerBattery,
            trackerTemp = trackerTemp,
            wasDistanceViolated = wasDistanceViolated,
            distanceViolationCounter = distanceViolationCounter,
            firstViolationTs = firstViolationTs,
            firstViolationRt = firstViolationRt,
            firstViolationWasJump = firstViolationWasJump,
            distToHomeAuthority = distToHomeAuthority,
            isGpsGap = isGpsGap,
            trackerBaroAltEma = trackerBaroAltEma,
            isTrackerMode = isTrackerMode,
            health = SystemHealthState(
                isHardwareOnline = isHardwareOnline,
                localInternetLoss = isLocalInternetLoss,
                signalLoss = isSignalLoss,
                gpsStalled = isGpsStalling,
                status = status,
                isJammer = isJammer,
                batteryLevel = trackerBattery,
                batteryTemp = trackerTemp,
                isTamperDetected = isTamperDetected,
                tiltDegrees = trackerTiltDegrees,
                acousticDb = trackerAcousticDb,
                baroAlt = trackerBaroAlt,
                lux = trackerLux,
                isNear = isNear,
                luxBaseline = luxBaseline,
                acousticFloorDb = acousticFloorDb,
                adaptiveVibrationFloor = adaptiveVibrationFloor,
                peakVibrationShock = peakVibrationShock,
                currentMa = trackerCurrentMa,
                isPowerTamper = isPowerTamper,
                isLocationPending = isLocationPending,
                locationPendingReason = locationPendingReason,
                isPowerSaveMode = isPowerSaveMode,
                standbyBucket = standbyBucket,
                netInterface = netInterface,
                isStorageLow = isStorageLow,
                isStorageCritical = isStorageCritical,
                isBatterySteepDischarge = isBatterySteepDischarge,
                isCoolingModeActive = isCoolingModeActive
            ),
            capabilities = capabilities
        )

        val report = MainAlarmLogic.detectViolations(evaluationState)
        
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
                    val canTrigger = (nowRt - lastGlobalTriggerTs) >= ALERT_TRIGGER_GRACE_PERIOD_MS
                    if (canTrigger) {
                        eval.isTriggered = true
                        eval.firstTriggerTs = now
                        eval.firstTriggerRt = nowRt
                        eval.isResolved = false
                        triggerOccurredInThisCycle = true
                        listener?.onLogEvent(type, "$versionTag ALARM TRIGGERED: ${violation.title}", true, violation.extremeValue, null, 0L, isSpecial, specialColor, trackerLat, trackerLng, trackerAccuracy, maxTrackerAccuracy, snrSnapshot, vibeSnapshot)
                        
                        if (nowRt - lastSirenStopTs < SIREN_RESUME_COOLDOWN_MS) {
                            lastSirenStopTs = 0L 
                        }
                    }
                }
                eval.lastLogTs = now
                eval.lastLogRt = nowRt
                eval.title = violation.title
                eval.subtitle = violation.subtitle
                newAlarms[type] = eval
            } else if (eval.isTriggered) {
                if (!eval.isResolved) {
                    eval.isResolved = true
                    val durationMs = if (eval.firstTriggerRt > 0) nowRt - eval.firstTriggerRt else now - eval.firstTriggerTs
                    listener?.onLogEvent(type, "$versionTag ALARM RESOLVED: ${violation.title}", false, violation.extremeValue, null, durationMs, isSpecial, specialColor, trackerLat, trackerLng, trackerAccuracy, maxTrackerAccuracy, snrSnapshot, vibeSnapshot)
                }
                newAlarms[type] = eval
            }
        }

        if (triggerOccurredInThisCycle) {
            lastGlobalTriggerTs = nowRt
        }

        synchronized(activeAlarms) {
            activeAlarms.clear()
            activeAlarms.putAll(newAlarms)
        }
        updateAlarmsJson()
    }

    fun dismissResolvedAlarms() {
        synchronized(activeAlarms) {
            val iterator = activeAlarms.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.isResolved) {
                    iterator.remove()
                }
            }
        }
        updateAlarmsJson()
    }

    private fun updateAlarmsJson() {
        val jsonArray = JSONArray()
        synchronized(activeAlarms) {
            activeAlarms.values.forEach { eval ->
                val obj = JSONObject()
                obj.put("type", eval.type)
                obj.put("isTriggered", eval.isTriggered)
                obj.put("isResolved", eval.isResolved)
                obj.put("title", eval.title)
                obj.put("subtitle", eval.subtitle)
                obj.put("isSirenDisabled", currentSettings.globalMute)
                jsonArray.put(obj)
            }
        }
        lastAlarmsJson = jsonArray.toString()
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
        synchronized(activeAlarms) {
            activeAlarms.clear()
        }
        lastAlarmsJson = "[]"
        wasDistanceViolated = false
        distanceViolationCounter = 0
        firstViolationTs = 0L
        firstViolationRt = 0L
        lastSirenStopTs = 0L
        lastGlobalTriggerTs = 0L
    }

    private data class AlarmEvaluation(
        val type: String,
        var title: String,
        var subtitle: String = "",
        var isTriggered: Boolean = false,
        var firstTriggerTs: Long = 0L,
        var firstTriggerRt: Long = 0L,
        var lastLogTs: Long = 0L,
        var lastLogRt: Long = 0L,
        var isResolved: Boolean = true
    )
}
