package com.jd.app.monitor

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.flow.SharedFlow

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
fun OsmMap(
    state: MapViewState,
    cameraActions: SharedFlow<CameraAction>? = null,
    initialCenter: GeoPoint? = null,
    onEvent: (UiEvent) -> Unit,
    mapViewRef: MutableState<MapView?>
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val isTrackerMode = state.appMode == "tracker"

    val mapController = remember(mapViewRef.value) {
        mapViewRef.value?.let { MapController(context, it, density) }
    }

    LaunchedEffect(mapController, cameraActions) {
        cameraActions?.collect { action ->
            when (action) {
                is CameraAction.CenterTracker -> mapController?.centerTracker(GeoPoint(state.smoothedTrackerLat, state.smoothedTrackerLng))
                is CameraAction.CenterViewer -> mapController?.centerViewer(GeoPoint(state.smoothedViewerLat, state.smoothedViewerLng))
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
            val sp = initialCenter ?: GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
            controller.setZoom(18.0); controller.setCenter(sp)

            val scaleBar = ScaleBarOverlay(this).apply {
                setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.metric)
                setAlignBottom(true)
                setAlignRight(false)
                setScaleBarOffset(20, 20)
                setCentred(false)
            }
            overlays.add(scaleBar)

            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onEvent(UiEvent.MapTap(p))
                    return true
                }
                override fun longPressHelper(p: GeoPoint): Boolean = true
            }))

            overlays.add(object : Overlay() {
                override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) { onEvent(UiEvent.SetMapLocked(false)) }
                    return false
                }
            })
        }
    }, update = {
        mapController?.update(state, { onEvent(UiEvent.MapTap(it)) }, { if (!isTrackerMode) onEvent(UiEvent.RemoveHomePoint(it)) })
    }, onRelease = {
        mapController?.detach()
    }, modifier = Modifier.fillMaxSize())
}

@Composable
fun MapToolsOverlay(
    state: MapViewState,
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
            MapToolButton(icon = Icons.Default.Place, label = "TRACKER", onClick = { onEvent(UiEvent.CenterTracker) }, iconColor = if(state.isTrackerValid) BrandJd else Color.Gray)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) {
            MapToolButton(icon = Icons.Default.LocationOn, label = "OUT", onClick = { onEvent(UiEvent.SetGeofenceViolationsVisible(!state.isGeofenceViolationsVisible)) }, iconColor = if (state.isGeofenceViolationsVisible) Color.Red else Color.Gray)
            MapToolButton(icon = Icons.Default.Warning, label = "JUMP", onClick = { onEvent(UiEvent.SetViolationsVisible(!state.isViolationsVisible)) }, iconColor = if (state.isViolationsVisible) Color(0xFFFF00FF) else Color.Gray)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) {
            MapToolButton(icon = Icons.Default.Refresh, label = "LOAD", onClick = { onEvent(UiEvent.ImportTrails) }, iconColor = BrandJd)
            MapToolButton(icon = Icons.Default.Check, label = "SAVE", onClick = { onEvent(UiEvent.ExportTrails) }, iconColor = Indigo500)
        }

        if (!isTrackerMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(sp)) {
                MapToolButton(icon = Icons.Default.Add, label = "ADD", onClick = { onEvent(UiEvent.SetGeofenceMode(GeofenceMode.ADD)) }, iconColor = if (state.geofenceMode == GeofenceMode.ADD) Color.White else BrandJd, containerColor = if (state.geofenceMode == GeofenceMode.ADD) BrandJd else Color.White)
                MapToolButton(icon = Icons.Default.Close, label = "DEL", onClick = { onEvent(UiEvent.SetGeofenceMode(GeofenceMode.REMOVE)) }, iconColor = if (state.geofenceMode == GeofenceMode.REMOVE) Color.White else Rose500, containerColor = if (state.geofenceMode == GeofenceMode.REMOVE) Rose500 else Color.White)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(sp)) {
            MapToolButton(icon = Icons.Default.Info, label = "FENCE", onClick = { onEvent(UiEvent.SetFenceVisible(!state.isFenceVisible)) }, iconColor = if (state.isFenceVisible) BrandJd else Color.Gray)
            MapToolButton(icon = Icons.Default.Delete, label = "CLEAR", onClick = { onEvent(UiEvent.CenterTracker); onEvent(UiEvent.ClearTrails) }, iconColor = Rose500)
        }
    }
}
