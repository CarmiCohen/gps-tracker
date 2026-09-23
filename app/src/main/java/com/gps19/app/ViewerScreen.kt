package com.gps19.app

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gps19.core.engine.*
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewerScreen: Viewer-mode UI.
 * Sep.23.50:
 * - Issue #1203 RESOLVED: Unified ViewModel scope. Using MainViewModel directly 
 *   to eliminate resource churn and state fragmentation (R-ID 419).
 */

@Composable
fun ViewerScreen(
    sessionState: SessionUiState,
    settingsState: SettingsUiState,
    spatialState: SpatialUiState,
    navigationState: NavigationState,
    kinematicState: KinematicState,
    diagnosticState: DiagnosticState,
    viewModel: MainViewModel,
    logsFlow: StateFlow<List<LogEntry>>,
    onToggleMap: () -> Unit,
    onToggleLog: () -> Unit,
    onToggleSettings: () -> Unit,
    onExit: () -> Unit,
    onImportConfig: () -> Unit,
    onExportLogs: () -> Unit,
    onClearLogs: () -> Unit,
    onMainEvent: (UiEvent) -> Unit,
    onFullInitialization: () -> Unit,
    onResetStats: () -> Unit = {},
    onClearHome: () -> Unit = {},
    onSaveTrail: () -> Unit = {},
    onLoadTrail: () -> Unit = {}
) {
    val nav = navigationState
    val isMapVisible = nav.isMapVisible
    val isLogVisible = nav.isLogVisible
    val isSettingsOpen = nav.isSettingsOpen
    val isRibbonsVisible = nav.isRibbonsVisible
    val isGnssDetailVisible = nav.isGnssDetailVisible
    val isAnyOverlayOpen = isSettingsOpen || isLogVisible || isRibbonsVisible || isGnssDetailVisible

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val gpsIndexData by viewModel.gpsIndexData.collectAsStateWithLifecycle()
    val rtt by viewModel.rtt.collectAsStateWithLifecycle()
    val currentMa by viewModel.currentMa.collectAsStateWithLifecycle()
    
    val hudConnectivity by viewModel.hudConnectivityState.collectAsStateWithLifecycle()
    val hudTelemetry by viewModel.hudTelemetryState.collectAsStateWithLifecycle()
    val hudHealth by viewModel.hudHealthState.collectAsStateWithLifecycle()

    val mapViewState by viewModel.mapViewState.collectAsStateWithLifecycle()

    val onDashboard = {
        if (isMapVisible) onToggleMap()
        if (isLogVisible) onToggleLog()
        if (isSettingsOpen) onToggleSettings()
        if (isRibbonsVisible) onMainEvent(UiEvent.ToggleRibbons(false))
        if (isGnssDetailVisible) onMainEvent(UiEvent.ToggleGnssDetail(false))
    }

    val isSystemReady = sessionState.isSetupBypassActive || (
            sessionState.permissions.isFineLocationGranted &&
            sessionState.permissions.isBatteryWhitelisted && 
            sessionState.permissions.isAutoStartGranted &&
            sessionState.permissions.isOverlayGranted &&
            sessionState.permissions.isMicrophoneGranted &&
            sessionState.permissions.isExactAlarmGranted && 
            sessionState.permissions.isPostNotificationsGranted &&
            sessionState.permissions.isBackgroundLocationGranted &&
            sessionState.permissions.isActivityRecognitionGranted &&
            (sessionState.appMode != null) &&
            (sessionState.appMode != "tracker" || sessionState.permissions.isMicrophoneGranted) &&
            (sessionState.appMode == "tracker" || spatialState.homePoints.isNotEmpty()) &&
            (!sessionState.permissions.hasBackgroundRestriction || 
             (sessionState.permissions.backgroundStatus == CapabilityStatus.GRANTED && sessionState.permissions.autostartStatus == CapabilityStatus.GRANTED) || 
             (sessionState.permissions.backgroundStatus == CapabilityStatus.UNKNOWN && sessionState.permissions.isManualOverride)))

    val systemIssuesCount = if (sessionState.isSetupBypassActive) 0 else {
        var count = 0
        if (!sessionState.permissions.isFineLocationGranted) count++
        if (!sessionState.permissions.isBatteryWhitelisted) count++
        if (!sessionState.permissions.isAutoStartGranted) count++
        if (!sessionState.permissions.isExactAlarmGranted) count++
        if (!sessionState.permissions.isOverlayGranted) count++
        if (!sessionState.permissions.isPostNotificationsGranted) count++
        if (!sessionState.permissions.isBackgroundLocationGranted) count++
        if (!sessionState.permissions.isActivityRecognitionGranted) count++
        if (sessionState.appMode == "tracker" && !sessionState.permissions.isMicrophoneGranted) count++
        if (sessionState.appMode != "tracker" && spatialState.homePoints.isEmpty()) count++
        val configIssue = sessionState.permissions.hasBackgroundRestriction && 
                         (sessionState.permissions.backgroundStatus == CapabilityStatus.GRANTED || 
                          sessionState.permissions.autostartStatus == CapabilityStatus.GRANTED) &&
                         !(sessionState.permissions.backgroundStatus == CapabilityStatus.UNKNOWN && sessionState.permissions.isManualOverride)
        if (configIssue) count++
        count
    }

    val header = @Composable {
        HeaderBar(
            isLogVisible = nav.isLogVisible,
            isSettingsOpen = nav.isSettingsOpen,
            isRibbonsVisible = nav.isRibbonsVisible,
            isMapVisible = nav.isMapVisible,
            requiresExtraTopPadding = sessionState.permissions.requiresExtraTopPadding,
            isSystemReady = isSystemReady,
            systemIssuesCount = systemIssuesCount,
            onDashboard = onDashboard,
            onS = onToggleSettings,
            onL = onToggleLog,
            onM = onToggleMap,
            onR = { onMainEvent(UiEvent.ToggleRibbons(!isRibbonsVisible)) },
            onEvent = { event -> onMainEvent(event) }
        )
    }

    val statusBar = @Composable {
        GlobalStatusBar(
            connectivity = hudConnectivity,
            telemetry = hudTelemetry,
            health = hudHealth,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { onMainEvent(UiEvent.SetRedScreenVisible(true)) })
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (sessionState.hydrationLevel < 3) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandJd, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            }
        } else {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (sessionState.hydrationLevel >= 4) {
                        header()
                    }
                    
                    Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                        if (sessionState.hydrationLevel >= 5) {
                            statusBar()
                        }
                        
                        Box(modifier = Modifier.weight(1f)) {
                            if (sessionState.hydrationLevel >= 6 && isMapVisible && !isAnyOverlayOpen) {
                                AppMapContainer(
                                    state = mapViewState,
                                    onEvent = { event -> viewModel.onEvent(event) },
                                    onClearTrails = { viewModel.clearTrails() },
                                    onSaveTrail = onSaveTrail,
                                    onLoadTrail = onLoadTrail
                                )
                            } else if (sessionState.hydrationLevel >= 4 && !isMapVisible) {
                                ViewerDashboard(
                                    appMode = sessionState.appMode ?: "viewer",
                                    isDashboardExpanded = nav.isDashboardExpanded,
                                    isBatteryWhitelisted = sessionState.permissions.isBatteryWhitelisted,
                                    isLocalOnline = diagnosticState.connectivity.isLocalOnline,
                                    isRelayConnected = diagnosticState.connectivity.isRelayConnected,
                                    lastRemoteActivityTs = diagnosticState.connectivity.lastRemoteActivityTs,
                                    trackerLocationTs = kinematicState.trackerLocation.kinetic.gpsTs,
                                    dashboardState = dashboardState,
                                    gpsIdx = gpsIndexData,
                                    rttValue = rtt,
                                    trackerCurrentMa = currentMa,
                                    systemPulse = mapViewState.systemPulseRt,
                                    onEvent = { event -> onMainEvent(event) }
                                )
                            }
                        }
                    }
                }
            } else {
                if (sessionState.hydrationLevel >= 6 && isMapVisible && !isAnyOverlayOpen) {
                    AppMapContainer(
                        state = mapViewState,
                        onEvent = { event -> viewModel.onEvent(event) },
                        onClearTrails = { viewModel.clearTrails() },
                        onSaveTrail = onSaveTrail,
                        onLoadTrail = onLoadTrail
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
                                    if (sessionState.hydrationLevel >= 4) {
                                        header()
                                    }
                                }
                                if (sessionState.hydrationLevel >= 5) {
                                    statusBar()
                                }
                            }
                        }
                        
                        if (sessionState.hydrationLevel >= 4 && !isMapVisible) {
                            ViewerDashboard(
                                appMode = sessionState.appMode ?: "viewer",
                                isDashboardExpanded = nav.isDashboardExpanded,
                                isBatteryWhitelisted = sessionState.permissions.isBatteryWhitelisted,
                                isLocalOnline = diagnosticState.connectivity.isLocalOnline,
                                isRelayConnected = diagnosticState.connectivity.isRelayConnected,
                                lastRemoteActivityTs = diagnosticState.connectivity.lastRemoteActivityTs,
                                trackerLocationTs = kinematicState.trackerLocation.kinetic.gpsTs,
                                dashboardState = dashboardState,
                                gpsIdx = gpsIndexData,
                                rttValue = rtt,
                                trackerCurrentMa = currentMa,
                                systemPulse = mapViewState.systemPulseRt,
                                onEvent = { event -> onMainEvent(event) }
                            )
                        }
                    }
                }
            }
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
                        tamperReason = dashboardState.tamperReason,
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
                SessionTerminationButton(
                    appMode = appMode,
                    onTerminate = { onEvent(UiEvent.ShowStopTrackingConfirmation(true)) }
                )
            } else {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Tap status card above to expand dashboard", color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
