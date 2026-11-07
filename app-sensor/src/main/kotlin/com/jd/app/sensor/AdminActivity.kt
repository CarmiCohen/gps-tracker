package com.jd.app.sensor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jd.core.ui.HudState
import com.jd.core.ui.HudStatusBar
import com.jd.core.ui.RevolutionTheme
import com.jd.core.ui.StatusRed
import com.jd.core.ui.getAppVersionName
import com.jd.core.shared.SystemLogger
import com.jd.core.shared.ForensicIdentity
import com.jd.core.shared.RelayConfig
import com.jd.core.network.ForensicIdentityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AdminActivity: The "Admin Console" for the Sensor.
 * Updated Nov4.26: Added menu upper line with setup button and settings controls.
 */
@AndroidEntryPoint
class AdminActivity : ComponentActivity() {

    @Inject
    lateinit var identityManager: ForensicIdentityManager

    private val viewModel: SensorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RevolutionTheme {
                val trackerId by identityManager.selfId.collectAsStateWithLifecycle(initialValue = "LOADING...")
                val relayUrl by viewModel.relayUrl.collectAsStateWithLifecycle()
                val hudState by viewModel.hudState.collectAsStateWithLifecycle()
                val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
                val logs by SystemLogger.logs.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                
                LaunchedEffect(Unit) {
                    identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_SENSOR)
                    while(true) {
                        SystemLogger.updateUiSnapshot()
                        delay(1000)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLocked by rememberSaveable { mutableStateOf(true) }
                    var showSettings by rememberSaveable { mutableStateOf(false) }
                    var showAlertsOverlay by rememberSaveable { mutableStateOf(false) }
                    var showSoundOverlay by rememberSaveable { mutableStateOf(false) }
                    var showPhoneSetupOverlay by rememberSaveable { mutableStateOf(false) }
                    var showCleanSetupOverlay by rememberSaveable { mutableStateOf(false) }

                    val alertSettings by viewModel.alertSettings.collectAsStateWithLifecycle()
                    val sirenType by viewModel.sirenType.collectAsStateWithLifecycle()

                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isLocked = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("System Idle", color = Color.Gray.copy(alpha = 0.2f))
                        }
                    } else {
                        PermissionGuard {
                            if (showAlertsOverlay) {
                                com.jd.core.ui.AlertManagementOverlay(
                                    isTracker = true,
                                    draftAlertSettings = alertSettings,
                                    onUpdateAlertSettings = { viewModel.updateAlertSettings(it) },
                                    onClose = { showAlertsOverlay = false }
                                )
                            } else if (showSoundOverlay) {
                                com.jd.core.ui.AlarmSoundOverlay(
                                    isTracker = true,
                                    draftAlertSettings = alertSettings,
                                    selectedSirenType = sirenType,
                                    isSirenPlaying = false,
                                    onUpdateAlertSettings = { viewModel.updateAlertSettings(it) },
                                    onUpdateSirenType = { viewModel.updateSirenType(it) },
                                    onUpdateAlarmVolume = { viewModel.updateAlarmVolume(it) },
                                    onTestSiren = { },
                                    onClose = { showSoundOverlay = false }
                                )
                            } else if (showPhoneSetupOverlay) {
                                com.jd.core.ui.PhoneSetupOverlay(
                                    isTracker = true,
                                    homePointsCount = 0,
                                    onOpenAppInfo = { requestAppInfo() },
                                    onOpenBatterySettings = { requestIgnoreBatteryOptimization() },
                                    onOpenOverlayPermission = { requestOverlayPermission() },
                                    onOpenExactAlarmPermission = { },
                                    onOpenDndSettings = {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                        startActivity(intent)
                                    },
                                    onOpenFullScreenIntentSettings = {
                                        val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                            android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, android.net.Uri.parse("package:$packageName"))
                                        } else {
                                            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.parse("package:$packageName")
                                            }
                                        }
                                        startActivity(intent)
                                    },
                                    onOpenDiagnostics = { },
                                    onTestAlarm = { },
                                    onRefreshPermissions = { },
                                    onOpenMap = { },
                                    onClose = { showPhoneSetupOverlay = false }
                                )
                            } else if (showCleanSetupOverlay) {
                                com.jd.core.ui.CleanSetupOverlay(
                                    isTracker = true,
                                    onClearHomePoints = { },
                                    onResetStats = { },
                                    onFullInit = { },
                                    onClose = { showCleanSetupOverlay = false }
                                )
                            } else if (showSettings) {
                                SettingsOverlay(
                                    currentRelayUrl = relayUrl,
                                    onOpenAlertsSetup = { showAlertsOverlay = true },
                                    onOpenSoundSetup = { showSoundOverlay = true },
                                    onOpenPhoneSetup = { showPhoneSetupOverlay = true },
                                    onOpenCleanSetup = { showCleanSetupOverlay = true },
                                    onClose = { showSettings = false },
                                    onLoadDefaults = { 
                                        scope.launch {
                                            identityManager.resetIdentity(ForensicIdentity.PREFIX_SENSOR)
                                        }
                                    },
                                    onSetCustomId = { customId ->
                                        scope.launch {
                                            identityManager.setSelfId(customId)
                                        }
                                    },
                                    onUpdateRelayUrl = { newUrl ->
                                        viewModel.updateRelayUrl(newUrl)
                                    }
                                )
                            } else {
                                SensorAdminConsole(
                                    hudState = hudState,
                                    isServiceRunning = isServiceRunning,
                                    trackerId = trackerId,
                                    relayUrl = relayUrl,
                                    logs = logs,
                                    onSettingsClick = { showSettings = true },
                                    onIgnoreBatteryOptimization = { requestIgnoreBatteryOptimization() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun requestAppInfo() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}

internal fun getRequiredSensorPermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> {
    return mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE
    ).apply {
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
    }
}

internal fun checkSensorPermissions(
    context: android.content.Context,
    permissions: List<String>,
    checker: (android.content.Context, String) -> Int = { ctx, perm -> ctx.checkSelfPermission(perm) }
): Boolean {
    return permissions.all {
        checker(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissionGuard(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val requiredPermissions = remember { getRequiredSensorPermissions() }

    var allGranted by remember {
        mutableStateOf(checkSensorPermissions(context, requiredPermissions))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allGranted = checkSensorPermissions(context, requiredPermissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        allGranted = permissions.values.all { it }
    }

    if (!allGranted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Permissions Required", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { launcher.launch(requiredPermissions.toTypedArray()) }) {
                Text("Grant Permissions")
            }
        }
    } else {
        content()
    }
}

@Composable
fun SettingsOverlay(
    currentRelayUrl: String,
    onOpenAlertsSetup: () -> Unit,
    onOpenSoundSetup: () -> Unit,
    onOpenPhoneSetup: () -> Unit,
    onOpenCleanSetup: () -> Unit,
    onClose: () -> Unit,
    onLoadDefaults: () -> Unit,
    onSetCustomId: (String) -> Unit,
    onUpdateRelayUrl: (String) -> Unit
) {
    var customId by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    var relayUrlInput by rememberSaveable(currentRelayUrl) { mutableStateOf(currentRelayUrl) }
    var isRelayUrlError by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CONFIGURATION", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onClose) { Text("CLOSE (X)") }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Alarming & Audio Setup", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenAlertsSetup,
                modifier = Modifier.weight(1f)
            ) {
                Text("ALERTS SETUP")
            }
            Button(
                onClick = onOpenSoundSetup,
                modifier = Modifier.weight(1f)
            ) {
                Text("SIREN SETUP")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Device & Maintenance Setup", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenPhoneSetup,
                modifier = Modifier.weight(1f)
            ) {
                Text("PHONE SETUP")
            }
            Button(
                onClick = onOpenCleanSetup,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = com.jd.core.ui.StatusRed)
            ) {
                Text("CLEAN SETUP")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        Text("Relay Server URL Setup", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = relayUrlInput,
            onValueChange = {
                relayUrlInput = it
                isRelayUrlError = it.isNotBlank() && !RelayConfig.isValid(it)
            },
            label = { Text("Relay URL") },
            isError = isRelayUrlError,
            supportingText = {
                if (isRelayUrlError) {
                    Text("Enter a full address starting with http://, https://, ws://, or wss://")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (!isRelayUrlError && relayUrlInput.isNotBlank()) {
                        onUpdateRelayUrl(relayUrlInput.trim())
                    }
                },
                enabled = relayUrlInput.isNotBlank() && !isRelayUrlError,
                modifier = Modifier.weight(1f)
            ) {
                Text("APPLY RELAY URL")
            }
            OutlinedButton(
                onClick = {
                    val defaultUrl = RelayConfig.DEFAULT_URL
                    relayUrlInput = defaultUrl
                    onUpdateRelayUrl(defaultUrl)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("RESET DEFAULT")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Manual Device ID Setup", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = customId,
            onValueChange = { 
                customId = it
                isError = it.isNotEmpty() && !ForensicIdentity.isValid(it)
            },
            label = { Text("Enter Custom ID (Any non-blank chars)") },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text("Device ID cannot be blank")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (!isError && customId.isNotBlank()) {
                    onSetCustomId(customId)
                    onClose()
                }
            },
            enabled = customId.isNotBlank() && !isError,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("APPLY CUSTOM ID")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { 
                onLoadDefaults()
                onClose()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
        ) {
            Text("LOAD FACTORY DEFAULTS")
        }
    }
}

@Composable
fun SensorAdminConsole(
    hudState: HudState,
    isServiceRunning: Boolean,
    trackerId: String,
    relayUrl: String,
    logs: List<SystemLogger.LogEntry>, 
    onSettingsClick: () -> Unit,
    onIgnoreBatteryOptimization: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showSetupDialog by rememberSaveable { mutableStateOf(false) }
    var showToolsOverlay by rememberSaveable { mutableStateOf(false) }

    if (showToolsOverlay) {
        com.jd.core.ui.ToolsOverlay(
            onTriggerBurst = { SystemLogger.i("TOOLS", "Triggered high-frequency IMU/GNSS burst (100Hz).") },
            onPruneDatabase = { SystemLogger.i("TOOLS", "Executed manual database pruning.") },
            onTriggerTestAlarm = { SystemLogger.i("TOOLS", "Simulated test violation event.") },
            onResetWatchdog = { SystemLogger.i("TOOLS", "Reset service watchdog timer.") },
            onClose = { showToolsOverlay = false }
        )
        return
    }

    Column {
        // Menu Upper Line (Top Menu Bar with Setup, Settings, Tools, and Info)
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(onClick = { showSetupDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Build, contentDescription = "Setup", tint = Color(0xFF06B6D4), modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { showToolsOverlay = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Tools", tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { SystemLogger.i("UI", "Map view requested from upper menu.") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Place, contentDescription = "Map", tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { SystemLogger.i("UI", "Events log requested from upper menu.") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Menu, contentDescription = "Events", tint = Color(0xFFF43F5E), modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { SystemLogger.i("UI", "Graphs analytics requested from upper menu.") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Star, contentDescription = "Graphs", tint = Color(0xFF8B5CF6), modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { showSetupDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFF3B82F6), modifier = Modifier.size(15.dp))
                    }
                }
                Text(
                    text = "SENSOR: ${trackerId.take(6).uppercase()}",
                    color = Color(0xFF06B6D4),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
                }
            }
        }

        HudStatusBar(hudState = hudState)
        
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SENSOR ADMIN",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
                }
            }

            Text(text = "ID: ${trackerId.take(12)}...", fontSize = 12.sp, color = Color.Gray)
            Text(text = "Relay URL: $relayUrl", fontSize = 12.sp, color = Color.Gray)
            Text(text = "Version: ${getAppVersionName(context)}", fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onIgnoreBatteryOptimization, modifier = Modifier.fillMaxWidth()) {
                Text("WHITELIST BATTERY")
            }

            Button(
                onClick = {
                    val intent = Intent(context, SensorService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("START SENSOR SERVICE")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "LIVE EVENT LOG", style = MaterialTheme.typography.labelLarge)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.05f))
            ) {
                LazyColumn(state = listState) {
                    items(logs) { entry ->
                        Text(
                            text = entry.toString(),
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    if (showSetupDialog) {
        AlertDialog(
            onDismissRequest = { showSetupDialog = false },
            title = { Text("Sensor Setup & Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sensor Device ID: $trackerId")
                    Text("Relay URL: $relayUrl")
                    Text("App Version: ${getAppVersionName(context)}")
                    Text(serviceStatusText(isServiceRunning))
                    Text(relayStatusText(hudState.isRelayConnected))
                }
            },
            confirmButton = {
                Button(onClick = { showSetupDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSetupDialog = false
                    onSettingsClick()
                }) {
                    Text("CHANGE RELAY URL")
                }
            }
        )
    }
}
