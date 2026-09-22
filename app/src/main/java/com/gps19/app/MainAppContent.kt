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
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * MainAppContent: The top-level Composable for the application.
 * Sep.22.08:
 * - Issue #1166: State Partitioning & Slicing. Refactored to consume specialized 
 *   UI state slices (Session, Settings, Spatial, Navigation) to minimize 
 *   recomposition evaluation costs (R-ID 405).
 * Sep.22.00:
 * - Issue #1177: Static Role Branding. Passed isPeerActive from uiState 
 *   to LandingScreen to drive dynamic role indicators (R-ID 398).
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
    val sessionState by viewModel.sessionUiState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val spatialState by viewModel.spatialUiState.collectAsStateWithLifecycle()
    val navigationState by viewModel.navigationState.collectAsStateWithLifecycle()
    val simulationState by viewModel.simulationUiState.collectAsStateWithLifecycle()
    
    val kinematicState by viewModel.kinematicState.collectAsStateWithLifecycle()
    val diagnosticState by viewModel.diagnosticState.collectAsStateWithLifecycle()
    
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
    val startupTime = remember { System.currentTimeMillis() }

    // Logic Helper for System Readiness (R-ID 405)
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

    fun proceedToMode(mode: String) {
        viewModel.onEvent(UiEvent.SetManualSelection(true))
        viewModel.onEvent(UiEvent.SetSettlingActive(false))
        viewModel.onEvent(UiEvent.SetAppMode(mode))
        viewModel.onEvent(UiEvent.SetSystemActive(true))
        
        val elapsed = System.currentTimeMillis() - startupTime
        if (elapsed < STARTUP_SETTLING_DELAY_MS) {
            val remaining = STARTUP_SETTLING_DELAY_MS - elapsed
            scope.launch {
                delay(remaining)
                onStartService(mode)
            }
        } else {
            onStartService(mode)
        }
        
        if (!isSystemReady) {
            viewModel.onEvent(UiEvent.TogglePhoneSetup(true))
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            navigationState.pendingMode?.let { mode ->
                proceedToMode(mode)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !sessionState.permissions.isBackgroundLocationGranted) {
                showBackgroundDisclosure = true
            } else {
                proceedToMode(mode)
                viewModel.onEvent(UiEvent.SetPendingMode(null))
            }
        }
    }

    fun checkAndRequestPermissions(mode: String) {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (mode == "tracker") {
            permissions.add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        viewModel.onEvent(UiEvent.SetPendingMode(mode))
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    fun hasRequiredPermissions(mode: String): Boolean {
        val fineLocation = sessionState.permissions.isFineLocationGranted
        val audio = if (mode == "tracker") sessionState.permissions.isMicrophoneGranted else true
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) sessionState.permissions.isPostNotificationsGranted else true
        val activityRec = if (mode == "tracker" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sessionState.permissions.isActivityRecognitionGranted
        } else true
        return fineLocation && audio && notification && activityRec
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
                
                if (hasRequiredPermissions(mode)) {
                    viewModel.onEvent(UiEvent.SetSystemActive(true))
                    onStartService(mode)
                } else {
                    checkAndRequestPermissions(mode)
                }
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
            onDismissRequest = { showBackgroundDisclosure = false; viewModel.onEvent(UiEvent.SetManualSelection(false)) },
            title = { Text(stringResource(R.string.perm_background_title)) },
            text = { Text(stringResource(R.string.perm_background_desc)) },
            confirmButton = {
                Button(onClick = {
                    showBackgroundDisclosure = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }) { Text(stringResource(R.string.perm_background_btn_accept)) }
            },
            dismissButton = { 
                Button(onClick = { 
                    showBackgroundDisclosure = false
                    viewModel.onEvent(UiEvent.SetManualSelection(false))
                    navigationState.pendingMode?.let { proceedToMode(it) }
                    viewModel.onEvent(UiEvent.SetPendingMode(null))
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
            BackHandler(enabled = diagnosticState.isRedScreenVisible && sessionState.appMode != null) { viewModel.onEvent(UiEvent.DismissAlarms) }

            Box(modifier = Modifier.fillMaxSize()) {
                if (sessionState.hydrationLevel >= 2) {
                    NavHost(navController = navController, startDestination = Screen.Landing.route) {
                        composable(Screen.Landing.route) {
                            BackHandler { onCleanupAndExit() }
                            if (sessionState.hydrationLevel >= 3) {
                                LandingScreen(isPeerActive = sessionState.isPeerActive) { mode ->
                                    if (hasRequiredPermissions(mode)) { 
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !sessionState.permissions.isBackgroundLocationGranted) {
                                            viewModel.onEvent(UiEvent.SetPendingMode(mode)); showBackgroundDisclosure = true
                                        } else proceedToMode(mode)
                                    } else checkAndRequestPermissions(mode)
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
                                    else -> onCleanupAndExit()
                                }
                            }
                            if (sessionState.hydrationLevel >= 3) {
                                TrackerScreen(
                                    sessionState = sessionState, settingsState = settingsState, spatialState = spatialState, navigationState = navigationState,
                                    kinematicState = kinematicState, diagnosticState = diagnosticState, viewModel = viewModel, logsFlow = viewModel.eventLogsFlow,
                                    onToggleMap = { viewModel.onEvent(UiEvent.ToggleMap(!navigationState.isMapVisible)) }, 
                                    onToggleLog = { viewModel.onEvent(UiEvent.ToggleLog(!navigationState.isLogVisible)) }, 
                                    onToggleSettings = { viewModel.onEvent(UiEvent.ToggleSettings(!navigationState.isSettingsOpen)) },
                                    onExit = onCleanupAndExit,
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
                                    else -> onCleanupAndExit()
                                }
                            }
                            if (sessionState.hydrationLevel >= 3) {
                                ViewerScreen(
                                    sessionState = sessionState, settingsState = settingsState, spatialState = spatialState, navigationState = navigationState,
                                    kinematicState = kinematicState, diagnosticState = diagnosticState, viewModel = viewModel, logsFlow = viewModel.eventLogsFlow, 
                                    onToggleMap = { viewModel.onEvent(UiEvent.ToggleMap(!navigationState.isMapVisible)) }, 
                                    onToggleLog = { viewModel.onEvent(UiEvent.ToggleLog(!navigationState.isLogVisible)) },
                                    onToggleSettings = { viewModel.onEvent(UiEvent.ToggleSettings(!navigationState.isSettingsOpen)) },
                                    onExit = onCleanupAndExit,
                                    onImportConfig = { importLauncher.launch("application/json") }, onExportLogs = { MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider) },
                                    onClearLogs = { viewModel.onEvent(UiEvent.ClearLogs) }, onResetStats = { viewModel.onEvent(UiEvent.ResetStats) }, onClearHome = { viewModel.onEvent(UiEvent.ClearHomePoints) },
                                    onSaveTrail = { MainFileHelper.manualExportTrails(activity, viewModel, viewModel.timeProvider) }, onLoadTrail = { importTrailLauncher.launch("application/json") }
                                )
                            }
                        }
                        composable(Screen.Diagnostics.route) {
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

                if (diagnosticState.isRedScreenVisible && sessionState.appMode != null && sessionState.hydrationLevel >= 3) {
                    AlarmOverlay(
                        alarms = diagnosticState.activeAlarms, isMuted = diagnosticState.isAlarmSilenced,
                        isLocationPending = kinematicState.trackerHealth.isLocationPending,
                        backgroundStatus = sessionState.permissions.backgroundStatus, hasBackgroundRestriction = sessionState.permissions.hasBackgroundRestriction,
                        onHardwarePermissionClick = { onRequestHardwarePermission() },
                        onMute = { 
                            val currentCauses = diagnosticState.activeAlarms.filter { !it.isResolved }.joinToString { it.title }.ifBlank { context.getString(R.string.status_muted) }
                            viewModel.onEvent(UiEvent.StopSiren(currentCauses))
                        },
                        onClose = { viewModel.onEvent(UiEvent.DismissAlarms) },
                        onGoToMap = { viewModel.onEvent(UiEvent.DismissAlarms); viewModel.onEvent(UiEvent.ToggleMap(true)) }
                    )
                }

                if (navigationState.isStopTrackingConfirmationVisible && sessionState.hydrationLevel >= 3) {
                    var timeLeft by remember { mutableStateOf(5) }
                    LaunchedEffect(Unit) { while (timeLeft > 0) { delay(1000); timeLeft-- }; viewModel.onEvent(UiEvent.ShowStopTrackingConfirmation(false)) }
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(UiEvent.ShowStopTrackingConfirmation(false)) },
                        title = { Text(stringResource(R.string.stop_tracking_title)) },
                        text = { Text(stringResource(R.string.stop_tracking_desc, timeLeft)) },
                        confirmButton = { Button(onClick = { viewModel.onEvent(UiEvent.ConfirmStopTracking); onStopTracking() }) { Text(stringResource(R.string.btn_stop_tracking)) } },
                        dismissButton = { Button(onClick = { viewModel.onEvent(UiEvent.ShowStopTrackingConfirmation(false)) }) { Text(stringResource(R.string.btn_cancel)) } }
                    )
                }
            }
        }
    }
}
