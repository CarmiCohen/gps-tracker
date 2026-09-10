package com.gps19.app

import com.gps19.core.engine.*
import org.osmdroid.util.GeoPoint

/**
 * MainUiState: Persistent and slow-changing state for the UI structure.
 * Sep.10.20:
 * - Idea #243 Rigorous Audit: Consolidated freshness and validity flags into 
 *   MapViewState to eliminate UI-side derived state (R-ID 287).
 * Sep.11.10:
 * - Idea #243: Map State Partitioning RESOLVED. Added MapViewState to group 
 *   map parameters and reduce recomposition cost (R-ID 287).
 */
data class MainUiState(
    val isInitialized: Boolean = false,
    val hydrationLevel: Int = 0, // 0:Cold, 1:Surface, 2:Core, 3:Full, 4:MapBase, 5:MapTrails, 6:MapPositions, 7:MapViolations, 8:MapReady
    val appMode: String? = null,
    val isSystemActive: Boolean = false,
    val deviceId: String = MainRepository.DEFAULT_TRACKER_ID,
    val viewerId: String = MainRepository.DEFAULT_VIEWER_ID,
    val relayUrl: String = SettingsRepository.DEFAULT_RELAY_URL,
    val alertSettings: AlertSettings = AlertSettings(),
    val lastAlarmAckTs: Long = 0L,
    val selectedSirenType: String = "Siren",
    val navigation: NavigationState = NavigationState(isMapVisible = true),
    val homePoints: List<GeoPoint> = emptyList(),
    val geofenceMode: GeofenceMode = GeofenceMode.IDLE,
    val maxDistance: Double = 60.0,
    val permissions: PermissionState = PermissionState(),
    val appStartTime: Long = 0L,
    val centeringTrackerTrigger: Int = 0,
    val centeringViewerTrigger: Int = 0,
    val zoomInTrigger: Int = 0,
    val zoomOutTrigger: Int = 0,
    val isFenceVisible: Boolean = false,
    val isViolationsVisible: Boolean = true,
    val isGeofenceViolationsVisible: Boolean = true,
    val isMapButtonsVisible: Boolean = false,
    val isMapLocked: Boolean = true,
    val mapFollowMode: MapFollowMode = MapFollowMode.AUTO,
    val draftSettings: DraftSettings = DraftSettings(),
    val isIdentitySanitized: Boolean = false,
    val isRecoveryPending: Boolean = false,
    val isSafeMode: Boolean = false,
    val isForensicStallSimulated: Boolean = false,
    val isStorageSimulated: Boolean = false,
    val isStorageCriticalSimulated: Boolean = false,
    val isManualSelectionInProgress: Boolean = false,
    val isSettlingActive: Boolean = true,
    val isSetupBypassActive: Boolean = false
) {
    val isFullyHydrated: Boolean get() = hydrationLevel >= 3
    val isMapHydrated: Boolean get() = hydrationLevel >= 4

    val isSystemReady: Boolean
        get() = isSetupBypassActive || (
                permissions.isFineLocationGranted &&
                permissions.isBatteryWhitelisted && 
                permissions.isAutoStartGranted &&
                permissions.isOverlayGranted &&
                permissions.isMicrophoneGranted &&
                permissions.isExactAlarmGranted && 
                permissions.isPostNotificationsGranted &&
                permissions.isBackgroundLocationGranted &&
                permissions.isActivityRecognitionGranted &&
                (appMode != null) &&
                (appMode != "tracker" || permissions.isMicrophoneGranted) &&
                (appMode == "tracker" || homePoints.isNotEmpty()) &&
                (!permissions.hasBackgroundRestriction || 
                 (permissions.backgroundStatus == CapabilityStatus.GRANTED && permissions.autostartStatus == CapabilityStatus.GRANTED) || 
                 (permissions.backgroundStatus == CapabilityStatus.UNKNOWN && permissions.isManualOverride)))

    val systemIssuesCount: Int
        get() {
            if (isSetupBypassActive) return 0
            var count = 0
            if (!permissions.isFineLocationGranted) count++
            if (!permissions.isBatteryWhitelisted) count++
            if (!permissions.isAutoStartGranted) count++
            if (!permissions.isExactAlarmGranted) count++
            if (!permissions.isOverlayGranted) count++
            if (!permissions.isPostNotificationsGranted) count++
            if (!permissions.isBackgroundLocationGranted) count++
            if (!permissions.isActivityRecognitionGranted) count++
            if (appMode == "tracker" && !permissions.isMicrophoneGranted) count++
            if (appMode != "tracker" && homePoints.isEmpty()) count++
            
            val configIssue = permissions.hasBackgroundRestriction && 
                             (permissions.backgroundStatus == CapabilityStatus.GRANTED || 
                              permissions.autostartStatus == CapabilityStatus.GRANTED) &&
                             !(permissions.backgroundStatus == CapabilityStatus.UNKNOWN && permissions.isManualOverride)
            if (count > 0 || configIssue) {
                // If configIssue is true, at least one is incremented.
            }
            if (configIssue) count++
            
            return count
        }
}

/**
 * MapViewState: Partitioned state for AppMapContainer to reduce recomposition cost (R-ID 287).
 */
data class MapViewState(
    val appMode: String? = null,
    val hydrationLevel: Int = 0,
    val isMapButtonsVisible: Boolean = false,
    val isFenceVisible: Boolean = false,
    val geofenceMode: GeofenceMode = GeofenceMode.IDLE,
    val isViolationsVisible: Boolean = false,
    val isGeofenceViolationsVisible: Boolean = false,
    val maxDistance: Double = 0.0,
    val isMapLocked: Boolean = false,
    val mapFollowMode: MapFollowMode = MapFollowMode.NONE,
    val centeringTrackerTrigger: Int = 0,
    val centeringViewerTrigger: Int = 0,
    val zoomInTrigger: Int = 0,
    val zoomOutTrigger: Int = 0,
    val homePoints: List<GeoPoint> = emptyList(),
    val trackerLat: Double = 0.0,
    val trackerLng: Double = 0.0,
    val trackerSpeed: Double = 0.0,
    val trackerAccuracy: Double = 0.0,
    val trackerMaxAccuracy: Double = 0.0,
    val trackerGpsTs: Long = 0L,
    val trackerTelemetryTs: Long = 0L,
    val trackerLocPending: Boolean = false,
    val trackerLocPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val trackerLastValidFixRt: Long = 0L,
    val viewerLat: Double = 0.0,
    val viewerLng: Double = 0.0,
    val viewerSpeed: Double = 0.0,
    val viewerAccuracy: Double = 0.0,
    val viewerMaxAcc: Double = 0.0,
    val viewerGpsTs: Long = 0L,
    val viewerTelemetryTs: Long = 0L,
    val viewerLocPending: Boolean = false,
    val viewerLocPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val viewerLastValidFixRt: Long = 0L,
    val replayCursorPos: GeoPoint? = null,
    val systemPulse: Long = 0L,
    val systemPulseRt: Long = 0L,
    val trackerSegments: List<MapTrailSegment> = emptyList(),
    val viewerSegments: List<MapTrailSegment> = emptyList(),
    val violations: List<ViolationPoint> = emptyList(),
    val showAccuracyBadge: Boolean = true,
    val showSettingsButton: Boolean = true,
    val showToolsOverlay: Boolean = true,
    
    // R-ID 287 Rigorous Audit additions
    val isTrackerFresh: Boolean = false,
    val isViewerFresh: Boolean = false,
    val isTrackerValid: Boolean = false,
    val isViewerValid: Boolean = false
)

/**
 * KinematicState: High-frequency transient state.
 */
class KinematicState(
    var localLocation: LocationUpdate = LocationUpdate(),
    var trackerLocation: LocationUpdate = LocationUpdate(),
    var localHealth: SystemHealthState = SystemHealthState(),
    var trackerHealth: SystemHealthState = SystemHealthState(),
    var distanceTrackerToHome: Double? = null,
    var distanceTrackerToViewer: Double? = null,
    var distanceViewerToHome: Double? = null,
    var distanceViewerToTracker: Double? = null,
    var replayCursorPos: GeoPoint? = null,
    var pulse: Long = 0L
) {
    fun copyFrom(other: KinematicState) {
        this.localLocation.copyFrom(other.localLocation)
        this.trackerLocation.copyFrom(other.trackerLocation)
        this.localHealth.copyFrom(other.localHealth)
        this.trackerHealth.copyFrom(other.trackerHealth)
        this.distanceTrackerToHome = other.distanceTrackerToHome
        this.distanceTrackerToViewer = other.distanceTrackerToViewer
        this.distanceViewerToHome = other.distanceViewerToHome
        this.distanceViewerToTracker = other.distanceViewerToTracker
        this.replayCursorPos = other.replayCursorPos
        this.pulse = other.pulse
    }

    fun reset() {
        localLocation = LocationUpdate()
        trackerLocation = LocationUpdate()
        localHealth.reset()
        trackerHealth.reset()
        distanceTrackerToHome = null
        distanceTrackerToViewer = null
        distanceViewerToHome = null
        distanceViewerToTracker = null
        replayCursorPos = null
        pulse = 0L
    }
}

/**
 * DiagnosticState: Low-frequency scalar state.
 */
class DiagnosticState(
    var battery: BatteryState = BatteryState(),
    var stats: StatsState = StatsState(),
    var viewerSatsView: Int = 0,
    var viewerSatsUsed: Int = 0,
    var trackerStats: StatsState = StatsState(),
    var trackerBattery: BatteryState = BatteryState(),
    var trackerSatsView: Int = 0,
    var trackerSatsUsed: Int = 0,
    var connectivity: ConnectivityState = ConnectivityState(),
    var activeAlarms: List<AlarmInfo> = emptyList(),
    var isNewViolationDetected: Boolean = false,
    var powerAlarmPending: Boolean = false,
    var isAlarmSilenced: Boolean = false,
    var isSirenPlaying: Boolean = false,
    var isRedScreenVisible: Boolean = false,
    var maxTrackerAccuracy: Double = 0.0,
    var maxViewerAccuracy: Double = 0.0,
    var cumulativeRecoveryBlackoutMs: Long = 0L,
    var recoveryCount: Int = 0,
    var isMaliAnomaly: Boolean = false,
    var isGnssThrottled: Boolean = false,
    var trackerIsGnssThrottled: Boolean = false,
    var pulse: Long = 0L,
    
    // R-ID 259: Energy Footprint structured fields
    var lastEnergyDeltaMa: Int = 0,
    var lastEnergyDeltaTemp: Double = 0.0,
    var lastEnergyDurationMs: Long = 0L
) {
    fun copyFrom(other: DiagnosticState) {
        this.battery.copyFrom(other.battery)
        this.stats.copyFrom(other.stats)
        this.viewerSatsView = other.viewerSatsView
        this.viewerSatsUsed = other.viewerSatsUsed
        this.trackerStats.copyFrom(other.trackerStats)
        this.trackerBattery.copyFrom(other.trackerBattery)
        this.trackerSatsView = other.trackerSatsView
        this.trackerSatsUsed = other.trackerSatsUsed
        this.connectivity.copyFrom(other.connectivity)
        this.activeAlarms = other.activeAlarms
        this.isNewViolationDetected = other.isNewViolationDetected
        this.powerAlarmPending = other.powerAlarmPending
        this.isAlarmSilenced = other.isAlarmSilenced
        this.isSirenPlaying = other.isSirenPlaying
        this.isRedScreenVisible = other.isRedScreenVisible
        this.maxTrackerAccuracy = other.maxTrackerAccuracy
        this.maxViewerAccuracy = other.maxViewerAccuracy
        this.cumulativeRecoveryBlackoutMs = other.cumulativeRecoveryBlackoutMs
        this.recoveryCount = other.recoveryCount
        this.isMaliAnomaly = other.isMaliAnomaly
        this.isGnssThrottled = other.isGnssThrottled
        this.trackerIsGnssThrottled = other.trackerIsGnssThrottled
        this.pulse = other.pulse
        this.lastEnergyDeltaMa = other.lastEnergyDeltaMa
        this.lastEnergyDeltaTemp = other.lastEnergyDeltaTemp
        this.lastEnergyDurationMs = other.lastEnergyDurationMs
    }

    fun reset() {
        battery.reset()
        stats.reset()
        viewerSatsView = 0
        viewerSatsUsed = 0
        trackerStats.reset()
        trackerBattery.reset()
        trackerSatsView = 0
        trackerSatsUsed = 0
        connectivity.reset()
        activeAlarms = emptyList()
        isNewViolationDetected = false
        powerAlarmPending = false
        isAlarmSilenced = false
        isSirenPlaying = false
        isRedScreenVisible = false
        maxTrackerAccuracy = 0.0
        maxViewerAccuracy = 0.0
        cumulativeRecoveryBlackoutMs = 0L
        recoveryCount = 0
        isMaliAnomaly = false
        isGnssThrottled = false
        trackerIsGnssThrottled = false
        pulse = 0L
        lastEnergyDeltaMa = 0
        lastEnergyDeltaTemp = 0.0
        lastEnergyDurationMs = 0L
    }
}

enum class MapFollowMode { TRACKER, VIEWER, AUTO, NONE }

enum class GeofenceMode { IDLE, ADD, REMOVE }

@kotlinx.serialization.Serializable
data class DraftSettings(
    val deviceId: String = "", val viewerId: String = "", val relayUrl: String = "",
    val maxDistance: String = "", val alertSettings: AlertSettings = AlertSettings()
)

data class PermissionState(
    val isFineLocationGranted: Boolean = false,
    val isBatteryWhitelisted: Boolean = false, 
    val isAutoStartGranted: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isMicrophoneGranted: Boolean = false,
    val isExactAlarmGranted: Boolean = false,
    val isPostNotificationsGranted: Boolean = false,
    val isBackgroundLocationGranted: Boolean = false,
    val isActivityRecognitionGranted: Boolean = false,
    val hasBackgroundRestriction: Boolean = false,
    val backgroundStatus: CapabilityStatus = CapabilityStatus.UNKNOWN,
    val autostartStatus: CapabilityStatus = CapabilityStatus.UNKNOWN,
    val isManualOverride: Boolean = false,
    val requiresWakeLockRenewal: Boolean = false,
    val requiresExtraTopPadding: Boolean = false,
    val requiresAdaptationMuzzle: Boolean = false,
    val isA15Device: Boolean = false,
    val isSamsungDevice: Boolean = false
)

data class NavigationState(
    val isMapVisible: Boolean = false, val isLogVisible: Boolean = false, val isSettingsOpen: Boolean = false,
    val isPhoneSetupVisible: Boolean = false, val isDashboardExpanded: Boolean = true,
    val isRibbonsVisible: Boolean = false,
    val isStrictMode: Boolean = false,
    val isGnssDetailVisible: Boolean = false,
    val isStopTrackingConfirmationVisible: Boolean = false,
    val isDiagnosticsVisible: Boolean = false,
    val activeSubSettings: SubSettings? = null,
    val wasMapVisibleBeforeOverlay: Boolean = true,
    val pendingMode: String? = null,
    val replayCursorTs: Long? = null,
    val serviceRecoveryTrigger: Int = 0
)

enum class SubSettings { ALERTS, SOUND, CLEAN }

sealed class UiEvent {
    data class ToggleMap(val visible: Boolean) : UiEvent()
    data class ToggleLog(val visible: Boolean) : UiEvent()
    data class ToggleSettings(val visible: Boolean) : UiEvent()
    data class TogglePhoneSetup(val visible: Boolean) : UiEvent()
    data class ToggleRibbons(val visible: Boolean) : UiEvent()
    data class ToggleStrictMode(val visible: Boolean) : UiEvent()
    data class SetRedScreenVisible(val visible: Boolean) : UiEvent()
    data class SetDashboardExpanded(val expanded: Boolean) : UiEvent()
    data class SetUiVisible(val visible: Boolean) : UiEvent()
    object DismissAlarms : UiEvent()
    data class SetAppMode(val mode: String?) : UiEvent()
    data class SetPendingMode(val mode: String?) : UiEvent()
    data class SetSystemActive(val active: Boolean) : UiEvent()
    data class SetSystemMode(val mode: String) : UiEvent()
    data class StopSiren(val causes: String? = null) : UiEvent()
    object ResetStats : UiEvent()
    object ClearLogs : UiEvent()
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
    data class UpdateDraftDeviceId(val id: String) : UiEvent()
    data class UpdateDraftViewerId(val id: String) : UiEvent()
    data class UpdateDraftRelayUrl(val url: String) : UiEvent()
    data class UpdateDraftMaxDistance(val distance: String) : UiEvent()
    data class UpdateDraftAlertSettings(val settings: AlertSettings) : UiEvent()
    data class UpdateDraftAlarmVolume(val volume: Float) : UiEvent()
    object CommitSettings : UiCommand()
    object RefreshPermissionStatus : UiEvent()
    object RequestTestAlarm : UiEvent()
    data class ToggleAlertsSetup(val visible: Boolean) : UiEvent()
    data class ToggleAlarmSoundSetup(val visible: Boolean) : UiEvent()
    object ToggleTestSiren : UiEvent()
    data class SetJammerSuspicion(val isJammer: Boolean) : UiEvent()
    data class SetSignalLoss(val isSignalLoss: Boolean) : UiEvent()
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
    object DismissIdentitySanitization : UiEvent()
    data class NavigateToDiagnostics(val visible: Boolean) : UiEvent()
    object TriggerRecovery : UiEvent()
    data class SetRecoveryPending(val pending: Boolean) : UiEvent()
    data class SetReplayCursor(val ts: Long?) : UiEvent()
    data class SetForensicSimulation(val active: Boolean) : UiEvent()
    object ExecuteStressTest : UiEvent()
    data class SetStorageSimulation(val active: Boolean, val isCritical: Boolean) : UiEvent()
    data class SetManualSelection(val active: Boolean) : UiEvent()
    data class SetSettlingActive(val active: Boolean) : UiEvent()
    data class ToggleSetupBypass(val active: Boolean) : UiEvent()
}

sealed class UiCommand {
    object SyncRequest : UiCommand()
    data class UiVisibilityChanged(val visible: Boolean) : UiCommand()
    data class StopSiren(val causes: String? = null) : UiCommand()
    object ClearTrails : UiCommand()
    object StatsReset : UiCommand()
    object SettingsUpdated : UiCommand()
    object ZoomIn : UiCommand()
    object ZoomOut : UiCommand()
    object FullInitializationReset : UiCommand()
    object ExecuteTestAlarm : UiCommand()
    object MapZoomIn : UiCommand()
    object MapZoomOut : UiCommand()
    object ExecuteStressTest : UiCommand()
    data class SimulateStoragePressure(val active: Boolean, val isCritical: Boolean) : UiCommand()
    object CommitSettings : UiCommand()
}
