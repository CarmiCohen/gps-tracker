package com.gps19.app

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.Overlay
import com.gps19.app.BuildConfig
import com.gps19.core.engine.*

/**
 * MapComponents: Shared map logic for Tracker and Viewer.
 * Oct.1.5:
 * - Issue #MAP-SOT-03: Implemented AnchorLockedBadge in AppMapContainer.
 * Sep.30.42:
 * - Issue #1390: Integrated CameraAction SharedFlow to handle imperative map commands.
 * Sep.28.6:
 * - Issue #1167 RESOLVED: Extracted osmdroid management and imperative coordination 
 *   into MapController. (R-ID 522).
 */

@Composable
fun AppMapContainer(
    state: MapViewState,
    cameraActions: kotlinx.coroutines.flow.SharedFlow<CameraAction>? = null,
    onEvent: (UiEvent) -> Unit,
    onClearTrails: () -> Unit,
    onSaveTrail: () -> Unit,
    onLoadTrail: () -> Unit
) {
    val isTrackerMode = state.appMode == "tracker"
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val toggleTopPadding = if (isLandscape) 12.dp else 110.dp

    val initialCenter = remember(state.trackerLat, state.trackerLng) {
        when {
            state.isTrackerValid -> GeoPoint(state.trackerLat, state.trackerLng)
            state.isViewerValid -> GeoPoint(state.viewerLat, state.viewerLng)
            else -> GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
        }
    }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMap(
            state = state,
            cameraActions = cameraActions,
            initialCenter = initialCenter,
            onTap = { onEvent(UiEvent.MapTap(it)) },
            onRemoveMarker = { if (!isTrackerMode) onEvent(UiEvent.RemoveHomePoint(it)) },
            onLockChange = { onLockChange -> onEvent(UiEvent.SetMapLocked(onLockChange)) },
            mapViewRef = mapViewRef
        )

        Text(
            text = BuildConfig.VERSION_NAME, 
            color = Color.White, 
            fontSize = 9.sp, 
            fontWeight = FontWeight.Black, 
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 4.dp, bottom = 2.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )

        if (state.showSettingsButton) {
            MapSettingsToggle(
                isMapButtonsVisible = state.isMapButtonsVisible, 
                onToggle = { onEvent(UiEvent.SetMapButtonsVisible(!state.isMapButtonsVisible)) }, 
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = toggleTopPadding)
            )
        }

        if (state.isAnchorLocked) {
            AnchorLockedBadge(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = toggleTopPadding + 8.dp)
            )
        }
        
        if (state.showToolsOverlay && state.isMapButtonsVisible) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).fillMaxHeight(0.85f).width(140.dp)) { 
                    MapToolsOverlay(
                        state = state,
                        onClear = onClearTrails, 
                        onSave = onSaveTrail, 
                        onLoad = onLoadTrail, 
                        onEvent = onEvent
                    ) 
                }
            }
        }

        if (state.trackerLocPending && state.trackerLocPendingReason != LocationPendingReason.NONE) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).background(Amber500.copy(alpha = 0.95f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(text = "UNCERTAINTY: ${state.trackerLocPendingReason.name.replace("_", " ")}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun AnchorLockedBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = Color.Black.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandJd)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = BrandJd,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "ANCHOR LOCKED",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
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
fun OsmMap(
    state: MapViewState,
    cameraActions: kotlinx.coroutines.flow.SharedFlow<CameraAction>? = null,
    initialCenter: GeoPoint? = null,
    onTap: (GeoPoint) -> Unit,
    onRemoveMarker: (Int) -> Unit,
    onLockChange: (Boolean) -> Unit,
    mapViewRef: MutableState<MapView?>
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density

    val mapController = remember(mapViewRef.value) {
        mapViewRef.value?.let { MapController(context, it, density) }
    }

    LaunchedEffect(mapController, cameraActions) {
        cameraActions?.collect { action ->
            when (action) {
                is CameraAction.CenterTracker -> mapController?.centerTracker(state.smoothedTrackerPos)
                is CameraAction.CenterViewer -> mapController?.centerViewer(state.smoothedViewerPos)
                is CameraAction.ZoomIn -> mapController?.zoomIn()
                is CameraAction.ZoomOut -> mapController?.zoomOut()
            }
        }
    }

    DisposableEffect(mapController) {
        val callback = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) { mapController?.trimMemory(level) }
            override fun onConfigurationChanged(newConfig: Configuration) {}
            override fun onLowMemory() { mapController?.trimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) }
        }
        context.registerComponentCallbacks(callback)
        onDispose { context.unregisterComponentCallbacks(callback) }
    }

    AndroidView(factory = { 
        MapView(context).apply { 
            mapViewRef.value = this; setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); isClickable = true
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            val sp = if (initialCenter != null) initialCenter else GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
            controller.setZoom(18.0); controller.setCenter(sp)
            
            val scaleBar = ScaleBarOverlay(this).apply { 
                setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.metric)
                setScaleBarOffset(20, 20)
                setCentred(false)
            }
            overlays.add(scaleBar)
            
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    if (state.appMode != "tracker") { performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); onTap(p) }
                    return true
                }
                override fun longPressHelper(p: GeoPoint): Boolean = true
            }))

            overlays.add(object : Overlay() {
                override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) { onLockChange(false) }
                    return false
                }
            })
        } 
    }, update = { 
        mapController?.update(state, onTap, onRemoveMarker)
    }, onRelease = { 
        mapController?.detach()
    }, modifier = Modifier.fillMaxSize())
}

@Composable
fun MapToolsOverlay(
    state: MapViewState,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onEvent: (UiEvent) -> Unit
) {
    val sc = rememberScrollState(); val sp = 16.dp; val prp = Color(0xFF800080)
    val isTrackerMode = state.appMode == "tracker"

    Column(modifier = Modifier.wrapContentWidth().verticalScroll(sc).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(sp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { 
            MapToolButton(label = "IN", symbol = "+", onClick = { onEvent(UiEvent.MapZoomIn) }, iconColor = prp)
            MapToolButton(label = "OUT", symbol = "-", onClick = { onEvent(UiEvent.MapZoomOut) }, iconColor = prp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { 
            MapToolButton(icon = Icons.Default.Person, label = "VIEWER", onClick = { onEvent(UiEvent.CenterViewer) }, iconColor = if(state.isViewerValid) ViewerCyan else Color.Gray)
            MapToolButton(icon = Icons.Default.Agriculture, label = "TRACKER", onClick = { onEvent(UiEvent.CenterTracker) }, iconColor = if(state.isTrackerValid) BrandJd else Color.Gray) 
        }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { 
            MapToolButton(icon = if (state.isGeofenceViolationsVisible) Icons.Default.LocationOn else Icons.Default.LocationOff, label = "OUT", onClick = { onEvent(UiEvent.SetGeofenceViolationsVisible(!state.isGeofenceViolationsVisible)) }, iconColor = if (state.isGeofenceViolationsVisible) Color.Red else Color.Gray)
            MapToolButton(icon = if (state.isViolationsVisible) Icons.Default.Report else Icons.Default.ReportOff, label = "JUMP", onClick = { onEvent(UiEvent.SetViolationsVisible(!state.isViolationsVisible)) }, iconColor = if (state.isViolationsVisible) Color(0xFFFF00FF) else Color.Gray) 
        }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { 
            MapToolButton(icon = Icons.Default.Upload, label = "LOAD", onClick = onLoad, iconColor = BrandJd)
            MapToolButton(icon = Icons.Default.Save, label = "SAVE", onClick = onSave, iconColor = Indigo500) 
        }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { 
            MapToolButton(icon = Icons.Default.AddLocation, label = "ADD", onClick = { if (!isTrackerMode) onEvent(UiEvent.SetGeofenceMode(GeofenceMode.ADD)) }, iconColor = if (state.geofenceMode == GeofenceMode.ADD) Color.White else BrandJd, containerColor = if (state.geofenceMode == GeofenceMode.ADD) BrandJd else Color.White)
            MapToolButton(icon = Icons.Default.WrongLocation, label = "DEL", onClick = { if (!isTrackerMode) onEvent(UiEvent.SetGeofenceMode(GeofenceMode.REMOVE)) }, iconColor = if (state.geofenceMode == GeofenceMode.REMOVE) Color.White else Rose500, containerColor = if (state.geofenceMode == GeofenceMode.REMOVE) Rose500 else Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { 
            MapToolButton(icon = if (state.isFenceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, label = "FENCE", onClick = { onEvent(UiEvent.SetFenceVisible(!state.isFenceVisible)) }, iconColor = if (state.isFenceVisible) BrandJd else Color.Gray)
            MapToolButton(icon = Icons.Default.Delete, label = "CLEAR", onClick = { onEvent(UiEvent.CenterTracker); onClear() }, iconColor = Rose500) 
        }
    }
}

@Composable
fun MapToolButton(icon: ImageVector? = null, symbol: String? = null, label: String, onClick: () -> Unit, iconColor: Color, containerColor: Color = Color.White) {
    val prp = Color(0xFF800080)
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
