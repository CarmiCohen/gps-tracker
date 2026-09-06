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
import androidx.compose.material.icons.filled.Person
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
 * ViewerScreen: Pocket-mode UI.
 * Sep.06.35:
 * - Issue #930 RESOLVED: Deep-Linking. Implemented onHistLink and 
 *   onDetailsLink callbacks for LogOverlay (R-ID 930).
 * Sep.03.25:
 * - Idea #240: ContextShadow Automation. Updated AudioSynthesizer calls to use 
 *   the injected instance from viewModel (R-ID 240).
 */

@Composable
fun ViewerScreen(
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
    val nav = uiState.navigation
    val isMapVisible = nav.isMapVisible
    val isLogVisible = nav.isLogVisible
    val isSettingsOpen = nav.isSettingsOpen
    val isRibbonsVisible = nav.isRibbonsVisible
    val isGnssDetailVisible = nav.isGnssDetailVisible
    val isAnyOverlayOpen = isSettingsOpen || isLogVisible || isRibbonsVisible || isGnssDetailVisible

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val gpsIndexData by viewModel.gpsIndexData.collectAsStateWithLifecycle()
    val rtt by viewModel.rtt.collectAsStateWithLifecycle()
    val trackerCurrentMa by viewModel.trackerCurrentMa.collectAsStateWithLifecycle()

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
            hudState = hudState,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { viewModel.onEvent(UiEvent.SetRedScreenVisible(true)) })
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (uiState.hydrationLevel < 3) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandJd, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            }
        } else {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (uiState.hydrationLevel >= 4) {
                        header()
                    }
                    
                    Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                        if (uiState.hydrationLevel >= 5) {
                            statusBar()
                        }
                        
                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.isMapHydrated && isMapVisible && !isAnyOverlayOpen && uiState.hydrationLevel >= 6) {
                                AppMapContainer(
                                    appMode = uiState.appMode,
                                    hydrationLevel = uiState.hydrationLevel,
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
                                    trackerLat = kinematicState.trackerLocation.lat,
                                    trackerLng = kinematicState.trackerLocation.lng,
                                    trackerSpeed = kinematicState.trackerLocation.speed,
                                    trackerAccuracy = kinematicState.trackerLocation.accuracy,
                                    trackerMaxAccuracy = kinematicState.trackerLocation.maxAccuracy,
                                    trackerGpsTs = kinematicState.trackerLocation.gpsTs,
                                    trackerTelemetryTs = kinematicState.trackerLocation.ts,
                                    trackerLocPending = kinematicState.trackerHealth.isLocationPending,
                                    trackerLocPendingReason = kinematicState.trackerHealth.locationPendingReason,
                                    trackerLastValidFixRt = kinematicState.trackerHealth.lastValidFixRt,
                                    viewerLat = kinematicState.localLocation.lat,
                                    viewerLng = kinematicState.localLocation.lng,
                                    viewerSpeed = kinematicState.localLocation.speed,
                                    viewerAccuracy = kinematicState.localLocation.accuracy,
                                    viewerMaxAcc = kinematicState.localLocation.maxAccuracy,
                                    viewerGpsTs = kinematicState.localLocation.gpsTs,
                                    viewerTelemetryTs = 0L,
                                    viewerLocPending = kinematicState.localHealth.isLocationPending,
                                    viewerLastValidFixRt = kinematicState.localHealth.lastValidFixRt,
                                    replayCursorPos = kinematicState.replayCursorPos,
                                    systemPulse = systemPulse,
                                    systemPulseRt = systemPulseRt,
                                    onEvent = { viewModel.onEvent(it) },
                                    onClearTrails = { viewModel.clearTrails(context) },
                                    trackerSegments = trackerSegments,
                                    viewerSegments = viewerSegments,
                                    violations = violations, onSaveTrail = onSaveTrail, onLoadTrail = onLoadTrail, 
                                    showAccuracyBadge = false,
                                    showSettingsButton = true,
                                    showToolsOverlay = true
                                )
                            } else if (uiState.hydrationLevel >= 4 && !isMapVisible) {
                                ViewerDashboard(
                                    appMode = uiState.appMode ?: "viewer",
                                    isDashboardExpanded = uiState.navigation.isDashboardExpanded,
                                    isBatteryWhitelisted = uiState.permissions.isBatteryWhitelisted,
                                    isLocalOnline = diagnosticState.connectivity.isLocalOnline,
                                    isRelayConnected = diagnosticState.connectivity.isRelayConnected,
                                    lastRemoteActivityTs = diagnosticState.connectivity.lastRemoteActivityTs,
                                    trackerLocationTs = kinematicState.trackerLocation.gpsTs,
                                    dashboardState = dashboardState,
                                    gpsIdx = gpsIndexData,
                                    rttValue = rtt,
                                    trackerCurrentMa = trackerCurrentMa,
                                    systemPulse = systemPulse,
                                    onEvent = { viewModel.onEvent(it) }
                                )
                            }
                        }
                    }
                }
            } else {
                if (uiState.isMapHydrated && isMapVisible && !isAnyOverlayOpen && uiState.hydrationLevel >= 6) {
                    AppMapContainer(
                        appMode = uiState.appMode,
                        hydrationLevel = uiState.hydrationLevel,
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
                        trackerLat = kinematicState.trackerLocation.lat,
                        trackerLng = kinematicState.trackerLocation.lng,
                        trackerSpeed = kinematicState.trackerLocation.speed,
                        trackerAccuracy = kinematicState.trackerLocation.accuracy,
                        trackerMaxAccuracy = kinematicState.trackerLocation.maxAccuracy,
                        trackerGpsTs = kinematicState.trackerLocation.gpsTs,
                        trackerTelemetryTs = kinematicState.trackerLocation.ts,
                        trackerLocPending = kinematicState.trackerHealth.isLocationPending,
                        trackerLocPendingReason = kinematicState.trackerHealth.locationPendingReason,
                        trackerLastValidFixRt = kinematicState.trackerHealth.lastValidFixRt,
                        viewerLat = kinematicState.localLocation.lat,
                        viewerLng = kinematicState.localLocation.lng,
                        viewerSpeed = kinematicState.localLocation.speed,
                        viewerAccuracy = kinematicState.localLocation.accuracy,
                        viewerMaxAcc = kinematicState.localLocation.maxAccuracy,
                        viewerGpsTs = kinematicState.localLocation.gpsTs,
                        viewerTelemetryTs = 0L,
                        viewerLocPending = kinematicState.localHealth.isLocationPending,
                        viewerLastValidFixRt = kinematicState.localHealth.lastValidFixRt,
                        replayCursorPos = kinematicState.replayCursorPos,
                        systemPulse = systemPulse,
                        systemPulseRt = systemPulseRt,
                        onEvent = { viewModel.onEvent(it) },
                        onClearTrails = { viewModel.clearTrails(context) },
                        trackerSegments = trackerSegments,
                        viewerSegments = viewerSegments,
                        violations = violations, onSaveTrail = onSaveTrail, onLoadTrail = onLoadTrail, 
                        showAccuracyBadge = false,
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
                                    if (uiState.hydrationLevel >= 4) {
                                        header()
                                    }
                                }
                                if (uiState.hydrationLevel >= 5) {
                                    statusBar()
                                }
                            }
                        }

                        if (uiState.isMapHydrated && isMapVisible && uiState.hydrationLevel >= 7) {
                            Box(Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp), contentAlignment = Alignment.CenterEnd) {
                                MapSettingsToggle(
                                    isMapButtonsVisible = uiState.isMapButtonsVisible,
                                    onToggle = { viewModel.onEvent(UiEvent.SetMapButtonsVisible(!uiState.isMapButtonsVisible)) }
                                )
                            }
                            
                            if (uiState.isMapButtonsVisible) {
                                Box(Modifier.fillMaxWidth().padding(end = 8.dp), contentAlignment = Alignment.CenterEnd) {
                                    MapToolsOverlay(
                                        isTrackerMode = false,
                                        trackerValid = PhysicsUtils.isValidLocation(kinematicState.trackerLocation.lat, kinematicState.trackerLocation.lng),
                                        viewerValid = PhysicsUtils.isValidLocation(kinematicState.localLocation.lat, kinematicState.localLocation.lng),
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
                        
                        if (uiState.hydrationLevel >= 4 && !isMapVisible) {
                            ViewerDashboard(
                                appMode = uiState.appMode ?: "viewer",
                                isDashboardExpanded = uiState.navigation.isDashboardExpanded,
                                isBatteryWhitelisted = uiState.permissions.isBatteryWhitelisted,
                                isLocalOnline = diagnosticState.connectivity.isLocalOnline,
                                isRelayConnected = diagnosticState.connectivity.isRelayConnected,
                                lastRemoteActivityTs = diagnosticState.connectivity.lastRemoteActivityTs,
                                trackerLocationTs = kinematicState.trackerLocation.gpsTs,
                                dashboardState = dashboardState,
                                gpsIdx = gpsIndexData,
                                rttValue = rtt,
                                trackerCurrentMa = trackerCurrentMa,
                                systemPulse = systemPulse,
                                onEvent = { viewModel.onEvent(it) }
                            )
                        }
                    }
                }
            }
        }

        // Issue #885: Staggered overlay composition to distribute JIT load.
        if (isSettingsOpen && uiState.hydrationLevel >= 8) {
            SettingsOverlay(
                activeSubSettings = uiState.navigation.activeSubSettings,
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
                        viewModel.audioSynthesizer.stopSiren(timeProvider = viewModel.timeProvider)
                    } else {
                        val s = uiState.draftSettings.alertSettings
                        val volume = if (s.useMaxVolume) 1.0f else if (s.useCustomVolume) s.alarmVolume else 1.0f
                        viewModel.audioSynthesizer.playSiren(
                            uiState.selectedSirenType, force = true, volume = volume, 
                            overrideSilence = s.overrideSilence,
                            loop = true, vibrate = s.vibrationEnabled,
                            timeProvider = viewModel.timeProvider
                        )
                    }
                },
                onShowPhoneSetup = { viewModel.onEvent(UiEvent.TogglePhoneSetup(true)) },
                onEvent = { viewModel.onEvent(it) }
            )
        } else if (isLogVisible && uiState.hydrationLevel >= 9) {
            val showDetails by viewModel.repository.logFilterDetails.collectAsStateWithLifecycle()
            val showRecovered by viewModel.repository.logFilterRecovered.collectAsStateWithLifecycle()
            LogOverlay(
                logsFlow = logsFlow, onExport = onExportLogs, onToggle = onToggleLog, onClear = onClearLogs,
                showDetails = showDetails, showRecovered = showRecovered, 
                onSetShowDetails = { viewModel.onEvent(UiEvent.SetLogFilterShowDetails(it)) }, 
                onSetShowRecovered = { viewModel.onEvent(UiEvent.SetLogFilterShowRecovered(it)) },
                appStartTime = uiState.appStartTime,
                systemPulse = systemPulse,
                isTelemetryFresh = dashboardState.isTelemetryFresh,
                onHistLink = { ts -> 
                    viewModel.onEvent(UiEvent.SetReplayCursor(ts))
                    viewModel.onEvent(UiEvent.ToggleRibbons(true))
                },
                onDetailsLink = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) }
            )
        } else if (isRibbonsVisible && uiState.hydrationLevel >= 10) {
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
        } else if (isGnssDetailVisible && uiState.hydrationLevel >= 11) {
            GnssDetailOverlay(
                gnssDetailFlow = viewModel.activeGnssDetail,
                onClose = { viewModel.onEvent(UiEvent.ToggleGnssDetail(false)) }
            )
        }
    }
}

@Composable
fun ViewerDashboard(
    appMode: String,
    isDashboardExpanded: Boolean,
    isBatteryWhitelisted: Boolean,
    isLocalOnline: Boolean,
    isRelayConnected: Boolean,
    lastRemoteActivityTs: Long,
    trackerLocationTs: Long,
    dashboardState: DashboardState,
    gpsIdx: GpsIndexData,
    rttValue: Int,
    trackerCurrentMa: Int,
    systemPulse: Long,
    onEvent: (UiEvent) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            if (isDashboardExpanded) {
                if (!isLandscape) {
                    val gpsAge = if (trackerLocationTs > 0) systemPulse - trackerLocationTs else Long.MAX_VALUE
                    Spacer(Modifier.height(4.dp))
                    TelemetryBox(
                        appMode = appMode,
                        isBatteryWhitelisted = isBatteryWhitelisted,
                        isLocalOnline = isLocalOnline,
                        isRelayConnected = isRelayConnected,
                        lastRemoteActivityTs = lastRemoteActivityTs,
                        systemPulse = systemPulse,
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
                        trackerCurrentMa = trackerCurrentMa,
                        gpsIdx = gpsIdx,
                        rttValue = rttValue,
                        cpuLoad = dashboardState.cpuLoad,
                        ioWait = dashboardState.ioWait,
                        maxIoLatency = dashboardState.maxIoLatency,
                        isUltraLongStationary = dashboardState.isUltraLongStationary,
                        onShowGnssDetail = { onEvent(UiEvent.ToggleGnssDetail(true)) }
                    )
                    DebugTable(
                        isLinkFresh = dashboardState.isLinkFresh,
                        isTelemetryFresh = dashboardState.isTelemetryFresh,
                        isGpsFresh = dashboardState.isGpsFresh,
                        trackerStateName = dashboardState.trackerState.name,
                        gpsAgeSec = if (gpsAge != Long.MAX_VALUE) gpsAge / 1000 else -1L,
                        rtt = rttValue,
                        currentMa = trackerCurrentMa
                    )
                }
                
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
                    Text("TERMINATE MONITORING SESSION", color = Rose500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
