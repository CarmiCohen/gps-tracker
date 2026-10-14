package com.gps19.app

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gps19.core.engine.*

/**
 * ViewerScreen: Viewer-mode UI.
 * Oct.13.21:
 * - SIMP-OCT13.21-S1: Pure Event UI. Finalized removal of onSaveTrail and 
 *   onLoadTrail lambdas from signature.
 */

@Composable
fun ViewerScreen(
    stateProvider: UiStateProvider,
    onMainEvent: (UiEvent) -> Unit
) {
    val session by stateProvider.session.collectAsStateWithLifecycle()
    val nav by stateProvider.navigation.collectAsStateWithLifecycle()

    val isMapVisible = nav.isMapVisible
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (session.hydrationLevel < 3) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrandJd, strokeWidth = 2.dp, modifier = Modifier.size(32.dp)) }
        } else {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (session.hydrationLevel >= 6 && isMapVisible) {
                                AppMapContainer(stateProvider = stateProvider)
                            } else if (session.hydrationLevel >= 4 && !isMapVisible) {
                                ViewerDashboard(stateProvider = stateProvider, isDashboardExpanded = nav.isDashboardExpanded, onEvent = { onMainEvent(it) })
                            }
                        }
                    }
                }
            } else {
                if (session.hydrationLevel >= 6 && isMapVisible) {
                    AppMapContainer(stateProvider = stateProvider)
                }
                Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                    Spacer(Modifier.height(82.dp)) // Reserve space for Unified HUD
                    if (session.hydrationLevel >= 4 && !isMapVisible) {
                        ViewerDashboard(stateProvider = stateProvider, isDashboardExpanded = nav.isDashboardExpanded, onEvent = { onMainEvent(it) })
                    }
                }
            }

            // Unified HUD Overlay
            UnifiedHUDOverlay(
                stateProvider = stateProvider,
                onMainEvent = onMainEvent
            )
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
                        maxIoLatency = dashboardState.maxIoLatency, isUltraLongStationary = dashboardState.isUltraLongStationary, isSuspiciousNoise = dashboardState.isSuspiciousNoise, onShowGnssDetail = { onEvent(UiEvent.ToggleGnssDetail(true)) }
                    )
                    DebugTable(isLinkFresh = dashboardState.isLinkFresh, isTelemetryFresh = dashboardState.isTelemetryFresh, isGpsFresh = dashboardState.isGpsFresh, trackerStateName = dashboardState.trackerState.name, gpsAgeSec = if (gpsAge != Long.MAX_VALUE) gpsAge / 1000 else -1L, rtt = rttValue, currentMa = currentMaValue)
                }
                Spacer(Modifier.height(16.dp)); SessionTerminationButton(appMode = sessionState.appMode ?: "viewer", onTerminate = { onEvent(UiEvent.ShowStopTrackingConfirmation(true)) })
            } else { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text("Tap status card above to expand dashboard", color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.Medium) } }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
