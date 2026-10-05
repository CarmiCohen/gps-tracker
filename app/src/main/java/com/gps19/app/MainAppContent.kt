package com.gps19.app

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gps19.core.engine.STARTUP_SETTLING_DELAY_MS
import com.gps19.core.engine.CapabilityStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * MainAppContent: Root UI composition.
 * Oct.5.6:
 * - Issue #1328: Phase 2 - UI Performance Hardening. Refactored state collection 
 *   to eliminate root-level high-frequency recompositions. Moved kinematic and 
 *   diagnostic state collection down to specialized screens. (R1328).
 * Oct.4.1:
 * - Issue #1202: UI Event Routing Unification.
 */
@Composable
fun MainAppContent(
    activity: ComponentActivity,
    viewModel: MainViewModel,
    onStartService: (String) -> Unit,
    onCleanupAndExit: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAppInfo: () -> Unit,
    onRequestAppInfoForMode: (String) -> Unit = {},
    onRequestExactAlarm: () -> Unit,
    onRequestHardwarePermission: () -> Unit,
    onStopTracking: () -> Unit
) {
    // R1328: Collect only core navigation and session states at root.
    val sessionState by viewModel.sessionUiState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val spatialState by viewModel.spatialUiState.collectAsStateWithLifecycle()
    val navigationState by viewModel.navigationState.collectAsStateWithLifecycle()
    
    // R1328: HudHealth collected for AlarmOverlay logic but is decoupled from KinematicState.
    val hudHealth by viewModel.hudHealthState.collectAsStateWithLifecycle()
    
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onEvent(UiEvent.SetUiVisible(true))
                Lifecycle.Event.ON_PAUSE -> viewModel.onEvent(UiEvent.SetUiVisible(false))
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showBackgroundDisclosure by remember { mutableStateOf(false) }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            navigationState.pendingMode?.let { mode ->
                viewModel.onEvent(UiEvent.RequestProceedToMode(mode))
                viewModel.onEvent(UiEvent.SetPendingMode(null))
            }
        } else {
            viewModel.onEvent(UiEvent.SetManualSelection(false))
            Toast.makeText(activity, context.getString(R.string.perm_background_denied_toast), Toast.LENGTH_LONG).show()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onEvent(UiEvent.RefreshPermissionStatus)
        navigationState.pendingMode?.let { mode ->
            viewModel.onEvent(UiEvent.InitiateMode(mode))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffects.collectLatest { effect ->
            when (effect) {
                is UiEffect.Navigate -> {
                    navController.navigate(effect.route) {
                        effect.popUpTo?.let { popUpTo(it) { inclusive = effect.inclusive } }
                        launchSingleTop = true
                    }
                }
                is UiEffect.StartService -> onStartService(effect.mode)
                UiEffect.CleanupAndExit -> onCleanupAndExit()
                UiEffect.StopTracking -> onStopTracking()
                is UiEffect.RequestPermissions -> requestPermissionLauncher.launch(effect.permissions.toTypedArray())
                is UiEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is UiEffect.ShowBackgroundDisclosure -> {
                    showBackgroundDisclosure = true
                }
            }
        }
    }

    LaunchedEffect(sessionState.isInitialized, sessionState.appMode, navigationState.isDiagnosticsVisible, spatialState.isManualSelectionInProgress, sessionState.isSettlingActive, sessionState.isSystemActive) {
        if (!sessionState.isInitialized) return@LaunchedEffect
        
        val mode = sessionState.appMode
        val isDiagnostics = navigationState.isDiagnosticsVisible

        if (isDiagnostics) {
            if (navController.currentDestination?.route != Screen.Diagnostics.route) {
                navController.navigate(Screen.Diagnostics.route) { launchSingleTop = true }
            }
            return@LaunchedEffect
        }

        if (mode != null && sessionState.isSettlingActive && !spatialState.isManualSelectionInProgress) {
            if (navController.currentDestination?.route == Screen.Landing.route) {
                delay(STARTUP_SETTLING_DELAY_MS)
                viewModel.onEvent(UiEvent.SetSettlingActive(false))
                viewModel.onEvent(UiEvent.InitiateMode(mode))
            }
        }

        if (sessionState.isSettlingActive && mode != null) return@LaunchedEffect

        when (mode) {
            "tracker" -> {
                if (navController.currentDestination?.route != Screen.Tracker.route) {
                    navController.navigate(Screen.Tracker.route) { 
                        popUpTo(Screen.Landing.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            "viewer" -> {
                if (navController.currentDestination?.route != Screen.Viewer.route) {
                    navController.navigate(Screen.Viewer.route) { 
                        popUpTo(Screen.Landing.route) { inclusive = true } 
                        launchSingleTop = true
                    }
                }
            }
            null -> {
                if (sessionState.isSystemActive) return@LaunchedEffect
                if (navigationState.pendingMode == null) viewModel.onEvent(UiEvent.SetManualSelection(false))
                if (navController.currentDestination?.route != Screen.Landing.route) {
                    navController.navigate(Screen.Landing.route) { 
                        popUpTo(Screen.Landing.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    if (sessionState.hydrationLevel == 0) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val configuration = LocalConfiguration.current
    val view = LocalView.current
    val window = activity.window

     LaunchedEffect(configuration.orientation) {
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    if (showBackgroundDisclosure) {
        AlertDialog(
            onDismissRequest = { 
                showBackgroundDisclosure = false
                viewModel.onEvent(UiEvent.ConfirmBackgroundDisclosure(false, navigationState.pendingMode ?: ""))
            },
            title = { Text(stringResource(R.string.perm_background_title)) },
            text = { Text(stringResource(R.string.perm_background_desc)) },
            confirmButton = {
                Button(onClick = {
                    showBackgroundDisclosure = false
                    viewModel.onEvent(UiEvent.ConfirmBackgroundDisclosure(true, navigationState.pendingMode ?: ""))
                }) { Text(stringResource(R.string.perm_background_btn_accept)) }
            },
            dismissButton = { 
                Button(onClick = { 
                    showBackgroundDisclosure = false
                    viewModel.onEvent(UiEvent.ConfirmBackgroundDisclosure(false, navigationState.pendingMode ?: ""))
                }) { Text(stringResource(R.string.perm_background_btn_reject)) } 
            }
        )
    }

    if (settingsState.isIdentitySanitized) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(UiEvent.DismissIdentitySanitization) },
            title = { Text(stringResource(R.string.sanitization_title)) },
            text = { Text(stringResource(R.string.sanitization_desc)) },
            confirmButton = { Button(onClick = { viewModel.onEvent(UiEvent.DismissIdentitySanitization) }) { Text(stringResource(R.string.btn_dismiss)) } }
        )
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { MainFileHelper.importConfig(activity, viewModel, uri) } }
    val importTrailLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> MainFileHelper.importTrails(activity, viewModel, uris) }
    
    GpsTrackerTheme(appMode = sessionState.appMode) {
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
            BackHandler(enabled = hudHealth.isRedScreenVisible && sessionState.appMode != null) { viewModel.onEvent(UiEvent.DismissAlarms) }

            Box(modifier = Modifier.fillMaxSize()) {
                if (sessionState.hydrationLevel >= 2) {
                    NavHost(navController = navController, startDestination = Screen.Landing.route) {
                        composable(Screen.Landing.route) {
                            BackHandler { viewModel.onEvent(UiEvent.ManualExit) }
                            if (sessionState.hydrationLevel >= 3) {
                                LandingScreen(isPeerActive = sessionState.isPeerActive) { mode ->
                                    viewModel.onEvent(UiEvent.InitiateMode(mode))
                                }
                            }
                        }
                        composable(Screen.Tracker.route) {
                            BackHandler {
                                val nav = navigationState
                                when {
                                    nav.isDiagnosticsVisible -> viewModel.onEvent(UiEvent.NavigateToDiagnostics(false))
                                    nav.isPhoneSetupVisible -> viewModel.onEvent(UiEvent.TogglePhoneSetup(false))
                                    nav.activeSubSettings != null -> viewModel.onEvent(UiEvent.SetSubSettings(null))
                                    nav.isSettingsOpen -> { viewModel.onEvent(UiEvent.CommitSettings); viewModel.onEvent(UiEvent.ToggleSettings(false)) }
                                    nav.isLogVisible -> viewModel.onEvent(UiEvent.ToggleLog(false))
                                    nav.isRibbonsVisible -> viewModel.onEvent(UiEvent.ToggleRibbons(false))
                                    !nav.isMapVisible -> viewModel.onEvent(UiEvent.ToggleMap(true))
                                    else -> viewModel.onEvent(UiEvent.ManualExit)
                                }
                            }
                            if (sessionState.hydrationLevel >= 3) {
                                TrackerScreen(
                                    sessionState = sessionState, settingsState = settingsState, spatialState = spatialState, navigationState = navigationState,
                                    viewModel = viewModel, logsFlow = viewModel.eventLogsFlow,
                                    onToggleMap = { viewModel.onEvent(UiEvent.ToggleMap(!navigationState.isMapVisible)) }, 
                                    onToggleLog = { viewModel.onEvent(UiEvent.ToggleLog(!navigationState.isLogVisible)) }, 
                                    onToggleSettings = { viewModel.onEvent(UiEvent.ToggleSettings(!navigationState.isSettingsOpen)) },
                                    onExit = { viewModel.onEvent(UiEvent.ManualExit) }, onMainEvent = { viewModel.onEvent(it) }, onFullInitialization = { viewModel.fullInitialization(context) },
                                    onResetStats = { viewModel.onEvent(UiEvent.ResetStats) }, onExportLogs = { MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider) }, 
                                    onImportConfig = { importLauncher.launch("application/json") }, onClearLogs = { viewModel.onEvent(UiEvent.ClearLogs) }, onClearHome = { viewModel.onEvent(UiEvent.ClearHomePoints) },
                                    onSaveTrail = { MainFileHelper.manualExportTrails(activity, viewModel, viewModel.timeProvider) }, onLoadTrail = { importTrailLauncher.launch("application/json") }
                                )
                            }
                        }
                        composable(Screen.Viewer.route) {
                            BackHandler {
                                val nav = navigationState
                                when {
                                    nav.isDiagnosticsVisible -> viewModel.onEvent(UiEvent.NavigateToDiagnostics(false))
                                    nav.isPhoneSetupVisible -> viewModel.onEvent(UiEvent.TogglePhoneSetup(false))
                                    nav.activeSubSettings != null -> viewModel.onEvent(UiEvent.SetSubSettings(null))
                                    nav.isSettingsOpen -> { viewModel.onEvent(UiEvent.CommitSettings); viewModel.onEvent(UiEvent.ToggleSettings(false)) }
                                    nav.isLogVisible -> viewModel.onEvent(UiEvent.ToggleLog(false))
                                    nav.isRibbonsVisible -> viewModel.onEvent(UiEvent.ToggleRibbons(false))
                                    !nav.isMapVisible -> viewModel.onEvent(UiEvent.ToggleMap(true))
                                    else -> viewModel.onEvent(UiEvent.ManualExit)
                                }
                            }
                            if (sessionState.hydrationLevel >= 3) {
                                ViewerScreen(
                                    sessionState = sessionState, settingsState = settingsState, spatialState = spatialState, navigationState = navigationState,
                                    viewModel = viewModel, logsFlow = viewModel.eventLogsFlow,
                                    onToggleMap = { viewModel.onEvent(UiEvent.ToggleMap(!navigationState.isMapVisible)) }, 
                                    onToggleLog = { viewModel.onEvent(UiEvent.ToggleLog(!navigationState.isLogVisible)) },
                                    onToggleSettings = { viewModel.onEvent(UiEvent.ToggleSettings(!navigationState.isSettingsOpen)) },
                                    onExit = { viewModel.onEvent(UiEvent.ManualExit) }, onMainEvent = { viewModel.onEvent(it) }, onFullInitialization = { viewModel.fullInitialization(context) },
                                    onImportConfig = { importLauncher.launch("application/json") }, onExportLogs = { MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider) },
                                    onClearLogs = { viewModel.onEvent(UiEvent.ClearLogs) }, onResetStats = { viewModel.onEvent(UiEvent.ResetStats) }, onClearHome = { viewModel.onEvent(UiEvent.ClearHomePoints) },
                                    onSaveTrail = { MainFileHelper.manualExportTrails(activity, viewModel, viewModel.timeProvider) }, onLoadTrail = { importTrailLauncher.launch("application/json") }
                                )
                            }
                        }
                        composable(Screen.Diagnostics.route) {
                            val simulationState by viewModel.simulationUiState.collectAsStateWithLifecycle()
                            val diagnosticState by viewModel.diagnosticState.collectAsStateWithLifecycle()
                            BackHandler { viewModel.onEvent(UiEvent.NavigateToDiagnostics(false)) }
                            if (sessionState.hydrationLevel >= 3) {
                                DiagnosticsScreen(
                                    permissions = sessionState.permissions,
                                    recoveryCount = diagnosticState.recoveryCount,
                                    cumulativeRecoveryBlackoutMs = diagnosticState.cumulativeRecoveryBlackoutMs,
                                    isForensicStallSimulated = simulationState.isForensicStallSimulated,
                                    isStorageSimulated = simulationState.isStorageSimulated,
                                    isStorageCriticalSimulated = simulationState.isStorageCriticalSimulated,
                                    isSetupBypassActive = sessionState.isSetupBypassActive,
                                    onBack = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(false)) },
                                    onRefresh = { viewModel.onEvent(UiEvent.RefreshPermissionStatus) },
                                    onToggleManualOverride = { viewModel.onEvent(UiEvent.ToggleXiaomiManualOverride) },
                                    onToggleForensicSimulation = { active -> viewModel.onEvent(UiEvent.SetForensicSimulation(active)) },
                                    onToggleStorageSimulation = { active, critical -> viewModel.onEvent(UiEvent.SetStorageSimulation(active, critical)) },
                                    onToggleSetupBypass = { active -> viewModel.onEvent(UiEvent.ToggleSetupBypass(active)) },
                                    onExecuteStressTest = { viewModel.onEvent(UiEvent.ExecuteStressTest) },
                                    onRequestBatteryExemption = onRequestBatteryExemption,
                                    onRequestOverlayPermission = onRequestOverlayPermission,
                                    onRequestAppInfo = onRequestAppInfo,
                                    onRequestExactAlarm = onRequestExactAlarm,
                                    onRequestHardwarePermission = onRequestHardwarePermission
                                )
                            }
                        }
                    }
                }
                
                if (navigationState.isPhoneSetupVisible && sessionState.hydrationLevel >= 3) {
                    PhoneSetupOverlay(
                        onClose = { viewModel.onEvent(UiEvent.TogglePhoneSetup(false)) }, onWhitelist = { onRequestBatteryExemption() },
                        onOverlay = { onRequestOverlayPermission() }, onAppInfo = { onRequestAppInfo() },
                        onExactAlarm = { onRequestExactAlarm() }, onHardwarePermission = { onRequestHardwarePermission() },
                        onRefresh = { viewModel.onEvent(UiEvent.RefreshPermissionStatus) }, onToggleManualOverride = { viewModel.onEvent(UiEvent.ToggleXiaomiManualOverride) },
                        onTestAlarm = { viewModel.onEvent(UiEvent.RequestTestAlarm) },
                        onNavigateToDiagnostics = { viewModel.onEvent(UiEvent.TogglePhoneSetup(false)); viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) },
                        isSetupBypassActive = sessionState.isSetupBypassActive, permissions = sessionState.permissions, homePointsCount = spatialState.homePoints.size,
                        isTrackerMode = sessionState.appMode == "tracker", onGoToMap = { viewModel.onEvent(UiEvent.TogglePhoneSetup(false)); viewModel.onEvent(UiEvent.ToggleMap(true)) }
                    )
                }

                if (navigationState.isStopTrackingConfirmationVisible && sessionState.hydrationLevel >= 3) {
                    var timeLeft by remember { mutableStateOf(5) }
                    LaunchedEffect(Unit) { while (timeLeft > 0) { delay(1000); timeLeft-- }; viewModel.onEvent(UiEvent.ShowStopTrackingConfirmation(false)) }
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(UiEvent.ShowStopTrackingConfirmation(false)) },
                        title = { Text(stringResource(R.string.stop_tracking_title)) },
                        text = { Text(stringResource(R.string.stop_tracking_desc, timeLeft)) },
                        confirmButton = { Button(onClick = { viewModel.onEvent(UiEvent.ConfirmStopTracking) }) { Text(stringResource(R.string.btn_stop_tracking)) } },
                        dismissButton = { Button(onClick = { viewModel.onEvent(UiEvent.ShowStopTrackingConfirmation(false)) }) { Text(stringResource(R.string.btn_cancel)) } }
                    )
                }

                // Centralized OverlayHost for Shared Overlays
                OverlayHost(
                    viewModel = viewModel,
                    navigationState = navigationState,
                    settingsState = settingsState,
                    sessionState = sessionState,
                    importLauncher = importLauncher,
                    activity = activity
                )
                
                if (hudHealth.isRedScreenVisible && sessionState.appMode == "viewer" && sessionState.hydrationLevel >= 3) {
                    // R1328: AlarmOverlay now binds to hudHealth (granular) and kinematicState (internal).
                    val kinematicState by viewModel.kinematicState.collectAsStateWithLifecycle()
                    AlarmOverlay(
                        alarms = hudHealth.activeAlarms, isMuted = hudHealth.isAlarmSilenced,
                        locatable = kinematicState.trackerHealth,
                        backgroundStatus = sessionState.permissions.backgroundStatus, hasBackgroundRestriction = sessionState.permissions.hasBackgroundRestriction,
                        onHardwarePermissionClick = { onRequestHardwarePermission() },
                        onMute = { 
                            val currentCauses = hudHealth.activeAlarms.filter { !it.isResolved }.joinToString { it.title }.ifBlank { context.getString(R.string.status_muted) }
                            viewModel.onEvent(UiEvent.StopSiren(currentCauses))
                        },
                        onClose = { viewModel.onEvent(UiEvent.DismissAlarms) },
                        onGoToMap = { viewModel.onEvent(UiEvent.DismissAlarms); viewModel.onEvent(UiEvent.ToggleMap(true)) }
                    )
                }
            }
        }
    }
}

@Composable
fun OverlayHost(
    viewModel: MainViewModel,
    navigationState: NavigationState,
    settingsState: SettingsUiState,
    sessionState: SessionUiState,
    importLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    activity: ComponentActivity
) {
    if (navigationState.isSettingsOpen) {
        val diagnosticState by viewModel.diagnosticState.collectAsStateWithLifecycle()
        SettingsOverlay(
            activeSubSettings = navigationState.activeSubSettings,
            draftDeviceId = settingsState.draftSettings.deviceId,
            draftViewerId = settingsState.draftSettings.viewerId,
            draftRelayUrl = settingsState.draftSettings.relayUrl,
            draftMaxDistance = settingsState.draftSettings.maxDistance,
            draftAlertSettings = settingsState.draftSettings.alertSettings,
            selectedSirenType = settingsState.selectedSirenType,
            isSirenPlaying = diagnosticState.isSirenPlaying,
            onClose = { viewModel.onEvent(UiEvent.ToggleSettings(false)) }, 
            onReset = { viewModel.onEvent(UiEvent.ResetStats) },
            onExport = { MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider) }, 
            onClear = { viewModel.onEvent(UiEvent.ClearHomePoints) }, 
            onImportConfig = { importLauncher.launch("application/json") },
            onFullInitialization = { viewModel.fullInitialization(activity) },
            onUpdateDeviceId = { id -> viewModel.onEvent(UiEvent.UpdateDraftDeviceId(id)) },
            onUpdateViewerId = { id -> viewModel.onEvent(UiEvent.UpdateDraftViewerId(id)) },
            onUpdateRelayUrl = { url -> viewModel.onEvent(UiEvent.UpdateDraftRelayUrl(url)) },
            onUpdateMaxDistance = { dist -> viewModel.onEvent(UiEvent.UpdateDraftMaxDistance(dist)) },
            onUpdateAlertSettings = { settings -> viewModel.onEvent(UiEvent.UpdateDraftAlertSettings(settings)) },
            onUpdateSirenType = { type -> viewModel.onEvent(UiEvent.SetSirenType(type)) },
            onUpdateAlarmVolume = { vol -> viewModel.onEvent(UiEvent.UpdateDraftAlarmVolume(vol)) },
            onTestSiren = { viewModel.onEvent(UiEvent.ToggleTestSiren) },
            onShowPhoneSetup = { 
                viewModel.onEvent(UiEvent.ToggleSettings(false))
                viewModel.onEvent(UiEvent.TogglePhoneSetup(true))
            },
            onEvent = { event -> viewModel.onEvent(event) }
        )
    } else if (navigationState.isLogVisible) {
        val showDetails by viewModel.repository.logFilterDetails.collectAsStateWithLifecycle()
        val showRecovered by viewModel.repository.logFilterRecovered.collectAsStateWithLifecycle()
        LogOverlay(
            logsFlow = viewModel.eventLogsFlow, 
            onExport = { MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider) }, 
            onToggle = { viewModel.onEvent(UiEvent.ToggleLog(false)) }, 
            onClear = { viewModel.onEvent(UiEvent.ClearLogs) },
            showDetails = showDetails, 
            showRecovered = showRecovered, 
            onSetShowDetails = { show -> viewModel.onEvent(UiEvent.SetLogFilterShowDetails(show)) }, 
            onSetShowRecovered = { show -> viewModel.onEvent(UiEvent.SetLogFilterShowRecovered(show)) },
            appStartTime = sessionState.appStartTime,
            systemPulse = viewModel.timeProvider.currentTimeMillis(),
            isTelemetryFresh = true,
            onHistLink = { ts -> 
                viewModel.onEvent(UiEvent.SetReplayCursor(ts))
                viewModel.onEvent(UiEvent.ToggleRibbons(true))
            },
            onDetailsLink = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) }
        )
    } else if (navigationState.isRibbonsVisible) {
        RibbonsOverlay(
            isStrictMode = navigationState.isStrictMode,
            replayCursorTs = navigationState.replayCursorTs,
            history4MFlow = viewModel.history4MFlow,
            history16MFlow = viewModel.history16MFlow,
            history1HFlow = viewModel.history1HFlow,
            history4HFlow = viewModel.history4HFlow,
            history24HFlow = viewModel.history24HFlow,
            history7DFlow = viewModel.history7DFlow,
            onToggleStrictMode = { strict -> viewModel.onEvent(UiEvent.ToggleStrictMode(strict)) },
            onScrub = { ts -> viewModel.onEvent(UiEvent.SetReplayCursor(ts)) },
            onDismiss = { viewModel.onEvent(UiEvent.ToggleRibbons(false)) }
        )
    } else if (navigationState.isGnssDetailVisible) {
        GnssDetailOverlay(
            gnssDetailFlow = viewModel.activeGnssDetail,
            onClose = { viewModel.onEvent(UiEvent.ToggleGnssDetail(false)) }
        )
    }
}
