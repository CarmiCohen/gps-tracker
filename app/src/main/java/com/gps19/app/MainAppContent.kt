package com.gps19.app

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gps19.core.engine.LANDING_PAGE_PAUSE_MS
import kotlinx.coroutines.delay

/**
 * MainAppContent: The top-level Composable for the application.
 * v9.3.11:
 * - Issue #059: Integrated DiagnosticsScreen into NavHost and handled overlay visibility.
 * v9.3.0:
 * - Issue #042: Sanitization Visibility. Added AlertDialog to notify user 
 *   when malformed IDs are automatically reset.
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
    onRequestExactAlarm: () -> Unit,
    onRequestXiaomiPermission: () -> Unit,
    checkAndRequestPermissions: (String) -> Unit,
    onStopTracking: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val systemPulse by viewModel.systemPulse.collectAsStateWithLifecycle()
    val systemPulseRealtime by viewModel.systemPulseRealtime.collectAsStateWithLifecycle()
    val redScreenVisible by viewModel.redScreenVisible.collectAsStateWithLifecycle()
    
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onEvent(UiEvent.RefreshPermissionStatus)
                    viewModel.onEvent(UiEvent.SetUiVisible(true))
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.onEvent(UiEvent.SetUiVisible(false))
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showBackgroundDisclosure by remember { mutableStateOf(false) }

    // Navigation logic based on app mode and diagnostics visibility
    LaunchedEffect(uiState.isInitialized, uiState.appMode, uiState.navigation.isDiagnosticsVisible) {
        if (!uiState.isInitialized) return@LaunchedEffect
        
        val mode = uiState.appMode
        val isDiagnostics = uiState.navigation.isDiagnosticsVisible

        if (isDiagnostics) {
            if (navController.currentDestination?.route != Screen.Diagnostics.route) {
                navController.navigate(Screen.Diagnostics.route)
            }
            return@LaunchedEffect
        }

        if (mode != null) {
            // R926: Mandatory transition delay (LANDING_PAGE_PAUSE_MS) and Service Launch Integrity
            if (navController.currentDestination?.route == Screen.Landing.route) {
                delay(LANDING_PAGE_PAUSE_MS)
                onStartService(mode)
            }
        }

        when (mode) {
            "tracker" -> {
                if (navController.currentDestination?.route != Screen.Tracker.route) {
                    navController.navigate(Screen.Tracker.route) { popUpTo(Screen.Landing.route) { inclusive = true } }
                }
            }
            "viewer" -> {
                if (navController.currentDestination?.route != Screen.Viewer.route) {
                    navController.navigate(Screen.Viewer.route) { popUpTo(Screen.Landing.route) { inclusive = true } }
                }
            }
            null -> {
                if (navController.currentDestination?.route != Screen.Landing.route) {
                    navController.navigate(Screen.Landing.route) { popUpTo(0) { inclusive = true } }
                }
            }
        }
    }

    if (!uiState.isInitialized) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val eventLogs by viewModel.eventLogsFlow.collectAsStateWithLifecycle()
    val trackerTrail by viewModel.trackerTrailFlow.collectAsStateWithLifecycle()
    val viewerTrail by viewModel.viewerTrailFlow.collectAsStateWithLifecycle()
    val violations by viewModel.violationPointsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
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

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            viewModel.pendingMode?.let { mode ->
                viewModel.onEvent(UiEvent.SetAppMode(mode))
                onStartService(mode)
                viewModel.pendingMode = null
            }
        } else {
            Toast.makeText(activity, context.getString(R.string.perm_background_denied_toast), Toast.LENGTH_LONG).show()
        }
    }

    fun hasRequiredPermissions(mode: String): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        // RECORD_AUDIO is only required for Tracker mode
        val audio = if (mode == "tracker") {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else true
        
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        
        return fineLocation && coarseLocation && audio && notification
    }

    if (showBackgroundDisclosure) {
        AlertDialog(
            onDismissRequest = { showBackgroundDisclosure = false },
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
            dismissButton = { Button(onClick = { showBackgroundDisclosure = false }) { Text(stringResource(R.string.perm_background_btn_reject)) } }
        )
    }

    if (uiState.isIdentitySanitized) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(UiEvent.DismissIdentitySanitization) },
            title = { Text(stringResource(R.string.sanitization_title)) },
            text = { Text(stringResource(R.string.sanitization_desc)) },
            confirmButton = {
                Button(onClick = { viewModel.onEvent(UiEvent.DismissIdentitySanitization) }) {
                    Text(stringResource(R.string.btn_dismiss))
                }
            }
        )
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { MainFileHelper.importConfig(activity, viewModel, uri) } }
    val importTrailLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> MainFileHelper.importTrails(activity, viewModel, uris) }
    
    GpsTrackerTheme(appMode = uiState.appMode) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            BackHandler(enabled = redScreenVisible && uiState.appMode != null) {
                viewModel.onEvent(UiEvent.DismissAlarms)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(navController = navController, startDestination = Screen.Landing.route) {
                    composable(Screen.Landing.route) {
                        BackHandler { onCleanupAndExit() }
                        LandingScreen { mode -> 
                            if (hasRequiredPermissions(mode)) { 
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    viewModel.pendingMode = mode; showBackgroundDisclosure = true
                                } else {
                                    viewModel.onEvent(UiEvent.SetAppMode(mode)); onStartService(mode)
                                }
                            } else { 
                                viewModel.pendingMode = mode; checkAndRequestPermissions(mode)
                            } 
                        }
                    }
                    composable(Screen.Tracker.route) {
                        BackHandler {
                            val nav = uiState.navigation
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
                        TrackerScreen(
                            uiState = uiState, viewModel = viewModel, logs = eventLogs, trail = trackerTrail, viewerTrail = viewerTrail, violations = violations,
                            systemPulse = systemPulse, systemPulseRealtime = systemPulseRealtime,
                            onToggleMap = { viewModel.onEvent(UiEvent.ToggleMap(!uiState.navigation.isMapVisible)) }, 
                            onToggleLog = { viewModel.onEvent(UiEvent.ToggleLog(!uiState.navigation.isLogVisible)) }, 
                            onToggleSettings = { viewModel.onEvent(UiEvent.ToggleSettings(!uiState.navigation.isSettingsOpen)) },
                            onExit = onCleanupAndExit,
                            onResetStats = { viewModel.onEvent(UiEvent.ResetStats) }, onExportLogs = { MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider) }, 
                            onImportConfig = { importLauncher.launch("application/json") }, onClearLogs = { viewModel.onEvent(UiEvent.ClearLogs) }, onClearHome = { viewModel.onEvent(UiEvent.ClearHomePoints) },
                            onSaveTrail = { MainFileHelper.manualExportTrails(activity, viewModel, viewModel.timeProvider) }, onLoadTrail = { importTrailLauncher.launch("application/json") }
                        )
                    }
                    composable(Screen.Viewer.route) {
                        BackHandler {
                            val nav = uiState.navigation
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
                        ViewerScreen(
                            uiState = uiState, viewModel = viewModel, logs = eventLogs, trackerTrail = trackerTrail, viewerTrail = viewerTrail, violations = violations,
                            systemPulse = systemPulse, systemPulseRealtime = systemPulseRealtime,
                            onToggleMap = { viewModel.onEvent(UiEvent.ToggleMap(!uiState.navigation.isMapVisible)) }, 
                            onToggleLog = { viewModel.onEvent(UiEvent.ToggleLog(!uiState.navigation.isLogVisible)) }, 
                            onToggleSettings = { viewModel.onEvent(UiEvent.ToggleSettings(!uiState.navigation.isSettingsOpen)) },
                            onExit = onCleanupAndExit,
                            onImportConfig = { importLauncher.launch("application/json") }, onExportLogs = { MainFileHelper.manualExportLogs(activity, viewModel, viewModel.timeProvider) },
                            onClearLogs = { viewModel.onEvent(UiEvent.ClearLogs) }, onResetStats = { viewModel.onEvent(UiEvent.ResetStats) }, onClearHome = { viewModel.onEvent(UiEvent.ClearHomePoints) },
                            onSaveTrail = { MainFileHelper.manualExportTrails(activity, viewModel, viewModel.timeProvider) }, onLoadTrail = { importTrailLauncher.launch("application/json") }
                        )
                    }
                    composable(Screen.Diagnostics.route) {
                        BackHandler {
                            viewModel.onEvent(UiEvent.NavigateToDiagnostics(false))
                        }
                        DiagnosticsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(false)) },
                            onRequestBatteryExemption = onRequestBatteryExemption,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onRequestAppInfo = onRequestAppInfo,
                            onRequestExactAlarm = onRequestExactAlarm,
                            onRequestXiaomiPermission = onRequestXiaomiPermission
                        )
                    }
                }
                
                if (uiState.navigation.isPhoneSetupVisible) {
                    PhoneSetupOverlay(
                        onClose = { viewModel.onEvent(UiEvent.TogglePhoneSetup(false)) }, 
                        onWhitelist = { onRequestBatteryExemption() }, 
                        onOverlay = { onRequestOverlayPermission() }, 
                        onAppInfo = { onRequestAppInfo() },
                        onExactAlarm = { onRequestExactAlarm() },
                        onXiaomi = { onRequestXiaomiPermission() },
                        onRefresh = { viewModel.onEvent(UiEvent.RefreshPermissionStatus) }, 
                        onToggleXiaomiOverride = { viewModel.onEvent(UiEvent.ToggleXiaomiManualOverride) },
                        onTestAlarm = { viewModel.onEvent(UiEvent.TriggerTestAlarm) },
                        onNavigateToDiagnostics = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) },
                        isBatteryWhitelisted = uiState.permissions.isBatteryWhitelisted, 
                        isOverlayGranted = uiState.permissions.isOverlayGranted,
                        isMicrophoneGranted = uiState.permissions.isMicrophoneGranted,
                        isExactAlarmGranted = uiState.permissions.isExactAlarmGranted,
                        isPostNotificationsGranted = uiState.permissions.isPostNotificationsGranted,
                        isBackgroundLocationGranted = uiState.permissions.isBackgroundLocationGranted,
                        xiaomiStatus = uiState.permissions.xiaomiStatus,
                        isXiaomiManualOverride = uiState.permissions.isXiaomiManualOverride,
                        homePointsCount = uiState.homePoints.size,
                        isTrackerMode = uiState.appMode == "tracker",
                        onGoToMap = { viewModel.onEvent(UiEvent.TogglePhoneSetup(false)); viewModel.onEvent(UiEvent.ToggleMap(true)) }
                    )
                }

                if (redScreenVisible && uiState.appMode != null) {
                    AlarmOverlay(
                        alarms = uiState.activeAlarms, isMuted = uiState.isAlarmSilenced,
                        isLocationPending = uiState.integrity.isLocationPending,
                        xiaomiStatus = uiState.permissions.xiaomiStatus,
                        onXiaomiPermissionClick = { onRequestXiaomiPermission() },
                        onMute = { 
                            val currentCauses = uiState.activeAlarms.filter { !it.isResolved }.joinToString { it.title }.ifBlank { context.getString(R.string.status_muted) }
                            viewModel.onEvent(UiEvent.StopSiren(currentCauses))
                        },
                        onClose = { viewModel.onEvent(UiEvent.DismissAlarms) },
                        onGoToMap = { viewModel.onEvent(UiEvent.DismissAlarms); viewModel.onEvent(UiEvent.ToggleMap(true)) }
                    )
                }

                if (uiState.navigation.isStopTrackingConfirmationVisible) {
                    var timeLeft by remember { mutableStateOf(5) }
                    LaunchedEffect(Unit) {
                        while (timeLeft > 0) {
                            delay(1000)
                            timeLeft--
                        }
                        viewModel.onEvent(UiEvent.ShowStopTrackingConfirmation(false))
                    }

                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(UiEvent.ShowStopTrackingConfirmation(false)) },
                        title = { Text(stringResource(R.string.stop_tracking_title)) },
                        text = { Text(stringResource(R.string.stop_tracking_desc, timeLeft)) },
                        confirmButton = {
                            Button(onClick = { 
                                viewModel.onEvent(UiEvent.ConfirmStopTracking)
                                onStopTracking() 
                            }) { Text(stringResource(R.string.btn_stop_tracking)) }
                        },
                        dismissButton = {
                            Button(onClick = { viewModel.onEvent(UiEvent.ShowStopTrackingConfirmation(false)) }) { Text(stringResource(R.string.btn_cancel)) }
                        }
                    )
                }
            }
        }
    }
}
