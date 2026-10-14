package com.gps19.app

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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
 * Oct.13.30:
 * - SIMP-OCT13.21-S2: Finalized Pure Event transition. Pruned all lambda 
 *   injection parameters. Centralized Activity-level orchestration in 
 *   AppEffectAggregator via UiEffect.
 */
@Composable
fun MainAppContent(
    activity: MainActivity,
    viewModel: MainViewModel
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

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { MainFileHelper.importConfig(activity, viewModel, uri) } }
    val importTrailLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> MainFileHelper.importTrails(activity, viewModel, uris) }

    AppEffectAggregator(
        viewModel = viewModel,
        navController = navController,
        sessionState = sessionState,
        navigationState = navigationState,
        spatialState = spatialState,
        activity = activity,
        requestPermissionLauncher = requestPermissionLauncher,
        importLauncher = importLauncher,
        importTrailLauncher = importTrailLauncher,
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
                                    stateProvider = viewModel,
                                    onMainEvent = { viewModel.onEvent(it) }
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
                                    onMainEvent = { viewModel.onEvent(it) }
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
                                    onExecuteLogPressureTest = { viewModel.onEvent(UiEvent.ExecuteLogPressureTest) },
                                    onRequestBatteryExemption = { viewModel.onEvent(UiEvent.RequestBatteryExemption) },
                                    onRequestOverlayPermission = { viewModel.onEvent(UiEvent.RequestOverlayPermission) },
                                    onRequestAppInfo = { viewModel.onEvent(UiEvent.RequestAppInfo) },
                                    onRequestExactAlarm = { viewModel.onEvent(UiEvent.RequestExactAlarm) },
                                    onRequestHardwarePermission = { viewModel.onEvent(UiEvent.RequestHardwarePermission) }
                                )
                            }
                        }
                    }
                }
                
                OverlayHost(
                    viewModel = viewModel,
                    navigationState = navigationState
                )
            }
        }
    }
}

@Composable
private fun AppEffectAggregator(
    viewModel: MainViewModel,
    navController: NavHostController,
    sessionState: SessionUiState,
    navigationState: NavigationState,
    spatialState: SpatialUiState,
    activity: MainActivity,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>,
    importLauncher: ActivityResultLauncher<String>,
    importTrailLauncher: ActivityResultLauncher<String>,
    onShowBackgroundDisclosure: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

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

    LaunchedEffect(Unit) {
        viewModel.uiEffects.collectLatest { effect ->
            when (effect) {
                is UiEffect.Navigate -> {
                    navController.navigate(effect.route) {
                        effect.popUpTo?.let { popUpTo(it) { inclusive = effect.inclusive } }
                        launchSingleTop = true
                    }
                }
                is UiEffect.StartService -> activity.startTrackingService()
                UiEffect.CleanupAndExit -> activity.cleanupAndExit()
                UiEffect.StopTracking -> activity.stopTrackingService()
                is UiEffect.RequestPermissions -> requestPermissionLauncher.launch(effect.permissions.toTypedArray())
                is UiEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is UiEffect.ShowBackgroundDisclosure -> onShowBackgroundDisclosure()
                
                UiEffect.LaunchTrailExport -> MainFileHelper.manualExportTrails(activity, viewModel, viewModel.timeProvider)
                UiEffect.LaunchTrailImport -> importTrailLauncher.launch("application/json")
                UiEffect.LaunchLogExport -> MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider)
                UiEffect.LaunchConfigImport -> importLauncher.launch("application/json")
                UiEffect.LaunchFullInitialization -> viewModel.fullInitialization(activity)
                
                UiEffect.LaunchBatteryExemption -> activity.launchBatteryExemptionSetting()
                UiEffect.LaunchOverlayPermission -> activity.launchOverlayPermissionSetting()
                UiEffect.LaunchAppInfo -> activity.launchAppInfo()
                UiEffect.LaunchExactAlarm -> activity.launchExactAlarmSetting()
                UiEffect.LaunchHardwarePermission -> activity.launchOverlayPermissionSetting()
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
    navigationState: NavigationState
) {
    val hudHealth by viewModel.hudHealthState.collectAsStateWithLifecycle()
    val sessionState by viewModel.session.collectAsStateWithLifecycle()

    if (navigationState.isSettingsOpen) {
        SettingsOverlay(
            stateProvider = viewModel,
            onEvent = { event -> viewModel.onEvent(event) }
        )
    } else if (navigationState.isLogVisible) {
        LogOverlay(
            stateProvider = viewModel,
            onSetShowDetails = { show -> viewModel.onEvent(UiEvent.SetLogFilterShowDetails(show)) }, 
            onSetShowRecovered = { show -> viewModel.onEvent(UiEvent.SetLogFilterShowRecovered(show)) },
            onHistLink = { ts -> 
                viewModel.onEvent(UiEvent.SetReplayCursor(ts))
                viewModel.onEvent(UiEvent.ToggleRibbons(true))
            },
            onDetailsLink = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) },
            onMainEvent = { viewModel.onEvent(it) }
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

    if (hudHealth.isRedScreenVisible && sessionState.appMode == "viewer" && sessionState.hydrationLevel >= 3) {
        BackHandler { viewModel.onEvent(UiEvent.DismissAlarms) }
        AlarmOverlay(
            stateProvider = viewModel,
            onHardwarePermissionClick = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) },
            onMute = { 
                val currentCauses = hudHealth.activeAlarms.filter { !it.isResolved }.joinToString { it.title }.ifBlank { "Muted" }
                viewModel.onEvent(UiEvent.StopSiren(currentCauses))
            },
            onClose = { viewModel.onEvent(UiEvent.DismissAlarms) },
            onGoToMap = { viewModel.onEvent(UiEvent.DismissAlarms); viewModel.onEvent(UiEvent.ToggleMap(true)) }
        )
    }
}
