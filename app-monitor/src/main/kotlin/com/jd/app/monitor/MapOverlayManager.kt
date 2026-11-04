package com.jd.app.monitor

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.round

class ShadowCache<K, V>(private val maxCapacity: Int) {
    private val map = Collections.synchronizedMap(object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxCapacity
        }
    })
    fun get(key: K): V? = map[key]
    fun put(key: K, value: V) { map[key] = value }
    fun clear() { map.clear() }
}

class MapOverlayManager(
    private val context: Context,
    private val mapView: MapView,
    private val density: Float
) {
    private val resources = context.resources
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val trailFolder = FolderOverlay()
    private val viewerTrailFolder = FolderOverlay()
    private val fenceFolder = FolderOverlay()
    private val homeMarkersFolder = FolderOverlay()
    private val accuracyCirclesFolder = FolderOverlay()
    private val violationMarkersFolder = FolderOverlay()
    private val violationAccuracyFolder = FolderOverlay()
    private val replayFolder = FolderOverlay()

    val trackerMarker = Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
    val viewerMarker = Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
    private val trackerCircle = Polygon(mapView).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 3f; setInfoWindow(null) }
    private val viewerCircle = Polygon(mapView).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 3f; setInfoWindow(null) }

    private val replayMarker = Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
    private val replayCircle = Polygon(mapView).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 2f; setInfoWindow(null) }

    private val homeMarkerPool = mutableStateListOf<Marker>()
    private val violationMarkerPool = mutableStateListOf<Marker>()
    private val violationCirclePool = mutableStateListOf<Polygon>()
    private val trackerPolylinePool = mutableStateListOf<Polyline>()
    private val viewerPolylinePool = mutableStateListOf<Polyline>()

    private val trackerIconFresh = createTrackerBitmap(density, true).toDrawable(resources)
    private val trackerIconStale = createTrackerBitmap(density, false).toDrawable(resources)
    private val viewerIconFresh = createViewerBitmap(density, true).toDrawable(resources)
    private val viewerIconStale = createViewerBitmap(density, false).toDrawable(resources)
    private val jumpIcon = createJumpMarkerBitmap(density).toDrawable(resources)
    private val geofenceIcon = createGeofenceViolationBitmap(density).toDrawable(resources)
    private val replayIcon = createReplayMarkerBitmap(density).toDrawable(resources)

    private val homeIcons = HashMap<Int, BitmapDrawable>()

    private var lastHomeRendered: List<GeoPoint>? = null
    private var lastFenceState: Boolean? = null
    private var lastIsTrackerMode: Boolean? = null
    private var lastGeofenceMode: GeofenceMode? = null

    private var lastTrailChecksum = -1
    private var lastViewerTrailChecksum = -1
    private var lastViolationsSize = -1
    private var lastViolationsRef: List<ViolationPoint>? = null
    private var lastViolationVisibility: Pair<Boolean, Boolean>? = null
    private var cachedFilteredViolations = emptyList<ViolationPoint>()

    private var lastTrackerPos: GeoPoint? = null
    private var lastTrackerDrift: Double = -1.0
    private var lastTrackerFresh: Boolean? = null

    private var lastViewerPos: GeoPoint? = null
    private var lastViewerDrift: Double = -1.0
    private var lastViewerFresh: Boolean? = null

    private var lastReplayPos: GeoPoint? = null

    private var lastTrailUpdateTs = 0L
    private var lastViewerTrailUpdateTs = 0L
    private var lastDriftUpdateTs = 0L
    private var lastViolationUpdateTs = 0L
    private val BUDGET_THROTTLE_MS = 1000L

    private var trackerTrailJob: Job? = null
    private var viewerTrailJob: Job? = null
    private var violationJob: Job? = null
    private var homeJob: Job? = null
    private var currentPositionsJob: Job? = null

    private data class CircleKey(val latQ: Long, val lngQ: Long, val radiusQ: Long)
    private val circleCache = ShadowCache<CircleKey, List<GeoPoint>>(600)
    private val pendingCalculations = Collections.synchronizedSet(mutableSetOf<CircleKey>())

    private fun getAsyncCircle(center: GeoPoint, radius: Double, onReady: (List<GeoPoint>) -> Unit): List<GeoPoint>? {
        val latQ = (center.latitude * 1_000_000).toLong()
        val lngQ = (center.longitude * 1_000_000).toLong()
        val radiusQ = (round(radius * 2.0)).toLong()

        val key = CircleKey(latQ, lngQ, radiusQ)
        val cached = circleCache.get(key)
        if (cached != null) return cached

        if (pendingCalculations.add(key)) {
            scope.launch(Dispatchers.Default) {
                try {
                    val points = Polygon.pointsAsCircle(center, radiusQ / 2.0)
                    circleCache.put(key, points)
                    withContext(Dispatchers.Main) {
                        onReady(points)
                        pendingCalculations.remove(key)
                    }
                } catch (e: Exception) {
                    pendingCalculations.remove(key)
                }
            }
        }
        return null
    }

    init {
        mapView.overlays.add(trailFolder)
        mapView.overlays.add(viewerTrailFolder)
        mapView.overlays.add(violationAccuracyFolder)
        mapView.overlays.add(violationMarkersFolder)
        mapView.overlays.add(accuracyCirclesFolder)
        mapView.overlays.add(fenceFolder)
        mapView.overlays.add(homeMarkersFolder)
        mapView.overlays.add(replayFolder)
    }

    fun onDetach() {
        scope.cancel()
    }

    fun trimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            circleCache.clear()
            pendingCalculations.clear()
            val currentHomeSize = lastHomeRendered?.size ?: 0
            while (homeMarkerPool.size > currentHomeSize) homeMarkerPool.removeAt(homeMarkerPool.size - 1)
            trackerPolylinePool.clear()
            viewerPolylinePool.clear()
            trailFolder.items.clear()
            viewerTrailFolder.items.clear()
            mapView.tileProvider.tileCache.clear()
            System.gc()
        }
    }

    fun updateHomePoints(
        home: List<GeoPoint>,
        isFenceVisible: Boolean,
        maxD: Double,
        isTrackerMode: Boolean,
        geofenceMode: GeofenceMode,
        onTap: (GeoPoint) -> Unit,
        onRemoveMarker: (Int) -> Unit
    ): Boolean {
        if (lastHomeRendered == home && lastFenceState == isFenceVisible &&
            lastIsTrackerMode == isTrackerMode && lastGeofenceMode == geofenceMode) return false

        lastHomeRendered = home
        lastFenceState = isFenceVisible
        lastIsTrackerMode = isTrackerMode
        lastGeofenceMode = geofenceMode

        homeJob?.cancel()
        homeJob = scope.launch {
            val polygons = mutableListOf<Polygon>()
            val markers = mutableListOf<Marker>()

            if (isFenceVisible) {
                val yieldBatch = if (home.size > 200) 5 else 2
                home.forEachIndexed { idx, p ->
                    val poly = Polygon(mapView).apply {
                        fillPaint.color = 0x28CBD5E1.toInt(); outlinePaint.color = 0xC8CBD5E1.toInt(); outlinePaint.strokeWidth = 2f; setInfoWindow(null)
                    }
                    val cachedPoints = getAsyncCircle(p, maxD) { points ->
                        poly.points = points
                        mapView.invalidate()
                    }
                    if (cachedPoints != null) poly.points = cachedPoints
                    polygons.add(poly)

                    val marker = if (idx < homeMarkerPool.size) homeMarkerPool[idx] else Marker(mapView).also { m -> m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); m.setInfoWindow(null); homeMarkerPool.add(m) }
                    marker.position = p; marker.icon = homeIcons.getOrPut(idx + 1) { createHomeBitmap(density, idx + 1).toDrawable(resources) }
                    marker.setOnMarkerClickListener { _, mv ->
                        if (!isTrackerMode && geofenceMode == GeofenceMode.REMOVE) {
                            mv.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onRemoveMarker(idx)
                            Toast.makeText(context, "Home point removed", Toast.LENGTH_SHORT).show()
                        }
                        else if (!isTrackerMode && geofenceMode == GeofenceMode.ADD) Toast.makeText(context, "Switch to DEL mode to remove points", Toast.LENGTH_SHORT).show()
                        true
                    }
                    markers.add(marker)
                    if (idx % yieldBatch == 0) yield()
                }
            }

            fenceFolder.items.clear()
            fenceFolder.items.addAll(polygons)
            homeMarkersFolder.items.clear()
            homeMarkersFolder.items.addAll(markers)
            mapView.invalidate()

            val activeHomeSize = if (isFenceVisible) home.size else 0
            while (homeMarkerPool.size > maxOf(activeHomeSize + 5, MARKER_POOL_PRUNE_THRESHOLD)) {
                homeMarkerPool.removeAt(homeMarkerPool.size - 1)
            }
        }
        return false
    }

    fun updateTrails(
        trackerSegments: List<MapTrailSegment>,
        viewerSegments: List<MapTrailSegment>,
        systemPulseRt: Long
    ): Boolean {
        val canUpdateTracker = (systemPulseRt - lastTrailUpdateTs) > BUDGET_THROTTLE_MS || lastTrailChecksum == -1
        val canUpdateViewer = (systemPulseRt - lastViewerTrailUpdateTs) > BUDGET_THROTTLE_MS || lastViewerTrailChecksum == -1

        if (canUpdateTracker) {
            val currentTrackerChecksum = trackerSegments.fold(0) { acc, seg -> acc * 31 + seg.checksum }
            if (lastTrailChecksum != currentTrackerChecksum) {
                lastTrailChecksum = currentTrackerChecksum
                lastTrailUpdateTs = systemPulseRt
                trackerTrailJob?.cancel()
                trackerTrailJob = scope.launch {
                    val polylines = mutableListOf<Polyline>()
                    val yieldBatch = if (trackerSegments.size > 200) 5 else 2

                    trackerSegments.forEachIndexed { idx, segment ->
                        val line = if (idx < trackerPolylinePool.size) trackerPolylinePool[idx] else Polyline(mapView).also { l ->
                            l.outlinePaint.strokeWidth = 4f; l.setInfoWindow(null); trackerPolylinePool.add(l)
                        }
                        val points = mutableListOf<GeoPoint>()
                        for (i in 0 until segment.lats.size) {
                            points.add(GeoPoint(segment.lats[i], segment.lngs[i]))
                        }
                        line.setPoints(points)
                        line.outlinePaint.color = segment.color
                        polylines.add(line)
                        if (segment.lats.size > 200 || idx % yieldBatch == 0) yield()
                    }
                    trailFolder.items.clear()
                    trailFolder.items.addAll(polylines)
                    mapView.invalidate()
                    while(trackerPolylinePool.size > maxOf(trackerSegments.size + 5, MARKER_POOL_PRUNE_THRESHOLD)) {
                        trackerPolylinePool.removeAt(trackerPolylinePool.size - 1)
                    }
                }
            }
        }

        if (canUpdateViewer) {
            val currentViewerChecksum = viewerSegments.fold(0) { acc, seg -> acc * 31 + seg.checksum }
            if (lastViewerTrailChecksum != currentViewerChecksum) {
                lastViewerTrailChecksum = currentViewerChecksum
                lastViewerTrailUpdateTs = systemPulseRt
                viewerTrailJob?.cancel()
                viewerTrailJob = scope.launch {
                    val polylines = mutableListOf<Polyline>()
                    val yieldBatch = if (viewerSegments.size > 200) 5 else 2

                    viewerSegments.forEachIndexed { idx, segment ->
                        val line = if (idx < viewerPolylinePool.size) viewerPolylinePool[idx] else Polyline(mapView).also { l ->
                            l.outlinePaint.strokeWidth = 4f; l.setInfoWindow(null); viewerPolylinePool.add(l)
                        }
                        val points = mutableListOf<GeoPoint>()
                        for (i in 0 until segment.lats.size) {
                            points.add(GeoPoint(segment.lats[i], segment.lngs[i]))
                        }
                        line.setPoints(points)
                        line.outlinePaint.color = segment.color
                        polylines.add(line)
                        if (segment.lats.size > 200 || idx % yieldBatch == 0) yield()
                    }
                    viewerTrailFolder.items.clear()
                    viewerTrailFolder.items.addAll(polylines)
                    mapView.invalidate()
                    while(viewerPolylinePool.size > maxOf(viewerSegments.size + 5, MARKER_POOL_PRUNE_THRESHOLD)) {
                        viewerPolylinePool.removeAt(viewerPolylinePool.size - 1)
                    }
                }
            }
        }
        return false
    }

    fun updateViolations(
        violations: List<ViolationPoint>,
        isViolationsVisible: Boolean,
        isGeofenceViolationsVisible: Boolean,
        systemPulseRt: Long
    ): Boolean {
        val visibilityPair = Pair(isViolationsVisible, isGeofenceViolationsVisible)
        if (lastViolationsRef === violations && lastViolationVisibility == visibilityPair) return false
        if (lastViolationsSize == violations.size && lastViolationVisibility == visibilityPair) return false
        if ((systemPulseRt - lastViolationUpdateTs) < BUDGET_THROTTLE_MS && lastViolationsSize != -1) return false

        lastViolationsSize = violations.size
        lastViolationsRef = violations
        lastViolationVisibility = visibilityPair
        lastViolationUpdateTs = systemPulseRt

        violationJob?.cancel()
        violationJob = scope.launch {
            val filtered = violations.filter { v ->
                val isJump = v.type == ALERT_ID_JUMP_ALERT || v.type == ALERT_ID_VISUAL_JUMP
                val isGeo = v.type == ALERT_ID_TRACKER_GEOFENCE
                (isJump && isViolationsVisible) || (isGeo && isGeofenceViolationsVisible)
            }
            cachedFilteredViolations = filtered

            val markers = mutableListOf<Marker>()
            val circles = mutableListOf<Polygon>()
            val yieldBatch = if (filtered.size > 200) 5 else 2

            filtered.forEachIndexed { index, v ->
                val m = if (index < violationMarkerPool.size) violationMarkerPool[index] else Marker(mapView).also { m -> m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); m.setInfoWindow(null); violationMarkerPool.add(m) }
                val isJump = v.type == ALERT_ID_JUMP_ALERT || v.type == ALERT_ID_VISUAL_JUMP
                val gp = v.toGeoPoint()
                m.position = gp; m.icon = if (isJump) jumpIcon else geofenceIcon
                markers.add(m)

                val hAcc = if (v.maxAccuracy > 0.0) v.maxAccuracy else v.accuracy
                if (hAcc > 0.0) {
                    val c = if (index < violationCirclePool.size) violationCirclePool[index] else Polygon(mapView).also { p -> p.fillPaint.color = 0; p.outlinePaint.strokeWidth = 2f; p.setInfoWindow(null); violationCirclePool.add(p) }
                    c.outlinePaint.color = (if (isJump) 0x60FF00FF else 0x60FF0000).toInt()
                    val cachedPoints = getAsyncCircle(gp, hAcc) { points -> c.points = points }
                    if (cachedPoints != null) c.points = cachedPoints
                    circles.add(c)
                }
                if (index % yieldBatch == 0) yield()
            }

            violationMarkersFolder.items.clear()
            violationMarkersFolder.items.addAll(markers)
            violationAccuracyFolder.items.clear()
            violationAccuracyFolder.items.addAll(circles)
            mapView.invalidate()

            while (violationMarkerPool.size > maxOf(filtered.size + 5, MARKER_POOL_PRUNE_THRESHOLD)) {
                violationMarkerPool.removeAt(violationMarkerPool.size - 1)
                if (violationCirclePool.size > violationMarkerPool.size) violationCirclePool.removeAt(violationCirclePool.size - 1)
            }
        }
        return false
    }

    fun updateReplayCursor(pos: GeoPoint?): Boolean {
        if (lastReplayPos == pos) return false
        replayFolder.items.clear()
        if (pos != null) {
            replayMarker.position = pos
            replayMarker.icon = replayIcon
            replayFolder.add(replayMarker)
            val cachedPoints = getAsyncCircle(pos, 5.0) { points ->
                replayCircle.points = points
                mapView.invalidate()
            }
            if (cachedPoints != null) replayCircle.points = cachedPoints
            replayCircle.outlinePaint.color = android.graphics.Color.WHITE
            replayFolder.add(replayCircle)
        }
        lastReplayPos = pos
        return true
    }

    fun updateCurrentPositions(
        trackerValid: Boolean,
        trackerPos: GeoPoint?,
        isTrackerFresh: Boolean,
        trackerAccuracy: Double,
        maxTrackerAccuracy: Double,
        trackerSpeed: Double,
        isTrackerPending: Boolean,
        trackerLastValidFixRt: Long,
        viewerValid: Boolean,
        viewerPos: GeoPoint?,
        isViewerFresh: Boolean,
        viewerAccuracy: Double,
        viewerMaxAcc: Double,
        viewerSpeed: Double,
        isViewerPending: Boolean,
        viewerLastValidFixRt: Long,
        systemPulseRt: Long
    ): Boolean {
        val canUpdateDrift = (systemPulseRt - lastDriftUpdateTs) >= BUDGET_THROTTLE_MS
        if (!canUpdateDrift && lastTrackerPos == trackerPos && lastViewerPos == viewerPos && lastTrackerFresh == isTrackerFresh && lastViewerFresh == isViewerFresh) return false

        currentPositionsJob?.cancel()
        currentPositionsJob = scope.launch {
            var changed = false
            if (trackerValid && trackerPos != null) {
                val baseAcc = if (maxTrackerAccuracy > 0.0) maxTrackerAccuracy else trackerAccuracy
                if (baseAcc > 0.0) {
                    val drift = baseAcc
                    val posChanged = trackerPos != lastTrackerPos
                    val driftSignificant = abs(drift - lastTrackerDrift) > 0.5
                    if (posChanged || driftSignificant) {
                        accuracyCirclesFolder.items.remove(trackerCircle)
                        yield()
                        val cachedPoints = getAsyncCircle(trackerPos, drift) { points ->
                            trackerCircle.points = points
                            mapView.invalidate()
                        }
                        if (cachedPoints != null) trackerCircle.points = cachedPoints
                        trackerCircle.outlinePaint.color = if (isTrackerFresh) BrandJd.copy(alpha = 0.7f).toArgb() else Slate500.copy(alpha = 0.7f).toArgb()
                        accuracyCirclesFolder.add(trackerCircle)
                        lastTrackerPos = trackerPos
                        lastTrackerDrift = drift
                        changed = true
                    }
                }
                if (trackerMarker.position != trackerPos) {
                    trackerMarker.position = trackerPos
                    changed = true
                }
                if (lastTrackerFresh != isTrackerFresh) {
                    trackerMarker.icon = if (isTrackerFresh) trackerIconFresh else trackerIconStale
                    lastTrackerFresh = isTrackerFresh
                    changed = true
                }
                if (!mapView.overlays.contains(trackerMarker)) {
                    mapView.overlays.add(trackerMarker)
                    changed = true
                }
            } else if (mapView.overlays.contains(trackerMarker)) {
                accuracyCirclesFolder.items.remove(trackerCircle)
                mapView.overlays.remove(trackerMarker)
                lastTrackerPos = null
                lastTrackerFresh = null
                changed = true
            }

            yield()

            if (viewerValid && viewerPos != null) {
                if (viewerMarker.position != viewerPos) {
                    viewerMarker.position = viewerPos
                    changed = true
                }
                if (lastViewerFresh != isViewerFresh) {
                    viewerMarker.icon = if (isViewerFresh) viewerIconFresh else viewerIconStale
                    lastViewerFresh = isViewerFresh
                    changed = true
                }
                if (!mapView.overlays.contains(viewerMarker)) {
                    mapView.overlays.add(viewerMarker)
                    changed = true
                }
            } else if (mapView.overlays.contains(viewerMarker)) {
                mapView.overlays.remove(viewerMarker)
                lastViewerPos = null
                lastViewerFresh = null
                changed = true
            }

            if (changed) {
                lastDriftUpdateTs = systemPulseRt
                mapView.invalidate()
            }
        }
        return false
    }

    private fun createTrackerBitmap(density: Float, isFresh: Boolean): Bitmap {
        val sz = (32 * density).toInt()
        val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        b.applyCanvas {
            p.style = Paint.Style.STROKE; p.color = android.graphics.Color.WHITE; p.strokeWidth = density
            drawCircle(sz/2f, sz/2f, sz/2f - density, p)
            p.color = if (isFresh) BrandJd.toArgb() else Slate500.toArgb(); p.style = Paint.Style.FILL
            drawCircle(sz/2f, sz/2f, sz/2f - 3.5f * density, p)
        }
        return b
    }

    private fun createViewerBitmap(density: Float, isFresh: Boolean): Bitmap {
        val sz = (20 * density).toInt()
        val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        b.applyCanvas {
            p.style = Paint.Style.STROKE; p.color = android.graphics.Color.WHITE; p.strokeWidth = density
            drawCircle(sz/2f, sz/2f, sz/2f - density, p)
            p.color = if (isFresh) ViewerCyan.toArgb() else Slate500.toArgb(); p.style = Paint.Style.FILL
            drawCircle(sz/2f, sz/2f, sz/2f - 3.5f * density, p)
        }
        return b
    }

    private fun createHomeBitmap(density: Float, index: Int): Bitmap {
        val sz = (32 * density).toInt()
        val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        b.applyCanvas {
            p.color = 0xFFCBD5E1.toInt(); drawCircle(sz/2f, sz/2f, sz/2.2f, p)
            p.style = Paint.Style.STROKE; p.color = 0xFF334155.toInt(); p.strokeWidth = 2 * density; drawCircle(sz/2f, sz/2f, sz/2.2f, p)
            p.style = Paint.Style.FILL; p.color = android.graphics.Color.RED; p.textSize = 14 * density; p.textAlign = Paint.Align.CENTER; p.isFakeBoldText = true
            drawText("$index", sz/2f, sz/2f + (p.textSize/3f), p)
        }
        return b
    }

    private fun createJumpMarkerBitmap(density: Float): Bitmap {
        val sz = (10 * density).toInt()
        val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        b.applyCanvas {
            p.style = Paint.Style.STROKE; p.color = 0xFFFF00FF.toInt(); p.strokeWidth = 2f * density
            val off = p.strokeWidth / 2f; drawRect(off, off, sz - off, sz - off, p)
        }
        return b
    }

    private fun createGeofenceViolationBitmap(density: Float): Bitmap {
        val sz = (12 * density).toInt()
        val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        b.applyCanvas {
            p.style = Paint.Style.STROKE; p.color = android.graphics.Color.RED; p.strokeWidth = 2f * density
            val off = p.strokeWidth / 2f; drawCircle(sz/2f, sz/2f, sz/2f - off, p)
        }
        return b
    }

    private fun createReplayMarkerBitmap(density: Float): Bitmap {
        val sz = (24 * density).toInt()
        val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        b.applyCanvas {
            p.color = android.graphics.Color.WHITE; drawCircle(sz/2f, sz/2f, sz/2f - density, p)
            p.style = Paint.Style.STROKE; p.color = android.graphics.Color.BLUE; p.strokeWidth = 2f * density
            drawCircle(sz/2f, sz/2f, sz/3f, p)
        }
        return b
    }
}
