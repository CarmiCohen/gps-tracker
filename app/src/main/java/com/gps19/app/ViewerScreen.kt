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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gps19.core.engine.*
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewerScreen: Viewer-mode UI.
 * Oct.5.20:
 * - SIMP-1426-4: Boilerplate Reduction. Refactored signature to use unified 
 *   UiStateProvider, eliminating redundant state parameter passing.
 * - SIMP-1426-3: Refactored to consume unified UiStateProvider. (R1426-3).
 */

@Composable
fun ViewerScreen(
    stateProvider: UiStateProvider,
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
    val session by stateProvider.session.collectAsStateWithLifecycle()
    val nav by stateProvider.navigation.collectAsStateWithLifecycle()
    val spatial by stateProvider.spatial.collectAsStateWithLifecycle()

    val isMapVisible = nav.isMapVisible
    val isAnyOverlayOpen = nav.isSettingsOpen || nav.isLogVisible || nav.isRibbonsVisible || nav.isGnssDetailVisible
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val onDashboard = {
        if (isMapVisible) onToggleMap()
        if (nav.isLogVisible) onToggleLog()
        if (nav.isSettingsOpen) onToggleSettings()
        if (nav.isRibbonsVisible) onMainEvent(UiEvent.ToggleRibbons(false))
        if (nav.isGnssDetailVisible) onMainEvent(UiEvent.ToggleGnssDetail(false))
    }

    val isSystemReady = session.isSystemReady(spatial.homePoints.size)
    val systemIssuesCount = session.systemIssuesCount(spatial.homePoints.size)

    val header = @Composable {
        HeaderBar(
            isLogVisible = nav.isLogVisible, isSettingsOpen = nav.isSettingsOpen, isRibbonsVisible = nav.isRibbonsVisible, isMapVisible = nav.isMapVisible, 
            requiresExtraTopPadding = session.permissions.requiresExtraTopPadding, isSystemReady = isSystemReady, systemIssuesCount = systemIssuesCount, 
            onDashboard = onDashboard, onS = onToggleSettings, onL = onToggleLog, onM = onToggleMap, onR = { onMainEvent(UiEvent.ToggleRibbons(!nav.isRibbonsVisible)) }, onEvent = { onMainEvent(it) }
        )
    }

    val statusBar = @Composable {
        GlobalStatusBar(stateProvider = stateProvider, modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onMainEvent(UiEvent.SetRedScreenVisible(true)) }) })
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (session.hydrationLevel < 3) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrandJd, strokeWidth = 2.dp, modifier = Modifier.size(32.dp)) }
        } else {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (session.hydrationLevel >= 4 && !isAnyOverlayOpen) header()
                    Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                        if (session.hydrationLevel >= 5 && !isAnyOverlayOpen) statusBar()
                        Box(modifier = Modifier.weight(1f)) {
                            if (session.hydrationLevel >= 6 && isMapVisible) {
                                AppMapContainer(mapViewStateFlow = stateProvider.mapViewState, cameraActions = (stateProvider as? MainViewModel)?.cameraActions, onEvent = { onMainEvent(it) }, onClearTrails = { (stateProvider as? MainViewModel)?.clearTrails() }, onSaveTrail = onSaveTrail, onLoadTrail = onLoadTrail)
                            } else if (session.hydrationLevel >= 4 && !isMapVisible) {
                                ViewerDashboard(stateProvider = stateProvider, isDashboardExpanded = nav.isDashboardExpanded, onEvent = { onMainEvent(it) })
                            }
                        }
                    }
                }
            } else {
                if (session.hydrationLevel >= 6 && isMapVisible) {
                    AppMapContainer(mapViewStateFlow = stateProvider.mapViewState, cameraActions = (stateProvider as? MainViewModel)?.cameraActions, onEvent = { onMainEvent(it) }, onClearTrails = { (stateProvider as? MainViewModel)?.clearTrails() }, onSaveTrail = onSaveTrail, onLoadTrail = onLoadTrail)
                }
                Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                    Surface(color = if (isAnyOverlayOpen || isMapVisible) Color.Transparent else MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth().zIndex(10f)) { Column { if (session.hydrationLevel >= 4 && !isAnyOverlayOpen) header(); if (session.hydrationLevel >= 5 && !isAnyOverlayOpen) statusBar() } }
                    if (session.hydrationLevel >= 4 && !isMapVisible) {
                        ViewerDashboard(stateProvider = stateProvider, isDashboardExpanded = nav.isDashboardExpanded, onEvent = { onMainEvent(it) })
                    }
                }
            }
        }
    }
}

@Composable
fun ViewerDashboard(
    stateProvider: UiStateProvider,
    isDashboardExpanded: Boolean,
    onEvent: (UiEvent) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val dashboardState by stateProvider.dashboardState.collectAsStateWithLifecycle()
    val sessionState by stateProvider.session.collectAsStateWithLifecycle()
    val diagnosticState by stateProvider.diagnostic.collectAsStateWithLifecycle()
    val kinematicState by stateProvider.kinematic.collectAsStateWithLifecycle()
    val gpsIdx by stateProvider.gpsIndexData.collectAsStateWithLifecycle()
    val rttValue by stateProvider.rtt.collectAsStateWithLifecycle()
    val currentMaValue by stateProvider.currentMa.collectAsStateWithLifecycle()
    val systemPulse by stateProvider.systemPulseRt.collectAsStateWithLifecycle()
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            if (isDashboardExpanded) {
                if (!isLandscape) {
                    val trackerLocationTs = kinematicState.trackerLocation.kinetic.gpsTs
                    val gpsAge = if (trackerLocationTs > 0) systemPulse - trackerLocationTs else Long.MAX_VALUE
                    Spacer(Modifier.height(4.dp))
                    TelemetryBox(
                        appMode = sessionState.appMode ?: "viewer", isBatteryWhitelisted = sessionState.permissions.isBatteryWhitelisted, isLocalOnline = diagnosticState.connectivity.isLocalOnline, isRelayConnected = diagnosticState.connectivity.isRelayConnected, 
                        lastRemoteActivityTs = diagnosticState.connectivity.lastRemoteActivityTs, systemPulse = systemPulse, isGpsFresh = dashboardState.isGpsFresh, isTelemetryFresh = dashboardState.isTelemetryFresh, 
                        isLinkFresh = dashboardState.isLinkFresh, trackerState = dashboardState.trackerState, isLocationPending = dashboardState.isLocationPending, locationPendingReason = dashboardState.locationPendingReason, 
                        status = dashboardState.status, tamperReason = dashboardState.tamperReason, isTamperDetected = dashboardState.isTamperDetected, isBatterySteepDischarge = dashboardState.isBatterySteepDischarge, 
                        isBatteryLow = dashboardState.isBatteryLow, isBatteryCritical = dashboardState.isBatteryCritical, maxDropMs = dashboardState.maxDropMs, lastSeenTs = dashboardState.lastSeenTs, totalDropMs = dashboardState.totalDropMs, 
                        totalUptimeMs = dashboardState.totalUptimeMs, sessionMs = dashboardState.sessionMs, engineVersion = dashboardState.engineVersion, sinceConnMs = dashboardState.sinceConnMs, sinceDiscoMs = dashboardState.sinceDiscoMs, 
                        violationUptimeMs = dashboardState.violationUptimeMs, watchdogCountdownSec = dashboardState.watchdogCountdownSec, watchdogOk = dashboardState.watchdogOk, isPowerSaveMode = dashboardState.isPowerSaveMode, 
                        standbyBucket = dashboardState.standbyBucket, netInterface = dashboardState.netInterface, isStorageLow = dashboardState.isStorageLow, isStorageCritical = dashboardState.isStorageCritical, 
                        distToHome = dashboardState.distToHome, distToViewer = dashboardState.distToViewer, lat = dashboardState.lat, lng = dashboardState.lng, gpsSpeedMps = dashboardState.gpsSpeedMps, trackerAccuracy = dashboardState.trackerAccuracy, 
                        trackerMaxAcc = dashboardState.trackerMaxAcc, viewerAccuracy = dashboardState.viewerAccuracy, viewerMaxAcc = dashboardState.viewerMaxAcc, satsUsed = dashboardState.satsUsed, satsView = dashboardState.satsView, 
                        isSatsIndexWarning = dashboardState.isSatsIndexWarning, snr = dashboardState.snr, vibration = dashboardState.vibration, heading = dashboardState.heading, tilt = dashboardState.tilt, acousticDb = dashboardState.acousticDb, 
                        baroAlt = dashboardState.baroAlt, lux = dashboardState.lux, proximityCm = dashboardState.proximityCm, proximityDebounceMs = dashboardState.proximityDebounceMs, rollingVibration = dashboardState.rollingVibration, 
                        trackerMaxTemp = dashboardState.trackerMaxTemp, viewerMaxTemp = dashboardState.viewerMaxTemp, peakShock = dashboardState.peakShock, vibrationFloor = dashboardState.vibrationFloor, luxBaseline = dashboardState.luxBaseline, 
                        acousticFloorDb = dashboardState.acousticFloorDb, trackerCurrentMa = currentMaValue, gpsIdx = gpsIdx, rttValue = rttValue, cpuLoad = dashboardState.cpuLoad, ioWait = dashboardState.ioWait, 
                        maxIoLatency = dashboardState.maxIoLatency, isUltraLongStationary = dashboardState.isUltraLongStationary, onShowGnssDetail = { onEvent(UiEvent.ToggleGnssDetail(true)) }
                    )
                    DebugTable(isLinkFresh = dashboardState.isLinkFresh, isTelemetryFresh = dashboardState.isTelemetryFresh, isGpsFresh = dashboardState.isGpsFresh, trackerStateName = dashboardState.trackerState.name, gpsAgeSec = if (gpsAge != Long.MAX_VALUE) gpsAge / 1000 else -1L, rtt = rttValue, currentMa = currentMaValue)
                }
                Spacer(Modifier.height(16.dp)); SessionTerminationButton(appMode = sessionState.appMode ?: "viewer", onTerminate = { onEvent(UiEvent.ShowStopTrackingConfirmation(true)) })
            } else { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text("Tap status card above to expand dashboard", color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.Medium) } }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
