package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.serialization.Serializable
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Models: UI and Persistence data structures for GPS Tracker.
 * v8.9.7:
 * - Plunge Matching: Added sitVzTs to ConnectionPoint, TrackerStatus, and LocationState for forensic parity.
 * v8.9.6:
 * - Issue 193: Added isTelemetryFresh to DashboardState to resolve Zombie Telemetry UX.
 * v8.9.5:
 * - Issue 192: Added currentMa to ConnectionPoint, LocationState, and IntegrityStateUi for full forensic parity.
 * - Build Fix: Corrected CommitSettings inheritance to UiEvent.
 * v8.9.3:
 * - Issue 188: Preserved historical GPS timestamps in TrailPoint model.
 * v8.9.2:
 * - Issue 182: Synchronized source headers with v8.9.2 baseline.
 */

@Serializable
data class SerializableGeoPoint(val lat: Double, val lng: Double) {
    fun toGeoPoint() = GeoPoint(lat, lng)
}

fun GeoPoint.toSerializable() = SerializableGeoPoint(latitude, longitude)

@Serializable
data class TrailPoint(
    val lat: Double,
    val lng: Double,
    val timestamp: Long = 0L,
    val isJump: Boolean = false
) {
    fun toGeoPoint() = GeoPoint(lat, lng)
}

@Serializable
data class AlertSettings(
    val localInternet: Boolean = true,
    val serverConnection: Boolean = true,
    val relayConnection: Boolean = true,
    val jammerDetection: Boolean = true,
    val signalLoss: Boolean = true,
    val gpsStalling: Boolean = true,
    val distance: Boolean = true,
    val power: Boolean = true,
    val lowBattery: Boolean = true,
    val batteryHealth: Boolean = true, 
    val longTimeGap: Boolean = true,
    val highTemperature: Boolean = true,
    val overrideSilence: Boolean = true,
    val useMaxVolume: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val alarmVolume: Float = 0.8f,
    val useCustomVolume: Boolean = false,
    val tiltAlert: Boolean = true,
    val acousticAlert: Boolean = true,
    val liftAlert: Boolean = true,
    val tamperAlert: Boolean = true,
    val chairOccupied: Boolean = true,
    val globalMute: Boolean = false,
    val systemStorageLow: Boolean = true
)

data class ConnectionPoint(
    val localId: String = UUID.randomUUID().toString(),
    val ts: Long, val rtt: Int, val localSig: Int, val remoteSig: Int,
    val isConnected: Boolean, val isGap: Boolean = false, val gpsAccuracy: Float = 0f,
    val isTick: Boolean = false, val hasGps: Boolean = false, val gpsIndex: Float = 0f,
    val noiseIdx: Float = 0f, val luxIdx: Float = 0f, val vibeIdx: Float = 0f, val proxIdx: Float = 1f,
    val liftIdx: Float = 0f, val snrIdx: Float = 0f,
    val verticalVelocity: Float = 0f,
    val sitVz: Float = 0f, val sitVzTs: Long = 0L, val sitDz: Float = 0f,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val speed: Float = 0f, val bearing: Float = 0f,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val sitBaro: Float = 0f,
    val sitTilt: Float = 0f,
    val sitShock: Float = 0f,
    val currentMa: Int = 0
)

data class ViolationPoint(
    val localId: String = UUID.randomUUID().toString(),
    val point: GeoPoint, val type: String, val ts: Long
)

@Serializable
data class LogEntry(
    val localId: String = UUID.randomUUID().toString(),
    val timestamp: Long, val message: String, val type: String,
    val isImportant: Boolean, val id: String = "", val viewerId: String = "",
    val count: Int = 1, val extremeValue: Double? = null,
    val durationMs: Long = 0L,
    val isSpecial: Boolean = false, 
    val specialColor: Int? = null,    
    val firstSeenTs: Long = 0L,
    val role: String = "tracker"
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("localId", localId); put("timestamp", timestamp)
            put("localTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))
            put("message", message); put("type", type); put("isImportant", isImportant)
            put("id", id); put("viewer_id", viewerId); put("count", count)
            put("duration_ms", durationMs)
            put("is_special", isSpecial)
            put("first_seen_ts", if (firstSeenTs == 0L) timestamp else firstSeenTs)
            put("role", role)
            specialColor?.let { put("special_color", it) }
            extremeValue?.let { if (!it.isNaN() && !it.isInfinite()) put("extreme_value", it) }
        }
    }

    companion object {
        fun fromJSONObject(obj: JSONObject): LogEntry {
            val ts = obj.optLong("timestamp")
            val localId = obj.optString("localId").ifBlank { UUID.randomUUID().toString() }
            
            return LogEntry(
                localId = localId,
                timestamp = ts,
                message = obj.optString("message"),
                type = obj.optString("type"),
                isImportant = obj.optBoolean("isImportant"),
                id = obj.optString("id"),
                viewerId = obj.optString("viewer_id"), // Standardized to viewer_id only
                count = obj.optInt("count", 1),
                durationMs = obj.optLong("duration_ms", 0L),
                isSpecial = obj.optBoolean("is_special", false),
                specialColor = if (obj.has("special_color")) obj.getInt("special_color") else null,
                firstSeenTs = if (obj.has("first_seen_ts")) obj.getLong("first_seen_ts") else ts,
                role = obj.optString("role", "tracker"),
                extremeValue = if (obj.has("extreme_value")) {
                    val ev = obj.optDouble("extreme_value")
                    if (ev.isNaN() || ev.isInfinite()) null else ev
                } else null
            )
        }
    }
}

@Serializable
data class TrackerStatus(
    override val lat: Double = 0.0,
    override val lng: Double = 0.0,
    override val alt: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val accuracy: Float = 0f,
    val maxAccuracy: Float = 0f,
    override val gpsTs: Long = 0L,
    override val ts: Long = 0L,
    val uptimeMs: Long = 0L,
    val lastConnTs: Long = 0L,
    val lastDiscTs: Long = 0L,
    val totalDropMs: Long = 0L,
    val maxDropMs: Long = 0L,
    val maxDropTs: Long = 0L,
    val totalConnectedMs: Long = 0L,
    val sessionConnectedMs: Long = 0L,
    val battery: Int = 100,
    val temp: Float = 0f,
    val maxTemp: Float = 0f,
    val isCharging: Boolean = false,
    val currentMa: Int = 0,
    val satsView: Int = 0,
    val satsUsed: Int = 0,
    val peakVibrationShock: Float = 0f,
    val peakVibrationShockTs: Long = 0L,
    val isPowerTamper: Boolean = false,
    val violationUptimeMs: Long = 0L,
    val violationPercentage: Float = 0f,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val lastSitTs: Long = 0L,
    val verticalVelocity: Float = 0f,
    val sitVz: Float = 0f,
    val sitVzTs: Long = 0L,
    val sitDz: Float = 0f,
    val sitBaro: Float = 0f,
    val sitTilt: Float = 0f,
    val sitShock: Float = 0f,
    val isSuspicious: Boolean = false,
    val isTamperDetected: Boolean = false,
    val vibration: Float = 0f,
    val heading: Float = 0f,
    val tiltDegrees: Float = 0f,
    val acousticDb: Double = 0.0,
    val baroAlt: Float = 0f,
    val lux: Float = 0f,
    val isNear: Boolean = true,
    val luxBaseline: Float = 0f,
    val acousticFloorDb: Double = 0.0,
    val adaptiveVibrationFloor: Float = 0.12f,
    val proxIdx: Float = 1.0f,
    val isClockRegression: Boolean = false,
    val isStalled: Boolean = false,
    val isJammer: Boolean = false,
    val isJump: Boolean = false,
    val isTrajectoryPromoted: Boolean = false,
    val jumpTier: Int = 0,
    val isLocationPending: Boolean = false,
    val isPowerSaveMode: Boolean = false, 
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val gnssDetail: GnssDetail? = null,
    val snrIdx: Float = 0f,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false
) : SpatialAnchor

data class AlarmInfo(val title: String, val subtitle: String, val type: String = "", val isResolved: Boolean = false, val isSirenDisabled: Boolean = false)

enum class TrackerState { MOVING, PARKING, JUMPING, OFFLINE, UNKNOWN }

data class LocationState(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val speed: Float = 0f,
    val accuracy: Float = 0f,
    val bearing: Float = 0f,
    val timestamp: Long = 0L,
    val telemetryTs: Long = 0L, 
    val isVisualJump: Boolean = false,
    val isTrajectoryPromoted: Boolean = false,
    val jumpTier: Int = 0,
    val isJammer: Boolean = false,
    val isStalled: Boolean = false,
    val vibration: Float = 0f,
    val heading: Float = 0f,
    val tiltDegrees: Float = 0f,
    val acousticDb: Double = 0.0,
    val baroAlt: Float = 0f,
    val lux: Float = 0f,
    val isNear: Boolean = true,
    val isSuspicious: Boolean = false,
    val isTamperDetected: Boolean = false,
    val peakVibrationShock: Float = 0f,
    val peakVibrationShockTs: Long = 0L,
    val luxBaseline: Float = 0f,
    val acousticFloorDb: Double = 0.0,
    val adaptiveVibrationFloor: Float = 0.12f,
    val proxIdx: Float = 1.0f,
    val proximityCm: Float = -1.0f,
    val micPending: Boolean = false,
    val isPowerTamper: Boolean = false,
    val violationUptimeMs: Long = 0L,
    val violationPercentage: Float = 0f,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val lastSitTs: Long = 0L,
    val verticalVelocity: Float = 0f,
    val sitVz: Float = 0f,
    val sitVzTs: Long = 0L,
    val sitDz: Float = 0f,
    val sitBaro: Float = 0f,
    val sitTilt: Float = 0f,
    val sitShock: Float = 0f,
    val isClockRegression: Boolean = false,
    val isLocationPending: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val gnssDetail: GnssDetail? = null,
    val snrIdx: Float = 0f,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val currentMa: Int = 0
)

data class DashboardState(
    val maxDrop: String = "00:00:00",
    val lastSeen: String = "--:--:--",
    val totalDrop: String = "00:00:00",
    val watchdogOk: Boolean = true,
    val watchdogCountdown: String = "--",
    val totalUptime: String = "00:00:00",
    val session: String = "00:00:00",
    val sinceConn: String = "00:00:00",
    val sinceDisco: String = "00:00:00",
    val gpsIndex: String = "--",
    val trackerAccuracy: String = "--",
    val satsIndex: String = "--",
    val trackerMaxAcc: String = "--",
    val viewerAccuracy: String = "--",
    val viewerMaxAcc: String = "--",
    val ageIndex: String = "--",
    val accIndex: String = "--",
    val isSatsIndexWarning: Boolean = false,
    val trackerConnIndex: Int = 0,
    val viewerConnIndex: Int = 0,
    val trackerTemp: String = "--",
    val trackerMaxTemp: String = "--",
    val viewerTemp: String = "--",
    val viewerMaxTemp: String = "--",
    val trackerState: TrackerState = TrackerState.UNKNOWN,
    val vibration: String = "--",
    val heading: String = "--",
    val lat: String = "--",
    val lng: String = "--",
    val tilt: String = "--",
    val acoustic: String = "--",
    val lift: String = "--",
    val lux: String = "--",
    val proximity: String = "--",
    val proximityCm: String = "--", 
    val gpsSpeed: String = "--",     
    val isSuspicious: Boolean = false,
    val isTamperDetected: Boolean = false,
    val isSitDetected: Boolean = false,
    val lastSitTs: Long = 0L,
    val peakShock: String = "--",
    val luxBaseline: String = "--",
    val acousticFloor: String = "--",
    val vibrationFloor: String = "--",
    val isMicPending: Boolean = false,
    val isPowerTamper: Boolean = false,
    val violationUptime: String = "00:00:00",
    val violationPercentage: String = "0.0%",
    val lastChairSit: String = "--",
    val engineVersion: String = "--",
    val plungeSpeed: String = "--",
    val chairForensics: String = "--",
    val isLocationPending: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val snr: String = "--",
    val distToHome: String = "--",    
    val distToViewer: String = "--",  
    val isGpsFresh: Boolean = true,    
    val isLinkFresh: Boolean = true,
    val isTelemetryFresh: Boolean = true,
    val isGpsVisible: Boolean = true,  
    val isLinkVisible: Boolean = true,   
    val isBatterySteepDischarge: Boolean = false, 
    val isCoolingModeActive: Boolean = false,
    val currentMa: String = "--"
)

sealed class UiEvent {
    data class ToggleMap(val visible: Boolean) : UiEvent()
    data class ToggleLog(val visible: Boolean) : UiEvent()
    data class ToggleSettings(val visible: Boolean) : UiEvent()
    data class TogglePhoneSetup(val visible: Boolean) : UiEvent()
    data class ToggleRibbons(val visible: Boolean) : UiEvent()
    data class SetRedScreenVisible(val visible: Boolean) : UiEvent()
    data class SetDashboardExpanded(val expanded: Boolean) : UiEvent()
    data class SetUiVisible(val visible: Boolean) : UiEvent()
    object DismissAlarms : UiEvent()
    data class SetAppMode(val mode: String?) : UiEvent()
    data class SetSystemActive(val active: Boolean) : UiEvent()
    data class StopSiren(val causes: String? = null) : UiEvent()
    object ResetStats : UiEvent()
    object ClearLogs : UiEvent()
    object ClearTrails : UiEvent()
    object ClearHomePoints : UiEvent()
    object ManualExit : UiEvent()
    data class LogAction(val type: String, val message: String, val isImportant: Boolean = false, val isSpecial: Boolean = false, val specialColor: Int? = null) : UiEvent()
    data class AddHomePoint(val point: GeoPoint) : UiEvent()
    data class RemoveHomePoint(val index: Int) : UiEvent()
    data class SetGeofenceMode(val mode: GeofenceMode) : UiEvent()
    data class MapTap(val point: GeoPoint) : UiEvent()
    data class SetMaxDistance(val distance: Double) : UiEvent()
    data class SetHomePoints(val points: List<GeoPoint>) : UiEvent()
    object SaveHomePoints : UiEvent()
    data class SetAlertSettings(val settings: AlertSettings) : UiEvent()
    data class SetSirenType(val type: String) : UiEvent()
    data class SetFenceVisible(val visible: Boolean) : UiEvent()
    data class SetViolationsVisible(val visible: Boolean) : UiEvent()
    data class SetGeofenceViolationsVisible(val visible: Boolean) : UiEvent() 
    data class SetMapButtonsVisible(val visible: Boolean) : UiEvent()
    data class SetMapLocked(val locked: Boolean) : UiEvent()
    object MapZoomIn : UiEvent()
    object MapZoomOut : UiEvent()
    object CenterTracker : UiEvent()
    object CenterViewer : UiEvent()
    data class SetDeviceId(val id: String) : UiEvent()
    data class SetViewerId(val id: String) : UiEvent()
    data class SetRelayUrl(val url: String) : UiEvent()
    data class SetJammerSuspicion(val isJammer: Boolean) : UiEvent()
    data class SetSignalLoss(val isSignalLoss: Boolean) : UiEvent()
    data class UpdateDraftDeviceId(val id: String) : UiEvent()
    data class UpdateDraftViewerId(val id: String) : UiEvent()
    data class UpdateDraftRelayUrl(val url: String) : UiEvent()
    data class UpdateDraftMaxDistance(val distance: String) : UiEvent()
    data class UpdateDraftAlertSettings(val settings: AlertSettings) : UiEvent()
    data class UpdateDraftAlarmVolume(val volume: Float) : UiEvent()
    object CommitSettings : UiEvent()
    object RefreshPermissionStatus : UiEvent()
    object TriggerTestAlarm : UiEvent()
    data class ToggleAlertsSetup(val visible: Boolean) : UiEvent()
    data class ToggleAlarmSoundSetup(val visible: Boolean) : UiEvent()
    object ToggleTestSiren : UiEvent()
    object CalibrateChair : UiEvent()
    data class BulkUpdateSettings(
        val deviceId: String? = null,
        val viewerId: String? = null,
        val relayUrl: String? = null,
        val maxDistance: Double? = null,
        val homePoints: List<GeoPoint>? = null,
        val alertSettings: AlertSettings? = null
    ) : UiEvent()
    data class ShowStopTrackingConfirmation(val show: Boolean) : UiEvent()
    object ConfirmStopTracking : UiEvent()
    data class SetSubSettings(val sub: SubSettings?) : UiEvent()
    data class SetLogFilterShowDetails(val show: Boolean) : UiEvent()
    data class SetLogFilterShowRecovered(val show: Boolean) : UiEvent()
    data class ToggleGnssDetail(val visible: Boolean) : UiEvent()
    object ToggleXiaomiManualOverride : UiEvent()
}

sealed class UiCommand {
    object SyncRequest : UiCommand()
    data class UiVisibilityChanged(val visible: Boolean) : UiCommand()
    data class StopSiren(val causes: String? = null) : UiCommand()
    object ClearTrails : UiCommand()
    object StatsReset : UiCommand()
    data class SendSettingsCmd(val data: String) : UiCommand()
    object SettingsUpdated : UiCommand()
    object PushSettings : UiCommand()
    object ZoomIn : UiCommand()
    object ZoomOut : UiCommand()
    object FullInitializationReset : UiCommand()
    object CalibrateChair : UiCommand()
    object TriggerTestAlarm : UiCommand()
    object MapZoomIn : UiCommand()
    object MapZoomOut : UiCommand()
}

data class IntegrityState(
    val signalLoss: Boolean = false, val gpsStalled: Boolean = false, val jammerSuspicion: Boolean = false,
    val localInternetLoss: Boolean = false, val isHardwareOnline: Boolean = true, val batteryLevel: Int = 100,
    val batteryTemp: Float = 0f, val maxTemp: Float = 0f, val isCharging: Boolean = false, val currentMa: Int = 0,
    val activeAlarmsJson: String? = null,
    val isSuspicious: Boolean = false,
    val isTamperDetected: Boolean = false,
    val micPending: Boolean = false,
    val isPowerTamper: Boolean = false,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val lastSitTs: Long = 0L,
    val sitVz: Float = 0f,
    val sitVzTs: Long = 0L,
    val sitDz: Float = 0f,
    val sitBaro: Float = 0f,
    val sitTilt: Float = 0f,
    val sitShock: Float = 0f,
    val isClockRegression: Boolean = false,
    val isLocationPending: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val isBatterySteepDischarge: Boolean = false, 
    val isCoolingModeActive: Boolean = false
)

data class StatsState(
    val totalConnectedMs: Long = 0L, val sessionConnectedMs: Long = 0L,
    val maxDropMs: Long = 0L, val maxDropTs: Long = 0L, val totalDropMs: Long = 0L,
    val uptimeMs: Long = 0L, val lastConnTs: Long = 0L, val lastDiscTs: Long = 0L,
    val violationUptimeMs: Long = 0L, val violationPercentage: Float = 0f
)

data class BatteryState(
    val level: Int = 100, val temp: Float = 0f, val isCharging: Boolean = false, val isChargingStable: Boolean = false
)

data class ConnectivityState(
    val isLocalOnline: Boolean = true, val isRelayConnected: Boolean = false, val isTrackerConnected: Boolean = false,
    val lastUpdateTs: Long = 0L,
    val lastRemoteActivityTs: Long = 0L, val connectedViewers: List<String> = emptyList()
)

data class IntegrityStateUi(
    val signalLoss: Boolean = false, val gpsStalled: Boolean = false, val jammerSuspicion: Boolean = false,
    val localInternetLoss: Boolean = false, val isHardwareOnline: Boolean = true,
    val isSuspicious: Boolean = false, val isTamperDetected: Boolean = false,
    val micPending: Boolean = false,
    val isPowerTamper: Boolean = false,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val lastSitTs: Long = 0L,
    val isClockRegression: Boolean = false,
    val isLocationPending: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val isBatterySteepDischarge: Boolean = false, 
    val isCoolingModeActive: Boolean = false,
    val currentMa: Int = 0
)
