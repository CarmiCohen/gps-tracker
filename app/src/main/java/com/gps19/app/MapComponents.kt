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
import org.osmdroid.util.BoundingBox
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
 * Sep.10.12:
 * - Idea #243: Map State Partitioning RESOLVED. Refactored AppMapContainer 
 *   and OsmMap to consume MapViewState, reducing parameter surface and 
 *   optimizing recomposition (R-ID 287).
 * Sep.08.13:
 * - Fix: Corrected isViewerFresh calculation in AppMapContainer to use 
 *   viewerTelemetryTs instead of trackerTelemetryTs (R-ID 282).
 */

@Composable
fun AppMapContainer(
    state: MapViewState,
    onEvent: (UiEvent) -> Unit,
    onClearTrails: () -> Unit,
    onSaveTrail: () -> Unit,
    onLoadTrail: () -> Unit
) {
    val context = LocalContext.current
    val isTrackerMode = state.appMode == "tracker"

    val isTrackerFresh by remember(state.trackerGpsTs, state.trackerTelemetryTs, state.systemPulse) {
        derivedStateOf {
            if (state.trackerGpsTs <= 0) false
            else {
                val telemetryAge = if (state.trackerTelemetryTs > 0) state.systemPulse - state.trackerTelemetryTs else Long.MAX_VALUE
                val sourceGpsAge = if (state.trackerTelemetryTs > 0) maxOf(0L, state.trackerTelemetryTs - state.trackerGpsTs) else 0L
                (telemetryAge + sourceGpsAge) < GPS_UI_FAIL_THRESHOLD_MS
            }
        }
    }

    val isViewerFresh by remember(state.viewerGpsTs, state.viewerTelemetryTs, state.systemPulse) {
        derivedStateOf {
            if (state.viewerGpsTs <= 0) false
            else {
                val telemetryAge = if (state.viewerTelemetryTs > 0) state.systemPulse - state.viewerTelemetryTs else Long.MAX_VALUE
                val sourceGpsAge = if (state.viewerTelemetryTs > 0) maxOf(0L, state.viewerTelemetryTs - state.viewerGpsTs) else 0L
                (telemetryAge + sourceGpsAge) < GPS_UI_FAIL_THRESHOLD_MS
            }
        }
    }

    val initialCenter = remember(state.trackerLat, state.viewerLat) {
        when {
            PhysicsUtils.isValidLocation(state.trackerLat, state.trackerLng) -> GeoPoint(state.trackerLat, state.trackerLng)
            PhysicsUtils.isValidLocation(state.viewerLat, state.viewerLng) -> GeoPoint(state.viewerLat, state.viewerLng)
            else -> GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
        }
    }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMap(
            state = state,
            isTrackerFresh = isTrackerFresh,
            isViewerFresh = isViewerFresh,
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
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 12.dp)
            )
        }
        
        if (state.showToolsOverlay && state.isMapButtonsVisible) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).fillMaxHeight(0.85f).width(140.dp)) { 
                    MapToolsOverlay(
                        isTrackerMode = isTrackerMode, 
                        trackerValid = PhysicsUtils.isValidLocation(state.trackerLat, state.trackerLng), 
                        viewerValid = PhysicsUtils.isValidLocation(state.viewerLat, state.viewerLng),
                        showFence = state.isFenceVisible, onToggleFence = { onEvent(UiEvent.SetFenceVisible(!state.isFenceVisible)) }, geofenceMode = state.geofenceMode, onSetGeofenceMode = { onSetGeofenceMode -> onEvent(UiEvent.SetGeofenceMode(onSetGeofenceMode)) },
                        showViolations = state.isViolationsVisible, onToggleViolations = { onEvent(UiEvent.SetViolationsVisible(!state.isViolationsVisible)) },
                        showGeofenceViolations = state.isGeofenceViolationsVisible, onToggleGeofenceViolations = { onEvent(UiEvent.SetGeofenceViolationsVisible(!state.isGeofenceViolationsVisible)) },
                        onClear = onClearTrails, onSave = onSaveTrail, onLoad = onLoadTrail, onCenterTracker = { onEvent(UiEvent.CenterTracker) }, onCenterViewer = { onEvent(UiEvent.CenterViewer) }, onZoomIn = { onEvent(UiEvent.MapZoomIn) }, onZoomOut = { onEvent(UiEvent.MapZoomOut) }
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
    isTrackerFresh: Boolean,
    isViewerFresh: Boolean,
    initialCenter: GeoPoint? = null,
    onTap: (GeoPoint) -> Unit,
    onRemoveMarker: (Int) -> Unit,
    onLockChange: (Boolean) -> Unit,
    mapViewRef: MutableState<MapView?>
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    
    val isTrackerMode = state.appMode == "tracker"

    val overlayManager = remember(mapViewRef.value) {
        mapViewRef.value?.let { MapOverlayManager(context, it, density) }
    }

    DisposableEffect(overlayManager) {
        val callback = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                overlayManager?.trimMemory(level)
            }
            override fun onConfigurationChanged(newConfig: Configuration) {}
            override fun onLowMemory() {
                overlayManager?.trimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
            }
        }
        context.registerComponentCallbacks(callback)
        onDispose {
            context.unregisterComponentCallbacks(callback)
        }
    }

    val smoothedTrackerPos = remember { mutableStateOf<GeoPoint?>(null) }
    val smoothedViewerPos = remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(state.trackerLat, state.trackerLng, state.trackerSpeed) {
        if (PhysicsUtils.isValidLocation(state.trackerLat, state.trackerLng)) {
            val last = smoothedTrackerPos.value
            val alpha = if (state.trackerSpeed < STATIONARY_SPEED_THRESHOLD_MPS) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
            smoothedTrackerPos.value = if (last == null || PhysicsUtils.calculateDistance(last.latitude, last.longitude, state.trackerLat, state.trackerLng) > 100.0) {
                GeoPoint(state.trackerLat, state.trackerLng)
            } else {
                GeoPoint(
                    PhysicsUtils.smoothCoordinate(last.latitude, state.trackerLat, alpha),
                    PhysicsUtils.smoothCoordinate(last.longitude, state.trackerLng, alpha)
                )
            }
        }
    }

    LaunchedEffect(state.viewerLat, state.viewerLng, state.viewerSpeed) {
        if (PhysicsUtils.isValidLocation(state.viewerLat, state.viewerLng)) {
            val last = smoothedViewerPos.value
            val alpha = if (state.viewerSpeed < STATIONARY_SPEED_THRESHOLD_MPS) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
            smoothedViewerPos.value = if (last == null || PhysicsUtils.calculateDistance(last.latitude, last.longitude, state.viewerLat, state.viewerLng) > 100.0) {
                GeoPoint(state.viewerLat, state.viewerLng)
            } else {
                GeoPoint(
                    PhysicsUtils.smoothCoordinate(last.latitude, state.viewerLat, alpha),
                    PhysicsUtils.smoothCoordinate(last.longitude, state.viewerLng, alpha)
                )
            }
        }
    }

    val localLockStatus = remember { mutableStateOf(state.isMapLocked) }
    LaunchedEffect(state.isMapLocked) { localLockStatus.value = state.isMapLocked }

    var lastTriggerTs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(localLockStatus.value, state.trackerLat, state.trackerLng, state.viewerLat, state.viewerLng, isTrackerFresh, isViewerFresh, state.mapFollowMode, smoothedTrackerPos.value, smoothedViewerPos.value) {
        if (localLockStatus.value) {
            if (state.systemPulse - lastTriggerTs < 500) return@LaunchedEffect
            val sTrk = smoothedTrackerPos.value
            val sVwr = smoothedViewerPos.value
            val view = mapViewRef.value ?: return@LaunchedEffect
            
            when (state.mapFollowMode) {
                MapFollowMode.VIEWER -> { if (sVwr != null) view.controller.setCenter(sVwr) }
                MapFollowMode.TRACKER -> { if (sTrk != null) view.controller.setCenter(sTrk) }
                MapFollowMode.AUTO -> {
                    if (sTrk != null && sVwr != null && isTrackerFresh && isViewerFresh) {
                        val dist = PhysicsUtils.calculateDistance(sTrk.latitude, sTrk.longitude, sVwr.latitude, sVwr.longitude)
                        if (dist in 100.0..100000.0) {
                            val box = BoundingBox.fromGeoPoints(listOf(sTrk, sVwr))
                            view.zoomToBoundingBox(box.increaseByScale(1.4f), false)
                            if (view.zoomLevelDouble > 18.0) view.controller.setZoom(18.0)
                        } else view.controller.setCenter(sTrk)
                    } else if (sTrk != null || sVwr != null) {
                        view.controller.setCenter(sTrk ?: sVwr!!)
                    }
                }
                MapFollowMode.NONE -> {}
            }
        }
    }

    LaunchedEffect(state.centeringTrackerTrigger) {
        val sTrk = smoothedTrackerPos.value
        if (state.centeringTrackerTrigger > 0 && sTrk != null) {
            lastTriggerTs = state.systemPulse; mapViewRef.value?.controller?.animateTo(sTrk); mapViewRef.value?.controller?.setZoom(18.0)
        }
    }

    LaunchedEffect(state.centeringViewerTrigger) {
        val sVwr = smoothedViewerPos.value
        if (state.centeringViewerTrigger > 0 && sVwr != null) {
            lastTriggerTs = state.systemPulse; mapViewRef.value?.controller?.animateTo(sVwr); mapViewRef.value?.controller?.setZoom(18.0)
        }
    }

    LaunchedEffect(state.zoomInTrigger) { if (state.zoomInTrigger > 0) mapViewRef.value?.controller?.zoomIn() }
    LaunchedEffect(state.zoomOutTrigger) { if (state.zoomOutTrigger > 0) mapViewRef.value?.controller?.zoomOut() }

    AndroidView(factory = { 
        MapView(context).apply { 
            mapViewRef.value = this; setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); isClickable = true
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            val sp = if (initialCenter != null) initialCenter else GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
            controller.setZoom(18.0); controller.setCenter(sp)
            ScaleBarOverlay(this).apply { setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.metric) }
            
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    if (!isTrackerMode) { performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); onTap(p) }
                    return true
                }
                override fun longPressHelper(p: GeoPoint): Boolean = true
            }))

            overlays.add(object : Overlay() {
                override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) { localLockStatus.value = false; onLockChange(false) }
                    return false
                }
            })
        } 
    }, update = { view ->
        Snapshot.withoutReadObservation {
            overlayManager?.let { om ->
                var changed = false
                
                if (state.hydrationLevel >= 4) {
                    changed = om.updateHomePoints(state.homePoints, state.isFenceVisible, state.maxDistance, isTrackerMode, state.geofenceMode, onTap, onRemoveMarker) || changed
                }
                
                if (state.hydrationLevel >= 5) {
                    changed = om.updateTrails(state.trackerSegments, state.viewerSegments, state.systemPulseRt) || changed
                }
                
                if (state.hydrationLevel >= 6) {
                    changed = om.updateCurrentPositions(
                        trackerValid = smoothedTrackerPos.value != null,
                        trackerPos = smoothedTrackerPos.value,
                        isTrackerFresh = isTrackerFresh,
                        trackerAccuracy = state.trackerAccuracy,
                        maxTrackerAccuracy = state.trackerMaxAccuracy,
                        trackerSpeed = state.trackerSpeed,
                        isTrackerPending = state.trackerLocPending,
                        trackerLastValidFixRt = state.trackerLastValidFixRt,
                        viewerValid = smoothedViewerPos.value != null,
                        viewerPos = smoothedViewerPos.value,
                        isViewerFresh = isViewerFresh,
                        viewerAccuracy = state.viewerAccuracy,
                        viewerMaxAcc = state.viewerMaxAcc,
                        viewerSpeed = state.viewerSpeed,
                        isViewerPending = state.viewerLocPending,
                        viewerLastValidFixRt = state.viewerLastValidFixRt,
                        systemPulseRt = state.systemPulseRt
                    ) || changed
                }

                if (state.hydrationLevel >= 7) {
                    changed = om.updateViolations(state.violations, state.isViolationsVisible, state.isGeofenceViolationsVisible, state.systemPulseRt) || changed
                }
                
                if (state.hydrationLevel >= 8) {
                    changed = om.updateReplayCursor(state.replayCursorPos) || changed
                }
                
                if (changed) {
                    view.invalidate()
                }
            }
        }
    }, onRelease = { view -> 
        overlayManager?.onDetach()
        view.onDetach(); view.tileProvider.tileCache.clear(); view.tileProvider.detach() 
    }, modifier = Modifier.fillMaxSize())
}

@Composable
fun MapToolsOverlay(
    isTrackerMode: Boolean, trackerValid: Boolean = true, viewerValid: Boolean = true, showFence: Boolean, onToggleFence: () -> Unit,
    geofenceMode: GeofenceMode, onSetGeofenceMode: (GeofenceMode) -> Unit, showViolations: Boolean = true, onToggleViolations: () -> Unit = {},
    showGeofenceViolations: Boolean = true, onToggleGeofenceViolations: () -> Unit = {}, onClear: () -> Unit, onSave: () -> Unit, 
    onLoad: () -> Unit, onCenterTracker: () -> Unit = {}, onCenterViewer: () -> Unit = {},
    onZoomIn: () -> Unit = {}, onZoomOut: () -> Unit = { }
) {
    val sc = rememberScrollState(); val sp = 16.dp; val prp = Color(0xFF800080)
    val curTrk by rememberUpdatedState(isTrackerMode); val curTrkVal by rememberUpdatedState(trackerValid); val curVwrVal by rememberUpdatedState(viewerValid)
    val curFnc by rememberUpdatedState(showFence); val curGeo by rememberUpdatedState(geofenceMode); val curVio by rememberUpdatedState(showViolations); val curGeoVio by rememberUpdatedState(showGeofenceViolations)

    Column(modifier = Modifier.wrapContentWidth().verticalScroll(sc).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(sp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { MapToolButton(label = "IN", symbol = "+", onClick = onZoomIn, iconColor = prp); MapToolButton(label = "OUT", symbol = "-", onClick = onZoomOut, iconColor = prp) }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { MapToolButton(icon = Icons.Default.Person, label = "VIEWER", onClick = onCenterViewer, iconColor = if(curVwrVal) ViewerCyan else Color.Gray); MapToolButton(icon = Icons.Default.Agriculture, label = "TRACKER", onClick = onCenterTracker, iconColor = if(curTrkVal) BrandJd else Color.Gray) }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { MapToolButton(icon = if (curGeoVio) Icons.Default.LocationOn else Icons.Default.LocationOff, label = "OUT", onClick = onToggleGeofenceViolations, iconColor = if (curGeoVio) Color.Red else Color.Gray); MapToolButton(icon = if (curVio) Icons.Default.Report else Icons.Default.ReportOff, label = "JUMP", onClick = onToggleViolations, iconColor = if (curVio) Color(0xFFFF00FF) else Color.Gray) }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { MapToolButton(icon = Icons.Default.Upload, label = "LOAD", onClick = onLoad, iconColor = BrandJd); MapToolButton(icon = Icons.Default.Save, label = "SAVE", onClick = onSave, iconColor = Indigo500) }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { 
            MapToolButton(icon = Icons.Default.AddLocation, label = "ADD", onClick = { if (!curTrk) onSetGeofenceMode(GeofenceMode.ADD) }, iconColor = if (curGeo == GeofenceMode.ADD) Color.White else BrandJd, containerColor = if (curGeo == GeofenceMode.ADD) BrandJd else Color.White)
            MapToolButton(icon = Icons.Default.WrongLocation, label = "DEL", onClick = { if (!curTrk) onSetGeofenceMode(GeofenceMode.REMOVE) }, iconColor = if (curGeo == GeofenceMode.REMOVE) Color.White else Rose500, containerColor = if (curGeo == GeofenceMode.REMOVE) Rose500 else Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { MapToolButton(icon = if (curFnc) Icons.Default.Visibility else Icons.Default.VisibilityOff, label = "FENCE", onClick = onToggleFence, iconColor = if (curFnc) BrandJd else Color.Gray); MapToolButton(icon = Icons.Default.Delete, label = "CLEAR", onClick = { onCenterTracker(); onClear() }, iconColor = Rose500) }
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
