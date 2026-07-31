package com.gps19.app

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
 * July.31.01:
 * - Issue #657: Compose Snapshot Lock Failure. Wrapped AndroidView update block 
 *   in Snapshot.withoutReadObservation to eliminate lock verification overhead 
 *   during high-frequency telemetry bursts (R-HARDWARE-01).
 * July.30.56:
 * - Issue #642: Map Settings Icon Contrast. Standardized icon treatments for accessibility 
 *   on budget screens. Switched to solid backgrounds and stronger borders for map controls.
 */

@Composable
fun AppMapContainer(
    uiState: MainUiState,
    kinematicState: KinematicState,
    diagnosticState: DiagnosticState,
    systemPulse: Long,
    systemPulseRt: Long,
    onEvent: (UiEvent) -> Unit,
    onClearTrails: () -> Unit,
    trail: List<TrailPoint>,
    viewerTrail: List<TrailPoint>,
    violations: List<ViolationPoint>,
    onSaveTrail: () -> Unit,
    onLoadTrail: () -> Unit,
    showAccuracyBadge: Boolean = true,
    showSettingsButton: Boolean = true,
    showToolsOverlay: Boolean = true
) {
    val context = LocalContext.current
    val now = systemPulse
    
    val isTrackerMode = uiState.appMode == "tracker"
    val trackerLoc = if (isTrackerMode) kinematicState.localLocation else kinematicState.trackerLocation
    val viewerLoc = if (isTrackerMode) kinematicState.trackerLocation else kinematicState.localLocation

    // Freshness Logic
    fun calculateFreshness(loc: LocationState): Boolean {
        if (loc.timestamp <= 0) return false
        val telemetryAge = if (loc.telemetryTs > 0) now - loc.telemetryTs else Long.MAX_VALUE
        val sourceGpsAge = if (loc.telemetryTs > 0) maxOf(0L, loc.telemetryTs - loc.timestamp) else 0L
        return (telemetryAge + sourceGpsAge) < GPS_UI_FAIL_THRESHOLD_MS
    }

    val isTrackerFresh = calculateFreshness(trackerLoc)
    val isViewerFresh = calculateFreshness(viewerLoc)

    val initialCenter = remember(kinematicState.trackerLocation.lat, kinematicState.localLocation.lat) {
        when {
            PhysicsUtils.isValidLocation(trackerLoc.lat, trackerLoc.lng) -> GeoPoint(trackerLoc.lat, trackerLoc.lng)
            PhysicsUtils.isValidLocation(viewerLoc.lat, viewerLoc.lng) -> GeoPoint(viewerLoc.lat, viewerLoc.lng)
            else -> GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
        }
    }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMap(
            uiState = uiState,
            kinematicState = kinematicState,
            diagnosticState = diagnosticState,
            trail = trail,
            viewerTrail = viewerTrail,
            violations = violations,
            onTap = { onEvent(UiEvent.MapTap(it)) },
            onRemoveMarker = { if (!isTrackerMode) onEvent(UiEvent.RemoveHomePoint(it)) },
            isTrackerFresh = isTrackerFresh,
            isViewerFresh = isViewerFresh,
            initialCenter = initialCenter,
            systemPulse = now,
            systemPulseRt = systemPulseRt,
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

        if (showSettingsButton) {
            MapSettingsToggle(
                isMapButtonsVisible = uiState.isMapButtonsVisible, 
                onToggle = { onEvent(UiEvent.SetMapButtonsVisible(!uiState.isMapButtonsVisible)) }, 
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 12.dp)
            )
        }
        
        if (showToolsOverlay && uiState.isMapButtonsVisible) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).fillMaxHeight(0.85f).width(140.dp)) { 
                    MapToolsOverlay(
                        isTrackerMode = isTrackerMode, 
                        trackerValid = PhysicsUtils.isValidLocation(trackerLoc.lat, trackerLoc.lng), 
                        viewerValid = PhysicsUtils.isValidLocation(viewerLoc.lat, viewerLoc.lng),
                        showFence = uiState.isFenceVisible, onToggleFence = { onEvent(UiEvent.SetFenceVisible(!uiState.isFenceVisible)) }, geofenceMode = uiState.geofenceMode, onSetGeofenceMode = { onSetGeofenceMode -> onEvent(UiEvent.SetGeofenceMode(onSetGeofenceMode)) },
                        showViolations = uiState.isViolationsVisible, onToggleViolations = { onEvent(UiEvent.SetViolationsVisible(!uiState.isViolationsVisible)) },
                        showGeofenceViolations = uiState.isGeofenceViolationsVisible, onToggleGeofenceViolations = { onEvent(UiEvent.SetGeofenceViolationsVisible(!uiState.isGeofenceViolationsVisible)) },
                        onClear = onClearTrails, onSave = onSaveTrail, onLoad = onLoadTrail, onCenterTracker = { onEvent(UiEvent.CenterTracker) }, onCenterViewer = { onEvent(UiEvent.CenterViewer) }, onZoomIn = { onEvent(UiEvent.MapZoomIn) }, onZoomOut = { onEvent(UiEvent.MapZoomOut) }
                    ) 
                }
            }
        }

        val trackerHealth = if (isTrackerMode) kinematicState.localHealth else kinematicState.trackerHealth
        if (trackerHealth.isLocationPending && trackerHealth.locationPendingReason != LocationPendingReason.NONE) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).background(Amber500.copy(alpha = 0.95f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(text = "UNCERTAINTY: ${trackerHealth.locationPendingReason.name.replace("_", " ")}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
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
    uiState: MainUiState,
    kinematicState: KinematicState,
    diagnosticState: DiagnosticState,
    trail: List<TrailPoint>,
    viewerTrail: List<TrailPoint>,
    home: List<GeoPoint> = uiState.homePoints,
    violations: List<ViolationPoint>, 
    onTap: (GeoPoint) -> Unit,
    onRemoveMarker: (Int) -> Unit,
    isTrackerFresh: Boolean,
    isViewerFresh: Boolean,
    initialCenter: GeoPoint? = null,
    systemPulse: Long,
    systemPulseRt: Long,
    onLockChange: (Boolean) -> Unit,
    mapViewRef: MutableState<MapView?>
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    
    val isTrackerMode = uiState.appMode == "tracker"
    val trackerLoc = if (isTrackerMode) kinematicState.localLocation else kinematicState.trackerLocation
    val viewerLoc = if (isTrackerMode) kinematicState.trackerLocation else kinematicState.localLocation
    
    val trackerHealth = if (isTrackerMode) kinematicState.localHealth else kinematicState.trackerHealth
    val viewerHealth = if (isTrackerMode) kinematicState.trackerHealth else kinematicState.localHealth

    val overlayManager = remember(mapViewRef.value) {
        mapViewRef.value?.let { MapOverlayManager(context, it, density) }
    }

    // Smoothing States
    val smoothedTrackerPos = remember { mutableStateOf<GeoPoint?>(null) }
    val smoothedViewerPos = remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(trackerLoc.lat, trackerLoc.lng, trackerLoc.speed) {
        if (PhysicsUtils.isValidLocation(trackerLoc.lat, trackerLoc.lng)) {
            val last = smoothedTrackerPos.value
            val alpha = if (trackerLoc.speed < STATIONARY_SPEED_THRESHOLD_MPS) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
            smoothedTrackerPos.value = if (last == null || PhysicsUtils.calculateDistance(last.latitude, last.longitude, trackerLoc.lat, trackerLoc.lng) > 30.0) {
                GeoPoint(trackerLoc.lat, trackerLoc.lng)
            } else {
                GeoPoint(
                    PhysicsUtils.smoothCoordinate(last.latitude, trackerLoc.lat, alpha),
                    PhysicsUtils.smoothCoordinate(last.longitude, trackerLoc.lng, alpha)
                )
            }
        }
    }

    LaunchedEffect(viewerLoc.lat, viewerLoc.lng, viewerLoc.speed) {
        if (PhysicsUtils.isValidLocation(viewerLoc.lat, viewerLoc.lng)) {
            val last = smoothedViewerPos.value
            val alpha = if (viewerLoc.speed < STATIONARY_SPEED_THRESHOLD_MPS) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
            smoothedViewerPos.value = if (last == null || PhysicsUtils.calculateDistance(last.latitude, last.longitude, viewerLoc.lat, viewerLoc.lng) > 30.0) {
                GeoPoint(viewerLoc.lat, viewerLoc.lng)
            } else {
                GeoPoint(
                    PhysicsUtils.smoothCoordinate(last.latitude, viewerLoc.lat, alpha),
                    PhysicsUtils.smoothCoordinate(last.longitude, viewerLoc.lng, alpha)
                )
            }
        }
    }

    val localLockStatus = remember { mutableStateOf(uiState.isMapLocked) }
    LaunchedEffect(uiState.isMapLocked) { localLockStatus.value = uiState.isMapLocked }

    var lastTriggerTs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(localLockStatus.value, trackerLoc.lat, trackerLoc.lng, viewerLoc.lat, viewerLoc.lng, isTrackerFresh, isViewerFresh, uiState.mapFollowMode, smoothedTrackerPos.value, smoothedViewerPos.value) {
        if (localLockStatus.value) {
            if (systemPulse - lastTriggerTs < 500) return@LaunchedEffect
            val sTrk = smoothedTrackerPos.value
            val sVwr = smoothedViewerPos.value
            val view = mapViewRef.value ?: return@LaunchedEffect
            
            when (uiState.mapFollowMode) {
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
            }
        }
    }

    LaunchedEffect(uiState.centeringTrackerTrigger) {
        val sTrk = smoothedTrackerPos.value
        if (uiState.centeringTrackerTrigger > 0 && sTrk != null) {
            lastTriggerTs = systemPulse; mapViewRef.value?.controller?.animateTo(sTrk); mapViewRef.value?.controller?.setZoom(18.0)
        }
    }

    LaunchedEffect(uiState.centeringViewerTrigger) {
        val sVwr = smoothedViewerPos.value
        if (uiState.centeringViewerTrigger > 0 && sVwr != null) {
            lastTriggerTs = systemPulse; mapViewRef.value?.controller?.animateTo(sVwr); mapViewRef.value?.controller?.setZoom(18.0)
        }
    }

    LaunchedEffect(uiState.zoomInTrigger) { if (uiState.zoomInTrigger > 0) mapViewRef.value?.controller?.zoomIn() }
    LaunchedEffect(uiState.zoomOutTrigger) { if (uiState.zoomOutTrigger > 0) mapViewRef.value?.controller?.zoomOut() }

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
        // Issue #657: Wrapped in Snapshot.withoutReadObservation to eliminate 
        // lock verification failures during high-frequency telemetry updates.
        Snapshot.withoutReadObservation {
            overlayManager?.let { om ->
                val h = om.updateHomePoints(home, uiState.isFenceVisible, uiState.maxDistance, isTrackerMode, uiState.geofenceMode, onTap, onRemoveMarker)
                val t = om.updateTrails(trail, viewerTrail, systemPulseRt)
                val v = om.updateViolations(violations, uiState.isViolationsVisible, uiState.isGeofenceViolationsVisible, systemPulseRt)
                val p = om.updateCurrentPositions(
                    trackerValid = smoothedTrackerPos.value != null,
                    trackerPos = smoothedTrackerPos.value,
                    isTrackerFresh = isTrackerFresh,
                    trackerAccuracy = trackerLoc.accuracy,
                    maxTrackerAccuracy = trackerLoc.maxAccuracy,
                    trackerSpeed = trackerLoc.speed,
                    isTrackerPending = trackerHealth.isLocationPending,
                    trackerLastValidFixRt = trackerHealth.lastValidFixRt,
                    viewerValid = smoothedViewerPos.value != null,
                    viewerPos = smoothedViewerPos.value,
                    isViewerFresh = isViewerFresh,
                    viewerAccuracy = viewerLoc.accuracy,
                    viewerMaxAcc = viewerLoc.maxAccuracy,
                    viewerSpeed = viewerLoc.speed,
                    isViewerPending = viewerHealth.isLocationPending,
                    viewerLastValidFixRt = viewerHealth.lastValidFixRt,
                    systemPulseRt = systemPulseRt
                )
                // Issue #641: Conditionally invalidate only if overlays actually changed.
                if (h || t || v || p) {
                    view.invalidate()
                }
            }
        }
    }, onRelease = { view -> view.onDetach(); view.tileProvider.tileCache.clear(); view.tileProvider.detach() }, modifier = Modifier.fillMaxSize())
}

@Composable
fun MapToolsOverlay(
    isTrackerMode: Boolean, trackerValid: Boolean = true, viewerValid: Boolean = true, showFence: Boolean, onToggleFence: () -> Unit,
    geofenceMode: GeofenceMode, onSetGeofenceMode: (GeofenceMode) -> Unit, showViolations: Boolean = true, onToggleViolations: () -> Unit = {},
    showGeofenceViolations: Boolean = true, onToggleGeofenceViolations: () -> Unit = {}, onClear: () -> Unit, onSave: () -> Unit, 
    onLoad: () -> Unit, onCenterTracker: () -> Unit = {}, onCenterViewer: () -> Unit = {},
    onZoomIn: () -> Unit = {}, onZoomOut: () -> Unit = {}
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
