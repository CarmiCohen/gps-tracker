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
import kotlinx.coroutines.flow.StateFlow

/**
 * TrackerScreen: Tracker-mode UI.
 * Aug.05.128:
 * - Issue #740: AppMapContainer Recomposition Audit. Refactored call sites to pass 
 *   decomposed primitive parameters instead of monolithic state objects (R736).
 *   Fixed entity mapping: Local device is "Tracker" in TrackerScreen.
 * Aug.05.126:
 * - Issue #739: Shared Component R736 Compliance. Decomposed HeaderBar and 
 *   GlobalStatusBar call sites to pass primitive parameters (R736).
 */

@Composable
fun TrackerScreen(
    uiState: MainUiState,
    kinematicState: KinematicState,
    diagnosticState: DiagnosticState,
    viewModel: MainViewModel,
    logsFlow: StateFlow<List<LogEntry>>,
    trail: List<TrailPoint>,
    viewerTrail: List<TrailPoint>,
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
    val nav = uiState.navigation
    val isMapVisible = nav.isMapVisible
    val isLogVisible = nav.isLogVisible
    val isSettingsOpen = nav.isSettingsOpen
    val isRibbonsVisible = nav.isRibbonsVisible
    val isGnssDetailVisible = nav.isGnssDetailVisible
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()

    val onDashboard = {
        if (isMapVisible) onToggleMap()
        if (isLogVisible) onToggleLog()
        if (isSettingsOpen) onToggleSettings()
        if (isRibbonsVisible) viewModel.onEvent(UiEvent.ToggleRibbons(false))
        if (isGnssDetailVisible) viewModel.onEvent(UiEvent.ToggleGnssDetail(false))
    }

    val header = @Composable {
        HeaderBar(
            isLogVisible = nav.isLogVisible,
            isSettingsOpen = nav.isSettingsOpen,
            isRibbonsVisible = nav.isRibbonsVisible,
            isMapVisible = nav.isMapVisible,
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
            appMode = uiState.appMode,
            isSystemActive = uiState.isSystemActive,
            deviceId = uiState.deviceId,
            viewerId = uiState.viewerId,
            isLocalOnline = diagnosticState.connectivity.isLocalOnline,
            isRelayConnected = diagnosticState.connectivity.isRelayConnected,
            lastRemoteActivityTs = diagnosticState.connectivity.lastRemoteActivityTs,
            isRedScreenVisible = diagnosticState.isRedScreenVisible,
            batteryLevel = diagnosticState.battery.level,
            trackerBatteryLevel = diagnosticState.trackerBattery.level,
            isChargingStable = diagnosticState.battery.isChargingStable,
            trackerChargingStable = diagnosticState.trackerBattery.isChargingStable,
            activeAlarms = diagnosticState.activeAlarms,
            trackerSatsView = diagnosticState.trackerSatsView,
            trackerSatsUsed = diagnosticState.trackerSatsUsed,
            trackerBatteryTemp = diagnosticState.trackerBattery.temp,
            viewerBatteryTemp = diagnosticState.battery.temp,
            viewerSatsUsed = diagnosticState.viewerSatsUsed,
            viewerSatsView = diagnosticState.viewerSatsView,
            isSirenPlaying = diagnosticState.isSirenPlaying,
            trackerGpsTs = kinematicState.trackerLocation.timestamp,
            trackerTelemetryTs = kinematicState.trackerLocation.telemetryTs,
            trackerSpeedMps = kinematicState.trackerLocation.speed,
            trackerAccuracy = kinematicState.trackerLocation.accuracy,
            trackerMaxAccuracy = kinematicState.trackerLocation.maxAccuracy,
            localGpsTs = kinematicState.localLocation.timestamp,
            localAccuracy = kinematicState.localLocation.accuracy,
            localMaxAccuracy = kinematicState.localLocation.maxAccuracy,
            localLat = kinematicState.localLocation.lat,
            trackerLocPending = kinematicState.trackerHealth.isLocationPending,
            trackerLocPendingReason = kinematicState.trackerHealth.locationPendingReason,
            localLocPending = kinematicState.localHealth.isLocationPending,
            localLocPendingReason = kinematicState.localHealth.locationPendingReason,
            distanceTrackerToHome = kinematicState.distanceTrackerToHome,
            distanceTrackerToViewer = kinematicState.distanceTrackerToViewer,
            isTelemetryFresh = dashboardState.isTelemetryFresh,
            isGpsFresh = dashboardState.isGpsFresh,
            watchdogOk = dashboardState.watchdogOk,
            trackerState = dashboardState.trackerState,
            systemPulse = systemPulse,
            rttFlow = viewModel.rtt,
            remoteSignalFlow = viewModel.remoteSignal,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { viewModel.onEvent(UiEvent.SetRedScreenVisible(true)) })
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                header()
                
                Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                    statusBar()
                    
                    Box(modifier = Modifier.weight(1f)) {
                        if (isMapVisible) {
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
                                systemPulse = systemPulse,
                                systemPulseRt = systemPulseRt,
                                onEvent = { viewModel.onEvent(it) },
                                onClearTrails = { viewModel.clearTrails(context) },
                                trail = trail, viewerTrail = viewerTrail, 
                                violations = violations, onSaveTrail = onSaveTrail, onLoadTrail = onLoadTrail, 
                                showAccuracyBadge = true,
                                showSettingsButton = true,
                                showToolsOverlay = true
                            )
                        } else {
                            TrackerDashboard(uiState, kinematicState, diagnosticState, dashboardState, systemPulse, viewModel, onEvent = { viewModel.onEvent(it) })
                        }
                    }
                }
            }
        } else {
            if (isMapVisible) {
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
                    systemPulse = systemPulse,
                    systemPulseRt = systemPulseRt,
                    onEvent = { viewModel.onEvent(it) },
                    onClearTrails = { viewModel.clearTrails(context) },
                    trail = trail, viewerTrail = viewerTrail, 
                    violations = violations, onSaveTrail = onSaveTrail, onLoadTrail = onLoadTrail, 
                    showAccuracyBadge = true,
                    showSettingsButton = false,
                    showToolsOverlay = false 
                )
            }

            if (!isSettingsOpen && !isLogVisible && !isRibbonsVisible && !isGnssDetailVisible) {
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
                        TrackerDashboard(uiState, kinematicState, diagnosticState, dashboardState, systemPulse, viewModel, onEvent = { viewModel.onEvent(it) })
                    }
                }
            }
        }

        if (isSettingsOpen) {
            SettingsOverlay(
                uiState = uiState, diagnosticState = diagnosticState, onClose = onToggleSettings, onReset = onResetStats,
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
                viewModel = viewModel
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
                history4MFlow = viewModel.history4MFlow,
                history16MFlow = viewModel.history16MFlow,
                history1HFlow = viewModel.history1HFlow,
                history4HFlow = viewModel.history4HFlow,
                history24HFlow = viewModel.history24HFlow,
                history7DFlow = viewModel.history7DFlow,
                onToggleStrictMode = { viewModel.onEvent(UiEvent.ToggleStrictMode(it)) },
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
fun TrackerDashboard(uiState: MainUiState, kinematicState: KinematicState, diagnosticState: DiagnosticState, dashboardState: DashboardState, systemPulse: Long, viewModel: MainViewModel, onEvent: (UiEvent) -> Unit) {
    val now = systemPulse
    val gpsAge = if (kinematicState.localLocation.timestamp > 0) now - kinematicState.localLocation.timestamp else Long.MAX_VALUE
    val isGpsActive = uiState.isSystemActive && gpsAge < GPS_UI_FAIL_THRESHOLD_MS
    val hasFix = isGpsActive && kinematicState.localLocation.lat != 0.0 && kinematicState.localLocation.lat != DEFAULT_LAT
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            if (uiState.navigation.isDashboardExpanded) {
                Spacer(Modifier.height(2.dp))
                Icon(Icons.Default.Agriculture, null, tint = if(hasFix) MaterialTheme.colorScheme.primary else Slate500, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(4.dp))
                TelemetryBox(
                    uiState = uiState, 
                    diagnosticState = diagnosticState,
                    dashboard = dashboardState, 
                    systemPulse = systemPulse, 
                    gpsIndexDataFlow = viewModel.gpsIndexData, 
                    rttFlow = viewModel.rtt,
                    onShowGnssDetail = { onEvent(UiEvent.ToggleGnssDetail(true)) }
                )
                DebugTable(
                    uiState = uiState, 
                    kinematicState = kinematicState,
                    diagnosticState = diagnosticState,
                    dashboard = dashboardState, 
                    systemPulse = systemPulse,
                    rttFlow = viewModel.rtt,
                    currentMaFlow = viewModel.currentMa
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
