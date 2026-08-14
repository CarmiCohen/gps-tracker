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
 * Aug.14.02:
 * - Issue #170: Forensic Replay UI Audit. Integrated replayCursorPos into 
 *   AppMapContainer and OsmMap to visualize historical trace alignment (R170).
 * Aug.05.128:
 * - Issue #740: AppMapContainer Recomposition Audit.
 */

@Composable
fun AppMapContainer(
    appMode: String?,
    isMapButtonsVisible: Boolean,
    isFenceVisible: Boolean,
    geofenceMode: GeofenceMode,
    isViolationsVisible: Boolean,
    isGeofenceViolationsVisible: Boolean,
    maxDistance: Double,
    isMapLocked: Boolean,
    mapFollowMode: MapFollowMode,
    centeringTrackerTrigger: Int,
    centeringViewerTrigger: Int,
    zoomInTrigger: Int,
    zoomOutTrigger: Int,
    homePoints: List<GeoPoint>,
    trackerLat: Double,
    trackerLng: Double,
    trackerSpeed: Double,
    trackerAccuracy: Double,
    trackerMaxAccuracy: Double,
    trackerGpsTs: Long,
    trackerTelemetryTs: Long,
    trackerLocPending: Boolean,
    trackerLocPendingReason: LocationPendingReason,
    trackerLastValidFixRt: Long,
    viewerLat: Double,
    viewerLng: Double,
    viewerSpeed: Double,
    viewerAccuracy: Double,
    viewerMaxAcc: Double,
    viewerGpsTs: Long,
    viewerTelemetryTs: Long,
    viewerLocPending: Boolean,
    viewerLastValidFixRt: Long,
    replayCursorPos: GeoPoint?,
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
    
    val isTrackerMode = appMode == "tracker"

    fun calculateFreshness(ts: Long, telemetryTs: Long): Boolean {
        if (ts <= 0) return false
        val telemetryAge = if (telemetryTs > 0) now - telemetryTs else Long.MAX_VALUE
        val sourceGpsAge = if (telemetryTs > 0) maxOf(0L, telemetryTs - ts) else 0L
        return (telemetryAge + sourceGpsAge) < GPS_UI_FAIL_THRESHOLD_MS
    }

    val isTrackerFresh = calculateFreshness(trackerGpsTs, trackerTelemetryTs)
    val isViewerFresh = calculateFreshness(viewerGpsTs, viewerTelemetryTs)

    val initialCenter = remember(trackerLat, viewerLat) {
        when {
            PhysicsUtils.isValidLocation(trackerLat, trackerLng) -> GeoPoint(trackerLat, trackerLng)
            PhysicsUtils.isValidLocation(viewerLat, viewerLng) -> GeoPoint(viewerLat, viewerLng)
            else -> GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
        }
    }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMap(
            appMode = appMode,
            isMapLocked = isMapLocked,
            mapFollowMode = mapFollowMode,
            centeringTrackerTrigger = centeringTrackerTrigger,
            centeringViewerTrigger = centeringViewerTrigger,
            zoomInTrigger = zoomInTrigger,
            zoomOutTrigger = zoomOutTrigger,
            homePoints = homePoints,
            isFenceVisible = isFenceVisible,
            maxDistance = maxDistance,
            geofenceMode = geofenceMode,
            isViolationsVisible = isViolationsVisible,
            isGeofenceViolationsVisible = isGeofenceViolationsVisible,
            trackerLat = trackerLat,
            trackerLng = trackerLng,
            trackerSpeed = trackerSpeed,
            trackerAccuracy = trackerAccuracy,
            trackerMaxAccuracy = trackerMaxAccuracy,
            trackerLocPending = trackerLocPending,
            trackerLastValidFixRt = trackerLastValidFixRt,
            viewerLat = viewerLat,
            viewerLng = viewerLng,
            viewerSpeed = viewerSpeed,
            viewerAccuracy = viewerAccuracy,
            viewerMaxAcc = viewerMaxAcc,
            viewerLocPending = viewerLocPending,
            viewerLastValidFixRt = viewerLastValidFixRt,
            replayCursorPos = replayCursorPos,
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
                isMapButtonsVisible = isMapButtonsVisible, 
                onToggle = { onEvent(UiEvent.SetMapButtonsVisible(!isMapButtonsVisible)) }, 
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 12.dp)
            )
        }
        
        if (showToolsOverlay && isMapButtonsVisible) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).fillMaxHeight(0.85f).width(140.dp)) { 
                    MapToolsOverlay(
                        isTrackerMode = isTrackerMode, 
                        trackerValid = PhysicsUtils.isValidLocation(trackerLat, trackerLng), 
                        viewerValid = PhysicsUtils.isValidLocation(viewerLat, viewerLng),
                        showFence = isFenceVisible, onToggleFence = { onEvent(UiEvent.SetFenceVisible(!isFenceVisible)) }, geofenceMode = geofenceMode, onSetGeofenceMode = { onSetGeofenceMode -> onEvent(UiEvent.SetGeofenceMode(onSetGeofenceMode)) },
                        showViolations = isViolationsVisible, onToggleViolations = { onEvent(UiEvent.SetViolationsVisible(!isViolationsVisible)) },
                        showGeofenceViolations = isGeofenceViolationsVisible, onToggleGeofenceViolations = { onEvent(UiEvent.SetGeofenceViolationsVisible(!isGeofenceViolationsVisible)) },
                        onClear = onClearTrails, onSave = onSaveTrail, onLoad = onLoadTrail, onCenterTracker = { onEvent(UiEvent.CenterTracker) }, onCenterViewer = { onEvent(UiEvent.CenterViewer) }, onZoomIn = { onEvent(UiEvent.MapZoomIn) }, onZoomOut = { onEvent(UiEvent.MapZoomOut) }
                    ) 
                }
            }
        }

        if (trackerLocPending && trackerLocPendingReason != LocationPendingReason.NONE) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).background(Amber500.copy(alpha = 0.95f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(text = "UNCERTAINTY: ${trackerLocPendingReason.name.replace("_", " ")}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
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
    appMode: String?,
    isMapLocked: Boolean,
    mapFollowMode: MapFollowMode,
    centeringTrackerTrigger: Int,
    centeringViewerTrigger: Int,
    zoomInTrigger: Int,
    zoomOutTrigger: Int,
    homePoints: List<GeoPoint>,
    isFenceVisible: Boolean,
    maxDistance: Double,
    geofenceMode: GeofenceMode,
    isViolationsVisible: Boolean,
    isGeofenceViolationsVisible: Boolean,
    trackerLat: Double,
    trackerLng: Double,
    trackerSpeed: Double,
    trackerAccuracy: Double,
    trackerMaxAccuracy: Double,
    trackerLocPending: Boolean,
    trackerLastValidFixRt: Long,
    viewerLat: Double,
    viewerLng: Double,
    viewerSpeed: Double,
    viewerAccuracy: Double,
    viewerMaxAcc: Double,
    viewerLocPending: Boolean,
    viewerLastValidFixRt: Long,
    replayCursorPos: GeoPoint?,
    trail: List<TrailPoint>,
    viewerTrail: List<TrailPoint>,
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
    
    val isTrackerMode = appMode == "tracker"

    val overlayManager = remember(mapViewRef.value) {
        mapViewRef.value?.let { MapOverlayManager(context, it, density) }
    }

    val smoothedTrackerPos = remember { mutableStateOf<GeoPoint?>(null) }
    val smoothedViewerPos = remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(trackerLat, trackerLng, trackerSpeed) {
        if (PhysicsUtils.isValidLocation(trackerLat, trackerLng)) {
            val last = smoothedTrackerPos.value
            val alpha = if (trackerSpeed < STATIONARY_SPEED_THRESHOLD_MPS) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
            smoothedTrackerPos.value = if (last == null || PhysicsUtils.calculateDistance(last.latitude, last.longitude, trackerLat, trackerLng) > 30.0) {
                GeoPoint(trackerLat, trackerLng)
            } else {
                GeoPoint(
                    PhysicsUtils.smoothCoordinate(last.latitude, trackerLat, alpha),
                    PhysicsUtils.smoothCoordinate(last.longitude, trackerLng, alpha)
                )
            }
        }
    }

    LaunchedEffect(viewerLat, viewerLng, viewerSpeed) {
        if (PhysicsUtils.isValidLocation(viewerLat, viewerLng)) {
            val last = smoothedViewerPos.value
            val alpha = if (viewerSpeed < STATIONARY_SPEED_THRESHOLD_MPS) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
            smoothedViewerPos.value = if (last == null || PhysicsUtils.calculateDistance(last.latitude, last.longitude, viewerLat, viewerLng) > 30.0) {
                GeoPoint(viewerLat, viewerLng)
            } else {
                GeoPoint(
                    PhysicsUtils.smoothCoordinate(last.latitude, viewerLat, alpha),
                    PhysicsUtils.smoothCoordinate(last.longitude, viewerLng, alpha)
                )
            }
        }
    }

    val localLockStatus = remember { mutableStateOf(isMapLocked) }
    LaunchedEffect(isMapLocked) { localLockStatus.value = isMapLocked }

    var lastTriggerTs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(localLockStatus.value, trackerLat, trackerLng, viewerLat, viewerLng, isTrackerFresh, isViewerFresh, mapFollowMode, smoothedTrackerPos.value, smoothedViewerPos.value) {
        if (localLockStatus.value) {
            if (systemPulse - lastTriggerTs < 500) return@LaunchedEffect
            val sTrk = smoothedTrackerPos.value
            val sVwr = smoothedViewerPos.value
            val view = mapViewRef.value ?: return@LaunchedEffect
            
            when (mapFollowMode) {
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

    LaunchedEffect(centeringTrackerTrigger) {
        val sTrk = smoothedTrackerPos.value
        if (centeringTrackerTrigger > 0 && sTrk != null) {
            lastTriggerTs = systemPulse; mapViewRef.value?.controller?.animateTo(sTrk); mapViewRef.value?.controller?.setZoom(18.0)
        }
    }

    LaunchedEffect(centeringViewerTrigger) {
        val sVwr = smoothedViewerPos.value
        if (centeringViewerTrigger > 0 && sVwr != null) {
            lastTriggerTs = systemPulse; mapViewRef.value?.controller?.animateTo(sVwr); mapViewRef.value?.controller?.setZoom(18.0)
        }
    }

    LaunchedEffect(zoomInTrigger) { if (zoomInTrigger > 0) mapViewRef.value?.controller?.zoomIn() }
    LaunchedEffect(zoomOutTrigger) { if (zoomOutTrigger > 0) mapViewRef.value?.controller?.zoomOut() }

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
        val sTrail = trail.toList()
        val sViewerTrail = viewerTrail.toList()
        val sViolations = violations.toList()
        val sHome = homePoints.toList()

        Snapshot.withoutReadObservation {
            overlayManager?.let { om ->
                val h = om.updateHomePoints(sHome, isFenceVisible, maxDistance, isTrackerMode, geofenceMode, onTap, onRemoveMarker)
                val t = om.updateTrails(sTrail, sViewerTrail, systemPulseRt)
                val v = om.updateViolations(sViolations, isViolationsVisible, isGeofenceViolationsVisible, systemPulseRt)
                val r = om.updateReplayCursor(replayCursorPos)
                val p = om.updateCurrentPositions(
                    trackerValid = smoothedTrackerPos.value != null,
                    trackerPos = smoothedTrackerPos.value,
                    isTrackerFresh = isTrackerFresh,
                    trackerAccuracy = trackerAccuracy,
                    maxTrackerAccuracy = trackerMaxAccuracy,
                    trackerSpeed = trackerSpeed,
                    isTrackerPending = trackerLocPending,
                    trackerLastValidFixRt = trackerLastValidFixRt,
                    viewerValid = smoothedViewerPos.value != null,
                    viewerPos = smoothedViewerPos.value,
                    isViewerFresh = isViewerFresh,
                    viewerAccuracy = viewerAccuracy,
                    viewerMaxAcc = viewerMaxAcc,
                    viewerSpeed = viewerSpeed,
                    isViewerPending = viewerLocPending,
                    viewerLastValidFixRt = viewerLastValidFixRt,
                    systemPulseRt = systemPulseRt
                )
                if (h || t || v || r || p) {
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
