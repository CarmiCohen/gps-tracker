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
import androidx.compose.runtime.snapshots.SnapshotStateList
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
 * v8.9.78:
 * - Issue #016: Optimized trail rendering. Removed redundant O(N) count passes and minimized allocations.
 * - Issue #018: Stationary Anchor Hard-Lock. Added visual indicator to Map UI.
 * v8.9.75:
 * - Issue #014: Type Safety Optimization. Standardized telemetry fields to Double.
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
    
    val trackerLat = if (isTrackerMode) uiState.localLocation.lat else uiState.trackerLocation.lat
    val trackerLng = if (isTrackerMode) uiState.localLocation.lng else uiState.trackerLocation.lng
    val trackerBearing = if (isTrackerMode) uiState.localLocation.bearing else uiState.trackerLocation.bearing
    val trackerAccuracy = if (isTrackerMode) uiState.localLocation.accuracy else uiState.trackerLocation.accuracy
    val trackerMaxAcc = if (isTrackerMode) uiState.localLocation.maxAccuracy else uiState.trackerLocation.maxAccuracy
    val trackerSpeed = if (isTrackerMode) uiState.localLocation.speed else uiState.trackerLocation.speed
    
    val trackerLastValidFixRealtime = if (isTrackerMode) uiState.localLocation.lastValidFixRealtime else uiState.trackerLocation.lastValidFixRealtime
    val trackerLocationPending = if (isTrackerMode) uiState.localLocation.isLocationPending else uiState.trackerLocation.isLocationPending
    val trackerLocationPendingReason = if (isTrackerMode) uiState.localLocation.locationPendingReason else uiState.trackerLocation.locationPendingReason
    val trackerIsAnchorLocked = if (isTrackerMode) uiState.localLocation.isAnchorLocked else uiState.trackerLocation.isAnchorLocked

    val viewerLat = if (isTrackerMode) uiState.trackerLocation.lat else uiState.localLocation.lat
    val viewerLng = if (isTrackerMode) uiState.trackerLocation.lng else uiState.localLocation.lng
    val viewerBearing = if (isTrackerMode) uiState.trackerLocation.bearing else uiState.localLocation.bearing
    val viewerAccuracy = if (isTrackerMode) uiState.trackerLocation.accuracy else uiState.localLocation.accuracy
    val viewerMaxAcc = if (isTrackerMode) uiState.trackerLocation.maxAccuracy else uiState.localLocation.maxAccuracy
    val viewerSpeed = if (isTrackerMode) uiState.trackerLocation.speed else uiState.localLocation.speed
    
    val viewerLastValidFixRealtime = if (isTrackerMode) uiState.trackerLocation.lastValidFixRealtime else uiState.localLocation.lastValidFixRealtime
    val viewerLocationPending = if (isTrackerMode) uiState.trackerLocation.isLocationPending else uiState.localLocation.isLocationPending
    val viewerLocationPendingReason = if (isTrackerMode) uiState.trackerLocation.locationPendingReason else uiState.localLocation.locationPendingReason
    
    val trackerGpsAge = if (isTrackerMode) (if (uiState.localLocation.timestamp > 0) now - uiState.localLocation.timestamp else Long.MAX_VALUE)
                 else (if (uiState.trackerLocation.timestamp > 0) now - uiState.trackerLocation.timestamp else Long.MAX_VALUE)
    
    val isTrackerFresh = trackerGpsAge < GPS_UI_FAIL_THRESHOLD_MS

    val viewerGpsAge = if (isTrackerMode) (if (uiState.trackerLocation.timestamp > 0) now - uiState.trackerLocation.timestamp else Long.MAX_VALUE)
                       else (if (uiState.localLocation.timestamp > 0) now - uiState.localLocation.timestamp else Long.MAX_VALUE)
    val isViewerFresh = viewerGpsAge < GPS_UI_FAIL_THRESHOLD_MS

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
            lat = trackerLat,
            lng = trackerLng,
            bearing = trackerBearing,
            myLat = viewerLat,
            myLng = viewerLng,
            myB = viewerBearing,
            trail = trail,
            viewerTrail = viewerTrail,
            home = uiState.homePoints,
            onTap = { onEvent(UiEvent.MapTap(it)) },
            isFresh = isTrackerFresh,
            isMeFresh = isViewerFresh,
            maxD = uiState.maxDistance,
            onRemoveMarker = { if (!isTrackerMode) onEvent(UiEvent.RemoveHomePoint(it)) },
            violations = violations,
            isFenceVisible = uiState.isFenceVisible,
            isViolationsVisible = uiState.isViolationsVisible,
            isGeofenceViolationsVisible = uiState.isGeofenceViolationsVisible,
            accuracy = trackerAccuracy,
            maxAcc = trackerMaxAcc,
            speed = trackerSpeed,
            myAccuracy = viewerAccuracy,
            myMaxAcc = viewerMaxAcc,
            mySpeed = viewerSpeed,
            initialCenter = initialCenter,
            centeringTrackerTrigger = uiState.centeringTrackerTrigger,
            centeringViewerTrigger = uiState.centeringViewerTrigger,
            zoomInTrigger = uiState.zoomInTrigger,
            zoomOutTrigger = uiState.zoomOutTrigger,
            lastGpsTs = if (isTrackerMode) uiState.localLocation.timestamp else uiState.trackerLocation.timestamp,
            isTrackerMode = isTrackerMode,
            isLocked = uiState.isMapLocked,
            onLockChange = { onEvent(UiEvent.SetMapLocked(it)) },
            mapViewRef = mapViewRef,
            geofenceMode = uiState.geofenceMode,
            systemPulse = now,
            systemPulseRealtime = systemPulseRealtime,
            isLocationPending = trackerLocationPending,
            locationPendingReason = trackerLocationPendingReason,
            lastValidFixRealtime = trackerLastValidFixRealtime,
            isMeLocationPending = viewerLocationPending,
            meLocationPendingReason = viewerLocationPendingReason,
            meLastValidFixRealtime = viewerLastValidFixRealtime
        )

        if (trackerIsAnchorLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(BrandJd, RoundedCornerShape(4.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ANCHOR LOCKED", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Text(
            text = BuildConfig.VERSION_NAME,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 4.dp, bottom = 2.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
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
                        trackerValid = PhysicsUtils.isValidLocation(trackerLat, trackerLng),
                        viewerValid = PhysicsUtils.isValidLocation(viewerLat, viewerLng),
                        showFence = uiState.isFenceVisible,
                        onToggleFence = { onEvent(UiEvent.SetFenceVisible(!uiState.isFenceVisible)) },
                        geofenceMode = uiState.geofenceMode,
                        onSetGeofenceMode = { onEvent(UiEvent.SetGeofenceMode(it)) },
                        showViolations = uiState.isViolationsVisible,
                        onToggleViolations = { onEvent(UiEvent.SetViolationsVisible(!uiState.isViolationsVisible)) },
                        showGeofenceViolations = uiState.isGeofenceViolationsVisible,
                        onToggleGeofenceViolations = { onEvent(UiEvent.SetGeofenceViolationsVisible(!uiState.isGeofenceViolationsVisible)) },
                        onClear = onClearTrails,
                        onSave = onSaveTrail,
                        onLoad = onLoadTrail,
                        onCenterTracker = { onEvent(UiEvent.CenterTracker) },
                        onCenterViewer = { onEvent(UiEvent.CenterViewer) },
                        onZoomIn = { onEvent(UiEvent.MapZoomIn) },
                        onZoomOut = { onEvent(UiEvent.MapZoomOut) }
                    ) 
                }
            }
        }

        if (trackerLocationPending && trackerLocationPendingReason != LocationPendingReason.NONE) {
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 120.dp).background(Amber500.copy(alpha = 0.85f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(
                    text = "UNCERTAINTY: ${trackerLocationPendingReason.name.replace("_", " ")}",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun MapSettingsToggle(
    isMapButtonsVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val purple = Color(0xFF800080)
    val backgroundColor = if (isMapButtonsVisible) purple.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.85f)
    
    Box(
        modifier = modifier
            .size(44.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, purple.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isMapButtonsVisible) Icons.Default.Close else Icons.Default.Settings,
            contentDescription = "Toggle Map Controls",
            tint = purple,
            modifier = Modifier.size(24.dp)
        )
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
    val context = LocalContext.current
    val resources = remember(context) { context.resources }

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnRemoveMarker by rememberUpdatedState(onRemoveMarker)
    val currentGeofenceMode by rememberUpdatedState(geofenceMode)
    
    val mapEventsReceiver = remember {
        object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (!isTrackerMode) {
                    mapViewRef.value?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    currentOnTap(p)
                }
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean { return true }
        }
    }
    
    val mapEventsOverlay = remember { MapEventsOverlay(mapEventsReceiver) }
    val density = resources.displayMetrics.density
    val trackerBitmapFresh = remember(density) { createTrackerBitmap(density, true) }
    val trackerBitmapStale = remember(density) { createTrackerBitmap(density, false) }
    val viewerBitmapFresh = remember(density) { createViewerBitmap(density, true) }
    val viewerBitmapStale = remember(density) { createViewerBitmap(density, false) }
    val jumpBitmap = remember(density) { createJumpMarkerBitmap(density) }
    val geofenceBitmap = remember(density) { createGeofenceViolationBitmap(density) }
    
    val homeBitmaps = remember(density) { mutableMapOf<Int, Bitmap>() }

    val trackerMarkerRef = remember { mutableStateOf<Marker?>(null) }
    val viewerMarkerRef = remember { mutableStateOf<Marker?>(null) }
    val trackerCircleRef = remember { mutableStateOf<Polygon?>(null) }
    val viewerCircleRef = remember { mutableStateOf<Polygon?>(null) }
    val trailFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }
    val viewerTrailFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }
    val fenceFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }
    val homeMarkersFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }
    val accuracyCirclesFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }
    val violationMarkersFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }
    val violationAccuracyFolderRef = remember { mutableStateOf<FolderOverlay?>(null) }

    val violationMarkerPool = remember { mutableStateListOf<Marker>() }
    val violationCirclePool = remember { mutableStateListOf<Polygon>() }
    val homeMarkerPool = remember { mutableStateListOf<Marker>() }
    val trackerPolylinePool = remember { mutableStateListOf<Polyline>() }
    val viewerPolylinePool = remember { mutableStateListOf<Polyline>() }

    var lastTriggerTs by remember { mutableLongStateOf(0L) }

    val lastTrailRendered = remember { mutableStateOf<List<TrailPoint>?>(null) }
    val lastViewerTrailRendered = remember { mutableStateOf<List<TrailPoint>?>(null) }
    val lastHomeRendered = remember { mutableStateOf<List<GeoPoint>?>(null) }
    val lastViolationsRendered = remember { mutableStateOf<List<ViolationPoint>?>(null) }
    val lastFenceState = remember { mutableStateOf<Boolean?>(null) }
    val lastViolationVisibility = remember { mutableStateOf<Pair<Boolean, Boolean>?>(null) }

    LaunchedEffect(isLocked, lat, lng, myLat, myLng, isFresh, isMeFresh) {
        if (isLocked) {
            if (systemPulse - lastTriggerTs < 500) return@LaunchedEffect
            val trackerValid = PhysicsUtils.isValidLocation(lat, lng)
            val meValid = myLat != null && myLng != null && PhysicsUtils.isValidLocation(myLat, myLng)
            val view = mapViewRef.value ?: return@LaunchedEffect
            if (trackerValid && meValid && isFresh && isMeFresh) {
                val dist = PhysicsUtils.calculateDistance(lat, lng, myLat!!, myLng!!)
                if (dist in 100.0..100000.0) {
                    val box = BoundingBox.fromGeoPoints(listOf(GeoPoint(lat, lng), GeoPoint(myLat, myLng)))
                    view.zoomToBoundingBox(box.increaseByScale(1.4f), false)
                    if (view.zoomLevelDouble > 18.0) view.controller.setZoom(18.0)
                } else {
                    view.controller.setCenter(GeoPoint(lat, lng))
                }
            } else if (trackerValid || meValid) {
                val center = if (trackerValid) GeoPoint(lat, lng) else GeoPoint(myLat!!, myLng!!)
                view.controller.setCenter(center)
            }
        }
    }

    LaunchedEffect(centeringTrackerTrigger) {
        if (centeringTrackerTrigger > 0 && PhysicsUtils.isValidLocation(lat, lng)) {
            lastTriggerTs = systemPulse
            mapViewRef.value?.controller?.animateTo(GeoPoint(lat, lng))
            mapViewRef.value?.controller?.setZoom(18.0)
        }
    }

    LaunchedEffect(centeringViewerTrigger) {
        if (centeringViewerTrigger > 0 && myLat != null && myLng != null && PhysicsUtils.isValidLocation(myLat, myLng)) {
            lastTriggerTs = systemPulse
            mapViewRef.value?.controller?.animateTo(GeoPoint(myLat, myLng))
            mapViewRef.value?.controller?.setZoom(18.0)
        }
    }

    LaunchedEffect(zoomInTrigger) { if (zoomInTrigger > 0) mapViewRef.value?.controller?.zoomIn() }
    LaunchedEffect(zoomOutTrigger) { if (zoomOutTrigger > 0) mapViewRef.value?.controller?.zoomOut() }

    AndroidView(factory = { 
        MapView(context).apply { 
            mapViewRef.value = this
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isClickable = true
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            val sp = if (initialCenter != null && PhysicsUtils.isValidLocation(initialCenter.latitude, initialCenter.longitude)) initialCenter else GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
            controller.setZoom(18.0)
            controller.setCenter(sp)
            val scaleBar = ScaleBarOverlay(this).apply { setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.metric) }
            trackerMarkerRef.value = Marker(this).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
            viewerMarkerRef.value = Marker(this).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
            trackerCircleRef.value = Polygon(this).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 3f; setInfoWindow(null) }
            viewerCircleRef.value = Polygon(this).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 3f; setInfoWindow(null) }
            trailFolderRef.value = FolderOverlay()
            viewerTrailFolderRef.value = FolderOverlay()
            fenceFolderRef.value = FolderOverlay()
            accuracyCirclesFolderRef.value = FolderOverlay()
            homeMarkersFolderRef.value = FolderOverlay()
            violationMarkersFolderRef.value = FolderOverlay()
            violationAccuracyFolderRef.value = FolderOverlay()
            
            overlays.add(trailFolderRef.value)
            overlays.add(viewerTrailFolderRef.value)
            overlays.add(violationAccuracyFolderRef.value)
            overlays.add(violationMarkersFolderRef.value)
            overlays.add(accuracyCirclesFolderRef.value)
            overlays.add(fenceFolderRef.value)
            overlays.add(mapEventsOverlay)
            overlays.add(homeMarkersFolderRef.value)
            overlays.add(scaleBar)
            
            addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                val wInt = v.width
                val hInt = v.height
                if (wInt > 0 && hInt > 0) {
                    scaleBar.setCentred(true)
                    val offX = (wInt / 2).toInt()
                    val offY = (hInt - (48 * density).toInt()).toInt()
                    scaleBar.setScaleBarOffset(offX, offY)
                }
            }
            overlays.add(object : Overlay() {
                override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) onLockChange(false)
                    return false
                }
            })
        } 
    }, update = { view ->
        val trackerMarker = trackerMarkerRef.value ?: return@AndroidView
        val viewerMarker = viewerMarkerRef.value ?: return@AndroidView
        val trackerValid = PhysicsUtils.isValidLocation(lat, lng)
        val meValid = myLat != null && myLng != null && PhysicsUtils.isValidLocation(myLat, myLng)

        if (lastHomeRendered.value != home || lastFenceState.value != isFenceVisible) {
            fenceFolderRef.value?.items?.clear()
            val homeFolder = homeMarkersFolderRef.value!!
            homeFolder.items.clear()
            
            val activeHomeSize = if (isFenceVisible) home.size else 0
            if (homeMarkerPool.size > activeHomeSize + MARKER_POOL_PRUNE_THRESHOLD) {
                val keepSize = maxOf(activeHomeSize + 5, MARKER_POOL_PRUNE_THRESHOLD)
                while (homeMarkerPool.size > keepSize) {
                    homeMarkerPool.removeAt(homeMarkerPool.size - 1)
                }
            }
            
            if (isFenceVisible) {
                home.forEachIndexed { idx, p ->
                    fenceFolderRef.value?.add(Polygon(view).apply { 
                        val circlePoints = Polygon.pointsAsCircle(p, maxD)
                        this.points = circlePoints.map { GeoPoint(it.latitude, it.longitude) }
                        fillPaint.color = 0x28CBD5E1.toInt()
                        outlinePaint.color = 0xC8CBD5E1.toInt()
                        outlinePaint.strokeWidth = 2f
                        setInfoWindow(null) 
                    })
                    
                    val marker = if (idx < homeMarkerPool.size) {
                        homeMarkerPool[idx]
                    } else {
                        val m = Marker(view)
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        m.setInfoWindow(null)
                        homeMarkerPool.add(m)
                        m
                    }
                    marker.position = p
                    marker.icon = BitmapDrawable(resources, homeBitmaps.getOrPut(idx + 1) { createHomeBitmap(density, idx + 1) })
                    marker.setOnMarkerClickListener { mk, mv -> 
                        if (!isTrackerMode && currentGeofenceMode == GeofenceMode.REMOVE) {
                            mv.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            currentOnTap(p)
                            currentOnRemoveMarker(idx)
                            Toast.makeText(context, "Home point removed", Toast.LENGTH_SHORT).show()
                        } else if (!isTrackerMode && currentGeofenceMode == GeofenceMode.ADD) {
                            Toast.makeText(context, "Switch to DEL mode to remove points", Toast.LENGTH_SHORT).show()
                        }
                        true 
                    }
                    homeFolder.add(marker)
                }
            }
            lastHomeRendered.value = home.toList()
            lastFenceState.value = isFenceVisible
        }

        if (lastTrailRendered.value != trail || lastViewerTrailRendered.value != viewerTrail) {
            trailFolderRef.value?.items?.clear()
            val trSegs = drawTrailToFolder(view, trailFolderRef.value!!, trail, BrandJd.toArgb(), Slate500.toArgb(), trackerPolylinePool)
            
            viewerTrailFolderRef.value?.items?.clear()
            val viSegs = drawTrailToFolder(view, viewerTrailFolderRef.value!!, viewerTrail, ViewerOrange.toArgb(), Slate500.toArgb(), viewerPolylinePool)
            
            lastTrailRendered.value = trail.toList()
            lastViewerTrailRendered.value = viewerTrail.toList()

            if (trackerPolylinePool.size > trSegs + MARKER_POOL_PRUNE_THRESHOLD) {
                val keep = maxOf(trSegs + 5, MARKER_POOL_PRUNE_THRESHOLD)
                while(trackerPolylinePool.size > keep) trackerPolylinePool.removeAt(trackerPolylinePool.size - 1)
            }
            if (viewerPolylinePool.size > viSegs + MARKER_POOL_PRUNE_THRESHOLD) {
                val keep = maxOf(viSegs + 5, MARKER_POOL_PRUNE_THRESHOLD)
                while(viewerPolylinePool.size > keep) viewerPolylinePool.removeAt(viewerPolylinePool.size - 1)
            }
        }

        val visibilityPair = Pair(isViolationsVisible, isGeofenceViolationsVisible)
        if (lastViolationsRendered.value != violations || lastViolationVisibility.value != visibilityPair) {
            val markerFolder = violationMarkersFolderRef.value!!
            val accuracyFolder = violationAccuracyFolderRef.value!!
            markerFolder.items.clear()
            accuracyFolder.items.clear()
            
            val filteredViolations = violations.filter { v ->
                val isJumpViolation = v.type == ALERT_ID_JUMP_ALERT || v.type == ALERT_ID_VISUAL_JUMP
                val isGeofence = v.type == ALERT_ID_TRACKER_GEOFENCE
                (isJumpViolation && isViolationsVisible) || (isGeofence && isGeofenceViolationsVisible)
            }

            if (violationMarkerPool.size > filteredViolations.size + MARKER_POOL_PRUNE_THRESHOLD) {
                val keepSize = maxOf(filteredViolations.size + 5, MARKER_POOL_PRUNE_THRESHOLD)
                while (violationMarkerPool.size > keepSize) violationMarkerPool.removeAt(violationMarkerPool.size - 1)
                while (violationCirclePool.size > keepSize) violationCirclePool.removeAt(violationCirclePool.size - 1)
            }

            filteredViolations.forEachIndexed { index, v ->
                val marker = if (index < violationMarkerPool.size) {
                    violationMarkerPool[index]
                } else {
                    val m = Marker(view)
                    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    m.setInfoWindow(null)
                    violationMarkerPool.add(m)
                    m
                }
                val isJumpViolation = v.type == ALERT_ID_JUMP_ALERT || v.type == ALERT_ID_VISUAL_JUMP
                marker.position = v.point
                marker.icon = BitmapDrawable(resources, if (isJumpViolation) jumpBitmap else geofenceBitmap)
                markerFolder.add(marker)

                val historicalAcc = if (v.maxAccuracy > 0.0) v.maxAccuracy else v.accuracy
                if (historicalAcc > 0.0) {
                    val circle = if (index < violationCirclePool.size) {
                        violationCirclePool[index]
                    } else {
                        val p = Polygon(view).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 2f; setInfoWindow(null) }
                        violationCirclePool.add(p)
                        p
                    }
                    val circlePoints = Polygon.pointsAsCircle(v.point, historicalAcc)
                    circle.points = circlePoints.map { GeoPoint(it.latitude, it.longitude) }
                    circle.outlinePaint.color = (if (isJumpViolation) 0x60FF00FF else 0x60FF0000).toInt()
                    accuracyFolder.add(circle)
                }
            }
            
            lastViolationsRendered.value = violations.toList()
            lastViolationVisibility.value = visibilityPair
        }

        accuracyCirclesFolderRef.value?.items?.clear()
        if (trackerValid) {
            val baseAcc = if (maxAcc > 0.0) maxAcc else accuracy
            if (baseAcc > 0.0) {
                trackerCircleRef.value?.let { p ->
                    val effectiveAccuracy = if (isLocationPending && lastValidFixRealtime > 0) {
                        val elapsedSec = (systemPulseRealtime - lastValidFixRealtime) / 1000.0
                        val driftRate = if (speed > 1.0) {
                            speed.coerceIn(PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS, PENDING_UNCERTAINTY_SPEED_CAP_MPS)
                        } else {
                            PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS
                        }
                        baseAcc + (driftRate * elapsedSec)
                    } else {
                        baseAcc
                    }
                    
                    val circlePoints = Polygon.pointsAsCircle(GeoPoint(lat, lng), effectiveAccuracy)
                    p.points = circlePoints.map { GeoPoint(it.latitude, it.longitude) }
                    p.outlinePaint.color = if (isFresh) BrandJd.copy(alpha = 0.7f).toArgb() else Slate500.copy(alpha = 0.7f).toArgb()
                    accuracyCirclesFolderRef.value?.add(p)
                }
            }
        }
        if (meValid && myAccuracy != null) {
            val baseMyAcc = if (myMaxAcc > 0.0) myMaxAcc else myAccuracy
            if (baseMyAcc > 0.0) {
                viewerCircleRef.value?.let { p ->
                    val effectiveMyAccuracy = if (isMeLocationPending && meLastValidFixRealtime > 0) {
                        val elapsedSec = (systemPulseRealtime - meLastValidFixRealtime) / 1000.0
                        val driftRate = if (mySpeed > 1.0) {
                            mySpeed.coerceIn(PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS, PENDING_UNCERTAINTY_SPEED_CAP_MPS)
                        } else {
                            PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS
                        }
                        baseMyAcc + (driftRate * elapsedSec)
                    } else {
                        baseMyAcc
                    }
                    
                    val circlePoints = Polygon.pointsAsCircle(GeoPoint(myLat!!, myLng!!), effectiveMyAccuracy)
                    p.points = circlePoints.map { GeoPoint(it.latitude, it.longitude) }
                    p.outlinePaint.color = if (isMeFresh) ViewerOrange.copy(alpha = 0.7f).toArgb() else Slate500.copy(alpha = 0.7f).toArgb()
                    accuracyCirclesFolderRef.value?.add(p)
                }
            }
        }

        if (trackerValid) { 
            trackerMarker.position = GeoPoint(lat, lng)
            trackerMarker.icon = BitmapDrawable(resources, if (isFresh) trackerBitmapFresh else trackerBitmapStale)
            if (!view.overlays.contains(trackerMarker)) view.overlays.add(trackerMarker) 
        } else view.overlays.remove(trackerMarker)
        
        if (meValid) { 
            viewerMarker.position = GeoPoint(myLat!!, myLng!!)
            viewerMarker.icon = BitmapDrawable(resources, if (isMeFresh) viewerBitmapFresh else viewerBitmapStale)
            if (!view.overlays.contains(viewerMarker)) view.overlays.add(viewerMarker) 
        } else view.overlays.remove(viewerMarker)
        
        view.invalidate()
    }, onRelease = { view -> 
        view.onDetach() 
        view.tileProvider.tileCache.clear()
        view.tileProvider.detach()
    }, modifier = Modifier.fillMaxSize())
}

private fun drawTrailToFolder(view: MapView, folder: FolderOverlay, trailPoints: List<TrailPoint>, colorNormal: Int, colorHindsight: Int, pool: SnapshotStateList<Polyline>): Int {
    if (trailPoints.isEmpty()) return 0
    
    var currentSegment = mutableListOf<GeoPoint>()
    var isCurrentSegmentHindsight = false
    var poolIdx = 0
    
    trailPoints.forEachIndexed { idx, pt -> 
        if (pt.isJump) { 
            if (currentSegment.isNotEmpty()) { 
                addTrailSegmentToFolder(view, folder, currentSegment, if (isCurrentSegmentHindsight) colorHindsight else colorNormal, pool, poolIdx++); 
                currentSegment = mutableListOf() 
            } 
        } else {
            if (idx > 0 && pt.isHindsightCorrected != isCurrentSegmentHindsight && currentSegment.isNotEmpty()) {
                addTrailSegmentToFolder(view, folder, currentSegment, if (isCurrentSegmentHindsight) colorHindsight else colorNormal, pool, poolIdx++)
                currentSegment = mutableListOf()
                currentSegment.add(trailPoints[idx-1].toGeoPoint())
            }
            isCurrentSegmentHindsight = pt.isHindsightCorrected
            currentSegment.add(pt.toGeoPoint())
        }
    }
    if (currentSegment.isNotEmpty()) {
        addTrailSegmentToFolder(view, folder, currentSegment, if (isCurrentSegmentHindsight) colorHindsight else colorNormal, pool, poolIdx++)
    }
    return poolIdx
}

private fun addTrailSegmentToFolder(view: MapView, folder: FolderOverlay, segment: List<GeoPoint>, color: Int, pool: SnapshotStateList<Polyline>, poolIdx: Int) {
    val line = if (poolIdx < pool.size) {
        pool[poolIdx]
    } else {
        val l = Polyline(view)
        l.outlinePaint.strokeWidth = 4f
        l.setInfoWindow(null)
        pool.add(l)
        l
    }
    line.setPoints(segment)
    line.outlinePaint.color = color
    folder.add(line)
}

private fun createTrackerBitmap(density: Float, isFresh: Boolean): Bitmap { 
    val sz = (32 * density).toInt(); val bitmap = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888); val canvas = Canvas(bitmap); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.style = Paint.Style.STROKE; paint.color = android.graphics.Color.WHITE; paint.strokeWidth = 1.0f * density; canvas.drawCircle(sz/2f, sz/2f, sz/2f - density, paint)
    paint.color = if (isFresh) BrandJd.toArgb() else Slate500.toArgb(); paint.strokeWidth = 3.0f * density; canvas.drawCircle(sz/2f, sz/2f, sz/2f - 3.5f * density, paint)
    return bitmap
}

private fun createViewerBitmap(density: Float, isFresh: Boolean): Bitmap {
    val sz = (20 * density).toInt(); val bitmap = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888); val canvas = Canvas(bitmap); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.style = Paint.Style.STROKE; paint.color = android.graphics.Color.WHITE; paint.strokeWidth = 1.0f * density; canvas.drawCircle(sz/2f, sz/2f, sz/2f - density, paint)
    paint.color = if (isFresh) ViewerOrange.toArgb() else Slate500.toArgb(); paint.style = Paint.Style.FILL; canvas.drawCircle(sz/2f, sz/2f, sz/2f - 3.5f * density, paint)
    return bitmap
}

private fun createHomeBitmap(density: Float, index: Int): Bitmap {
    val sz = (32 * density).toInt(); val bitmap = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888); val canvas = Canvas(bitmap); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = 0xFFCBD5E1.toInt(); canvas.drawCircle(sz/2f, sz/2f, sz/2.2f, paint)
    paint.style = Paint.Style.STROKE; paint.color = 0xFF334155.toInt(); paint.strokeWidth = 2 * density; canvas.drawCircle(sz/2f, sz/2f, sz/2.2f, paint)
    paint.style = Paint.Style.FILL; paint.color = android.graphics.Color.RED; paint.textSize = 14 * density; paint.textAlign = Paint.Align.CENTER; paint.isFakeBoldText = true; canvas.drawText("$index", sz/2f, sz/2f + (paint.textSize/3f), paint)
    return bitmap
}

private fun createJumpMarkerBitmap(density: Float): Bitmap {
    val sz = (10 * density).toInt()
    val bitmap = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.style = Paint.Style.STROKE
    paint.color = 0xFFFF00FF.toInt() 
    paint.strokeWidth = 2.0f * density
    val offset = paint.strokeWidth / 2f
    canvas.drawRect(offset, offset, sz.toFloat() - offset, sz.toFloat() - offset, paint)
    return bitmap
}

private fun createGeofenceViolationBitmap(density: Float): Bitmap {
    val sz = (12 * density).toInt()
    val bitmap = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.style = Paint.Style.STROKE
    paint.color = android.graphics.Color.RED
    paint.strokeWidth = 2.0f * density
    val offset = paint.strokeWidth / 2f
    canvas.drawCircle(sz/2f, sz/2f, sz/2f - offset, paint)
    return bitmap
}

@Composable
fun MapToolsOverlay(
    isTrackerMode: Boolean,
    trackerValid: Boolean = true,
    viewerValid: Boolean = true,
    showFence: Boolean, 
    onToggleFence: () -> Unit,
    geofenceMode: GeofenceMode,
    onSetGeofenceMode: (GeofenceMode) -> Unit,
    showViolations: Boolean = true,
    onToggleViolations: () -> Unit = {},
    showGeofenceViolations: Boolean = true,
    onToggleGeofenceViolations: () -> Unit = {},
    onClear: () -> Unit, 
    onSave: () -> Unit, 
    onToggleLog: () -> Unit = {},
    onSaveTrail: () -> Unit = {},
    onLoad: () -> Unit, 
    onCenterTracker: () -> Unit = {},
    onCenterViewer: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val spacing = 16.dp
    val purple = Color(0xFF800080)
    
    val currentIsTrackerMode by rememberUpdatedState(isTrackerMode)
    val currentTrackerValid by rememberUpdatedState(trackerValid)
    val currentViewerValid by rememberUpdatedState(viewerValid)
    val currentShowFence by rememberUpdatedState(showFence)
    val currentGeofenceModeState by rememberUpdatedState(geofenceMode)
    val currentShowViolations by rememberUpdatedState(showViolations)
    val currentShowGeofenceViolations by rememberUpdatedState(showGeofenceViolations)

    Column(
        modifier = Modifier
            .wrapContentWidth()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp), 
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            MapToolButton(label = "IN", symbol = "+", onClick = onZoomIn, iconColor = purple)
            MapToolButton(label = "OUT", symbol = "-", onClick = onZoomOut, iconColor = purple)
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            MapToolButton(icon = Icons.Default.Person, label = "VIEWER", onClick = onCenterViewer, iconColor = if(viewerValid) ViewerOrange else Color.Gray)
            MapToolButton(icon = Icons.Default.Agriculture, label = "TRACKER", onClick = onCenterTracker, iconColor = if(trackerValid) BrandJd else Color.Gray)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            MapToolButton(icon = if (showGeofenceViolations) Icons.Default.LocationOn else Icons.Default.LocationOff, label = "OUT", onClick = onToggleGeofenceViolations, iconColor = if (showGeofenceViolations) Color.Red else Color.Gray)
            MapToolButton(icon = if (showViolations) Icons.Default.Report else Icons.Default.ReportOff, label = "JUMP", onClick = onToggleViolations, iconColor = if (showViolations) Color(0xFFFF00FF) else Color.Gray)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            MapToolButton(icon = Icons.Default.Upload, label = "LOAD", onClick = onLoad, iconColor = BrandJd)
            MapToolButton(icon = Icons.Default.Save, label = "SAVE", onClick = onSave, iconColor = Indigo500)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            MapToolButton(
                icon = Icons.Default.AddLocation, 
                label = "ADD", 
                onClick = { if (!isTrackerMode) onSetGeofenceMode(GeofenceMode.ADD) }, 
                iconColor = if (geofenceMode == GeofenceMode.ADD) Color.White else BrandJd,
                containerColor = if (geofenceMode == GeofenceMode.ADD) BrandJd else Color.Transparent
            )
            MapToolButton(
                icon = Icons.Default.WrongLocation, 
                label = "DEL", 
                onClick = { if (!isTrackerMode) onSetGeofenceMode(GeofenceMode.REMOVE) }, 
                iconColor = if (geofenceMode == GeofenceMode.REMOVE) Color.White else Rose500,
                containerColor = if (geofenceMode == GeofenceMode.REMOVE) Rose500 else Color.Transparent
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            MapToolButton(icon = if (showFence) Icons.Default.Visibility else Icons.Default.VisibilityOff, label = "FENCE", onClick = onToggleFence, iconColor = if (showFence) BrandJd else Color.Gray)
            MapToolButton(icon = Icons.Default.Delete, label = "CLEAR", onClick = { onCenterTracker(); onClear() }, iconColor = Rose500)
        }
    }
}

@Composable
fun MapToolButton(
    icon: ImageVector? = null,
    symbol: String? = null,
    label: String,
    onClick: () -> Unit,
    iconColor: Color,
    containerColor: Color = Color.Transparent
) {
    val purple = Color(0xFF800080)
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(containerColor, RoundedCornerShape(8.dp))
            .border(0.5.dp, purple, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (symbol != null) {
                Text(symbol, color = iconColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.height(26.dp))
            } else if (icon != null) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Text(label, color = if (containerColor != Color.Transparent) Color.White else purple, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}
