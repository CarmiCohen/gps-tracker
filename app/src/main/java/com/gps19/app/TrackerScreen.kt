package com.gps19.app

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
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

/**
 * TrackerScreen: Tracker-mode UI.
 * Oct.7.8:
 * - Issue #SIMP-1010-1: Adaptive Acoustic Gating. Propagated isSuspiciousNoise 
 *   flag to TelemetryBox for HUD visibility.
 * Oct.5.21:
 * - SIMP-1426-4: Boilerplate Reduction. Refactored signature to use MainViewModel 
 *   directly for event routing and state hydration manager. (R1426-4).
 */

@Composable
fun TrackerScreen(
    activity: ComponentActivity,
    viewModel: MainViewModel
) {
    val nav by viewModel.navigation.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val spatial by viewModel.spatial.collectAsStateWithLifecycle()

    val isMapVisible = nav.isMapVisible
    val isAnyOverlayOpen = nav.isSettingsOpen || nav.isLogVisible || nav.isRibbonsVisible || nav.isGnssDetailVisible || nav.isPhoneSetupVisible
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val onDashboard = {
        if (isMapVisible) viewModel.onEvent(UiEvent.ToggleMap(false))
        if (nav.isLogVisible) viewModel.onEvent(UiEvent.ToggleLog(false))
        if (nav.isSettingsOpen) viewModel.onEvent(UiEvent.ToggleSettings(false))
        if (nav.isRibbonsVisible) viewModel.onEvent(UiEvent.ToggleRibbons(false))
        if (nav.isGnssDetailVisible) viewModel.onEvent(UiEvent.ToggleGnssDetail(false))
        if (nav.isPhoneSetupVisible) viewModel.onEvent(UiEvent.TogglePhoneSetup(false))
    }

    val isSystemReady = session.isSystemReady(spatial.homePoints.size)
    val systemIssuesCount = session.systemIssuesCount(spatial.homePoints.size)

    val header = @Composable {
        HeaderBar(
            isLogVisible = nav.isLogVisible, isSettingsOpen = nav.isSettingsOpen, isRibbonsVisible = nav.isRibbonsVisible, isMapVisible = nav.isMapVisible, isPhoneSetupVisible = nav.isPhoneSetupVisible, 
            requiresExtraTopPadding = session.permissions.requiresExtraTopPadding, isSystemReady = isSystemReady, systemIssuesCount = systemIssuesCount, 
            onDashboard = onDashboard, onS = { viewModel.onEvent(UiEvent.ToggleSettings(!nav.isSettingsOpen)) }, onL = { viewModel.onEvent(UiEvent.ToggleLog(!nav.isLogVisible)) }, onM = { viewModel.onEvent(UiEvent.ToggleMap(!nav.isMapVisible)) }, onR = { viewModel.onEvent(UiEvent.ToggleRibbons(!nav.isRibbonsVisible)) }, onEvent = { viewModel.onEvent(it) }
        )
    }

    val statusBar = @Composable {
        GlobalStatusBar(stateProvider = viewModel, modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { viewModel.onEvent(UiEvent.SetRedScreenVisible(true)) }) })
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (session.hydrationLevel < 1) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrandJd, strokeWidth = 2.dp, modifier = Modifier.size(32.dp)) }
        } else {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    header(); Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                        statusBar()
                        Box(modifier = Modifier.weight(1f)) {
                            if (session.hydrationLevel >= 4 && isMapVisible && !isAnyOverlayOpen) {
                                AppMapContainer(stateProvider = viewModel)
                            } else if (session.hydrationLevel >= 2 && !isMapVisible) {
                                TrackerDashboard(viewModel = viewModel)
                            }
                        }
                    }
                }
            } else {
                if (session.hydrationLevel >= 4 && isMapVisible && !isAnyOverlayOpen) {
                    AppMapContainer(stateProvider = viewModel)
                }
                if (!isAnyOverlayOpen) {
                    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth().zIndex(10f)) { Column { Box(Modifier.statusBarsPadding()) { header() }; statusBar() } }
                        if (session.hydrationLevel >= 2 && !isMapVisible) {
                            TrackerDashboard(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackerDashboard(
    viewModel: MainViewModel
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val sessionState by viewModel.session.collectAsStateWithLifecycle()
    val diagnosticState by viewModel.diagnostic.collectAsStateWithLifecycle()
    val kinematicState by viewModel.kinematic.collectAsStateWithLifecycle()
    val gpsIdx by viewModel.gpsIndexData.collectAsStateWithLifecycle()
    val rttValue by viewModel.rtt.collectAsStateWithLifecycle()
    val currentMaValue by viewModel.currentMa.collectAsStateWithLifecycle()
    val systemPulse by viewModel.systemPulseRt.collectAsStateWithLifecycle()

    val appMode = sessionState.appMode ?: "tracker"

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            if (viewModel.navigation.value.isDashboardExpanded) {
                if (appMode == "tracker") {
                    Spacer(Modifier.height(2.dp)); Icon(Icons.Default.Agriculture, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)); Spacer(Modifier.height(4.dp))
                }
                TelemetryBox(
                    appMode = appMode, isBatteryWhitelisted = sessionState.permissions.isBatteryWhitelisted, isLocalOnline = diagnosticState.connectivity.isLocalOnline, isRelayConnected = diagnosticState.connectivity.isRelayConnected, 
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
                    acousticFloorDb = dashboardState.acousticFloorDb, trackerCurrentMa = dashboardState.trackerCurrentMa, gpsIdx = gpsIdx, rttValue = rttValue, cpuLoad = dashboardState.cpuLoad, ioWait = dashboardState.ioWait, 
                    maxIoLatency = dashboardState.maxIoLatency, isUltraLongStationary = dashboardState.isUltraLongStationary, isSuspiciousNoise = dashboardState.isSuspiciousNoise, onShowGnssDetail = { viewModel.onEvent(UiEvent.ToggleGnssDetail(true)) }
                )
                DebugTable(isLinkFresh = dashboardState.isLinkFresh, isTelemetryFresh = dashboardState.isTelemetryFresh, isGpsFresh = dashboardState.isGpsFresh, trackerStateName = dashboardState.trackerState.name, gpsAgeSec = if (kinematicState.localLocation.kinetic.gpsTs > 0) (systemPulse - kinematicState.localLocation.kinetic.gpsTs) / 1000 else -1L, rtt = rttValue, currentMa = currentMaValue)
                Spacer(Modifier.height(16.dp)); SessionTerminationButton(appMode = appMode, onTerminate = { viewModel.onEvent(UiEvent.ShowStopTrackingConfirmation(true)) })
            } else { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text("Tap status card above to expand dashboard", color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.Medium) } }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
