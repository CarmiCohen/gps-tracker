package com.jd.app.monitor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jd.core.ui.RevolutionTheme
import com.jd.core.shared.ForensicIdentity
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker

/**
 * MonitorActivity: The primary UI for the Monitor (Watcher).
 * Hardened for Revol31.0: Integrated osmdroid for trail visualization and jitter audit foundation.
 */
@AndroidEntryPoint
class MonitorActivity : ComponentActivity() {

    private val viewModel: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Osmdroid Configuration
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            RevolutionTheme {
                val trackerId by viewModel.targetId.collectAsStateWithLifecycle(initialValue = null)
                val monitorId by viewModel.selfId.collectAsStateWithLifecycle(initialValue = "LOADING...")
                val trail by viewModel.trail.collectAsStateWithLifecycle()
                val context = LocalContext.current

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

                if (trackerId == null) {
                    PairingWizard(onPair = { id -> viewModel.setTargetId(id) })
                } else {
                    MonitorScreen(
                        trackerId = trackerId!!,
                        monitorId = monitorId,
                        trail = trail,
                        onReset = { 
                            viewModel.clearTargetId()
                            context.stopService(Intent(context, MonitorSessionService::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PairingWizard(onPair: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

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
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Pair Device")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Version: ${getAppVersionName(LocalContext.current)}", fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp), color = androidx.compose.ui.graphics.Color.Gray)
    }
}

fun getAppVersionName(context: android.content.Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "Nov3.21_PLATINUM"
    } catch (_: Exception) {
        "Nov3.21_PLATINUM"
    }
}

@Composable
fun MonitorScreen(
    trackerId: String, 
    monitorId: String, 
    trail: List<TrailEntity>,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(18.0)
        }
    }

    // Update trail on map
    LaunchedEffect(trail) {
        mapView.overlays.clear()
        if (trail.isNotEmpty()) {
            val points = trail.map { GeoPoint(it.lat, it.lng) }
            val polyline = Polyline().apply {
                setPoints(points)
                outlinePaint.color = android.graphics.Color.BLUE
                outlinePaint.strokeWidth = 5f
            }
            mapView.overlays.add(polyline)
            
            // Add marker for the latest position
            val latest = trail.first()
            val marker = Marker(mapView).apply {
                position = GeoPoint(latest.lat, latest.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Latest Update"
            }
            mapView.overlays.add(marker)
            
            // Center map on latest point if it's the first time
            if (mapView.mapCenter.latitude == 0.0) {
                mapView.controller.setCenter(GeoPoint(latest.lat, latest.lng))
            }
        }
        mapView.invalidate()
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onReset() }
                        )
                    }
            ) {
                Column {
                    Text(text = "Tracking: ${trackerId.take(8)}...", style = MaterialTheme.typography.headlineMedium)
                    Text(text = "My ID: ${monitorId.take(8)}... | Version: ${getAppVersionName(LocalContext.current)}", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.Gray)
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
            
            if (trail.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Waiting for Telemetry...")
                }
            }
        }
    }
}
