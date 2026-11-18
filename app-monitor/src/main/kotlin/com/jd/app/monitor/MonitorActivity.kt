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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.jd.core.ui.EVENTS_STATE_KEY
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
import com.jd.core.shared.EventEntry
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.RelayConfig
import com.jd.core.shared.SensorSetupReport
import com.jd.core.network.ForensicIdentityManager
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.ScaleBarOverlay
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

                // The settings file (ISSUE-NOV10.8-O80): the system picker needs no storage permission.
                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument(SETTINGS_FILE_MIME_TYPE)
                ) { uri -> uri?.let(viewModel::exportSettings) }
                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> uri?.let(viewModel::importSettings) }
                val exportSettings = { exportLauncher.launch(viewModel.exportFileName()) }
                // The map's trail file (ISSUE-NOV17.7-O107), through the same system picker.
                val saveTrailsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument(SETTINGS_FILE_MIME_TYPE)
                ) { uri -> uri?.let(viewModel::saveTrails) }
                val loadTrailsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> uri?.let(viewModel::loadTrails) }
                val importSettings = { importLauncher.launch(arrayOf("*/*")) } // The content is checked on reading.
                LaunchedEffect(Unit) {
                    viewModel.transferMessages.collect { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() }
                }

                val alertSettings by viewModel.alertSettings.collectAsStateWithLifecycle()
                val sirenType by viewModel.sirenType.collectAsStateWithLifecycle()
                val isSirenPlaying by viewModel.isSirenPlaying.collectAsStateWithLifecycle()
                val alarmBook by viewModel.alarms.collectAsStateWithLifecycle()
                val geofence by viewModel.geofence.collectAsStateWithLifecycle()
                val sensorSetup by viewModel.sensorSetup.collectAsStateWithLifecycle()
                val events by viewModel.events.collectAsStateWithLifecycle()

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
                        onUpdateRelayUrl = { url -> viewModel.updateRelayUrl(url) },
                        onImportSettings = importSettings
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
                        sensorSetup = sensorSetup,
                        events = events,
                        onClearEvents = viewModel::clearEvents,
                        infoOverlay = { onClose -> MonitorInfoScreen(viewModel, onClose) },
                        graphsOverlay = { onClose -> MonitorGraphsScreen(viewModel, onClose) },
                        onShowGraphsAt = viewModel::showGraphsAt,
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
                        },
                        onExportSettings = exportSettings,
                        onImportSettings = importSettings,
                        onSaveTrails = { saveTrailsLauncher.launch(viewModel.suggestedTrailFileName()) },
                        onLoadTrails = { loadTrailsLauncher.launch(arrayOf("*/*")) }, // The content is checked on reading.
                        onClearTrails = viewModel::clearTrails,
                        onClose = { moveTaskToBack(true) }
                    )
                }
            }
        }
    }
}

/** The type the file picker gives the exported settings file. */
const val SETTINGS_FILE_MIME_TYPE = "application/json"

private const val WORLD_ZOOM = 3.0

/** The space between the map's control buttons. */
private val MAP_BUTTON_GAP = 8.dp

/** The geofence circle's alphas (0-255): a faint fill and a half-transparent edge (ISSUE-NOV17.7-O108; were 40 and 255). */
internal const val FENCE_FILL_ALPHA = 20
internal const val FENCE_OUTLINE_ALPHA = 160

/** The scale bar's distance from the map's right and bottom edges, in pixels. */
private const val SCALE_BAR_OFFSET_PX = 24
private const val DEVICE_ZOOM = 18.0

@Composable
fun PairingWizard(
    relayUrl: String,
    onPair: (String) -> Unit,
    onUpdateRelayUrl: (String) -> Unit,
    /** A reinstalled Monitor opens here, so the settings file is imported from here too (ISSUE-NOV10.8-O80). */
    onImportSettings: () -> Unit = {}
) {
    var text by rememberSaveable { mutableStateOf(ForensicIdentity.DEFAULT_TRACKER_ID) }
    var confirmImport by rememberSaveable { mutableStateOf(false) }
    if (confirmImport) {
        com.jd.core.ui.ImportSettingsDialog(
            onConfirm = {
                confirmImport = false
                onImportSettings()
            },
            onDismiss = { confirmImport = false }
        )
    }
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

        OutlinedButton(
            onClick = { confirmImport = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Import Settings from File")
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
    onFullInit: () -> Unit = {},
    /** The settings file's file pickers (ISSUE-NOV10.8-O80). */
    onExportSettings: () -> Unit = {},
    onImportSettings: () -> Unit = {},
    /** The map's trail file pickers and clear (ISSUE-NOV17.7-O107). */
    onSaveTrails: () -> Unit = {},
    onLoadTrails: () -> Unit = {},
    onClearTrails: () -> Unit = {},
    onClose: () -> Unit = {},
    sensorSetup: SensorSetupReport? = null,
    events: List<EventEntry> = emptyList(),
    onClearEvents: () -> Unit = {},
    /** The detailed Info screen (ISSUE-NOV10.8-O84 part 2), given its close action. */
    infoOverlay: @Composable (onClose: () -> Unit) -> Unit = {},
    /** The Graphs screen (ISSUE-NOV10.8-O84 part 3), given its close action. */
    graphsOverlay: @Composable (onClose: () -> Unit) -> Unit = {},
    /** Sets the time the Graphs open around: an event's, or null for now (ISSUE-NOV15.6-O100 part 4). */
    onShowGraphsAt: (timeMs: Long?) -> Unit = {}
) {
    val context = LocalContext.current
    var isMapButtonsVisible by rememberSaveable { mutableStateOf(true) }
    // The FENCE button shows or hides the geofence circles (ISSUE-NOV17.7-O108); editing Home Points always shows them.
    var isFenceVisible by rememberSaveable { mutableStateOf(true) }
    var showSetupDialog by rememberSaveable { mutableStateOf(false) }
    // What the Sensor phone lacks, as the Sensor reports it (ISSUE-NOV10.8-O83 part 2).
    val setupWarnings = remember(sensorSetup, alertSettings.acousticAlert) { sensorSetupWarnings(sensorSetup, alertSettings) }
    var showSetupWarnings by rememberSaveable { mutableStateOf(false) }
    var showAlertsOverlay by rememberSaveable { mutableStateOf(false) }
    var showEventsOverlay by rememberSaveable { mutableStateOf(false) }
    var showInfoOverlay by rememberSaveable { mutableStateOf(false) }
    var showGraphsOverlay by rememberSaveable { mutableStateOf(false) }
    // The Events list keeps its period and selection while the Graphs of an event cover it (ISSUE-NOV15.6-O100 part 4);
    // closing the list forgets them.
    val overlayStates = rememberSaveableStateHolder()
    val closeEvents = {
        showEventsOverlay = false
        overlayStates.removeState(EVENTS_STATE_KEY)
    }
    var showSoundOverlay by rememberSaveable { mutableStateOf(false) }
    var showPhoneSetupOverlay by rememberSaveable { mutableStateOf(false) }
    var showCleanSetupOverlay by rememberSaveable { mutableStateOf(false) }
    var isEditingRelayUrl by rememberSaveable { mutableStateOf(false) }
    var relayUrlInput by rememberSaveable(relayUrl) { mutableStateOf(relayUrl) }
    var followMode by rememberSaveable { mutableStateOf(MapFollowMode.AUTO) }
    // Every control that unpairs asks first; only the dialog's confirm button runs the pending action
    // (Chapter 127; full re-initialization also unpairs, ISSUE-NOV8.2-O46).
    var pendingUnpair by rememberSaveable { mutableStateOf<UnpairAction?>(null) }
    val requestUnpair = { pendingUnpair = UnpairAction.UNPAIR }
    // Home Point editing (ISSUE-NOV8.13-O51): a map tap adds a point; a tapped point or its list row (ISSUE-NOV10.8-O85)
    // asks before it is removed.
    var isEditingHome by rememberSaveable { mutableStateOf(false) }
    var pendingHomeRemoval by rememberSaveable { mutableStateOf<Int?>(null) }
    val currentOnAddHomePoint by rememberUpdatedState(onAddHomePoint)
    var isClearTrailsPending by rememberSaveable { mutableStateOf(false) }

    if (isClearTrailsPending) {
        AlertDialog(
            onDismissRequest = { isClearTrailsPending = false },
            title = { Text("Clear the trails?") },
            text = { Text("The Sensor's and this phone's trails are deleted from this phone. SAVE them first to keep a copy.") },
            confirmButton = {
                Button(
                    onClick = {
                        isClearTrailsPending = false
                        onClearTrails()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.jd.core.ui.StatusRed)
                ) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { isClearTrailsPending = false }) { Text("Cancel") } }
        )
    }

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

    if (showSetupWarnings) {
        // A list that became empty closes itself, and does not reopen with the next warning.
        if (setupWarnings.isEmpty()) SideEffect { showSetupWarnings = false }
        else SensorSetupWarningDialog(setupWarnings, onClose = { showSetupWarnings = false })
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

    if (showInfoOverlay) {
        BackHandler { showInfoOverlay = false }
        infoOverlay { showInfoOverlay = false }
        return
    }

    if (showGraphsOverlay) {
        BackHandler { showGraphsOverlay = false }
        graphsOverlay { showGraphsOverlay = false }
        return
    }

    if (showEventsOverlay) {
        BackHandler(onBack = closeEvents)
        overlayStates.SaveableStateProvider(EVENTS_STATE_KEY) {
            com.jd.core.ui.EventLogOverlay(
                events = events,
                showSource = true,
                onClear = onClearEvents,
                onClose = closeEvents,
                onShowGraphs = { timeMs ->
                    onShowGraphsAt(timeMs)
                    showGraphsOverlay = true
                }
            )
        }
        return
    }

    if (showAlertsOverlay) {
        BackHandler { showAlertsOverlay = false }
        com.jd.core.ui.AlertManagementOverlay(
            draftAlertSettings = alertSettings,
            onUpdateAlertSettings = onUpdateAlertSettings,
            onClose = { showAlertsOverlay = false }
        )
        return
    }

    if (showSoundOverlay) {
        BackHandler { showSoundOverlay = false }
        com.jd.core.ui.AlarmSoundOverlay(
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
            onTestAlarm = onTestSiren,
            onOpenMap = { isEditingHome = true },
            onClose = { showPhoneSetupOverlay = false }
        )
        return
    }

    if (showCleanSetupOverlay) {
        BackHandler { showCleanSetupOverlay = false }
        com.jd.core.ui.CleanSetupOverlay(
            onClearHomePoints = onClearHomePoints,
            onExportSettings = onExportSettings,
            onImportSettings = onImportSettings,
            onResetStats = onResetStats,
            onFullInit = { pendingUnpair = UnpairAction.FULL_REINITIALIZATION },
            onClose = { showCleanSetupOverlay = false }
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
    // A metric scale at the bottom right (ISSUE-NOV17.0-O101, user decision 2026-11-17); the overlays are rebuilt
    // below, so it is added again each time.
    val scaleBar = remember(mapView) {
        ScaleBarOverlay(mapView).apply {
            setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.metric)
            setAlignBottom(true)
            setAlignRight(true)
            setScaleBarOffset(SCALE_BAR_OFFSET_PX, SCALE_BAR_OFFSET_PX)
        }
    }
    var hasCentered by remember(mapView) { mutableStateOf(false) }
    val sensorTrail = remember(trail) { knownPositions(trail) }
    val sensorPosition = sensorTrail.firstOrNull()
    val localPath = remember(localTrail) { knownPositions(localTrail) }
    val breaches = remember(trail) { geofenceBreaches(trail) }

    // Automatic Centering & Connection Logic
    LaunchedEffect(remoteHudState.isPeerActive, trail, localTrail, followMode, localPosition, geofence, isEditingHome, isFenceVisible) {
        mapView.overlays.clear()
        mapView.overlays.add(scaleBar)

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
            // FENCE hides only the circle; the Home marker stays (ISSUE-NOV17.7-O108).
            if (isFenceVisible || isEditingHome) mapView.overlays.add(Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(center, geofence.radiusMeters)
                // More transparent, so the map under the fence stays readable (ISSUE-NOV17.7-O108).
                fillPaint.color = android.graphics.Color.argb(FENCE_FILL_ALPHA, 16, 185, 129)
                outlinePaint.color = android.graphics.Color.argb(FENCE_OUTLINE_ALPHA, 16, 185, 129)
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
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = deviceMarkerIcon(context, MONITOR_MARKER_COLOR)
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
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = deviceMarkerIcon(context, SENSOR_MARKER_COLOR)
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
                    onMap = { followMode = MapFollowMode.AUTO },
                    onEvents = { showEventsOverlay = true },
                    onInfo = { showInfoOverlay = true },
                    onGraphs = {
                        onShowGraphsAt(null)
                        showGraphsOverlay = true
                    },
                    onClose = onClose
                )
                // Line 1: Local Device Status
                HudStatusBar(hudState = localHudState, compact = true)
                // Line 2: Sensor / Tracker Status
                HudStatusBar(hudState = remoteHudState, compact = true)
                if (setupWarnings.isNotEmpty()) {
                    SensorSetupWarningLine(setupWarnings.size, onClick = { showSetupWarnings = true })
                }
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
            
            // The version at the bottom left, opposite the scale (ISSUE-NOV17.0-O101); left also on a right-to-left
            // phone, where the osmdroid scale stays at the right. The name alone, without "Version:" (ISSUE-NOV18.6-O116).
            Text(
                text = getAppVersionName(LocalContext.current),
                fontSize = 10.sp,
                color = Color.DarkGray,
                modifier = Modifier
                    .align(androidx.compose.ui.AbsoluteAlignment.BottomLeft)
                    .absolutePadding(left = 8.dp, bottom = 8.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
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
                    // Two buttons a row, so the trail buttons (ISSUE-NOV17.7-O107) also fit a landscape map.
                    Column(verticalArrangement = Arrangement.spacedBy(MAP_BUTTON_GAP)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(MAP_BUTTON_GAP)) {
                            MapToolButton(label = "IN", symbol = "+", onClick = { mapView.controller.zoomIn() }, iconColor = Color(0xFF800080))
                            MapToolButton(label = "OUT", symbol = "-", onClick = { mapView.controller.zoomOut() }, iconColor = Color(0xFF800080))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(MAP_BUTTON_GAP)) {
                            MapToolButton(icon = Icons.Default.Place, label = "SENSOR", onClick = {
                                followMode = MapFollowMode.TRACKER
                                sensorPosition?.let { mapView.controller.setCenter(GeoPoint(it.lat, it.lng)) }
                            }, iconColor = Color(0xFF10B981))
                            MapToolButton(icon = Icons.Default.Person, label = "LOCAL", onClick = {
                                followMode = MapFollowMode.VIEWER
                                localPosition?.let { mapView.controller.setCenter(GeoPoint(it.lat, it.lng)) }
                            }, iconColor = Color(0xFF06B6D4))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(MAP_BUTTON_GAP)) {
                            MapToolButton(
                                icon = Icons.Default.Home,
                                label = "HOME",
                                onClick = { isEditingHome = !isEditingHome },
                                iconColor = if (isEditingHome) Color.White else Color(0xFF10B981),
                                containerColor = if (isEditingHome) Color(0xFF10B981) else Color.White
                            )
                            MapToolButton(icon = Icons.Default.Delete, label = "CLEAR", onClick = { isClearTrailsPending = true }, iconColor = Color(0xFFF43F5E))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(MAP_BUTTON_GAP)) {
                            MapToolButton(icon = TrailLoadIcon, label = "LOAD", onClick = onLoadTrails, iconColor = Color(0xFF10B981))
                            MapToolButton(icon = TrailSaveIcon, label = "SAVE", onClick = onSaveTrails, iconColor = Color(0xFF6366F1))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(MAP_BUTTON_GAP)) {
                            MapToolButton(
                                label = "FENCE",
                                symbol = "◯",
                                onClick = { isFenceVisible = !isFenceVisible },
                                iconColor = if (isFenceVisible) Color.White else Color(0xFF10B981),
                                containerColor = if (isFenceVisible) Color(0xFF10B981) else Color.White
                            )
                        }
                    }
                }
            }

            if (isEditingHome) {
                BackHandler { isEditingHome = false }
                HomePointEditor(
                    geofence = geofence,
                    onSetRadius = onSetGeofenceRadius,
                    onRemovePoint = { pendingHomeRemoval = it },
                    onDone = { isEditingHome = false },
                    // Clear of the map control columns, which sit at the start edge.
                    modifier = Modifier.align(Alignment.BottomCenter).padding(start = 124.dp, end = 8.dp, bottom = 8.dp)
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
            title = { Text("Monitor Setup") }, // The detailed Info has its own screen (ISSUE-NOV10.8-O84 part 2).
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tracker ID: $trackerId")
                    Text("Monitor ID: $monitorId")
                    Text("Relay URL: $relayUrl")
                    Text("App Version: ${getAppVersionName(LocalContext.current)}")
                    Text(localHudState.relayStatusText)
                    Text(sensorSetupStatusText(sensorSetup, setupWarnings.size))
                    Text(sensorBehaviourStatusText(sensorSetup, alertSettings.sensorRevision))

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
                    // Unpairing moved here from the menu's gear (ISSUE-NOV17.7-O109); it still asks first (ISSUE-NOV5.2-O15).
                    Button(
                        onClick = {
                            showSetupDialog = false
                            requestUnpair()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = com.jd.core.ui.StatusRed)
                    ) {
                        Text("UNPAIR", fontSize = 10.sp)
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
    onMap: () -> Unit,
    onEvents: () -> Unit,
    onInfo: () -> Unit,
    onGraphs: () -> Unit,
    onClose: () -> Unit
) {
    // Events since ISSUE-NOV10.8-O84 Nov15.4, the detailed Info since Nov15.5, the Graphs since Nov15.6. One setup icon: unpairing
    // is the Setup dialog's UNPAIR button, no longer a gear (ISSUE-NOV17.7-O109); the red X sends the app to the background (ISSUE-NOV10.8-O76).
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
                IconButton(onClick = onMap, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Place, contentDescription = "Map", tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onEvents, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Events", tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onInfo, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFF3B82F6), modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onGraphs, modifier = Modifier.size(28.dp)) {
                    Icon(com.jd.core.ui.GraphsIcon, contentDescription = "Graphs", tint = Color(0xFFA78BFA), modifier = Modifier.size(15.dp))
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
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
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

/**
 * Home Point editing panel: how to edit, the point count, the geofence radius (ISSUE-NOV8.13-O51), and a list of the
 * points with a remove button each (ISSUE-NOV10.8-O85). [onRemovePoint] only asks; the caller's dialog confirms.
 */
@Composable
fun HomePointEditor(
    geofence: GeofenceConfig,
    onSetRadius: (Double) -> Unit,
    onRemovePoint: (Int) -> Unit,
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
            Text("Tap the map to add; tap a home or its X to remove", fontSize = 11.sp, color = Color.LightGray)
            // At most about three rows high, scrolling beyond, so the map above stays free for tapping.
            Column(
                modifier = Modifier
                    .heightIn(max = 96.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                geofence.points.forEachIndexed { index, point ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(32.dp)) {
                        // Left to right always: in a right-to-left locale the bidi algorithm reorders the number
                        // and the coordinates (seen on the A15).
                        Text(
                            homePointLabel(index, point),
                            fontSize = 12.sp,
                            style = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemovePoint(index) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove Home Point ${index + 1}",
                                tint = com.jd.core.ui.StatusRed
                            )
                        }
                    }
                }
            }
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

/** A Home Point's list row text: its number, as on the map marker, and its coordinates to five decimals (about 1 m). */
internal fun homePointLabel(index: Int, point: com.jd.core.shared.HomePoint): String =
    String.format(java.util.Locale.US, "%d.  %.5f, %.5f", index + 1, point.lat, point.lng)

/** A red dot, so an OUT marker cannot be mistaken for the tracker or a Home Point marker. */
private fun breachMarkerIcon(context: android.content.Context): android.graphics.drawable.Drawable =
    android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.OvalShape()).apply {
        val sizePx = (18 * context.resources.displayMetrics.density).toInt()
        paint.color = android.graphics.Color.RED
        intrinsicWidth = sizePx
        intrinsicHeight = sizePx
    }

/** Device colors of the HUD (HudStatusBar): cyan for the Monitor, green for the Sensor (Nov17.6). */
internal const val MONITOR_MARKER_COLOR = 0xFF06B6D4.toInt()
internal const val SENSOR_MARKER_COLOR = 0xFF10B981.toInt()

/** A device's location: a dot in the device color inside a white ring with a dark edge, readable on any map tile. */
private fun deviceMarkerIcon(context: android.content.Context, color: Int): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (24 * density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val c = sizePx / 2f
    paint.color = android.graphics.Color.argb(160, 0, 0, 0)
    canvas.drawCircle(c, c, c, paint)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(c, c, c - 1.5f * density, paint)
    paint.color = color
    canvas.drawCircle(c, c, c - 4.5f * density, paint)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

/** The Home Point square's side: half the 20 dp it had up to Nov18.0 (ISSUE-NOV17.7-O104). */
internal const val HOME_MARKER_SIZE_DP = 10

/** A green square, so a Home Point cannot be mistaken for the tracker's dot or an OUT dot. */
internal fun homeMarkerIcon(context: android.content.Context): android.graphics.drawable.Drawable =
    android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.RectShape()).apply {
        val sizePx = (HOME_MARKER_SIZE_DP * context.resources.displayMetrics.density).toInt()
        paint.color = android.graphics.Color.rgb(16, 185, 129)
        intrinsicWidth = sizePx
        intrinsicHeight = sizePx
    }


/** A Monitor action that unpairs the tracker, and what its confirmation dialog tells the user before it runs. */
internal enum class UnpairAction(val consequence: String, val confirmLabel: String) {
    UNPAIR(
        consequence = "The Monitor stops receiving this tracker until you pair again. Stored trails are kept.",
        confirmLabel = "Unpair"
    ),
    FULL_REINITIALIZATION(
        consequence = "Full re-initialization unpairs this tracker, restores every alert setting, the siren, and the relay URL to their defaults, and deletes all stored trails.",
        confirmLabel = "Re-initialize"
    )
}
