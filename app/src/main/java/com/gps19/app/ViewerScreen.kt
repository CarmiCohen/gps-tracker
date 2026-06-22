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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.osmdroid.util.GeoPoint
import com.gps19.core.engine.*

/**
 * ViewerScreen: Pocket-mode UI.
 * v8.9.18:
 * - Issue #221: Propagating systemPulseRealtime for Bayesian uncertainty scaling.
 * v8.9.2:
 * - Issue 182: Synchronized source headers with v8.9.2 baseline.
 * v8.8.36:
 * - Issue 165: Migrated to PhysicsUtils for location validation.
 * v8.8.21:
 * - Timing Integrity: Passed TimeProvider to AudioSynthesizer for synchronized siren control.
 */

@Composable
fun ViewerScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    logs: List<LogEntry>,
    trackerTrail: List<TrailPoint>,
    viewerTrail: List<TrailPoint>,
    violations: List<ViolationPoint>,
    systemPulse: Long,
    systemPulseRealtime: Long,
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
            uiState = uiState,
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
            uiState = uiState,
            dashboardState = dashboardState,
            systemPulse = systemPulse,
            rttFlow = viewModel.rtt,
            remoteSignalFlow = viewModel.remoteSignal,
            redScreenVisibleFlow = viewModel.redScreenVisible,
            onEvent = { viewModel.onEvent(it) },
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
                                uiState = uiState,
                                systemPulse = systemPulse,
                                systemPulseRealtime = systemPulseRealtime,
                                onEvent = { viewModel.onEvent(it) },
                                onClearTrails = { viewModel.clearTrails(context) },
                                trail = trackerTrail, viewerTrail = viewerTrail, 
                                violations = violations, onSaveTrail = onSaveTrail, onLoadTrail = onLoadTrail, 
                                showAccuracyBadge = false,
                                showSettingsButton = true,
                                showToolsOverlay = true
                            )
                        } else {
                            ViewerDashboard(uiState, dashboardState, systemPulse, viewModel, onEvent = { viewModel.onEvent(it) })
                        }
                    }
                }
            }
        } else {
            if (isMapVisible) {
                AppMapContainer(
                    uiState = uiState,
                    systemPulse = systemPulse,
                    systemPulseRealtime = systemPulseRealtime,
                    onEvent = { viewModel.onEvent(it) },
                    onClearTrails = { viewModel.clearTrails(context) },
                    trail = trackerTrail, viewerTrail = viewerTrail, 
                    violations = violations, onSaveTrail = onSaveTrail, onLoadTrail = onLoadTrail, 
                    showAccuracyBadge = false,
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
                                    isTrackerMode = false,
                                    trackerValid = PhysicsUtils.isValidLocation(uiState.trackerLocation.lat, uiState.trackerLocation.lng),
                                    viewerValid = PhysicsUtils.isValidLocation(uiState.localLocation.lat, uiState.localLocation.lng),
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
                        ViewerDashboard(uiState, dashboardState, systemPulse, viewModel, onEvent = { viewModel.onEvent(it) })
                    }
                }
            }
        }

        if (isSettingsOpen) {
            SettingsOverlay(
                uiState = uiState, onClose = onToggleSettings, onReset = onResetStats,
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
                    if (uiState.isSirenPlaying) {
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
                viewModel = viewModel,
                onCalibrateChair = { viewModel.onEvent(UiEvent.CalibrateChair) }
            )
        } else if (isLogVisible) {
            val showDetails by viewModel.repository.logFilterDetails.collectAsStateWithLifecycle()
            val showRecovered by viewModel.repository.logFilterRecovered.collectAsStateWithLifecycle()
            LogOverlay(
                logs = logs, onExport = onExportLogs, onToggle = onToggleLog, onClear = onClearLogs,
                showDetails = showDetails, showRecovered = showRecovered, 
                onSetShowDetails = { viewModel.onEvent(UiEvent.SetLogFilterShowDetails(it)) }, 
                onSetShowRecovered = { viewModel.onEvent(UiEvent.SetLogFilterShowRecovered(it)) },
                appStartTime = uiState.appStartTime,
                systemPulse = systemPulse
            )
        } else if (isRibbonsVisible) {
            RibbonsOverlay(viewModel = viewModel, onDismiss = { viewModel.onEvent(UiEvent.ToggleRibbons(false)) })
        } else if (isGnssDetailVisible) {
            GnssDetailOverlay(
                gnssDetailFlow = viewModel.activeGnssDetail,
                onClose = { viewModel.onEvent(UiEvent.ToggleGnssDetail(false)) }
            )
        }
    }
}

@Composable
fun ViewerDashboard(uiState: MainUiState, dashboardState: DashboardState, systemPulse: Long, viewModel: MainViewModel, onEvent: (UiEvent) -> Unit) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            if (uiState.navigation.isDashboardExpanded) {
                if (!isLandscape) {
                    Spacer(Modifier.height(4.dp))
                    TelemetryBox(
                        uiState = uiState, 
                        dashboard = dashboardState, 
                        systemPulse = systemPulse, 
                        gpsIndexDataFlow = viewModel.gpsIndexData, 
                        rttFlow = viewModel.rtt,
                        onCalibrateChair = { onEvent(UiEvent.CalibrateChair) },
                        onShowGnssDetail = { onEvent(UiEvent.ToggleGnssDetail(true)) }
                    )
                    DebugTable(
                        uiState = uiState, 
                        dashboard = dashboardState, 
                        systemPulse = systemPulse,
                        rttFlow = viewModel.rtt,
                        currentMaFlow = if (uiState.appMode == "viewer") viewModel.trackerCurrentMa else viewModel.currentMa
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
