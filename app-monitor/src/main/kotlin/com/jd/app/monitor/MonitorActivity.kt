package com.jd.app.monitor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jd.core.ui.CappedFontScale
import com.jd.core.ui.HudState
import com.jd.core.ui.HudStatusBar
import com.jd.core.ui.RevolutionTheme
import com.jd.core.ui.getAppVersionName
import com.jd.core.shared.ForensicIdentity
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.RelayConfig
import com.jd.core.network.ForensicIdentityManager
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker

/**
 * MonitorActivity: The primary UI for the Monitor (Watcher).
 * Hardened with upper menu line, setup button, fixed SRV/GPS LEDs, map settings wheel, and map control buttons.
 */
@AndroidEntryPoint
class MonitorActivity : ComponentActivity() {

    private val viewModel: MonitorViewModel by viewModels()
    @Inject lateinit var identityManager: ForensicIdentityManager

    override fun onResume() {
        super.onResume()
        viewModel.refreshLocationPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName

        setContent {
            RevolutionTheme {
                LaunchedEffect(Unit) {
                    identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_MONITOR)
                }
                val trackerId by viewModel.targetId.collectAsStateWithLifecycle(initialValue = null)
                val monitorId by viewModel.selfId.collectAsStateWithLifecycle(initialValue = "LOADING...")
                val relayUrl by viewModel.relayUrl.collectAsStateWithLifecycle()
                val trail by viewModel.trail.collectAsStateWithLifecycle()
                val localHudState by viewModel.localHudState.collectAsStateWithLifecycle()
                val remoteHudState by viewModel.remoteHudState.collectAsStateWithLifecycle()
                val localPosition by viewModel.localPosition.collectAsStateWithLifecycle()
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { viewModel.refreshLocationPermission() }
                LaunchedEffect(Unit) {
                    // Alarm notifications need the notification permission from Android 13.
                    val notificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else emptyArray()
                    locationPermissionLauncher.launch(MonitorLocationSource.LOCATION_PERMISSIONS + notificationPermission)
                }
                val context = LocalContext.current

                val alertSettings by viewModel.alertSettings.collectAsStateWithLifecycle()
                val sirenType by viewModel.sirenType.collectAsStateWithLifecycle()
                val isSirenPlaying by viewModel.isSirenPlaying.collectAsStateWithLifecycle()
                val alarmBook by viewModel.alarms.collectAsStateWithLifecycle()

                LaunchedEffect(trackerId) {
                    val serviceIntent = Intent(context, MonitorSessionService::class.java)
                    if (trackerId != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } else {
                        context.stopService(serviceIntent)
                    }
                }

                if (trackerId != null && alarmBook.pending.isNotEmpty()) {
                    AlarmAlertScreen(alarmBook.pending, trackerId, onAcknowledge = viewModel::acknowledgeAlarms)
                } else if (trackerId == null) {
                    PairingWizard(
                        relayUrl = relayUrl,
                        onPair = { id -> viewModel.setTargetId(id) },
                        onUpdateRelayUrl = { url -> viewModel.updateRelayUrl(url) }
                    )
                } else {
                    MonitorScreen(
                        localHudState = localHudState,
                        remoteHudState = remoteHudState,
                        localPosition = localPosition,
                        trackerId = trackerId!!,
                        monitorId = monitorId,
                        relayUrl = relayUrl,
                        trail = trail,
                        alertSettings = alertSettings,
                        sirenType = sirenType,
                        isSirenPlaying = isSirenPlaying,
                        onUpdateAlertSettings = { viewModel.updateAlertSettings(it) },
                        onUpdateSirenType = { viewModel.updateSirenType(it) },
                        onUpdateAlarmVolume = { viewModel.updateAlarmVolume(it) },
                        onTestSiren = { viewModel.testSirenAudio() },
                        onReset = { 
                            viewModel.clearTargetId()
                            context.stopService(Intent(context, MonitorSessionService::class.java))
                        },
                        onUpdateRelayUrl = { url -> viewModel.updateRelayUrl(url) }
                    )
                }
            }
        }
    }
}

private const val WORLD_ZOOM = 3.0
private const val DEVICE_ZOOM = 18.0

@Composable
fun PairingWizard(
    relayUrl: String,
    onPair: (String) -> Unit,
    onUpdateRelayUrl: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(ForensicIdentity.DEFAULT_TRACKER_ID) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var relayUrlInput by rememberSaveable(relayUrl) { mutableStateOf(relayUrl) }
    var showRelayConfig by rememberSaveable { mutableStateOf(false) }
    val isRelayUrlValid = RelayConfig.isValid(relayUrlInput)

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Watcher", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        TextField(
            value = text,
            onValueChange = { 
                text = it 
                isError = it.isNotEmpty() && !ForensicIdentity.isValid(it)
            },
            label = { Text("Enter Tracker ID") },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text("Device ID cannot be blank")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = { if (!isError && text.isNotEmpty()) onPair(text) },
            enabled = !isError && text.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Pair Device")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = { showRelayConfig = !showRelayConfig }) {
            Text(if (showRelayConfig) "Hide Relay Settings" else "Relay URL: $relayUrl")
        }

        if (showRelayConfig) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                TextField(
                    value = relayUrlInput,
                    onValueChange = { relayUrlInput = it },
                    label = { Text("Relay Server URL") },
                    isError = !isRelayUrlValid,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onUpdateRelayUrl(relayUrlInput.trim()) },
                        enabled = isRelayUrlValid,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save URL")
                    }
                    OutlinedButton(
                        onClick = {
                            val defaultUrl = RelayConfig.DEFAULT_URL
                            relayUrlInput = defaultUrl
                            onUpdateRelayUrl(defaultUrl)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Default")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Version: ${getAppVersionName(LocalContext.current)}", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun MonitorScreen(
    localHudState: HudState,
    remoteHudState: HudState,
    localPosition: MapPosition?,
    trackerId: String,
    monitorId: String, 
    relayUrl: String,
    trail: List<TrailEntity>,
    alertSettings: AlertSettings,
    sirenType: String,
    isSirenPlaying: Boolean,
    onUpdateAlertSettings: (AlertSettings) -> Unit,
    onUpdateSirenType: (String) -> Unit,
    onUpdateAlarmVolume: (Float) -> Unit,
    onTestSiren: () -> Unit,
    onReset: () -> Unit,
    onUpdateRelayUrl: (String) -> Unit
) {
    val context = LocalContext.current
    var isMapButtonsVisible by rememberSaveable { mutableStateOf(true) }
    var showSetupDialog by rememberSaveable { mutableStateOf(false) }
    var showAlertsOverlay by rememberSaveable { mutableStateOf(false) }
    var showSoundOverlay by rememberSaveable { mutableStateOf(false) }
    var showPhoneSetupOverlay by rememberSaveable { mutableStateOf(false) }
    var showCleanSetupOverlay by rememberSaveable { mutableStateOf(false) }
    var showToolsOverlay by rememberSaveable { mutableStateOf(false) }
    var isEditingRelayUrl by rememberSaveable { mutableStateOf(false) }
    var relayUrlInput by rememberSaveable(relayUrl) { mutableStateOf(relayUrl) }
    var followMode by rememberSaveable { mutableStateOf(MapFollowMode.AUTO) }
    // Every control that unpairs asks first; only the dialog's Unpair button calls onReset.
    var showUnpairConfirm by rememberSaveable { mutableStateOf(false) }
    val requestUnpair = { showUnpairConfirm = true }

    if (showUnpairConfirm) {
        AlertDialog(
            onDismissRequest = { showUnpairConfirm = false },
            title = { Text("Unpair from tracker $trackerId?") },
            text = { Text("The Monitor stops receiving this tracker until you pair again. Stored trail rows are kept.") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnpairConfirm = false
                        onReset()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.jd.core.ui.StatusRed)
                ) {
                    Text("Unpair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAlertsOverlay) {
        com.jd.core.ui.AlertManagementOverlay(
            isTracker = false,
            draftAlertSettings = alertSettings,
            onUpdateAlertSettings = onUpdateAlertSettings,
            onClose = { showAlertsOverlay = false }
        )
        return
    }

    if (showSoundOverlay) {
        com.jd.core.ui.AlarmSoundOverlay(
            isTracker = false,
            draftAlertSettings = alertSettings,
            selectedSirenType = sirenType,
            isSirenPlaying = isSirenPlaying,
            onUpdateAlertSettings = onUpdateAlertSettings,
            onUpdateSirenType = onUpdateSirenType,
            onUpdateAlarmVolume = onUpdateAlarmVolume,
            onTestSiren = onTestSiren,
            onClose = { showSoundOverlay = false }
        )
        return
    }

    if (showPhoneSetupOverlay) {
        com.jd.core.ui.PhoneSetupOverlay(
            isTracker = false,
            homePointsCount = 0,
            onOpenAppInfo = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            },
            onOpenBatterySettings = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            },
            onOpenOverlayPermission = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            },
            onOpenExactAlarmPermission = { },
            onOpenDndSettings = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                context.startActivity(intent)
            },
            onOpenFullScreenIntentSettings = {
                val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, android.net.Uri.parse("package:${context.packageName}"))
                } else {
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                }
                context.startActivity(intent)
            },
            onOpenDiagnostics = { showToolsOverlay = true },
            onTestAlarm = onTestSiren,
            onRefreshPermissions = { },
            onOpenMap = { showPhoneSetupOverlay = false },
            onClose = { showPhoneSetupOverlay = false }
        )
        return
    }

    if (showCleanSetupOverlay) {
        com.jd.core.ui.CleanSetupOverlay(
            isTracker = false,
            onClearHomePoints = requestUnpair,
            onResetStats = requestUnpair,
            onFullInit = requestUnpair,
            onClose = { showCleanSetupOverlay = false }
        )
        return
    }

    if (showToolsOverlay) {
        com.jd.core.ui.ToolsOverlay(
            onTriggerBurst = { },
            onPruneDatabase = { },
            onTriggerTestAlarm = { },
            onResetWatchdog = { },
            onClose = { showToolsOverlay = false }
        )
        return
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(WORLD_ZOOM)
        }
    }
    var hasCentered by remember(mapView) { mutableStateOf(false) }
    val sensorTrail = remember(trail) { knownPositions(trail) }
    val sensorPosition = sensorTrail.firstOrNull()

    // Automatic Centering & Connection Logic
    LaunchedEffect(remoteHudState.isPeerActive, trail, followMode, localPosition) {
        mapView.overlays.clear()
        if (sensorTrail.isNotEmpty()) {
            val points = sensorTrail.map { GeoPoint(it.lat, it.lng) }
            val polyline = Polyline().apply {
                setPoints(points)
                outlinePaint.color = android.graphics.Color.BLUE
                outlinePaint.strokeWidth = 5f
            }
            mapView.overlays.add(polyline)
            
            val latest = sensorTrail.first()
            val marker = Marker(mapView).apply {
                position = GeoPoint(latest.lat, latest.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Tracker Location"
            }
            mapView.overlays.add(marker)
        }

        resolveMapCenter(followMode, remoteHudState.isPeerActive, sensorPosition, localPosition)?.let {
            if (!hasCentered) {
                mapView.controller.setZoom(DEVICE_ZOOM)
                hasCentered = true
            }
            mapView.controller.setCenter(GeoPoint(it.lat, it.lng))
        }
        mapView.invalidate()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Menu Upper Line (Header Bar with Setup, Settings, and Actions)
                MonitorMenuBar(
                    monitorId = monitorId,
                    trackerId = trackerId,
                    onSetup = { showSetupDialog = true },
                    onUnpair = requestUnpair,
                    onTools = { showToolsOverlay = true },
                    onMap = { followMode = MapFollowMode.AUTO }
                )
                // Line 1: Local Device Status
                HudStatusBar(hudState = localHudState, compact = true)
                // Line 2: Sensor / Tracker Status
                HudStatusBar(hudState = remoteHudState, compact = true)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
            
            if (sensorTrail.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Waiting for Telemetry...", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }

            // Map Settings Wheel (Toggle Button)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 12.dp)
            ) {
                MapSettingsToggle(
                    isMapButtonsVisible = isMapButtonsVisible,
                    onToggle = { isMapButtonsVisible = !isMapButtonsVisible }
                )
            }

            // Map Control Buttons (Two Centering Buttons: SENSOR and LOCAL)
            if (isMapButtonsVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MapToolButton(label = "IN", symbol = "+", onClick = { mapView.controller.zoomIn() }, iconColor = Color(0xFF800080))
                        MapToolButton(label = "OUT", symbol = "-", onClick = { mapView.controller.zoomOut() }, iconColor = Color(0xFF800080))
                        MapToolButton(icon = Icons.Default.Place, label = "SENSOR", onClick = {
                            followMode = MapFollowMode.TRACKER
                            sensorPosition?.let { mapView.controller.setCenter(GeoPoint(it.lat, it.lng)) }
                        }, iconColor = Color(0xFF10B981))
                        MapToolButton(icon = Icons.Default.Person, label = "LOCAL", onClick = {
                            followMode = MapFollowMode.VIEWER
                            localPosition?.let { mapView.controller.setCenter(GeoPoint(it.lat, it.lng)) }
                        }, iconColor = Color(0xFF06B6D4))
                    }
                }
            }
        }
    }

    if (showSetupDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSetupDialog = false 
                isEditingRelayUrl = false
            },
            title = { Text("Monitor Setup & Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tracker ID: $trackerId")
                    Text("Monitor ID: $monitorId")
                    Text("Relay URL: $relayUrl")
                    Text("App Version: ${getAppVersionName(LocalContext.current)}")
                    Text(localHudState.relayStatusText)

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showSetupDialog = false
                                showAlertsOverlay = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ALERTS", fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                showSetupDialog = false
                                showSoundOverlay = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SIREN", fontSize = 10.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showSetupDialog = false
                                showPhoneSetupOverlay = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PHONE", fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                showSetupDialog = false
                                showCleanSetupOverlay = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = com.jd.core.ui.StatusRed)
                        ) {
                            Text("CLEAN", fontSize = 10.sp)
                        }
                    }

                    if (isEditingRelayUrl) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = relayUrlInput,
                            onValueChange = { relayUrlInput = it },
                            label = { Text("Edit Relay URL") },
                            isError = !RelayConfig.isValid(relayUrlInput),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isEditingRelayUrl) {
                        Button(
                            onClick = {
                                onUpdateRelayUrl(relayUrlInput.trim())
                                isEditingRelayUrl = false
                            },
                            enabled = RelayConfig.isValid(relayUrlInput)
                        ) {
                            Text("Save")
                        }
                    } else {
                        TextButton(onClick = { isEditingRelayUrl = true }) {
                            Text("Edit Relay URL")
                        }
                        Button(onClick = { showSetupDialog = false }) {
                            Text("Close")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun MonitorMenuBar(
    monitorId: String,
    trackerId: String,
    onSetup: () -> Unit,
    onUnpair: () -> Unit,
    onTools: () -> Unit,
    onMap: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(onClick = onSetup, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Build, contentDescription = "Setup", tint = Color(0xFF06B6D4), modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onUnpair, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings / Re-pair", tint = Color.White, modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onTools, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Tools", tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onMap, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Place, contentDescription = "Map", tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Events", tint = Color(0xFFF43F5E), modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Star, contentDescription = "Graphs", tint = Color(0xFF8B5CF6), modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onSetup, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFF3B82F6), modifier = Modifier.size(15.dp))
                }
            }
            Text(
                text = "MONITOR: ${monitorId.take(6).uppercase()} -> TRK:${trackerId.take(6).uppercase()}",
                color = Color(0xFF06B6D4),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onUnpair, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun MapSettingsToggle(isMapButtonsVisible: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val purple = Color(0xFF800080)
    val backgroundColor = if (isMapButtonsVisible) purple else Color.White
    val contentColor = if (isMapButtonsVisible) Color.White else purple

    Box(
        modifier = modifier
            .size(44.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(2.dp, purple, RoundedCornerShape(8.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isMapButtonsVisible) Icons.Default.Close else Icons.Default.Settings,
            contentDescription = "Toggle Map Controls",
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun MapToolButton(icon: ImageVector? = null, symbol: String? = null, label: String, onClick: () -> Unit, iconColor: Color, containerColor: Color = Color.White) {
    val prp = Color(0xFF800080)
    CappedFontScale {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(containerColor, RoundedCornerShape(8.dp))
                .border(1.dp, prp, RoundedCornerShape(8.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (symbol != null) Text(symbol, color = iconColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.height(26.dp))
                else if (icon != null) Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
                Text(label, color = if (containerColor != Color.White && containerColor != Color.Transparent) Color.White else prp, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
