package com.gps19.app

import com.gps19.core.engine.CapabilityStatus
import com.gps19.core.engine.SystemHealthState
import org.osmdroid.util.GeoPoint

/**
 * MainUiState: Unified immutable state for the entire UI structure.
 * July.20.07:
 * - Issue #107: Added isActivityRecognitionGranted to PermissionState.
 * - Issue #099: Added isA15Device to PermissionState.
 * - Issue #516: Unified background status using CapabilityStatus (R502).
 */
data class MainUiState(
    val isInitialized: Boolean = false,
    val appMode: String? = null,
    val isSystemActive: Boolean = false,
    val deviceId: String = MainRepository.DEFAULT_TRACKER_ID,
    val viewerId: String = MainRepository.DEFAULT_VIEWER_ID,
    val relayUrl: String = DEFAULT_RELAY_URL,
    val localLocation: LocationState = LocationState(),
    val battery: BatteryState = BatteryState(),
    val stats: StatsState = StatsState(),
    val viewerSatsView: Int = 0,
    val viewerSatsUsed: Int = 0,
    val trackerLocation: LocationState = LocationState(),
    val trackerStats: StatsState = StatsState(),
    val trackerBattery: BatteryState = BatteryState(),
    val trackerSatsView: Int = 0,
    val trackerSatsUsed: Int = 0,
    val connectivity: ConnectivityState = ConnectivityState(),
    val localHealth: SystemHealthState = SystemHealthState(),
    val trackerHealth: SystemHealthState = SystemHealthState(),
    val alertSettings: AlertSettings = AlertSettings(),
    val isAlarmSilenced: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val lastAlarmAckTs: Long = 0L,
    val activeAlarms: List<AlarmInfo> = emptyList(),
    val isNewViolationDetected: Boolean = false,
    val powerAlarmPending: Boolean = false,
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
    val maxTrackerAccuracy: Double = 0.0,
    val maxViewerAccuracy: Double = 0.0,
    val distanceTrackerToHome: Double? = null,
    val distanceTrackerToViewer: Double? = null,
    val distanceViewerToHome: Double? = null,
    val distanceViewerToTracker: Double? = null,
    val draftSettings: DraftSettings = DraftSettings(),
    val isIdentitySanitized: Boolean = false
) {
    val isSystemReady: Boolean
        get() = permissions.isBatteryWhitelisted && 
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
                 (permissions.backgroundStatus == CapabilityStatus.UNKNOWN && permissions.isManualOverride))

    val systemIssuesCount: Int
        get() {
            var count = 0
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

enum class MapFollowMode { TRACKER, VIEWER, AUTO }

enum class GeofenceMode { IDLE, ADD, REMOVE }

@kotlinx.serialization.Serializable
data class DraftSettings(
    val deviceId: String = "", val viewerId: String = "", val relayUrl: String = "",
    val maxDistance: String = "", val alertSettings: AlertSettings = AlertSettings()
)

data class PermissionState(
    val isBatteryWhitelisted: Boolean = false, 
    val isAutoStartGranted: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isMicrophoneGranted: Boolean = false,
    val isExactAlarmGranted: Boolean = false,
    val isPostNotificationsGranted: Boolean = true,
    val isBackgroundLocationGranted: Boolean = true,
    val isActivityRecognitionGranted: Boolean = true,
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
    val isGnssDetailVisible: Boolean = false,
    val isStopTrackingConfirmationVisible: Boolean = false,
    val isDiagnosticsVisible: Boolean = false,
    val activeSubSettings: SubSettings? = null,
    val wasMapVisibleBeforeOverlay: Boolean = true,
    val pendingMode: String? = null
)

enum class SubSettings { ALERTS, SOUND, CLEAN }
