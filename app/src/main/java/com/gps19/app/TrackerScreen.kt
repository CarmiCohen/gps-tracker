package com.gps19.app

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.osmdroid.util.GeoPoint
import com.gps19.core.engine.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/**
 * TrackerScreen: Tracker-mode UI.
 * Aug.22.04:
 * - Issue #140 Restoration Build Fix: Unified naming to maxIoLatency. 
 *   De-duplicated currentMa in TrackerDashboard signature and fixed 
 *   TelemetryBox parameter binding.
 */

@Composable
fun TrackerScreen(
    uiState: MainUiState,
    kinematicState: KinematicState,
    diagnosticState: DiagnosticState,
    viewModel: MainViewModel,
    logsFlow: StateFlow<List<LogEntry>>,
    trackerSegments: List<MapTrailSegment>,
    viewerSegments: List<MapTrailSegment>,
    violations: List<ViolationPoint>,
    systemPulse: Long,
    systemPulseRt: Long,
    onToggleMap: () -> Unit,
    onToggleLog: () -> Unit,
    onToggleSettings: () -> Unit,
    onExit: () -> Unit,
    onImportConfig: () -> Unit,
    onExportLogs: () -> Unit,
    onClearLogs: () -> Unit,
    onResetStats: () -> Unit = {},
    onClearHome: () -> Unit = {},
    onSaveTrail: () -> Unit = {},
    onLoadTrail: () -> Unit = {}
) {
    var isHydrated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200) 
        isHydrated = true
    }

    val nav = uiState.navigation
    val isMapVisible = nav.isMapVisible
    val isLogVisible = nav.isLogVisible
    val isSettingsOpen = nav.isSettingsOpen
    val isRibbonsVisible = nav.isRibbonsVisible
    val isGnssDetailVisible = nav.isGnssDetailVisible
    val isPhoneSetupVisible = nav.isPhoneSetupVisible
    
    val isAnyOverlayOpen = isSettingsOpen || isLogVisible || isRibbonsVisible || isGnssDetailVisible || isPhoneSetupVisible
    
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val gpsIndexData by viewModel.gpsIndexData.collectAsStateWithLifecycle()
    val rtt by viewModel.rtt.collectAsStateWithLifecycle()
    val currentMa by viewModel.currentMa.collectAsStateWithLifecycle()

    val onDashboard = {
        if (isMapVisible) onToggleMap()
        if (isLogVisible) onToggleLog()
        if (isSettingsOpen) onToggleSettings()
        if (isRibbonsVisible) viewModel.onEvent(UiEvent.ToggleRibbons(false))
        if (isGnssDetailVisible) viewModel.onEvent(UiEvent.ToggleGnssDetail(false))
        if (isPhoneSetupVisible) viewModel.onEvent(UiEvent.TogglePhoneSetup(false))
    }

    val header = @Composable {
        HeaderBar(
            isLogVisible = nav.isLogVisible,
            isSettingsOpen = nav.isSettingsOpen,
            isRibbonsVisible = nav.isRibbonsVisible,
            isMapVisible = nav.isMapVisible,
            isPhoneSetupVisible = nav.isPhoneSetupVisible, 
            requiresExtraTopPadding = uiState.permissions.requiresExtraTopPadding,
            isSystemReady = uiState.isSystemReady,
            systemIssuesCount = uiState.systemIssuesCount,
            onDashboard = onDashboard,
            onS = onToggleSettings,
            onL = onToggleLog,
            onM = onToggleMap,
            onR = { viewModel.onEvent(UiEvent.ToggleRibbons(!isRibbonsVisible)) },
            onEvent = { viewModel.onEvent(it) }
        )
    }

    val statusBar = @Composable {
        GlobalStatusBar(
            hudState = hudState,
            isSystemActive = uiState.isSystemActive,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { viewModel.onEvent(UiEvent.SetRedScreenVisible(true)) })
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!isHydrated) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandJd, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            }
        } else {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    header()
                    
                    Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                        statusBar()
                        
                        Box(modifier = Modifier.weight(1f)) {
                            if (isMapVisible && !isAnyOverlayOpen) {
                                AppMapContainer(
                                    appMode = uiState.appMode,
                                    isMapButtonsVisible = uiState.isMapButtonsVisible,
                                    isFenceVisible = uiState.isFenceVisible,
                                    geofenceMode = uiState.geofenceMode,
                                    isViolationsVisible = uiState.isViolationsVisible,
                                    isGeofenceViolationsVisible = uiState.isGeofenceViolationsVisible,
                                    maxDistance = uiState.maxDistance,
                                    isMapLocked = uiState.isMapLocked,
                                    mapFollowMode = uiState.mapFollowMode,
                                    centeringTrackerTrigger = uiState.centeringTrackerTrigger,
                                    centeringViewerTrigger = uiState.centeringViewerTrigger,
                                    zoomInTrigger = uiState.zoomInTrigger,
                                    zoomOutTrigger = uiState.zoomOutTrigger,
                                    homePoints = uiState.homePoints,
                                    trackerLat = kinematicState.localLocation.lat,
                                    trackerLng = kinematicState.localLocation.lng,
                                    trackerSpeed = kinematicState.localLocation.speed,
                                    trackerAccuracy = kinematicState.localLocation.accuracy,
                                    trackerMaxAccuracy = kinematicState.localLocation.maxAccuracy,
                                    trackerGpsTs = kinematicState.localLocation.timestamp,
                                    trackerTelemetryTs = 0L,
                                    trackerLocPending = kinematicState.localHealth.isLocationPending,
                                    trackerLocPendingReason = kinematicState.localHealth.locationPendingReason,
                                    trackerLastValidFixRt = kinematicState.localHealth.lastValidFixRt,
                                    viewerLat = kinematicState.trackerLocation.lat,
                                    viewerLng = kinematicState.trackerLocation.lng,
                                    viewerSpeed = kinematicState.trackerLocation.speed,
                                    viewerAccuracy = kinematicState.trackerLocation.accuracy,
                                    viewerMaxAcc = kinematicState.trackerLocation.maxAccuracy,
                                    viewerGpsTs = kinematicState.trackerLocation.timestamp,
                                    viewerTelemetryTs = kinematicState.trackerLocation.telemetryTs,
                                    viewerLocPending = kinematicState.trackerHealth.isLocationPending,
                                    viewerLastValidFixRt = kinematicState.trackerHealth.lastValidFixRt,
                                    replayCursorPos = kinematicState.replayCursorPos,
                                    systemPulse = systemPulse,
                                    systemPulseRt = systemPulseRt,
                                    onEvent = { viewModel.onEvent(it) },
                                    onClearTrails = { viewModel.clearTrails(context) },
                                    trackerSegments = trackerSegments,
                                    viewerSegments = viewerSegments,
                                    violations = violations, onSaveTrail = onSaveTrail, onLoadTrail = onLoadTrail, 
                                    showAccuracyBadge = true,
                                    showSettingsButton = true,
                                    showToolsOverlay = true
                                )
                            } else if (!isMapVisible) {
                                TrackerDashboard(
                                    appMode = uiState.appMode ?: "tracker",
                                    isSystemActive = uiState.isSystemActive,
                                    isDashboardExpanded = uiState.navigation.isDashboardExpanded,
                                    isBatteryWhitelisted = uiState.permissions.isBatteryWhitelisted,
                                    isLocalOnline = diagnosticState.connectivity.isLocalOnline,
                                    isRelayConnected = diagnosticState.connectivity.isRelayConnected,
                                    lastRemoteActivityTs = diagnosticState.connectivity.lastRemoteActivityTs,
                                    localLat = kinematicState.localLocation.lat,
                                    localLocationTs = kinematicState.localLocation.timestamp,
                                    isGpsFresh = dashboardState.isGpsFresh,
                                    isTelemetryFresh = dashboardState.isTelemetryFresh,
                                    isLinkFresh = dashboardState.isLinkFresh,
                                    trackerState = dashboardState.trackerState,
                                    isLocationPending = dashboardState.isLocationPending,
                                    locationPendingReason = dashboardState.locationPendingReason,
                                    status = dashboardState.status,
                                    isTamperDetected = dashboardState.isTamperDetected,
                                    isBatterySteepDischarge = dashboardState.isBatterySteepDischarge,
                                    isBatteryLow = dashboardState.isBatteryLow,
                                    isBatteryCritical = dashboardState.isBatteryCritical,
                                    maxDropMs = dashboardState.maxDropMs,
                                    lastSeenTs = dashboardState.lastSeenTs,
                                    totalDropMs = dashboardState.totalDropMs,
                                    totalUptimeMs = dashboardState.totalUptimeMs,
                                    sessionMs = dashboardState.sessionMs,
                                    engineVersion = dashboardState.engineVersion,
                                    sinceConnMs = dashboardState.sinceConnMs,
                                    sinceDiscoMs = dashboardState.sinceDiscoMs,
                                    violationUptimeMs = dashboardState.violationUptimeMs,
                                    watchdogCountdownSec = dashboardState.watchdogCountdownSec,
                                    watchdogOk = dashboardState.watchdogOk,
                                    isPowerSaveMode = dashboardState.isPowerSaveMode,
                                    standbyBucket = dashboardState.standbyBucket,
                                    netInterface = dashboardState.netInterface,
                                    isStorageLow = dashboardState.isStorageLow,
                                    isStorageCritical = dashboardState.isStorageCritical,
                                    distToHome = dashboardState.distToHome,
                                    distToViewer = dashboardState.distToViewer,
                                    lat = dashboardState.lat,
                                    lng = dashboardState.lng,
                                    gpsSpeedMps = dashboardState.gpsSpeedMps,
                                    trackerAccuracy = dashboardState.trackerAccuracy,
                                    trackerMaxAcc = dashboardState.trackerMaxAcc,
                                    viewerAccuracy = dashboardState.viewerAccuracy,
                                    viewerMaxAcc = dashboardState.viewerMaxAcc,
                                    satsUsed = dashboardState.satsUsed,
                                    satsView = dashboardState.satsView,
                                    isSatsIndexWarning = dashboardState.isSatsIndexWarning,
                                    snr = dashboardState.snr,
                                    vibration = dashboardState.vibration,
                                    heading = dashboardState.heading,
                                    tilt = dashboardState.tilt,
                                    acousticDb = dashboardState.acousticDb,
                                    baroAlt = dashboardState.baroAlt,
                                    lux = dashboardState.lux,
                                    proximityCm = dashboardState.proximityCm,
                                    proximityDebounceMs = dashboardState.proximityDebounceMs,
                                    rollingVibration = dashboardState.rollingVibration,
                                    trackerMaxTemp = dashboardState.trackerMaxTemp,
                                    viewerMaxTemp = dashboardState.viewerMaxTemp,
                                    peakShock = dashboardState.peakShock,
                                    vibrationFloor = dashboardState.vibrationFloor,
                                    luxBaseline = dashboardState.luxBaseline,
                                    acousticFloorDb = dashboardState.acousticFloorDb,
                                    trackerCurrentMa = dashboardState.trackerCurrentMa,
                                    gpsIdx = gpsIndexData,
                                    rttValue = rtt,
                                    currentMaValue = currentMa,
                                    systemPulse = systemPulse,
                                    cpuLoad = dashboardState.cpuLoad,
                                    ioWait = dashboardState.ioWait,
                                    maxIoLatency = dashboardState.maxIoLatency,
                                    onEvent = { viewModel.onEvent(it) }
                                )
                            }
                        }
                    }
                }
            } else {
                if (isMapVisible && !isAnyOverlayOpen) {
                    AppMapContainer(
                        appMode = uiState.appMode,
                        isMapButtonsVisible = uiState.isMapButtonsVisible,
                        isFenceVisible = uiState.isFenceVisible,
                        geofenceMode = uiState.geofenceMode,
                        isViolationsVisible = uiState.isViolationsVisible,
                        isGeofenceViolationsVisible = uiState.isGeofenceViolationsVisible,
                        maxDistance = uiState.maxDistance,
                        isMapLocked = uiState.isMapLocked,
                        mapFollowMode = uiState.mapFollowMode,
                        centeringTrackerTrigger = uiState.centeringTrackerTrigger,
                        centeringViewerTrigger = uiState.centeringViewerTrigger,
                        zoomInTrigger = uiState.zoomInTrigger,
                        zoomOutTrigger = uiState.zoomOutTrigger,
                        homePoints = uiState.homePoints,
                        trackerLat = kinematicState.localLocation.lat,
                        trackerLng = kinematicState.localLocation.lng,
                        trackerSpeed = kinematicState.localLocation.speed,
                        trackerAccuracy = kinematicState.localLocation.accuracy,
                        trackerMaxAccuracy = kinematicState.localLocation.maxAccuracy,
                        trackerGpsTs = kinematicState.localLocation.timestamp,
                        trackerTelemetryTs = 0L,
                        trackerLocPending = kinematicState.localHealth.isLocationPending,
                        trackerLocPendingReason = kinematicState.localHealth.locationPendingReason,
                        trackerLastValidFixRt = kinematicState.localHealth.lastValidFixRt,
                        viewerLat = kinematicState.trackerLocation.lat,
                        viewerLng = kinematicState.trackerLocation.lng,
                        viewerSpeed = kinematicState.trackerLocation.speed,
                        viewerAccuracy = kinematicState.trackerLocation.accuracy,
                        viewerMaxAcc = kinematicState.trackerLocation.maxAccuracy,
                        viewerGpsTs = kinematicState.localLocation.timestamp,
                        viewerTelemetryTs = 0L,
                        viewerLocPending = kinematicState.trackerHealth.isLocationPending,
                        viewerLastValidFixRt = kinematicState.trackerHealth.lastValidFixRt,
                        replayCursorPos = kinematicState.replayCursorPos,
                        systemPulse = systemPulse,
                        systemPulseRt = systemPulseRt,
                        onEvent = { viewModel.onEvent(it) },
                        onClearTrails = { viewModel.clearTrails(context) },
                        trackerSegments = trackerSegments,
                        viewerSegments = viewerSegments,
                        violations = violations, onSaveTrail = onSaveTrail, onLoadTrail = onLoadTrail, 
                        showAccuracyBadge = true,
                        showSettingsButton = false,
                        showToolsOverlay = false 
                    )
                }

                if (!isAnyOverlayOpen) {
                    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth().zIndex(10f)
                        ) {
                            Column {
                                Box(Modifier.statusBarsPadding()) {
                                    header()
                                }
                                statusBar()
                            }
                        }

                        if (isMapVisible) {
                            Box(Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp), contentAlignment = Alignment.CenterEnd) {
                                MapSettingsToggle(
                                    isMapButtonsVisible = uiState.isMapButtonsVisible,
                                    onToggle = { viewModel.onEvent(UiEvent.SetMapButtonsVisible(!uiState.isMapButtonsVisible)) }
                                )
                            }

                            if (uiState.isMapButtonsVisible) {
                                Box(Modifier.fillMaxWidth().padding(end = 8.dp), contentAlignment = Alignment.CenterEnd) {
                                    MapToolsOverlay(
                                        isTrackerMode = true,
                                        trackerValid = PhysicsUtils.isValidLocation(kinematicState.localLocation.lat, kinematicState.localLocation.lng),
                                        viewerValid = PhysicsUtils.isValidLocation(kinematicState.trackerLocation.lat, kinematicState.trackerLocation.lng),
                                        showFence = uiState.isFenceVisible,
                                        onToggleFence = { viewModel.onEvent(UiEvent.SetFenceVisible(!uiState.isFenceVisible)) },
                                        geofenceMode = uiState.geofenceMode,
                                        onSetGeofenceMode = { viewModel.onEvent(UiEvent.SetGeofenceMode(it)) },
                                        showViolations = uiState.isViolationsVisible,
                                        onToggleViolations = { viewModel.onEvent(UiEvent.SetViolationsVisible(!uiState.isViolationsVisible)) },
                                        showGeofenceViolations = uiState.isGeofenceViolationsVisible,
                                        onToggleGeofenceViolations = { viewModel.onEvent(UiEvent.SetGeofenceViolationsVisible(!uiState.isGeofenceViolationsVisible)) },
                                        onClear = { viewModel.clearTrails(context) },
                                        onSave = onSaveTrail,
                                        onLoad = onLoadTrail,
                                        onCenterTracker = { viewModel.onEvent(UiEvent.CenterTracker) },
                                        onCenterViewer = { viewModel.onEvent(UiEvent.CenterViewer) },
                                        onZoomIn = { viewModel.onEvent(UiEvent.MapZoomIn) },
                                        onZoomOut = { viewModel.onEvent(UiEvent.MapZoomOut) }
                                    )
                                }
                            }
                        }
                        
                        if (!isMapVisible) {
                            TrackerDashboard(
                                appMode = uiState.appMode ?: "tracker",
                                isSystemActive = uiState.isSystemActive,
                                isDashboardExpanded = uiState.navigation.isDashboardExpanded,
                                isBatteryWhitelisted = uiState.permissions.isBatteryWhitelisted,
                                isLocalOnline = diagnosticState.connectivity.isLocalOnline,
                                isRelayConnected = diagnosticState.connectivity.isRelayConnected,
                                lastRemoteActivityTs = diagnosticState.connectivity.lastRemoteActivityTs,
                                localLat = kinematicState.localLocation.lat,
                                localLocationTs = kinematicState.localLocation.timestamp,
                                isGpsFresh = dashboardState.isGpsFresh,
                                isTelemetryFresh = dashboardState.isTelemetryFresh,
                                isLinkFresh = dashboardState.isLinkFresh,
                                trackerState = dashboardState.trackerState,
                                isLocationPending = dashboardState.isLocationPending,
                                locationPendingReason = dashboardState.locationPendingReason,
                                status = dashboardState.status,
                                isTamperDetected = dashboardState.isTamperDetected,
                                isBatterySteepDischarge = dashboardState.isBatterySteepDischarge,
                                isBatteryLow = dashboardState.isBatteryLow,
                                isBatteryCritical = dashboardState.isBatteryCritical,
                                maxDropMs = dashboardState.maxDropMs,
                                lastSeenTs = dashboardState.lastSeenTs,
                                totalDropMs = dashboardState.totalDropMs,
                                totalUptimeMs = dashboardState.totalUptimeMs,
                                sessionMs = dashboardState.sessionMs,
                                engineVersion = dashboardState.engineVersion,
                                sinceConnMs = dashboardState.sinceConnMs,
                                sinceDiscoMs = dashboardState.sinceDiscoMs,
                                violationUptimeMs = dashboardState.violationUptimeMs,
                                watchdogCountdownSec = dashboardState.watchdogCountdownSec,
                                watchdogOk = dashboardState.watchdogOk,
                                isPowerSaveMode = dashboardState.isPowerSaveMode,
                                standbyBucket = dashboardState.standbyBucket,
                                netInterface = dashboardState.netInterface,
                                isStorageLow = dashboardState.isStorageLow,
                                isStorageCritical = dashboardState.isStorageCritical,
                                distToHome = dashboardState.distToHome,
                                distToViewer = dashboardState.distToViewer,
                                lat = dashboardState.lat,
                                lng = dashboardState.lng,
                                gpsSpeedMps = dashboardState.gpsSpeedMps,
                                trackerAccuracy = dashboardState.trackerAccuracy,
                                trackerMaxAcc = dashboardState.trackerMaxAcc,
                                viewerAccuracy = dashboardState.viewerAccuracy,
                                viewerMaxAcc = dashboardState.viewerMaxAcc,
                                satsUsed = dashboardState.satsUsed,
                                satsView = dashboardState.satsView,
                                isSatsIndexWarning = dashboardState.isSatsIndexWarning,
                                snr = dashboardState.snr,
                                vibration = dashboardState.vibration,
                                heading = dashboardState.heading,
                                tilt = dashboardState.tilt,
                                acousticDb = dashboardState.acousticDb,
                                baroAlt = dashboardState.baroAlt,
                                lux = dashboardState.lux,
                                proximityCm = dashboardState.proximityCm,
                                proximityDebounceMs = dashboardState.proximityDebounceMs,
                                rollingVibration = dashboardState.rollingVibration,
                                trackerMaxTemp = dashboardState.trackerMaxTemp,
                                viewerMaxTemp = dashboardState.viewerMaxTemp,
                                peakShock = dashboardState.peakShock,
                                vibrationFloor = dashboardState.vibrationFloor,
                                luxBaseline = dashboardState.luxBaseline,
                                acousticFloorDb = dashboardState.acousticFloorDb,
                                trackerCurrentMa = dashboardState.trackerCurrentMa,
                                gpsIdx = gpsIndexData,
                                rttValue = rtt,
                                currentMaValue = currentMa,
                                systemPulse = systemPulse,
                                cpuLoad = dashboardState.cpuLoad,
                                ioWait = dashboardState.ioWait,
                                maxIoLatency = dashboardState.maxIoLatency,
                                onEvent = { viewModel.onEvent(it) }
                            )
                        }
                    }
                }
            }
        }

        if (isSettingsOpen) {
            SettingsOverlay(
                activeSubSettings = nav.activeSubSettings,
                draftDeviceId = uiState.draftSettings.deviceId,
                draftViewerId = uiState.draftSettings.viewerId,
                draftRelayUrl = uiState.draftSettings.relayUrl,
                draftMaxDistance = uiState.draftSettings.maxDistance,
                draftAlertSettings = uiState.draftSettings.alertSettings,
                selectedSirenType = uiState.selectedSirenType,
                isSirenPlaying = diagnosticState.isSirenPlaying,
                onClose = onToggleSettings, onReset = onResetStats,
                onExport = onExportLogs, onClear = onClearHome, onImportConfig = onImportConfig,
                onFullInitialization = { viewModel.fullInitialization(context) },
                onUpdateDeviceId = { viewModel.onEvent(UiEvent.UpdateDraftDeviceId(it)) },
                onUpdateViewerId = { viewModel.onEvent(UiEvent.UpdateDraftViewerId(it)) },
                onUpdateRelayUrl = { viewModel.onEvent(UiEvent.UpdateDraftRelayUrl(it)) },
                onUpdateMaxDistance = { viewModel.onEvent(UiEvent.UpdateDraftMaxDistance(it)) },
                onUpdateAlertSettings = { viewModel.onEvent(UiEvent.UpdateDraftAlertSettings(it)) },
                onUpdateSirenType = { viewModel.onEvent(UiEvent.SetSirenType(it)) },
                onUpdateAlarmVolume = { viewModel.onEvent(UiEvent.UpdateDraftAlertSettings(uiState.draftSettings.alertSettings.copy(alarmVolume = it))) },
                onTestSiren = { 
                    if (diagnosticState.isSirenPlaying) {
                        AudioSynthesizer.stopSiren(timeProvider = viewModel.timeProvider)
                    } else {
                        val s = uiState.draftSettings.alertSettings
                        val volume = if (s.useMaxVolume) 1.0f else if (s.useCustomVolume) s.alarmVolume else 1.0f
                        AudioSynthesizer.playSiren(
                            uiState.selectedSirenType, force = true, volume = volume, 
                            overrideSilence = s.overrideSilence, context = context, 
                            loop = true, vibrate = s.vibrationEnabled,
                            timeProvider = viewModel.timeProvider
                        )
                    }
                },
                onShowPhoneSetup = { viewModel.onEvent(UiEvent.TogglePhoneSetup(true)) },
                onEvent = { viewModel.onEvent(it) }
            )
        } else if (isLogVisible) {
            val showDetails by viewModel.repository.logFilterDetails.collectAsStateWithLifecycle()
            val showRecovered by viewModel.repository.logFilterRecovered.collectAsStateWithLifecycle()
            LogOverlay(
                logsFlow = logsFlow, onExport = onExportLogs, onToggle = onToggleLog, onClear = onClearLogs,
                showDetails = showDetails, showRecovered = showRecovered, 
                onSetShowDetails = { viewModel.onEvent(UiEvent.SetLogFilterShowDetails(it)) }, 
                onSetShowRecovered = { viewModel.onEvent(UiEvent.SetLogFilterShowRecovered(it)) },
                appStartTime = uiState.appStartTime,
                systemPulse = systemPulse,
                isTelemetryFresh = dashboardState.isTelemetryFresh
            )
        } else if (isRibbonsVisible) {
            RibbonsOverlay(
                isStrictMode = uiState.navigation.isStrictMode,
                replayCursorTs = uiState.navigation.replayCursorTs,
                history4MFlow = viewModel.history4MFlow,
                history16MFlow = viewModel.history16MFlow,
                history1HFlow = viewModel.history1HFlow,
                history4HFlow = viewModel.history4HFlow,
                history24HFlow = viewModel.history24HFlow,
                history7DFlow = viewModel.history7DFlow,
                onToggleStrictMode = { viewModel.onEvent(UiEvent.ToggleStrictMode(it)) },
                onScrub = { viewModel.onEvent(UiEvent.SetReplayCursor(it)) },
                onDismiss = { viewModel.onEvent(UiEvent.ToggleRibbons(false)) }
            )
        } else if (isGnssDetailVisible) {
            GnssDetailOverlay(
                gnssDetailFlow = viewModel.activeGnssDetail,
                onClose = { viewModel.onEvent(UiEvent.ToggleGnssDetail(false)) }
            )
        }
    }
}

@Composable
fun TrackerDashboard(
    appMode: String,
    isSystemActive: Boolean,
    isDashboardExpanded: Boolean,
    isBatteryWhitelisted: Boolean,
    isLocalOnline: Boolean,
    isRelayConnected: Boolean,
    lastRemoteActivityTs: Long,
    localLat: Double,
    localLocationTs: Long,
    isGpsFresh: Boolean,
    isTelemetryFresh: Boolean,
    isLinkFresh: Boolean,
    trackerState: TrackerState,
    isLocationPending: Boolean,
    locationPendingReason: LocationPendingReason,
    status: SentinelStatus,
    isTamperDetected: Boolean,
    isBatterySteepDischarge: Boolean,
    isBatteryLow: Boolean,
    isBatteryCritical: Boolean,
    maxDropMs: Long,
    lastSeenTs: Long,
    totalDropMs: Long,
    totalUptimeMs: Long,
    sessionMs: Long,
    engineVersion: String,
    sinceConnMs: Long,
    sinceDiscoMs: Long,
    violationUptimeMs: Long,
    watchdogCountdownSec: Long,
    watchdogOk: Boolean,
    isPowerSaveMode: Boolean,
    standbyBucket: Int,
    netInterface: String,
    isStorageLow: Boolean,
    isStorageCritical: Boolean,
    distToHome: Double?,
    distToViewer: Double?,
    lat: Double,
    lng: Double,
    gpsSpeedMps: Double,
    trackerAccuracy: Double,
    trackerMaxAcc: Double,
    viewerAccuracy: Double,
    viewerMaxAcc: Double,
    satsUsed: Int,
    satsView: Int,
    isSatsIndexWarning: Boolean,
    snr: Double,
    vibration: Double,
    heading: Double,
    tilt: Double,
    acousticDb: Double,
    baroAlt: Double,
    lux: Double,
    proximityCm: Double,
    proximityDebounceMs: Long,
    rollingVibration: Double,
    trackerMaxTemp: Double,
    viewerMaxTemp: Double,
    peakShock: Double,
    vibrationFloor: Double,
    luxBaseline: Double,
    acousticFloorDb: Double,
    trackerCurrentMa: Int,
    gpsIdx: GpsIndexData,
    rttValue: Int,
    currentMaValue: Int,
    systemPulse: Long,
    cpuLoad: Double,
    ioWait: Double,
    maxIoLatency: Long,
    onEvent: (UiEvent) -> Unit
) {
    val gpsAge = if (localLocationTs > 0) systemPulse - localLocationTs else Long.MAX_VALUE
    val isGpsActive = isSystemActive && gpsAge < GPS_UI_FAIL_THRESHOLD_MS
    val hasFix = isGpsActive && localLat != 0.0 && localLat != DEFAULT_LAT
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            if (isDashboardExpanded) {
                Spacer(Modifier.height(2.dp))
                Icon(Icons.Default.Agriculture, null, tint = if(hasFix) MaterialTheme.colorScheme.primary else Slate500, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(4.dp))
                TelemetryBox(
                    appMode = appMode,
                    isBatteryWhitelisted = isBatteryWhitelisted,
                    isLocalOnline = isLocalOnline,
                    isRelayConnected = isRelayConnected,
                    lastRemoteActivityTs = lastRemoteActivityTs,
                    systemPulse = systemPulse,
                    isGpsFresh = isGpsFresh,
                    isTelemetryFresh = isTelemetryFresh,
                    isLinkFresh = isLinkFresh,
                    trackerState = trackerState,
                    isLocationPending = isLocationPending,
                    locationPendingReason = locationPendingReason,
                    status = status,
                    isTamperDetected = isTamperDetected,
                    isBatterySteepDischarge = isBatterySteepDischarge,
                    isBatteryLow = isBatteryLow,
                    isBatteryCritical = isBatteryCritical,
                    maxDropMs = maxDropMs,
                    lastSeenTs = lastSeenTs,
                    totalDropMs = totalDropMs,
                    totalUptimeMs = totalUptimeMs,
                    sessionMs = sessionMs,
                    engineVersion = engineVersion,
                    sinceConnMs = sinceConnMs,
                    sinceDiscoMs = sinceDiscoMs,
                    violationUptimeMs = violationUptimeMs,
                    watchdogCountdownSec = watchdogCountdownSec,
                    watchdogOk = watchdogOk,
                    isPowerSaveMode = isPowerSaveMode,
                    standbyBucket = standbyBucket,
                    netInterface = netInterface,
                    isStorageLow = isStorageLow,
                    isStorageCritical = isStorageCritical,
                    distToHome = distToHome,
                    distToViewer = distToViewer,
                    lat = lat,
                    lng = lng,
                    gpsSpeedMps = gpsSpeedMps,
                    trackerAccuracy = trackerAccuracy,
                    trackerMaxAcc = trackerMaxAcc,
                    viewerAccuracy = viewerAccuracy,
                    viewerMaxAcc = viewerMaxAcc,
                    satsUsed = satsUsed,
                    satsView = satsView,
                    isSatsIndexWarning = isSatsIndexWarning,
                    snr = snr,
                    vibration = vibration,
                    heading = heading,
                    tilt = tilt,
                    acousticDb = acousticDb,
                    baroAlt = baroAlt,
                    lux = lux,
                    proximityCm = proximityCm,
                    proximityDebounceMs = proximityDebounceMs,
                    rollingVibration = rollingVibration,
                    trackerMaxTemp = trackerMaxTemp,
                    viewerMaxTemp = viewerMaxTemp,
                    peakShock = peakShock,
                    vibrationFloor = vibrationFloor,
                    luxBaseline = luxBaseline,
                    acousticFloorDb = acousticFloorDb,
                    trackerCurrentMa = trackerCurrentMa,
                    gpsIdx = gpsIdx,
                    rttValue = rttValue,
                    cpuLoad = cpuLoad,
                    ioWait = ioWait,
                    maxIoLatency = maxIoLatency,
                    onShowGnssDetail = { onEvent(UiEvent.ToggleGnssDetail(true)) }
                )
                DebugTable(
                    isLinkFresh = isLinkFresh,
                    isTelemetryFresh = isTelemetryFresh,
                    isGpsFresh = isGpsFresh,
                    trackerStateName = trackerState.name,
                    gpsAgeSec = if (gpsAge != Long.MAX_VALUE) gpsAge / 1000 else -1L,
                    rtt = rttValue,
                    currentMa = currentMaValue
                )
                
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onEvent(UiEvent.ShowStopTrackingConfirmation(true)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Rose500.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.StopCircle, null, tint = Rose500, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TERMINATE TRACKING SESSION", color = Rose500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Tap status card above to expand dashboard", color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
