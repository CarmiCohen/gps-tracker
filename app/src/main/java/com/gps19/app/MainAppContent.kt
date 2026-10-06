package com.gps19.app

import android.app.Activity
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gps19.core.engine.STARTUP_SETTLING_DELAY_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * MainAppContent: Root UI composition.
 * Oct.6.1:
 * - AUDIT-1006-4: Fixed compilation errors in TrackerScreen and ViewerScreen 
 *   call sites. Finalized signature alignment with UiStateProvider pattern.
 * Oct.5.20:
 * - SIMP-1426-3: Refactored leaf components to consume unified UiStateProvider.
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
    val sessionState by viewModel.session.collectAsStateWithLifecycle()
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    val spatialState by viewModel.spatial.collectAsStateWithLifecycle()
    val navigationState by viewModel.navigation.collectAsStateWithLifecycle()
    
    val navController = rememberNavController()
    var showBackgroundDisclosure by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onEvent(UiEvent.RefreshPermissionStatus)
        navigationState.pendingMode?.let { mode -> viewModel.onEvent(UiEvent.InitiateMode(mode)) }
    }

    AppEffectAggregator(
        viewModel = viewModel,
        navController = navController,
        sessionState = sessionState,
        navigationState = navigationState,
        spatialState = spatialState,
        activity = activity,
        onStartService = onStartService,
        onCleanupAndExit = onCleanupAndExit,
        onStopTracking = onStopTracking,
        requestPermissionLauncher = requestPermissionLauncher,
        onShowBackgroundDisclosure = { showBackgroundDisclosure = true }
    )

    if (sessionState.hydrationLevel == 0) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
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
                                    activity = activity,
                                    viewModel = viewModel
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
                                    stateProvider = viewModel,
                                    onToggleMap = { viewModel.onEvent(UiEvent.ToggleMap(!navigationState.isMapVisible)) }, 
                                    onToggleLog = { viewModel.onEvent(UiEvent.ToggleLog(!navigationState.isLogVisible)) },
                                    onToggleSettings = { viewModel.onEvent(UiEvent.ToggleSettings(!navigationState.isSettingsOpen)) },
                                    onExit = { viewModel.onEvent(UiEvent.ManualExit) }, 
                                    onMainEvent = { viewModel.onEvent(it) }, 
                                    onFullInitialization = { viewModel.fullInitialization(activity) },
                                    onImportConfig = { importLauncher.launch("application/json") }, 
                                    onExportLogs = { MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider) },
                                    onClearLogs = { viewModel.onEvent(UiEvent.ClearLogs) }, 
                                    onResetStats = { viewModel.onEvent(UiEvent.ResetStats) }, 
                                    onClearHome = { viewModel.onEvent(UiEvent.ClearHomePoints) },
                                    onSaveTrail = { MainFileHelper.manualExportTrails(activity, viewModel, viewModel.timeProvider) }, 
                                    onLoadTrail = { importTrailLauncher.launch("application/json") }
                                )
                            }
                        }
                        composable(Screen.Diagnostics.route) {
                            BackHandler { viewModel.onEvent(UiEvent.NavigateToDiagnostics(false)) }
                            if (sessionState.hydrationLevel >= 3) {
                                DiagnosticsScreen(
                                    stateProvider = viewModel,
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
                        stateProvider = viewModel,
                        onClose = { viewModel.onEvent(UiEvent.TogglePhoneSetup(false)) }, 
                        onWhitelist = { onRequestBatteryExemption() },
                        onOverlay = { onRequestOverlayPermission() }, 
                        onAppInfo = { onRequestAppInfo() },
                        onExactAlarm = { onRequestExactAlarm() }, 
                        onHardwarePermission = { onRequestHardwarePermission() },
                        onRefresh = { viewModel.onEvent(UiEvent.RefreshPermissionStatus) }, 
                        onToggleManualOverride = { viewModel.onEvent(UiEvent.ToggleXiaomiManualOverride) },
                        onTestAlarm = { viewModel.onEvent(UiEvent.RequestTestAlarm) },
                        onNavigateToDiagnostics = { viewModel.onEvent(UiEvent.TogglePhoneSetup(false)); viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) },
                        onGoToMap = { viewModel.onEvent(UiEvent.TogglePhoneSetup(false)); viewModel.onEvent(UiEvent.ToggleMap(true)) }
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

                OverlayHost(
                    viewModel = viewModel,
                    navigationState = navigationState,
                    importLauncher = importLauncher,
                    activity = activity
                )
            }
        }
    }
}

/**
 * AppEffectAggregator: Centralized observer for root-level side-effects.
 * Issue #1426: Consolidates Lifecycle, UI Effects, Navigation mapping, and Orientation logic.
 */
@Composable
private fun AppEffectAggregator(
    viewModel: MainViewModel,
    navController: NavHostController,
    sessionState: SessionUiState,
    navigationState: NavigationState,
    spatialState: SpatialUiState,
    activity: Activity,
    onStartService: (String) -> Unit,
    onCleanupAndExit: () -> Unit,
    onStopTracking: () -> Unit,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>,
    onShowBackgroundDisclosure: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    // 1. Lifecycle Visibility Tracking
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

    // 2. Global UI Effect Routing
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
                is UiEffect.ShowBackgroundDisclosure -> onShowBackgroundDisclosure()
            }
        }
    }

    // 3. Automated Navigation & Mode Transitions
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

    // 4. Orientation-Driven System Bar Logic
    LaunchedEffect(configuration.orientation) {
        val window = activity.window
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun OverlayHost(
    viewModel: MainViewModel,
    navigationState: NavigationState,
    importLauncher: ActivityResultLauncher<String>,
    activity: ComponentActivity
) {
    val hudHealth by viewModel.hudHealthState.collectAsStateWithLifecycle()
    val sessionState by viewModel.session.collectAsStateWithLifecycle()

    if (navigationState.isSettingsOpen) {
        SettingsOverlay(
            stateProvider = viewModel,
            onReset = { viewModel.onEvent(UiEvent.ResetStats) },
            onExport = { MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider) }, 
            onClear = { viewModel.onEvent(UiEvent.ClearHomePoints) }, 
            onImportConfig = { importLauncher.launch("application/json") },
            onFullInitialization = { viewModel.fullInitialization(activity) },
            onShowPhoneSetup = { 
                viewModel.onEvent(UiEvent.ToggleSettings(false))
                viewModel.onEvent(UiEvent.TogglePhoneSetup(true))
            },
            onEvent = { event -> viewModel.onEvent(event) }
        )
    } else if (navigationState.isLogVisible) {
        LogOverlay(
            stateProvider = viewModel,
            onExport = { MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider) }, 
            onToggle = { viewModel.onEvent(UiEvent.ToggleLog(false)) }, 
            onClear = { viewModel.onEvent(UiEvent.ClearLogs) },
            onSetShowDetails = { show -> viewModel.onEvent(UiEvent.SetLogFilterShowDetails(show)) }, 
            onSetShowRecovered = { show -> viewModel.onEvent(UiEvent.SetLogFilterShowRecovered(show)) },
            onHistLink = { ts -> 
                viewModel.onEvent(UiEvent.SetReplayCursor(ts))
                viewModel.onEvent(UiEvent.ToggleRibbons(true))
            },
            onDetailsLink = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) }
        )
    } else if (navigationState.isRibbonsVisible) {
        RibbonsOverlay(
            stateProvider = viewModel,
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

    // Leaf-level AlarmOverlay collection
    if (hudHealth.isRedScreenVisible && sessionState.appMode == "viewer" && sessionState.hydrationLevel >= 3) {
        BackHandler { viewModel.onEvent(UiEvent.DismissAlarms) }
        AlarmOverlay(
            stateProvider = viewModel,
            onHardwarePermissionClick = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) },
            onMute = { 
                val currentCauses = hudHealth.activeAlarms.filter { !it.isResolved }.joinToString { it.title }.ifBlank { activity.getString(R.string.status_muted) }
                viewModel.onEvent(UiEvent.StopSiren(currentCauses))
            },
            onClose = { viewModel.onEvent(UiEvent.DismissAlarms) },
            onGoToMap = { viewModel.onEvent(UiEvent.DismissAlarms); viewModel.onEvent(UiEvent.ToggleMap(true)) }
        )
    }
}
