package com.jd.app.monitor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.RelayConfig
import com.jd.core.network.ForensicIdentityManager
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Polygon
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
                // Back never closes the Monitor: screens deeper in register their own handlers (composed later,
                // so they win); at the top level the app only goes to the background.
                BackHandler { moveTaskToBack(true) }
                LaunchedEffect(Unit) {
                    identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_MONITOR)
                }
                val pairing by viewModel.pairing.collectAsStateWithLifecycle()
                val trackerId = pairing?.trackerId
                val monitorId by viewModel.selfId.collectAsStateWithLifecycle(initialValue = "LOADING...")
                val relayUrl by viewModel.relayUrl.collectAsStateWithLifecycle()
                val trail by viewModel.trail.collectAsStateWithLifecycle()
                val localTrail by viewModel.localTrail.collectAsStateWithLifecycle()
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
                val geofence by viewModel.geofence.collectAsStateWithLifecycle()

                LaunchedEffect(pairing) {
                    val serviceIntent = Intent(context, MonitorSessionService::class.java)
                    when (sessionCommandFor(pairing)) {
                        SessionCommand.START ->
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        SessionCommand.STOP -> context.stopService(serviceIntent)
                        null -> Unit
                    }
                }

                if (pairing == null) {
                    // The stored pairing is not read yet; drawing the pairing screen here would flash it.
                } else if (trackerId != null && alarmBook.pending.isNotEmpty()) {
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
                        trackerId = trackerId,
                        monitorId = monitorId,
                        relayUrl = relayUrl,
                        trail = trail,
                        localTrail = localTrail,
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
                        onUpdateRelayUrl = { url -> viewModel.updateRelayUrl(url) },
                        onClearHomePoints = viewModel::clearHomePoints,
                        geofence = geofence,
                        onAddHomePoint = viewModel::addHomePoint,
                        onRemoveHomePoint = viewModel::removeHomePoint,
                        onSetGeofenceRadius = viewModel::setGeofenceRadius,
                        onResetStats = { viewModel.resetOperationalStatistics() },
                        onFullInit = {
                            viewModel.triggerFullSystemReinitialization()
                            context.stopService(Intent(context, MonitorSessionService::class.java))
                        }
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
    localTrail: List<TrailEntity>,
    alertSettings: AlertSettings,
    sirenType: String,
    isSirenPlaying: Boolean,
    onUpdateAlertSettings: (AlertSettings) -> Unit,
    onUpdateSirenType: (String) -> Unit,
    onUpdateAlarmVolume: (Float) -> Unit,
    onTestSiren: () -> Unit,
    onReset: () -> Unit,
    onUpdateRelayUrl: (String) -> Unit,
    onClearHomePoints: () -> Unit = {},
    geofence: GeofenceConfig = GeofenceConfig(),
    onAddHomePoint: (lat: Double, lng: Double) -> Unit = { _, _ -> },
    onRemoveHomePoint: (index: Int) -> Unit = {},
    onSetGeofenceRadius: (meters: Double) -> Unit = {},
    onResetStats: () -> Unit = {},
    onFullInit: () -> Unit = {}
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
    // Every control that unpairs asks first; only the dialog's confirm button runs the pending action
    // (Chapter 127; full re-initialization also unpairs, ISSUE-NOV8.2-O46).
    var pendingUnpair by rememberSaveable { mutableStateOf<UnpairAction?>(null) }
    val requestUnpair = { pendingUnpair = UnpairAction.UNPAIR }
    // Home Point editing (ISSUE-NOV8.13-O51): a map tap adds a point, a tapped point asks before it is removed.
    var isEditingHome by rememberSaveable { mutableStateOf(false) }
    var pendingHomeRemoval by rememberSaveable { mutableStateOf<Int?>(null) }
    val currentOnAddHomePoint by rememberUpdatedState(onAddHomePoint)

    pendingHomeRemoval?.let { index ->
        AlertDialog(
            onDismissRequest = { pendingHomeRemoval = null },
            title = { Text("Remove Home Point ${index + 1}?") },
            text = { Text("The tracker stops treating this area as home once it receives the change.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingHomeRemoval = null
                        onRemoveHomePoint(index)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.jd.core.ui.StatusRed)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingHomeRemoval = null }) { Text("Cancel") } }
        )
    }

    pendingUnpair?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingUnpair = null },
            title = { Text("Unpair from tracker $trackerId?") },
            text = { Text(action.consequence) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingUnpair = null
                        when (action) {
                            UnpairAction.UNPAIR -> onReset()
                            UnpairAction.FULL_REINITIALIZATION -> onFullInit()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.jd.core.ui.StatusRed)
                ) {
                    Text(action.confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnpair = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAlertsOverlay) {
        BackHandler { showAlertsOverlay = false }
        com.jd.core.ui.AlertManagementOverlay(
            isTracker = false,
            draftAlertSettings = alertSettings,
            onUpdateAlertSettings = onUpdateAlertSettings,
            onClose = { showAlertsOverlay = false }
        )
        return
    }

    if (showSoundOverlay) {
        BackHandler { showSoundOverlay = false }
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
        BackHandler { showPhoneSetupOverlay = false }
        com.jd.core.ui.PhoneSetupOverlay(
            isTracker = false,
            homePointsCount = geofence.points.size,
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
        BackHandler { showCleanSetupOverlay = false }
        com.jd.core.ui.CleanSetupOverlay(
            isTracker = false,
            onClearHomePoints = onClearHomePoints,
            onResetStats = onResetStats,
            onFullInit = { pendingUnpair = UnpairAction.FULL_REINITIALIZATION },
            onClose = { showCleanSetupOverlay = false }
        )
        return
    }

    if (showToolsOverlay) {
        BackHandler { showToolsOverlay = false }
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
            // The map has its own IN/OUT buttons; osmdroid's would cover the Home Point editor (ISSUE-NOV8.13-O51).
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(WORLD_ZOOM)
        }
    }
    var hasCentered by remember(mapView) { mutableStateOf(false) }
    val sensorTrail = remember(trail) { knownPositions(trail) }
    val sensorPosition = sensorTrail.firstOrNull()
    val localPath = remember(localTrail) { knownPositions(localTrail) }
    val breaches = remember(trail) { geofenceBreaches(trail) }

    // Automatic Centering & Connection Logic
    LaunchedEffect(remoteHudState.isPeerActive, trail, localTrail, followMode, localPosition, geofence, isEditingHome) {
        mapView.overlays.clear()

        // Bottom overlay: taps that no marker takes add a Home Point while editing.
        mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (!isEditingHome) return false
                currentOnAddHomePoint(p.latitude, p.longitude)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }))

        geofence.points.forEachIndexed { index, home ->
            val center = GeoPoint(home.lat, home.lng)
            mapView.overlays.add(Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(center, geofence.radiusMeters)
                fillPaint.color = android.graphics.Color.argb(40, 16, 185, 129)
                outlinePaint.color = android.graphics.Color.rgb(16, 185, 129)
                outlinePaint.strokeWidth = 3f
                setOnClickListener { _, _, _ -> false } // Taps inside the circle still reach the map.
            })
            mapView.overlays.add(Marker(mapView).apply {
                position = center
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = homeMarkerIcon(context)
                title = "Home ${index + 1}"
                setOnMarkerClickListener { marker, _ ->
                    if (isEditingHome) pendingHomeRemoval = index else marker.showInfoWindow()
                    true
                }
            })
        }

        // OUT markers: where the tracker reported leaving every Home Point.
        breaches.forEach { breach ->
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(breach.lat, breach.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = breachMarkerIcon(context)
                title = "Geofence breach"
                snippet = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(breach.timestamp))
            })
        }

        if (localPath.isNotEmpty()) {
            val localPoints = localPath.map { GeoPoint(it.lat, it.lng) }
            val localPolyline = Polyline().apply {
                setPoints(localPoints)
                outlinePaint.color = android.graphics.Color.parseColor("#06B6D4")
                outlinePaint.strokeWidth = 4f
            }
            mapView.overlays.add(localPolyline)
        }

        localPosition?.let { pos ->
            val localMarker = Marker(mapView).apply {
                position = GeoPoint(pos.lat, pos.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Local Device"
            }
            mapView.overlays.add(localMarker)
        }

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
                        MapToolButton(
                            icon = Icons.Default.Home,
                            label = "HOME",
                            onClick = { isEditingHome = !isEditingHome },
                            iconColor = if (isEditingHome) Color.White else Color(0xFF10B981),
                            containerColor = if (isEditingHome) Color(0xFF10B981) else Color.White
                        )
                    }
                }
            }

            if (isEditingHome) {
                BackHandler { isEditingHome = false }
                HomePointEditor(
                    geofence = geofence,
                    onSetRadius = onSetGeofenceRadius,
                    onDone = { isEditingHome = false },
                    // Clear of the map control column, which sits at the start edge.
                    modifier = Modifier.align(Alignment.BottomCenter).padding(start = 72.dp, end = 8.dp, bottom = 8.dp)
                )
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

/** Home Point editing panel: how to edit, the point count, and the geofence radius (ISSUE-NOV8.13-O51). */
@Composable
fun HomePointEditor(
    geofence: GeofenceConfig,
    onSetRadius: (Double) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The slider moves locally and stores once released, so dragging does not send a geofence per step.
    var radius by remember(geofence.radiusMeters) { mutableFloatStateOf(geofence.radiusMeters.toFloat()) }
    // Compact, so the map stays usable for tapping points and the map controls stay uncovered.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.85f),
        contentColor = Color.White
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "HOME: ${geofence.points.size} | ${radius.toInt()} m",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDone) { Text("DONE") }
            }
            Text("Tap the map to add, tap a home to remove", fontSize = 11.sp, color = Color.LightGray)
            Slider(
                value = radius,
                onValueChange = { radius = (it / RADIUS_STEP_METERS).toInt() * RADIUS_STEP_METERS },
                onValueChangeFinished = { onSetRadius(radius.toDouble()) },
                valueRange = GeofenceConfig.MIN_RADIUS_METERS.toFloat()..GeofenceConfig.MAX_RADIUS_METERS.toFloat(),
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

private const val RADIUS_STEP_METERS = 10f

/** A red dot, so an OUT marker cannot be mistaken for the tracker or a Home Point marker. */
private fun breachMarkerIcon(context: android.content.Context): android.graphics.drawable.Drawable =
    android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.OvalShape()).apply {
        val sizePx = (18 * context.resources.displayMetrics.density).toInt()
        paint.color = android.graphics.Color.RED
        intrinsicWidth = sizePx
        intrinsicHeight = sizePx
    }

/** A green square, so a Home Point cannot be mistaken for the tracker's pin or an OUT dot. */
private fun homeMarkerIcon(context: android.content.Context): android.graphics.drawable.Drawable =
    android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.RectShape()).apply {
        val sizePx = (20 * context.resources.displayMetrics.density).toInt()
        paint.color = android.graphics.Color.rgb(16, 185, 129)
        intrinsicWidth = sizePx
        intrinsicHeight = sizePx
    }


/** A Monitor action that unpairs the tracker, and what its confirmation dialog tells the user before it runs. */
internal enum class UnpairAction(val consequence: String, val confirmLabel: String) {
    UNPAIR(
        consequence = "The Monitor stops receiving this tracker until you pair again. Stored trail rows are kept.",
        confirmLabel = "Unpair"
    ),
    FULL_REINITIALIZATION(
        consequence = "Full re-initialization unpairs this tracker, restores every alert setting, the siren, and the relay URL to their defaults, and deletes all stored trail rows.",
        confirmLabel = "Re-initialize"
    )
}
