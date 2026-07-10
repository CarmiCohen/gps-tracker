package com.gps19.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Overlay
import com.gps19.app.BuildConfig
import com.gps19.core.engine.*

/**
 * MapComponents: Shared map logic for Tracker and Viewer.
 * v9.3.8:
 * - Issue #072 Clock Skew Hardening: Transitioned map freshness calculations 
 *   to a Receipt-Time Authority model to prevent markers from turning gray due 
 *   to device clock drift. markers now use totalGpsAge (local delay + source delay).
 */

@Composable
fun AppMapContainer(
    uiState: MainUiState,
    systemPulse: Long,
    systemPulseRealtime: Long,
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
    
    val trackerLoc = if (isTrackerMode) uiState.localLocation else uiState.trackerLocation
    val viewerLoc = if (isTrackerMode) uiState.trackerLocation else uiState.localLocation

    // Skew-Immune Freshness Logic for Map
    fun calculateFreshness(loc: LocationState): Boolean {
        if (loc.timestamp <= 0) return false
        val telemetryAge = if (loc.telemetryTs > 0) now - loc.telemetryTs else Long.MAX_VALUE
        val sourceGpsAge = if (loc.telemetryTs > 0) maxOf(0L, loc.telemetryTs - loc.timestamp) else 0L
        return (telemetryAge + sourceGpsAge) < GPS_UI_FAIL_THRESHOLD_MS
    }

    val isTrackerFresh = calculateFreshness(trackerLoc)
    val isViewerFresh = calculateFreshness(viewerLoc)

    val trackerLat = trackerLoc.lat; val trackerLng = trackerLoc.lng
    val trackerBearing = trackerLoc.bearing; val trackerAccuracy = trackerLoc.accuracy
    val trackerMaxAcc = trackerLoc.maxAccuracy; val trackerSpeed = trackerLoc.speed
    val trackerLastValidFixRealtime = trackerLoc.lastValidFixRealtime
    val trackerLocationPending = trackerLoc.isLocationPending
    val trackerLocationPendingReason = trackerLoc.locationPendingReason
    val trackerIsAnchorLocked = trackerLoc.isAnchorLocked

    val viewerLat = viewerLoc.lat; val viewerLng = viewerLoc.lng
    val viewerBearing = viewerLoc.bearing; val viewerAccuracy = viewerLoc.accuracy
    val viewerMaxAcc = viewerLoc.maxAccuracy; val viewerSpeed = viewerLoc.speed
    val viewerLastValidFixRealtime = viewerLoc.lastValidFixRealtime
    val viewerLocationPending = viewerLoc.isLocationPending
    val viewerLocationPendingReason = viewerLoc.locationPendingReason

    val initialCenter = remember(uiState.trackerLocation.lat, uiState.localLocation.lat) {
        when {
            PhysicsUtils.isValidLocation(trackerLat, trackerLng) -> GeoPoint(trackerLat, trackerLng)
            PhysicsUtils.isValidLocation(viewerLat, viewerLng) -> GeoPoint(viewerLat, viewerLng)
            else -> GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
        }
    }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMap(
            lat = trackerLat, lng = trackerLng, bearing = trackerBearing, myLat = viewerLat, myLng = viewerLng, myB = viewerBearing,
            trail = trail, viewerTrail = viewerTrail, home = uiState.homePoints, onTap = { onEvent(UiEvent.MapTap(it)) },
            isFresh = isTrackerFresh, isMeFresh = isViewerFresh, maxD = uiState.maxDistance, onRemoveMarker = { if (!isTrackerMode) onEvent(UiEvent.RemoveHomePoint(it)) },
            violations = violations, isFenceVisible = uiState.isFenceVisible, isViolationsVisible = uiState.isViolationsVisible, isGeofenceViolationsVisible = uiState.isGeofenceViolationsVisible,
            accuracy = trackerAccuracy, maxAcc = trackerMaxAcc, speed = trackerSpeed, myAccuracy = viewerAccuracy, myMaxAcc = viewerMaxAcc, mySpeed = viewerSpeed,
            initialCenter = initialCenter, centeringTrackerTrigger = uiState.centeringTrackerTrigger, centeringViewerTrigger = uiState.centeringViewerTrigger,
            zoomInTrigger = uiState.zoomInTrigger, zoomOutTrigger = uiState.zoomOutTrigger, lastGpsTs = trackerLoc.timestamp, isTrackerMode = isTrackerMode,
            isLocked = uiState.isMapLocked, onLockChange = { onEvent(UiEvent.SetMapLocked(it)) }, mapViewRef = mapViewRef, geofenceMode = uiState.geofenceMode,
            systemPulse = now, systemPulseRealtime = systemPulseRealtime, isLocationPending = trackerLocationPending, locationPendingReason = trackerLocationPendingReason,
            lastValidFixRealtime = trackerLastValidFixRealtime, isMeLocationPending = viewerLocationPending, meLocationPendingReason = viewerLocationPendingReason, meLastValidFixRealtime = viewerLastValidFixRealtime
        )

        if (trackerIsAnchorLocked) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).background(BrandJd, RoundedCornerShape(4.dp)).border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp)); Text("ANCHOR LOCKED", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Text(text = BuildConfig.VERSION_NAME, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 4.dp, bottom = 2.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(2.dp)).padding(horizontal = 4.dp, vertical = 1.dp))

        if (showSettingsButton) {
            MapSettingsToggle(isMapButtonsVisible = uiState.isMapButtonsVisible, onToggle = { onEvent(UiEvent.SetMapButtonsVisible(!uiState.isMapButtonsVisible)) }, modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 12.dp))
        }
        
        if (showToolsOverlay && uiState.isMapButtonsVisible) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).fillMaxHeight(0.85f).width(140.dp)) { 
                    MapToolsOverlay(
                        isTrackerMode = isTrackerMode, trackerValid = PhysicsUtils.isValidLocation(trackerLat, trackerLng), viewerValid = PhysicsUtils.isValidLocation(viewerLat, viewerLng),
                        showFence = uiState.isFenceVisible, onToggleFence = { onEvent(UiEvent.SetFenceVisible(!uiState.isFenceVisible)) }, geofenceMode = uiState.geofenceMode, onSetGeofenceMode = { onEvent(UiEvent.SetGeofenceMode(it)) },
                        showViolations = uiState.isViolationsVisible, onToggleViolations = { onEvent(UiEvent.SetViolationsVisible(!uiState.isViolationsVisible)) },
                        showGeofenceViolations = uiState.isGeofenceViolationsVisible, onToggleGeofenceViolations = { onEvent(UiEvent.SetGeofenceViolationsVisible(!uiState.isGeofenceViolationsVisible)) },
                        onClear = onClearTrails, onSave = onSaveTrail, onLoad = onLoadTrail, onCenterTracker = { onEvent(UiEvent.CenterTracker) }, onCenterViewer = { onEvent(UiEvent.CenterViewer) }, onZoomIn = { onEvent(UiEvent.MapZoomIn) }, onZoomOut = { onEvent(UiEvent.MapZoomOut) }
                    ) 
                }
            }
        }

        if (trackerLocationPending && trackerLocationPendingReason != LocationPendingReason.NONE) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).background(Amber500.copy(alpha = 0.85f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(text = "UNCERTAINTY: ${trackerLocationPendingReason.name.replace("_", " ")}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun MapSettingsToggle(isMapButtonsVisible: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val purple = Color(0xFF800080); val backgroundColor = if (isMapButtonsVisible) purple.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.85f)
    Box(modifier = modifier.size(44.dp).background(backgroundColor, RoundedCornerShape(8.dp)).border(1.dp, purple.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).clickable { onToggle() }, contentAlignment = Alignment.Center) {
        Icon(imageVector = if (isMapButtonsVisible) Icons.Default.Close else Icons.Default.Settings, contentDescription = "Toggle Map Controls", tint = purple, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun OsmMap(
    lat: Double, lng: Double, bearing: Double, myLat: Double?, myLng: Double?, myB: Double?, 
    trail: List<TrailPoint>, viewerTrail: List<TrailPoint>, home: List<GeoPoint>, 
    onTap: (GeoPoint) -> Unit, isFresh: Boolean, isMeFresh: Boolean = true,
    maxD: Double, onRemoveMarker: (Int) -> Unit, violations: List<ViolationPoint>, 
    isFenceVisible: Boolean, isViolationsVisible: Boolean = true, isGeofenceViolationsVisible: Boolean = true, 
    accuracy: Double, maxAcc: Double, speed: Double = 0.0, 
    myAccuracy: Double? = null, myMaxAcc: Double = 0.0, mySpeed: Double = 0.0,
    initialCenter: GeoPoint? = null, centeringTrackerTrigger: Int = 0, centeringViewerTrigger: Int = 0,
    zoomInTrigger: Int = 0, zoomOutTrigger: Int = 0,
    lastGpsTs: Long = 0L, isTrackerMode: Boolean = false,
    isLocked: Boolean = true, onLockChange: (Boolean) -> Unit = {},
    mapViewRef: MutableState<MapView?> = remember { mutableStateOf(null) },
    geofenceMode: GeofenceMode = GeofenceMode.IDLE,
    systemPulse: Long = 0L,
    systemPulseRealtime: Long = 0L,
    isLocationPending: Boolean = false,
    locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    lastValidFixRealtime: Long = 0L,
    isMeLocationPending: Boolean = false,
    meLocationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    meLastValidFixRealtime: Long = 0L
) {
    val context = LocalContext.current; val resources = remember(context) { context.resources }
    val currentOnTap by rememberUpdatedState(onTap); val currentOnRemoveMarker by rememberUpdatedState(onRemoveMarker); val currentGeofenceMode by rememberUpdatedState(geofenceMode)
    
    val mapEventsReceiver = remember {
        object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (!isTrackerMode) { mapViewRef.value?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); currentOnTap(p) }
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean { return true }
        }
    }
    
    val mapEventsOverlay = remember { MapEventsOverlay(mapEventsReceiver) }; val density = resources.displayMetrics.density
    val trackerIconFresh = remember(density) { BitmapDrawable(resources, createTrackerBitmap(density, true)) }
    val trackerIconStale = remember(density) { BitmapDrawable(resources, createTrackerBitmap(density, false)) }
    val viewerIconFresh = remember(density) { BitmapDrawable(resources, createViewerBitmap(density, true)) }
    val viewerIconStale = remember(density) { BitmapDrawable(resources, createViewerBitmap(density, false)) }
    val jumpIcon = remember(density) { BitmapDrawable(resources, createJumpMarkerBitmap(density)) }
    val geofenceIcon = remember(density) { BitmapDrawable(resources, createGeofenceViolationBitmap(density)) }
    val homeIcons = remember(density) { mutableMapOf<Int, BitmapDrawable>() }

    val trackerMarkerRef = remember { mutableStateOf<Marker?>(null) }; val viewerMarkerRef = remember { mutableStateOf<Marker?>(null) }
    val trackerCircleRef = remember { mutableStateOf<Polygon?>(null) }; val viewerCircleRef = remember { mutableStateOf<Polygon?>(null) }
    val trailFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }; val viewerTrailFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }
    val fenceFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }; val homeMarkersFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }
    val accuracyCirclesFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }; val violationMarkersFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }; val violationAccuracyFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }

    val homeMarkerPool = remember { mutableListOf<Marker>() }; val violationMarkerPool = remember { mutableListOf<Marker>() }; val violationCirclePool = remember { mutableListOf<Polygon>() }
    val trackerPolylinePool = remember { mutableListOf<Polyline>() }; val viewerPolylinePool = remember { mutableListOf<Polyline>() }

    var lastTriggerTs by remember { mutableLongStateOf(0L) }
    val localLockStatus = remember { mutableStateOf(isLocked) }
    LaunchedEffect(isLocked) { localLockStatus.value = isLocked }

    val lastTrailRendered = remember { mutableStateOf<List<TrailPoint>?>(null) }; val lastViewerTrailRendered = remember { mutableStateOf<List<TrailPoint>?>(null) }
    val lastHomeRendered = remember { mutableStateOf<List<GeoPoint>?>(null) }; val lastViolationsRendered = remember { mutableStateOf<List<ViolationPoint>?>(null) }
    val lastFenceState = remember { mutableStateOf<Boolean?>(null) }; val lastViolationVisibility = remember { mutableStateOf<Pair<Boolean, Boolean>?>(null) }

    LaunchedEffect(localLockStatus.value, lat, lng, myLat, myLng, isFresh, isMeFresh) {
        if (localLockStatus.value) {
            if (systemPulse - lastTriggerTs < 500) return@LaunchedEffect
            val trackerValid = PhysicsUtils.isValidLocation(lat, lng); val meValid = myLat != null && myLng != null && PhysicsUtils.isValidLocation(myLat, myLng)
            val view = mapViewRef.value ?: return@LaunchedEffect
            if (trackerValid && meValid && isFresh && isMeFresh) {
                val dist = PhysicsUtils.calculateDistance(lat, lng, myLat!!, myLng!!)
                if (dist in 100.0..100000.0) {
                    val box = BoundingBox.fromGeoPoints(listOf(GeoPoint(lat, lng), GeoPoint(myLat, myLng)))
                    view.zoomToBoundingBox(box.increaseByScale(1.4f), false)
                    if (view.zoomLevelDouble > 18.0) view.controller.setZoom(18.0)
                } else view.controller.setCenter(GeoPoint(lat, lng))
            } else if (trackerValid || meValid) {
                val center = if (trackerValid) GeoPoint(lat, lng) else GeoPoint(myLat!!, myLng!!)
                view.controller.setCenter(center)
            }
        }
    }

    LaunchedEffect(centeringTrackerTrigger) {
        if (centeringTrackerTrigger > 0 && PhysicsUtils.isValidLocation(lat, lng)) {
            lastTriggerTs = systemPulse; mapViewRef.value?.controller?.animateTo(GeoPoint(lat, lng)); mapViewRef.value?.controller?.setZoom(18.0)
        }
    }

    LaunchedEffect(centeringViewerTrigger) {
        if (centeringViewerTrigger > 0 && myLat != null && myLng != null && PhysicsUtils.isValidLocation(myLat, myLng)) {
            lastTriggerTs = systemPulse; mapViewRef.value?.controller?.animateTo(GeoPoint(myLat, myLng)); mapViewRef.value?.controller?.setZoom(18.0)
        }
    }

    LaunchedEffect(zoomInTrigger) { if (zoomInTrigger > 0) mapViewRef.value?.controller?.zoomIn() }
    LaunchedEffect(zoomOutTrigger) { if (zoomOutTrigger > 0) mapViewRef.value?.controller?.zoomOut() }

    AndroidView(factory = { 
        MapView(context).apply { 
            mapViewRef.value = this; setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); isClickable = true
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            val sp = if (initialCenter != null && PhysicsUtils.isValidLocation(initialCenter.latitude, initialCenter.longitude)) initialCenter else GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
            controller.setZoom(18.0); controller.setCenter(sp)
            val scaleBar = ScaleBarOverlay(this).apply { setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.metric) }
            trackerMarkerRef.value = Marker(this).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
            viewerMarkerRef.value = Marker(this).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
            trackerCircleRef.value = Polygon(this).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 3f; setInfoWindow(null) }
            viewerCircleRef.value = Polygon(this).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 3f; setInfoWindow(null) }
            trailFolderRef.value = FolderOverlay(); viewerTrailFolderRef.value = FolderOverlay(); fenceFolderRef.value = FolderOverlay(); accuracyCirclesFolderRef.value = FolderOverlay()
            homeMarkersFolderRef.value = FolderOverlay(); violationMarkersFolderRef.value = FolderOverlay(); violationAccuracyFolderRef.value = FolderOverlay()
            
            overlays.add(trailFolderRef.value); overlays.add(viewerTrailFolderRef.value); overlays.add(violationAccuracyFolderRef.value); overlays.add(violationMarkersFolderRef.value)
            overlays.add(accuracyCirclesFolderRef.value); overlays.add(fenceFolderRef.value); overlays.add(mapEventsOverlay); overlays.add(homeMarkersFolderRef.value); overlays.add(scaleBar)
            
            addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                val wInt = v.width; val hInt = v.height
                if (wInt > 0 && hInt > 0) {
                    scaleBar.setCentred(true); val offX = (wInt / 2).toInt(); val offY = (hInt - (48 * density).toInt()).toInt(); scaleBar.setScaleBarOffset(offX, offY)
                }
            }
            overlays.add(object : Overlay() {
                override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) { localLockStatus.value = false; onLockChange(false) }
                    return false
                }
            })
        } 
    }, update = { view ->
        val trackerMarker = trackerMarkerRef.value ?: return@AndroidView; val viewerMarker = viewerMarkerRef.value ?: return@AndroidView
        val trackerValid = PhysicsUtils.isValidLocation(lat, lng); val meValid = myLat != null && myLng != null && PhysicsUtils.isValidLocation(myLat, myLng)

        if (lastHomeRendered.value != home || lastFenceState.value != isFenceVisible) {
            fenceFolderRef.value?.items?.clear(); homeMarkersFolderRef.value?.items?.clear()
            val activeHomeSize = if (isFenceVisible) home.size else 0
            if (homeMarkerPool.size > activeHomeSize + MARKER_POOL_PRUNE_THRESHOLD) {
                while (homeMarkerPool.size > maxOf(activeHomeSize + 5, MARKER_POOL_PRUNE_THRESHOLD)) homeMarkerPool.removeAt(homeMarkerPool.size - 1)
            }
            if (isFenceVisible) {
                home.forEachIndexed { idx, p ->
                    fenceFolderRef.value?.add(Polygon(view).apply { 
                        points = Polygon.pointsAsCircle(p, maxD).map { GeoPoint(it.latitude, it.longitude) }
                        fillPaint.color = 0x28CBD5E1.toInt(); outlinePaint.color = 0xC8CBD5E1.toInt(); outlinePaint.strokeWidth = 2f; setInfoWindow(null) 
                    })
                    val marker = if (idx < homeMarkerPool.size) homeMarkerPool[idx] else Marker(view).also { m -> m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); m.setInfoWindow(null); homeMarkerPool.add(m) }
                    marker.position = p; marker.icon = homeIcons.getOrPut(idx + 1) { BitmapDrawable(resources, createHomeBitmap(density, idx + 1)) }
                    marker.setOnMarkerClickListener { mk, mv -> 
                        if (!isTrackerMode && currentGeofenceMode == GeofenceMode.REMOVE) { mv.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); currentOnTap(p); currentOnRemoveMarker(idx); Toast.makeText(context, "Home point removed", Toast.LENGTH_SHORT).show() }
                        else if (!isTrackerMode && currentGeofenceMode == GeofenceMode.ADD) Toast.makeText(context, "Switch to DEL mode to remove points", Toast.LENGTH_SHORT).show()
                        true 
                    }
                    homeMarkersFolderRef.value?.add(marker)
                }
            }
            lastHomeRendered.value = home.toList(); lastFenceState.value = isFenceVisible
        }

        if (lastTrailRendered.value != trail || lastViewerTrailRendered.value != viewerTrail) {
            trailFolderRef.value?.items?.clear(); val trSegs = drawTrailToFolder(view, trailFolderRef.value!!, trail, BrandJd.toArgb(), Slate500.toArgb(), trackerPolylinePool)
            viewerTrailFolderRef.value?.items?.clear(); val viSegs = drawTrailToFolder(view, viewerTrailFolderRef.value!!, viewerTrail, ViewerCyan.toArgb(), Slate500.toArgb(), viewerPolylinePool)
            lastTrailRendered.value = trail.toList(); lastViewerTrailRendered.value = viewerTrail.toList()
            while(trackerPolylinePool.size > maxOf(trSegs + 5, MARKER_POOL_PRUNE_THRESHOLD)) trackerPolylinePool.removeAt(trackerPolylinePool.size - 1)
            while(viewerPolylinePool.size > maxOf(viSegs + 5, MARKER_POOL_PRUNE_THRESHOLD)) viewerPolylinePool.removeAt(viewerPolylinePool.size - 1)
        }

        val visibilityPair = Pair(isViolationsVisible, isGeofenceViolationsVisible)
        if (lastViolationsRendered.value != violations || lastViolationVisibility.value != visibilityPair) {
            violationMarkersFolderRef.value?.items?.clear(); violationAccuracyFolderRef.value?.items?.clear()
            val filteredViolations = violations.filter { v -> val isJump = v.type == ALERT_ID_JUMP_ALERT || v.type == ALERT_ID_VISUAL_JUMP; val isGeo = v.type == ALERT_ID_TRACKER_GEOFENCE; (isJump && isViolationsVisible) || (isGeo && isGeofenceViolationsVisible) }
            if (violationMarkerPool.size > filteredViolations.size + MARKER_POOL_PRUNE_THRESHOLD) {
                while (violationMarkerPool.size > maxOf(filteredViolations.size + 5, MARKER_POOL_PRUNE_THRESHOLD)) { violationMarkerPool.removeAt(violationMarkerPool.size - 1); violationCirclePool.removeAt(violationCirclePool.size - 1) }
            }
            filteredViolations.forEachIndexed { index, v ->
                val m = if (index < violationMarkerPool.size) violationMarkerPool[index] else Marker(view).also { m -> m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); m.setInfoWindow(null); violationMarkerPool.add(m) }
                val isJump = v.type == ALERT_ID_JUMP_ALERT || v.type == ALERT_ID_VISUAL_JUMP
                m.position = v.point; m.icon = if (isJump) jumpIcon else geofenceIcon; violationMarkersFolderRef.value?.add(m)
                val hAcc = if (v.maxAccuracy > 0.0) v.maxAccuracy else v.accuracy
                if (hAcc > 0.0) {
                    val c = if (index < violationCirclePool.size) violationCirclePool[index] else Polygon(view).also { p -> p.fillPaint.color = 0; p.outlinePaint.strokeWidth = 2f; p.setInfoWindow(null); violationCirclePool.add(p) }
                    c.points = Polygon.pointsAsCircle(v.point, hAcc).map { GeoPoint(it.latitude, it.longitude) }; c.outlinePaint.color = (if (isJump) 0x60FF00FF else 0x60FF0000).toInt(); violationAccuracyFolderRef.value?.add(c)
                }
            }
            lastViolationsRendered.value = violations.toList(); lastViolationVisibility.value = visibilityPair
        }

        accuracyCirclesFolderRef.value?.items?.clear()
        if (trackerValid) {
            val baseAcc = if (maxAcc > 0.0) maxAcc else accuracy
            if (baseAcc > 0.0) {
                trackerCircleRef.value?.let { p ->
                    val drift = if (isLocationPending && lastValidFixRealtime > 0) baseAcc + (if (speed > 1.0) speed.coerceIn(PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS, PENDING_UNCERTAINTY_SPEED_CAP_MPS) else PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS) * ((systemPulseRealtime - lastValidFixRealtime) / 1000.0) else baseAcc
                    p.points = Polygon.pointsAsCircle(GeoPoint(lat, lng), drift).map { GeoPoint(it.latitude, it.longitude) }
                    p.outlinePaint.color = if (isFresh) BrandJd.copy(alpha = 0.7f).toArgb() else Slate500.copy(alpha = 0.7f).toArgb(); accuracyCirclesFolderRef.value?.add(p)
                }
            }
            trackerMarker.position = GeoPoint(lat, lng); trackerMarker.icon = if (isFresh) trackerIconFresh else trackerIconStale
            if (!view.overlays.contains(trackerMarker)) view.overlays.add(trackerMarker) 
        } else view.overlays.remove(trackerMarker)
        
        if (meValid) {
            val baseMyAcc = if (myMaxAcc > 0.0) myMaxAcc else myAccuracy!!
            if (baseMyAcc > 0.0) {
                viewerCircleRef.value?.let { p ->
                    val drift = if (isMeLocationPending && meLastValidFixRealtime > 0) baseMyAcc + (if (mySpeed > 1.0) mySpeed.coerceIn(PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS, PENDING_UNCERTAINTY_SPEED_CAP_MPS) else PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS) * ((systemPulseRealtime - meLastValidFixRealtime) / 1000.0) else baseMyAcc
                    p.points = Polygon.pointsAsCircle(GeoPoint(myLat!!, myLng!!), drift).map { GeoPoint(it.latitude, it.longitude) }
                    p.outlinePaint.color = if (isMeFresh) ViewerCyan.copy(alpha = 0.7f).toArgb() else Slate500.copy(alpha = 0.7f).toArgb(); accuracyCirclesFolderRef.value?.add(p)
                }
            }
            viewerMarker.position = GeoPoint(myLat!!, myLng!!); viewerMarker.icon = if (isMeFresh) viewerIconFresh else viewerIconStale
            if (!view.overlays.contains(viewerMarker)) view.overlays.add(viewerMarker) 
        } else view.overlays.remove(viewerMarker)
        view.invalidate()
    }, onRelease = { view -> view.onDetach(); view.tileProvider.tileCache.clear(); view.tileProvider.detach() }, modifier = Modifier.fillMaxSize())
}

private fun drawTrailToFolder(view: MapView, folder: FolderOverlay, trailPoints: List<TrailPoint>, colorNormal: Int, colorHindsight: Int, pool: MutableList<Polyline>): Int {
    if (trailPoints.isEmpty()) return 0
    var poolIdx = 0; var startIdx = 0
    while (startIdx < trailPoints.size) {
        val segmentPoints = mutableListOf<GeoPoint>(); val isHindsight = trailPoints[startIdx].isHindsightCorrected; var currentIdx = startIdx
        while (currentIdx < trailPoints.size) {
            val pt = trailPoints[currentIdx]; if ((pt.isJump || pt.isHindsightCorrected != isHindsight) && currentIdx > startIdx) break
            segmentPoints.add(pt.toGeoPoint()); currentIdx++
            if (pt.isJump) { startIdx = currentIdx; break }
        }
        if (segmentPoints.size > 1) {
            val line = if (poolIdx < pool.size) pool[poolIdx] else Polyline(view).also { l -> l.outlinePaint.strokeWidth = 4f; l.setInfoWindow(null); pool.add(l) }
            line.setPoints(segmentPoints); line.outlinePaint.color = if (isHindsight) colorHindsight else colorNormal; folder.add(line); poolIdx++
        }
        if (currentIdx == trailPoints.size) break
        startIdx = if (startIdx < currentIdx) (if (!trailPoints[currentIdx - 1].isJump) currentIdx - 1 else currentIdx) else startIdx + 1
    }
    return poolIdx
}

private fun createTrackerBitmap(density: Float, isFresh: Boolean): Bitmap { 
    val sz = (32 * density).toInt(); val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888); val c = Canvas(b); val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.STROKE; p.color = android.graphics.Color.WHITE; p.strokeWidth = density; c.drawCircle(sz/2f, sz/2f, sz/2f - density, p)
    p.color = if (isFresh) BrandJd.toArgb() else Slate500.toArgb(); p.strokeWidth = 3f * density; c.drawCircle(sz/2f, sz/2f, sz/2f - 3.5f * density, p)
    return b
}

private fun createViewerBitmap(density: Float, isFresh: Boolean): Bitmap {
    val sz = (20 * density).toInt(); val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888); val c = Canvas(b); val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.STROKE; p.color = android.graphics.Color.WHITE; p.strokeWidth = density; c.drawCircle(sz/2f, sz/2f, sz/2f - density, p)
    p.color = if (isFresh) ViewerCyan.toArgb() else Slate500.toArgb(); p.style = Paint.Style.FILL; c.drawCircle(sz/2f, sz/2f, sz/2f - 3.5f * density, p)
    return b
}

private fun createHomeBitmap(density: Float, index: Int): Bitmap {
    val sz = (32 * density).toInt(); val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888); val c = Canvas(b); val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = 0xFFCBD5E1.toInt(); c.drawCircle(sz/2f, sz/2f, sz/2.2f, p); p.style = Paint.Style.STROKE; p.color = 0xFF334155.toInt(); p.strokeWidth = 2 * density; c.drawCircle(sz/2f, sz/2f, sz/2.2f, p)
    p.style = Paint.Style.FILL; p.color = android.graphics.Color.RED; p.textSize = 14 * density; p.textAlign = Paint.Align.CENTER; p.isFakeBoldText = true; c.drawText("$index", sz/2f, sz/2f + (p.textSize/3f), p)
    return b
}

private fun createJumpMarkerBitmap(density: Float): Bitmap {
    val sz = (10 * density).toInt(); val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888); val c = Canvas(b); val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.STROKE; p.color = 0xFFFF00FF.toInt(); p.strokeWidth = 2f * density; val off = p.strokeWidth / 2f; c.drawRect(off, off, sz - off, sz - off, p)
    return b
}

private fun createGeofenceViolationBitmap(density: Float): Bitmap {
    val sz = (12 * density).toInt(); val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888); val c = Canvas(b); val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.STROKE; p.color = android.graphics.Color.RED; p.strokeWidth = 2f * density; val off = p.strokeWidth / 2f; c.drawCircle(sz/2f, sz/2f, sz/2f - off, p)
    return b
}

@Composable
fun MapToolsOverlay(
    isTrackerMode: Boolean, trackerValid: Boolean = true, viewerValid: Boolean = true, showFence: Boolean, onToggleFence: () -> Unit,
    geofenceMode: GeofenceMode, onSetGeofenceMode: (GeofenceMode) -> Unit, showViolations: Boolean = true, onToggleViolations: () -> Unit = {},
    showGeofenceViolations: Boolean = true, onToggleGeofenceViolations: () -> Unit = {}, onClear: () -> Unit, onSave: () -> Unit, 
    onToggleLog: () -> Unit = {}, onSaveTrail: () -> Unit = {}, onLoad: () -> Unit, onCenterTracker: () -> Unit = {}, onCenterViewer: () -> Unit = {},
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
            MapToolButton(icon = Icons.Default.AddLocation, label = "ADD", onClick = { if (!curTrk) onSetGeofenceMode(GeofenceMode.ADD) }, iconColor = if (curGeo == GeofenceMode.ADD) Color.White else BrandJd, containerColor = if (curGeo == GeofenceMode.ADD) BrandJd else Color.Transparent)
            MapToolButton(icon = Icons.Default.WrongLocation, label = "DEL", onClick = { if (!curTrk) onSetGeofenceMode(GeofenceMode.REMOVE) }, iconColor = if (curGeo == GeofenceMode.REMOVE) Color.White else Rose500, containerColor = if (curGeo == GeofenceMode.REMOVE) Rose500 else Color.Transparent)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(sp)) { MapToolButton(icon = if (curFnc) Icons.Default.Visibility else Icons.Default.VisibilityOff, label = "FENCE", onClick = onToggleFence, iconColor = if (curFnc) BrandJd else Color.Gray); MapToolButton(icon = Icons.Default.Delete, label = "CLEAR", onClick = { onCenterTracker(); onClear() }, iconColor = Rose500) }
    }
}

@Composable
fun MapToolButton(icon: ImageVector? = null, symbol: String? = null, label: String, onClick: () -> Unit, iconColor: Color, containerColor: Color = Color.Transparent) {
    val prp = Color(0xFF800080); Box(modifier = Modifier.size(50.dp).background(containerColor, RoundedCornerShape(8.dp)).border(0.5.dp, prp, RoundedCornerShape(8.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (symbol != null) Text(symbol, color = iconColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.height(26.dp))
            else if (icon != null) Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Text(label, color = if (containerColor != Color.Transparent) Color.White else prp, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}
