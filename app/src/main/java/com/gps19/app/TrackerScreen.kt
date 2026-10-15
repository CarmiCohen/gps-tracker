package com.gps19.app

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps19.core.engine.*

/**
 * TrackerScreen: Tracker-mode UI.
 * Oct.14.17:
 * - A15 Hardening [v14]: Using standard collectAsState to ensure flow 
 *   propagation on lower-end Mali GPUs. Removed NavHost logic to allow 
 *   direct rendering.
 */

@Composable
fun TrackerScreen(
    stateProvider: UiStateProvider,
    onMainEvent: (UiEvent) -> Unit
) {
    // Oct.14.17: Force direct collection to bypass A15 state stalls
    val nav by stateProvider.navigation.collectAsState()
    val session by stateProvider.session.collectAsState()

    val isMapVisible = nav.isMapVisible && session.hydrationLevel >= 6
    val isAnyOverlayOpen = nav.isAnyOverlayOpen
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (session.hydrationLevel < 1) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                CircularProgressIndicator(color = BrandJd, strokeWidth = 2.dp, modifier = Modifier.size(32.dp)) 
            }
        } else {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (isMapVisible && !isAnyOverlayOpen) {
                                AppMapContainer(stateProvider = stateProvider, onEvent = onMainEvent)
                            } else {
                                TrackerDashboard(stateProvider = stateProvider, onEvent = onMainEvent)
                            }
                        }
                    }
                }
            } else {
                if (isMapVisible && !isAnyOverlayOpen) {
                    AppMapContainer(stateProvider = stateProvider, onEvent = onMainEvent)
                }
                Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                    Spacer(Modifier.height(82.dp))
                    if (!isMapVisible) {
                        TrackerDashboard(stateProvider = stateProvider, onEvent = onMainEvent)
                    }
                }
            }

            UnifiedHUDOverlay(
                stateProvider = stateProvider,
                onMainEvent = onMainEvent
            )
        }
    }
}

@Composable
fun TrackerDashboard(
    stateProvider: UiStateProvider,
    onEvent: (UiEvent) -> Unit
) {
    val dashboardState by stateProvider.dashboardState.collectAsState()
    val sessionState by stateProvider.session.collectAsState()
    val diagnosticState by stateProvider.diagnostic.collectAsState()
    val kinematicState by stateProvider.kinematic.collectAsState()
    val gpsIdx by stateProvider.gpsIndexData.collectAsState()
    val rttValue by stateProvider.rtt.collectAsState()
    val currentMaValue by stateProvider.currentMa.collectAsState()
    val systemPulse by stateProvider.systemPulseRt.collectAsState()
    val navState by stateProvider.navigation.collectAsState()

    val appMode = sessionState.appMode ?: "tracker"

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            if (navState.isDashboardExpanded) {
                if (appMode == "tracker") {
                    Spacer(Modifier.height(2.dp))
                    Icon(Icons.Default.Agriculture, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(4.dp))
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
                    acousticFloorDb = dashboardState.acousticFloorDb, trackerCurrentMa = currentMaValue, gpsIdx = gpsIdx, rttValue = rttValue, cpuLoad = dashboardState.cpuLoad, ioWait = dashboardState.ioWait, 
                    maxIoLatency = dashboardState.maxIoLatency, isUltraLongStationary = dashboardState.isUltraLongStationary, isSuspiciousNoise = dashboardState.isSuspiciousNoise, onEvent = onEvent
                )
                DebugTable(isLinkFresh = dashboardState.isLinkFresh, isTelemetryFresh = dashboardState.isTelemetryFresh, isGpsFresh = dashboardState.isGpsFresh, trackerStateName = dashboardState.trackerState.name, gpsAgeSec = if (kinematicState.localLocation.kinetic.gpsTs > 0) (systemPulse - kinematicState.localLocation.kinetic.gpsTs) / 1000 else -1L, rtt = rttValue, currentMa = currentMaValue)
                Spacer(Modifier.height(16.dp))
                SessionTerminationButton(appMode = appMode, onEvent = onEvent)
            } else { 
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { 
                    Text("Tap status card above to expand dashboard", color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.Medium) 
                } 
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
