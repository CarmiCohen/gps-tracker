package com.gps19.app

import com.gps19.core.engine.*
import org.osmdroid.util.GeoPoint

/**
 * MainUiState: Persistent and slow-changing state for the UI structure.
 * Aug.26.16:
 * - Issue #739 Remediation: Expanded hydrationLevel to 7 levels to decompose 
 *   Map Engine initialization. Level 4 (Base), 5 (Trails), 6 (Markers/Circles), 
 *   7 (Full Map Hydration) allows staggered O(N) overlay creation (R739).
 * Aug.26.06:
 * - Issue #735 Hardening: Added isSetupBypassActive to allow developer-mode 
 *   bypass of the PhoneSetupOverlay for automated soak tests (R735).
 * Aug.26.05:
 * - Issue #323 Hardening: Added Level 4 to hydrationLevel (Idle Map Hydration) 
 *   to ensure heavy OSM engine initialization only occurs when the main 
 *   thread is free (R323).
 */
data class MainUiState(
    val isInitialized: Boolean = false,
    val hydrationLevel: Int = 0, // 0:Cold, 1:Surface, 2:Core, 3:Full, 4:MapBase, 5:MapTrails, 6:MapMarkers, 7:MapReady
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
    val isForensicStallSimulated: Boolean = false,
    val isStorageSimulated: Boolean = false,
    val isStorageCriticalSimulated: Boolean = false,
    val isManualSelectionInProgress: Boolean = false,
    val isSettlingActive: Boolean = true,
    val isSetupBypassActive: Boolean = false
) {
    val isFullyHydrated: Boolean get() = hydrationLevel >= 3
    val isMapHydrated: Boolean get() = hydrationLevel >= 4 // Map is visible but may still be adding overlays

    val isSystemReady: Boolean
        get() = isSetupBypassActive || (
                permissions.isFineLocationGranted &&
                permissions.isBatteryWhitelisted && 
                permissions.isAutoStartGranted &&
                permissions.isExactAlarmGranted && 
                permissions.isOverlayGranted &&
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
                             (permissions.backgroundStatus != CapabilityStatus.GRANTED || 
                              permissions.autostartStatus != CapabilityStatus.GRANTED) &&
                             !(permissions.backgroundStatus == CapabilityStatus.UNKNOWN && permissions.isManualOverride)
            if (configIssue) count++
            
            return count
        }
}

/**
 * KinematicState: High-frequency transient state.
 */
class KinematicState(
    var localLocation: LocationState = LocationState(),
    var trackerLocation: LocationState = LocationState(),
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
        localLocation.reset()
        trackerLocation.reset()
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
    var pulse: Long = 0L
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
        this.pulse = other.pulse
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
        pulse = 0L
    }
}

enum class MapFollowMode { TRACKER, VIEWER, AUTO }

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
    val isA15Device: Boolean = false
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
    val replayCursorTs: Long? = null
)

enum class SubSettings { ALERTS, SOUND, CLEAN }
