package com.gps19.app

import org.osmdroid.util.GeoPoint

/**
 * MainUiState: Unified immutable state for the entire UI structure.
 * v8.8.2: Aligned default IDs with MainRepository.DEFAULT_TRACKER_ID and DEFAULT_VIEWER_ID.
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
    val integrity: IntegrityStateUi = IntegrityStateUi(),
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
    val maxTrackerAccuracy: Float = 0f,
    val maxViewerAccuracy: Float = 0f,
    val distanceTrackerToHome: Double? = null,
    val distanceTrackerToViewer: Double? = null,
    val distanceViewerToHome: Double? = null,
    val draftSettings: DraftSettings = DraftSettings()
) {
    val isSystemReady: Boolean
        get() = permissions.isBatteryWhitelisted && 
                permissions.isAutoStartGranted &&
                permissions.isExactAlarmGranted && 
                permissions.isOverlayGranted &&
                permissions.isPostNotificationsGranted &&
                permissions.isBackgroundLocationGranted &&
                (appMode != null) &&
                (appMode != "tracker" || permissions.isMicrophoneGranted) &&
                (appMode == "tracker" || homePoints.isNotEmpty()) &&
                (!isXiaomiDevice() || permissions.xiaomiStatus == XiaomiPermissionStatus.GRANTED || 
                 (permissions.xiaomiStatus == XiaomiPermissionStatus.UNKNOWN && permissions.isXiaomiManualOverride))

    val systemIssuesCount: Int
        get() {
            var count = 0
            if (!permissions.isBatteryWhitelisted) count++
            if (!permissions.isAutoStartGranted) count++
            if (!permissions.isExactAlarmGranted) count++
            if (!permissions.isOverlayGranted) count++
            if (!permissions.isPostNotificationsGranted) count++
            if (!permissions.isBackgroundLocationGranted) count++
            if (appMode == "tracker" && !permissions.isMicrophoneGranted) count++
            if (appMode != "tracker" && homePoints.isEmpty()) count++
            
            val xiaomiIssue = isXiaomiDevice() && 
                             permissions.xiaomiStatus != XiaomiPermissionStatus.GRANTED && 
                             !(permissions.xiaomiStatus == XiaomiPermissionStatus.UNKNOWN && permissions.isXiaomiManualOverride)
            if (xiaomiIssue) count++
            
            return count
        }
}

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
    val xiaomiStatus: XiaomiPermissionStatus = XiaomiPermissionStatus.UNKNOWN,
    val isXiaomiManualOverride: Boolean = false
)

data class NavigationState(
    val isMapVisible: Boolean = false, val isLogVisible: Boolean = false, val isSettingsOpen: Boolean = false,
    val isPhoneSetupVisible: Boolean = false, val isDashboardExpanded: Boolean = true,
    val isRibbonsVisible: Boolean = false,
    val isGnssDetailVisible: Boolean = false,
    val isStopTrackingConfirmationVisible: Boolean = false,
    val activeSubSettings: SubSettings? = null,
    val wasMapVisibleBeforeOverlay: Boolean = true
)

enum class SubSettings { ALERTS, SOUND, CLEAN }
