package com.gps19.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toDrawable
import com.gps19.app.BuildConfig
import com.gps19.core.engine.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.round

/**
 * MapOverlayManager: Imperative manager for osmdroid overlays and pooling.
 * Aug.25.00:
 * - Issue #309 Remediation: Replaced SnapshotStateList/Map with standard 
 *   ArrayList/HashMap. These pools are imperative and used within AndroidView.update, 
 *   eliminating Compose lock verification failures on A15 hardware (R309).
 * Aug.24.00:
 * - Issue #255 Hardening: Refactored pools to SnapshotStateList and SnapshotStateMap 
 *   to resolve Compose lock verification failures during high-frequency telemetry (R255).
 * Aug.18.09:
 * - Issue #208 Performance Audit: Implemented circle geometry caching (R208).
 * - Issue #208 Performance Audit: Added drift and center quantization to 
 *   circle cache keys to maximize hit rates during movement (R208).
 * - Issue #208 Performance Audit: Implemented filtered violations caching 
 *   to eliminate O(N) churn in updateViolations (R208).
 */
class MapOverlayManager(
    private val context: Context,
    private val mapView: MapView,
    private val density: Float
) {
    private val resources = context.resources

    // Folders
    private val trailFolder = FolderOverlay()
    private val viewerTrailFolder = FolderOverlay()
    private val fenceFolder = FolderOverlay()
    private val homeMarkersFolder = FolderOverlay()
    private val accuracyCirclesFolder = FolderOverlay()
    private val violationMarkersFolder = FolderOverlay()
    private val violationAccuracyFolder = FolderOverlay()
    private val replayFolder = FolderOverlay()

    // Fixed Overlays
    val trackerMarker = Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
    val viewerMarker = Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
    private val trackerCircle = Polygon(mapView).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 3f; setInfoWindow(null) }
    private val viewerCircle = Polygon(mapView).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 3f; setInfoWindow(null) }

    // Issue #170: Replay Cursor
    private val replayMarker = Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
    private val replayCircle = Polygon(mapView).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 2f; setInfoWindow(null) }

    // Pools: Issue #309 Replaced SnapshotStateList with ArrayList
    private val homeMarkerPool = ArrayList<Marker>()
    private val violationMarkerPool = ArrayList<Marker>()
    private val violationCirclePool = ArrayList<Polygon>()
    private val trackerPolylinePool = ArrayList<Polyline>()
    private val viewerPolylinePool = ArrayList<Polyline>()

    // Icons
    private val trackerIconFresh = createTrackerBitmap(density, true).toDrawable(resources)
    private val trackerIconStale = createTrackerBitmap(density, false).toDrawable(resources)
    private val viewerIconFresh = createViewerBitmap(density, true).toDrawable(resources)
    private val viewerIconStale = createViewerBitmap(density, false).toDrawable(resources)
    private val jumpIcon = createJumpMarkerBitmap(density).toDrawable(resources)
    private val geofenceIcon = createGeofenceViolationBitmap(density).toDrawable(resources)
    private val replayIcon = createReplayMarkerBitmap(density).toDrawable(resources)
    
    // Issue #309 Replaced SnapshotStateMap with HashMap
    private val homeIcons = HashMap<Int, BitmapDrawable>()

    // State Caches
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

    // Issue #208: Circle Geometry Cache with Spatial Quantization
    private data class CircleKey(val latQ: Long, val lngQ: Long, val radiusQ: Long)
    private val circleCache = mutableMapOf<CircleKey, List<GeoPoint>>()

    private fun getCachedCircle(center: GeoPoint, radius: Double): List<GeoPoint> {
        // Issue #208: Quantize center (approx 0.1m) and radius (0.5m) to maximize hit rate
        val latQ = (center.latitude * 1_000_000).toLong() // Approx 0.11m precision
        val lngQ = (center.longitude * 1_000_000).toLong()
        val radiusQ = (round(radius * 2.0)).toLong() // 0.5m steps
        
        val key = CircleKey(latQ, lngQ, radiusQ)
        return circleCache.getOrPut(key) { 
            if (circleCache.size > 200) circleCache.clear() 
            Polygon.pointsAsCircle(center, radiusQ / 2.0) 
        }
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

    fun updateHomePoints(
        home: List<GeoPoint>,
        isFenceVisible: Boolean,
        maxD: Double,
        isTrackerMode: Boolean,
        geofenceMode: GeofenceMode,
        onTap: (GeoPoint) -> Unit,
        onRemoveMarker: (Int) -> Unit
    ): Boolean {
        // Issue #208: Reference check first to avoid O(N) list equality
        if (lastHomeRendered === home && lastFenceState == isFenceVisible && 
            lastIsTrackerMode == isTrackerMode && lastGeofenceMode == geofenceMode) return false
            
        if (lastHomeRendered == home && lastFenceState == isFenceVisible && 
            lastIsTrackerMode == isTrackerMode && lastGeofenceMode == geofenceMode) return false

        fenceFolder.items.clear()
        homeMarkersFolder.items.clear()
        
        val activeHomeSize = if (isFenceVisible) home.size else 0
        if (homeMarkerPool.size > activeHomeSize + MARKER_POOL_PRUNE_THRESHOLD) {
            while (homeMarkerPool.size > maxOf(activeHomeSize + 5, MARKER_POOL_PRUNE_THRESHOLD)) homeMarkerPool.removeAt(homeMarkerPool.size - 1)
        }

        if (isFenceVisible) {
            home.forEachIndexed { idx, p ->
                fenceFolder.add(Polygon(mapView).apply { 
                    points = getCachedCircle(p, maxD)
                    fillPaint.color = 0x28CBD5E1.toInt(); outlinePaint.color = 0xC8CBD5E1.toInt(); outlinePaint.strokeWidth = 2f; setInfoWindow(null) 
                })
                val marker = if (idx < homeMarkerPool.size) homeMarkerPool[idx] else Marker(mapView).also { m -> m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); m.setInfoWindow(null); homeMarkerPool.add(m) }
                marker.position = p; marker.icon = homeIcons.getOrPut(idx + 1) { createHomeBitmap(density, idx + 1).toDrawable(resources) as BitmapDrawable }
                marker.setOnMarkerClickListener { mk, mv -> 
                    if (!isTrackerMode && geofenceMode == GeofenceMode.REMOVE) { 
                        mv.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onTap(p)
                        onRemoveMarker(idx)
                        Toast.makeText(context, "Home point removed", Toast.LENGTH_SHORT).show() 
                    }
                    else if (!isTrackerMode && geofenceMode == GeofenceMode.ADD) Toast.makeText(context, "Switch to DEL mode to remove points", Toast.LENGTH_SHORT).show()
                    true 
                }
                homeMarkersFolder.add(marker)
            }
        }
        lastHomeRendered = home
        lastFenceState = isFenceVisible
        lastIsTrackerMode = isTrackerMode
        lastGeofenceMode = geofenceMode
        return true
    }

    fun updateTrails(
        trackerSegments: List<MapTrailSegment>, 
        viewerSegments: List<MapTrailSegment>, 
        systemPulseRt: Long
    ): Boolean {
        var changed = false
        val canUpdateTracker = (systemPulseRt - lastTrailUpdateTs) > BUDGET_THROTTLE_MS || lastTrailChecksum == -1
        val canUpdateViewer = (systemPulseRt - lastViewerTrailUpdateTs) > BUDGET_THROTTLE_MS || lastViewerTrailChecksum == -1

        val currentTrackerChecksum = trackerSegments.fold(0) { acc, seg -> acc * 31 + seg.checksum }
        if (canUpdateTracker && lastTrailChecksum != currentTrackerChecksum) {
            trailFolder.items.clear()
            trackerSegments.forEachIndexed { idx, segment ->
                val line = if (idx < trackerPolylinePool.size) trackerPolylinePool[idx] else Polyline(mapView).also { l -> l.outlinePaint.strokeWidth = 4f; l.setInfoWindow(null); trackerPolylinePool.add(l) }
                line.setPoints(segment.points)
                line.outlinePaint.color = segment.color
                trailFolder.add(line)
            }
            lastTrailChecksum = currentTrackerChecksum
            lastTrailUpdateTs = systemPulseRt
            while(trackerPolylinePool.size > maxOf(trackerSegments.size + 5, MARKER_POOL_PRUNE_THRESHOLD)) trackerPolylinePool.removeAt(trackerPolylinePool.size - 1)
            changed = true
        }
        
        val currentViewerChecksum = viewerSegments.fold(0) { acc, seg -> acc * 31 + seg.checksum }
        if (canUpdateViewer && lastViewerTrailChecksum != currentViewerChecksum) {
            viewerTrailFolder.items.clear()
            viewerSegments.forEachIndexed { idx, segment ->
                val line = if (idx < viewerPolylinePool.size) viewerPolylinePool[idx] else Polyline(mapView).also { l -> l.outlinePaint.strokeWidth = 4f; l.setInfoWindow(null); viewerPolylinePool.add(l) }
                line.setPoints(segment.points)
                line.outlinePaint.color = segment.color
                viewerTrailFolder.add(line)
            }
            lastViewerTrailChecksum = currentViewerChecksum
            lastViewerTrailUpdateTs = systemPulseRt
            while(viewerPolylinePool.size > maxOf(viewerSegments.size + 5, MARKER_POOL_PRUNE_THRESHOLD)) viewerPolylinePool.removeAt(viewerPolylinePool.size - 1)
            changed = true
        }
        return changed
    }

    fun updateViolations(violations: List<ViolationPoint>, isViolationsVisible: Boolean, isGeofenceViolationsVisible: Boolean, systemPulseRt: Long): Boolean {
        val visibilityPair = Pair(isViolationsVisible, isGeofenceViolationsVisible)
        
        // Issue #208: Skip filtering and re-rendering if data and visibility are identical
        if (lastViolationsRef === violations && lastViolationVisibility == visibilityPair) return false
        if (lastViolationsSize == violations.size && lastViolationVisibility == visibilityPair) return false
        if ((systemPulseRt - lastViolationUpdateTs) < BUDGET_THROTTLE_MS && lastViolationsSize != -1) return false

        violationMarkersFolder.items.clear()
        violationAccuracyFolder.items.clear()
        
        // Issue #208: Cache filtered results
        cachedFilteredViolations = violations.filter { v -> 
            val isJump = v.type == ALERT_ID_JUMP_ALERT || v.type == ALERT_ID_VISUAL_JUMP
            val isGeo = v.type == ALERT_ID_TRACKER_GEOFENCE
            (isJump && isViolationsVisible) || (isGeo && isGeofenceViolationsVisible) 
        }

        if (violationMarkerPool.size > cachedFilteredViolations.size + MARKER_POOL_PRUNE_THRESHOLD) {
            while (violationMarkerPool.size > maxOf(cachedFilteredViolations.size + 5, MARKER_POOL_PRUNE_THRESHOLD)) { 
                violationMarkerPool.removeAt(violationMarkerPool.size - 1)
                violationCirclePool.removeAt(violationCirclePool.size - 1) 
            }
        }

        cachedFilteredViolations.forEachIndexed { index, v ->
            val m = if (index < violationMarkerPool.size) violationMarkerPool[index] else Marker(mapView).also { m -> m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); m.setInfoWindow(null); violationMarkerPool.add(m) }
            val isJump = v.type == ALERT_ID_JUMP_ALERT || v.type == ALERT_ID_VISUAL_JUMP
            val gp = v.toGeoPoint()
            m.position = gp; m.icon = if (isJump) jumpIcon else geofenceIcon; violationMarkersFolder.add(m)
            
            val hAcc = if (v.maxAccuracy > 0.0) v.maxAccuracy else v.accuracy
            if (hAcc > 0.0) {
                val c = if (index < violationCirclePool.size) violationCirclePool[index] else Polygon(mapView).also { p -> p.fillPaint.color = 0; p.outlinePaint.strokeWidth = 2f; p.setInfoWindow(null); violationCirclePool.add(p) }
                c.points = getCachedCircle(gp, hAcc)
                c.outlinePaint.color = (if (isJump) 0x60FF00FF else 0x60FF0000).toInt()
                violationAccuracyFolder.add(c)
            }
        }
        lastViolationsSize = violations.size
        lastViolationsRef = violations
        lastViolationVisibility = visibilityPair
        lastViolationUpdateTs = systemPulseRt
        return true
    }

    fun updateReplayCursor(pos: GeoPoint?): Boolean {
        if (lastReplayPos == pos) return false
        
        replayFolder.items.clear()
        if (pos != null) {
            replayMarker.position = pos
            replayMarker.icon = replayIcon
            replayFolder.add(replayMarker)
            
            replayCircle.points = getCachedCircle(pos, 5.0)
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
        var changed = false
        val canUpdateDrift = (systemPulseRt - lastDriftUpdateTs) >= BUDGET_THROTTLE_MS

        if (trackerValid && trackerPos != null) {
            val baseAcc = if (maxTrackerAccuracy > 0.0) maxTrackerAccuracy else trackerAccuracy
            if (baseAcc > 0.0) {
                val drift = if (isTrackerPending && trackerLastValidFixRt > 0) {
                    baseAcc + (if (trackerSpeed > 1.0) trackerSpeed.coerceIn(PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS, PENDING_UNCERTAINTY_SPEED_CAP_MPS) else PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS) * ((systemPulseRt - trackerLastValidFixRt) / 1000.0)
                } else baseAcc
                
                val posChanged = trackerPos != lastTrackerPos
                val driftSignificant = abs(drift - lastTrackerDrift) > 0.5

                if (canUpdateDrift && (posChanged || driftSignificant)) {
                    accuracyCirclesFolder.items.remove(trackerCircle)
                    trackerCircle.points = getCachedCircle(trackerPos, drift)
                    trackerCircle.outlinePaint.color = if (isTrackerFresh) BrandJd.copy(alpha = 0.7f).toArgb() else Slate500.copy(alpha = 0.7f).toArgb()
                    accuracyCirclesFolder.add(trackerCircle)
                    lastTrackerPos = trackerPos
                    lastTrackerDrift = drift
                    lastDriftUpdateTs = systemPulseRt
                    changed = true
                } else if (!accuracyCirclesFolder.items.contains(trackerCircle)) {
                     accuracyCirclesFolder.add(trackerCircle)
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
        } else if (mapView.overlays.contains(trackerMarker) || accuracyCirclesFolder.items.contains(trackerCircle)) {
            accuracyCirclesFolder.items.remove(trackerCircle)
            mapView.overlays.remove(trackerMarker)
            lastTrackerPos = null
            lastTrackerFresh = null
            changed = true
        }

        if (viewerValid && viewerPos != null) {
            val baseMyAcc = if (viewerMaxAcc > 0.0) viewerMaxAcc else viewerAccuracy
            if (baseMyAcc > 0.0) {
                val drift = if (isViewerPending && viewerLastValidFixRt > 0) {
                    baseMyAcc + (if (viewerSpeed > 1.0) viewerSpeed.coerceIn(PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS, PENDING_UNCERTAINTY_SPEED_CAP_MPS) else PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS) * ((systemPulseRt - viewerLastValidFixRt) / 1000.0)
                } else baseMyAcc
                
                val posChanged = viewerPos != lastViewerPos
                val driftSignificant = abs(drift - lastViewerDrift) > 0.5

                if (canUpdateDrift && (posChanged || driftSignificant)) {
                    accuracyCirclesFolder.items.remove(viewerCircle)
                    viewerCircle.points = getCachedCircle(viewerPos, drift)
                    viewerCircle.outlinePaint.color = if (isViewerFresh) ViewerCyan.copy(alpha = 0.7f).toArgb() else Slate500.copy(alpha = 0.7f).toArgb()
                    accuracyCirclesFolder.add(viewerCircle)
                    lastViewerPos = viewerPos
                    lastViewerDrift = drift
                    lastDriftUpdateTs = systemPulseRt
                    changed = true
                } else if (!accuracyCirclesFolder.items.contains(viewerCircle)) {
                    accuracyCirclesFolder.add(viewerCircle)
                    changed = true
                }
            }
            
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
        } else if (mapView.overlays.contains(viewerMarker) || accuracyCirclesFolder.items.contains(viewerCircle)) {
            accuracyCirclesFolder.items.remove(viewerCircle)
            mapView.overlays.remove(viewerMarker)
            lastViewerPos = null
            lastViewerFresh = null
            changed = true
        }
        return changed
    }

    private fun createTrackerBitmap(density: Float, isFresh: Boolean): Bitmap { 
        val sz = (32 * density).toInt()
        val b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        b.applyCanvas {
            p.style = Paint.Style.STROKE; p.color = android.graphics.Color.WHITE; p.strokeWidth = density
            drawCircle(sz/2f, sz/2f, sz/2f - density, p)
            p.color = if (isFresh) BrandJd.toArgb() else Slate500.toArgb(); p.strokeWidth = 3f * density
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
