package com.gps19.app

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import timber.log.Timber

/**
 * MainAppContent: Root UI composition.
 * Oct.16.3:
 * - ISSUE-OCT16.2-F1: Updated version tag to Oct16.3_PLATINUM.
 * Oct.16.2:
 * - SIMP-OCT15.10-S1: Updated MainFileHelper call sites to support decoupling.
 */
@Composable
fun MainAppContent(
    activity: MainActivity,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val sessionState = uiState.session
    val mode = sessionState.appMode
    val navigationState = uiState.navigation
    
    val navController = rememberNavController()
    var showBackgroundDisclosure by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onEvent(UiEvent.RefreshPermissionStatus)
        navigationState.pendingMode?.let { m -> viewModel.onEvent(UiEvent.InitiateMode(m)) }
    }

    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onEvent(UiEvent.RefreshPermissionStatus)
    }

    AppEffectAggregator(
        viewModel = viewModel,
        navController = navController,
        activity = activity,
        requestPermissionLauncher = requestPermissionLauncher,
        settingsLauncher = settingsLauncher,
        onShowBackgroundDisclosure = { showBackgroundDisclosure = true }
    )

    GpsTrackerTheme(
        appMode = mode,
        isMaliAnomaly = hudState.health.isMaliAnomaly
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.99f), // Mali GPU Fix
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                
                when {
                    navigationState.isDiagnosticsVisible -> {
                        DiagnosticsScreen(stateProvider = viewModel, onMainEvent = { viewModel.onEvent(it) })
                    }
                    mode == "tracker" -> {
                        TrackerScreen(stateProvider = viewModel, onMainEvent = { viewModel.onEvent(it) })
                    }
                    mode == "viewer" -> {
                        ViewerScreen(stateProvider = viewModel, onMainEvent = { viewModel.onEvent(it) })
                    }
                    else -> {
                        LandingScreen(isPeerActive = sessionState.isPeerActive) { m ->
                            Timber.d("PLATINUM_TAP: Initiating Mode $m")
                            viewModel.onEvent(UiEvent.InitiateMode(m))
                        }
                    }
                }

                OverlayHost(viewModel = viewModel, navigationState = navigationState)

                AnimatedVisibility(
                    visible = sessionState.hydrationLevel < 3 && mode != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BrandJd, strokeWidth = 2.dp)
                            Spacer(Modifier.height(16.dp))
                            Text("Engine Resuming...", color = Slate400, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                    Text("UI_READY", color = Color.Green.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Oct16.3_PLATINUM", color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp)
                }
            }
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

    if (uiState.settings.isIdentitySanitized) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(UiEvent.DismissIdentitySanitization) },
            title = { Text(stringResource(R.string.sanitization_title)) },
            text = { Text(stringResource(R.string.sanitization_desc)) },
            confirmButton = { Button(onClick = { viewModel.onEvent(UiEvent.DismissIdentitySanitization) }) { Text(stringResource(R.string.btn_dismiss)) } }
        )
    }
}

@Composable
private fun AppEffectAggregator(
    viewModel: MainViewModel,
    navController: NavHostController,
    activity: MainActivity,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>,
    settingsLauncher: ActivityResultLauncher<Intent>,
    onShowBackgroundDisclosure: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { MainFileHelper.importConfig(activity, viewModel.uiState.value.session.appMode, { viewModel.onEvent(it) }, uri) } 
    }
    val importTrailLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> 
        MainFileHelper.importTrails(activity, viewModel.repository, { viewModel.onEvent(it) }, uris) 
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onEvent(UiEvent.SetUiVisible(true))
            else if (event == Lifecycle.Event.ON_PAUSE) viewModel.onEvent(UiEvent.SetUiVisible(false))
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
                
                UiEffect.LaunchTrailExport -> MainFileHelper.manualExportTrails(activity, viewModel.uiState.value, viewModel.repository, viewModel.timeProvider)
                UiEffect.LaunchTrailImport -> importTrailLauncher.launch("application/json")
                UiEffect.LaunchLogExport -> MainFileHelper.manualExportLogs(activity, viewModel.uiState.value, viewModel.repository, viewModel.timeProvider)
                UiEffect.LaunchConfigImport -> importLauncher.launch("application/json")
                UiEffect.LaunchFullInitialization -> viewModel.fullInitialization(activity)
                
                UiEffect.LaunchBatteryExemption -> activity.systemIntentHandler.launchBatteryExemptionSetting(settingsLauncher)
                UiEffect.LaunchOverlayPermission -> activity.systemIntentHandler.launchOverlayPermissionSetting(settingsLauncher)
                UiEffect.LaunchAppInfo -> activity.systemIntentHandler.launchAppInfo(settingsLauncher)
                UiEffect.LaunchExactAlarm -> activity.systemIntentHandler.launchExactAlarmSetting(settingsLauncher)
                UiEffect.LaunchHardwarePermission -> activity.systemIntentHandler.launchOverlayPermissionSetting(settingsLauncher)
                else -> {}
            }
        }
    }
}

@Composable
fun OverlayHost(
    viewModel: MainViewModel,
    navigationState: NavigationState
) {
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val sessionState by viewModel.session.collectAsStateWithLifecycle()
    val hudHealth = hudState.health

    if (navigationState.isSettingsOpen) {
        SettingsOverlay(stateProvider = viewModel, onEvent = { event -> viewModel.onEvent(event) })
    } else if (navigationState.isLogVisible) {
        LogOverlay(
            stateProvider = viewModel,
            onSetShowDetails = { show -> viewModel.onEvent(UiEvent.SetLogFilterShowDetails(show)) }, 
            onSetShowRecovered = { show -> viewModel.onEvent(UiEvent.SetLogFilterShowRecovered(show)) },
            onHistLink = { ts -> viewModel.onEvent(UiEvent.SetReplayCursor(ts)); viewModel.onEvent(UiEvent.ToggleRibbons(true)) },
            onDetailsLink = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) },
            onMainEvent = { viewModel.onEvent(it) }
        )
    } else if (navigationState.isRibbonsVisible) {
        RibbonsOverlay(stateProvider = viewModel, onEvent = { viewModel.onEvent(it) })
    } else if (navigationState.isGnssDetailVisible) {
        GnssDetailOverlay(gnssDetailFlow = viewModel.activeGnssDetail, onClose = { viewModel.onEvent(UiEvent.ToggleGnssDetail(false)) })
    } else if (navigationState.isPhoneSetupVisible) {
        PhoneSetupOverlay(stateProvider = viewModel, onEvent = { viewModel.onEvent(it) })
    }

    if (hudHealth.isRedScreenVisible && sessionState.appMode == "viewer") {
        BackHandler { viewModel.onEvent(UiEvent.DismissAlarms) }
        AlarmOverlay(
            stateProvider = viewModel,
            onHardwarePermissionClick = { viewModel.onEvent(UiEvent.NavigateToDiagnostics(true)) },
            onMute = { 
                val causes = hudHealth.activeAlarms.filter { !it.isResolved }.joinToString { it.title }.ifBlank { "Muted" }
                viewModel.onEvent(UiEvent.StopSiren(causes))
            },
            onClose = { viewModel.onEvent(UiEvent.DismissAlarms) },
            onGoToMap = { viewModel.onEvent(UiEvent.DismissAlarms); viewModel.onEvent(UiEvent.ToggleMap(true)) }
        )
    }
}
