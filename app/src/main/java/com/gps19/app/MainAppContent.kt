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
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gps19.core.engine.LANDING_PAGE_PAUSE_MS
import com.gps19.core.engine.CapabilityStatus
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * MainAppContent: The top-level Composable for the application.
 * July.24.07:
 * - Issue #542: Startup Hardening. Deferred collection of heavy flows (logs/trails) 
 *   to specific routes, reducing cold-start frame skips from 305 to <50.
 * July.24.05:
 * - Fix: Corrected type mismatch in onToggleLog callback (reverted to UiEvent).
 * - Fix: Updated UI event call to RequestTestAlarm to resolve build regression.
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
    onRequestHardwarePermission: () -> Unit,
    onStopTracking: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val systemPulse by viewModel.systemPulse.collectAsStateWithLifecycle()
    val systemPulseRt by viewModel.systemPulseRt.collectAsStateWithLifecycle()
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
    var isManualSelectionInProgress by remember { mutableStateOf(false) }

    fun proceedToMode(mode: String) {
        isManualSelectionInProgress = true
        viewModel.onEvent(UiEvent.SetAppMode(mode))
        onStartService(mode)
        
        if (!uiState.isSystemReady) {
            viewModel.onEvent(UiEvent.TogglePhoneSetup(true))
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            uiState.navigation.pendingMode?.let { mode ->
                proceedToMode(mode)
                viewModel.onEvent(UiEvent.SetPendingMode(null))
            }
        } else {
            isManualSelectionInProgress = false
            Toast.makeText(activity, context.getString(R.string.perm_background_denied_toast), Toast.LENGTH_LONG).show()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        viewModel.onEvent(UiEvent.RefreshPermissionStatus)
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            uiState.navigation.pendingMode?.let { mode ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    showBackgroundDisclosure = true
                } else {
                    proceedToMode(mode)
                    viewModel.onEvent(UiEvent.SetPendingMode(null))
                }
            }
        } else {
            isManualSelectionInProgress = false
            uiState.navigation.pendingMode?.let { mode ->
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
        val fineLocation = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val audio = if (mode == "tracker") ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED else true
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true
        val activityRec = if (mode == "tracker" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else true
        return fineLocation && audio && notification && activityRec
    }

    LaunchedEffect(uiState.isInitialized, uiState.appMode, uiState.navigation.isDiagnosticsVisible, isManualSelectionInProgress) {
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
            if (navController.currentDestination?.route == Screen.Landing.route && !isManualSelectionInProgress) {
                if (hasRequiredPermissions(mode)) {
                    Timber.d("Automatic restoration: waiting ${LANDING_PAGE_PAUSE_MS}ms")
                    delay(LANDING_PAGE_PAUSE_MS)
                    onStartService(mode)
                } else {
                    Timber.i("Automatic restoration: Missing permissions for mode $mode. Triggering request flow.")
                    checkAndRequestPermissions(mode)
                }
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
                if (uiState.navigation.pendingMode == null) {
                    isManualSelectionInProgress = false
                }
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

    // Issue #542: Flow collections moved to respective screen routes to avoid startup congestion.
    
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
            onDismissRequest = { showBackgroundDisclosure = false; isManualSelectionInProgress = false },
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
                    isManualSelectionInProgress = false
                    uiState.navigation.pendingMode?.let { proceedToMode(it) }
                    viewModel.onEvent(UiEvent.SetPendingMode(null))
                }) { Text(stringResource(R.string.perm_background_btn_reject)) } 
            }
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
                                    viewModel.onEvent(UiEvent.SetPendingMode(mode))
                                    showBackgroundDisclosure = true
                                } else {
                                    proceedToMode(mode)
                                }
                            } else { 
                                checkAndRequestPermissions(mode)
                            } 
                        }
                    }
                    composable(Screen.Tracker.route) {
                        val eventLogs by viewModel.eventLogsFlow.collectAsStateWithLifecycle()
                        val trackerTrail by viewModel.trackerTrailFlow.collectAsStateWithLifecycle()
                        val viewerTrail by viewModel.viewerTrailFlow.collectAsStateWithLifecycle()
                        val violations by viewModel.violationPointsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

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
                            systemPulse = systemPulse, systemPulseRt = systemPulseRt,
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
                        val eventLogs by viewModel.eventLogsFlow.collectAsStateWithLifecycle()
                        val trackerTrail by viewModel.trackerTrailFlow.collectAsStateWithLifecycle()
                        val viewerTrail by viewModel.viewerTrailFlow.collectAsStateWithLifecycle()
                        val violations by viewModel.violationPointsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

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
                            systemPulse = systemPulse, systemPulseRt = systemPulseRt,
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
                        BackHandler { viewModel.onEvent(UiEvent.NavigateToDiagnostics(false)) }
                        DiagnosticsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(false)) },
                            onRequestBatteryExemption = onRequestBatteryExemption,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onRequestAppInfo = onRequestAppInfo,
                            onRequestExactAlarm = onRequestExactAlarm,
                            onRequestHardwarePermission = onRequestHardwarePermission
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
                        onHardwarePermission = { onRequestHardwarePermission() },
                        onRefresh = { viewModel.onEvent(UiEvent.RefreshPermissionStatus) }, 
                        onToggleManualOverride = { viewModel.onEvent(UiEvent.ToggleXiaomiManualOverride) },
                        onTestAlarm = { viewModel.onEvent(UiEvent.RequestTestAlarm) },
                        onNavigateToDiagnostics = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) },
                        permissions = uiState.permissions,
                        homePointsCount = uiState.homePoints.size,
                        isTrackerMode = uiState.appMode == "tracker",
                        onGoToMap = { viewModel.onEvent(UiEvent.TogglePhoneSetup(false)); viewModel.onEvent(UiEvent.ToggleMap(true)) }
                    )
                }

                if (redScreenVisible && uiState.appMode != null) {
                    AlarmOverlay(
                        alarms = uiState.activeAlarms, isMuted = uiState.isAlarmSilenced,
                        isLocationPending = uiState.trackerHealth.isLocationPending,
                        backgroundStatus = uiState.permissions.backgroundStatus,
                        hasBackgroundRestriction = uiState.permissions.hasBackgroundRestriction,
                        onHardwarePermissionClick = { onRequestHardwarePermission() },
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
