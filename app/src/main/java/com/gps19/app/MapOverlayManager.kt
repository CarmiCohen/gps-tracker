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
import kotlin.math.abs
import kotlin.math.log10

/**
 * MapOverlayManager: Imperative manager for osmdroid overlays and pooling.
 * July.30.31:
 * - Issue #639: Performance: Tracker Mode ANR Hardening. Implemented granular 
 *   change detection and polygon caching to eliminate Main-thread blockage.
 * July.25.02:
 * - Issue #548: Integrated simplifyTrail thinning in drawTrailToFolder.
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

    // Fixed Overlays
    val trackerMarker = Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
    val viewerMarker = Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) }
    private val trackerCircle = Polygon(mapView).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 3f; setInfoWindow(null) }
    private val viewerCircle = Polygon(mapView).apply { fillPaint.color = 0; outlinePaint.strokeWidth = 3f; setInfoWindow(null) }

    // Pools
    private val homeMarkerPool = mutableListOf<Marker>()
    private val violationMarkerPool = mutableListOf<Marker>()
    private val violationCirclePool = mutableListOf<Polygon>()
    private val trackerPolylinePool = mutableListOf<Polyline>()
    private val viewerPolylinePool = mutableListOf<Polyline>()

    // Icons
    private val trackerIconFresh = createTrackerBitmap(density, true).toDrawable(resources)
    private val trackerIconStale = createTrackerBitmap(density, false).toDrawable(resources)
    private val viewerIconFresh = createViewerBitmap(density, true).toDrawable(resources)
    private val viewerIconStale = createViewerBitmap(density, false).toDrawable(resources)
    private val jumpIcon = createJumpMarkerBitmap(density).toDrawable(resources)
    private val geofenceIcon = createGeofenceViolationBitmap(density).toDrawable(resources)
    private val homeIcons = mutableMapOf<Int, BitmapDrawable>()

    // State Caches
    private var lastHomeRendered: List<GeoPoint>? = null
    private var lastFenceState: Boolean? = null
    private var lastTrailSize = -1
    private var lastViewerTrailSize = -1
    private var lastViolationsSize = -1
    private var lastViolationVisibility: Pair<Boolean, Boolean>? = null
    
    private var lastTrackerPos: GeoPoint? = null
    private var lastTrackerDrift: Double = -1.0
    private var lastViewerPos: GeoPoint? = null
    private var lastViewerDrift: Double = -1.0

    init {
        mapView.overlays.add(trailFolder)
        mapView.overlays.add(viewerTrailFolder)
        mapView.overlays.add(violationAccuracyFolder)
        mapView.overlays.add(violationMarkersFolder)
        mapView.overlays.add(accuracyCirclesFolder)
        mapView.overlays.add(fenceFolder)
        mapView.overlays.add(homeMarkersFolder)
    }

    fun updateHomePoints(
        home: List<GeoPoint>,
        isFenceVisible: Boolean,
        maxD: Double,
        isTrackerMode: Boolean,
        geofenceMode: GeofenceMode,
        onTap: (GeoPoint) -> Unit,
        onRemoveMarker: (Int) -> Unit
    ) {
        if (lastHomeRendered == home && lastFenceState == isFenceVisible) return

        fenceFolder.items.clear()
        homeMarkersFolder.items.clear()
        
        val activeHomeSize = if (isFenceVisible) home.size else 0
        if (homeMarkerPool.size > activeHomeSize + MARKER_POOL_PRUNE_THRESHOLD) {
            while (homeMarkerPool.size > maxOf(activeHomeSize + 5, MARKER_POOL_PRUNE_THRESHOLD)) homeMarkerPool.removeAt(homeMarkerPool.size - 1)
        }

        if (isFenceVisible) {
            home.forEachIndexed { idx, p ->
                fenceFolder.add(Polygon(mapView).apply { 
                    points = Polygon.pointsAsCircle(p, maxD).map { GeoPoint(it.latitude, it.longitude) }
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
        lastHomeRendered = home.toList()
        lastFenceState = isFenceVisible
    }

    fun updateTrails(trail: List<TrailPoint>, viewerTrail: List<TrailPoint>) {
        if (lastTrailSize == trail.size && lastViewerTrailSize == viewerTrail.size) return

        trailFolder.items.clear()
        val trSegs = drawTrailToFolder(mapView, trailFolder, trail, BrandJd.toArgb(), trackerPolylinePool)
        
        viewerTrailFolder.items.clear()
        val viSegs = drawTrailToFolder(mapView, viewerTrailFolder, viewerTrail, ViewerCyan.toArgb(), viewerPolylinePool)
        
        lastTrailSize = trail.size
        lastViewerTrailSize = viewerTrail.size
        
        while(trackerPolylinePool.size > maxOf(trSegs + 5, MARKER_POOL_PRUNE_THRESHOLD)) trackerPolylinePool.removeAt(trackerPolylinePool.size - 1)
        while(viewerPolylinePool.size > maxOf(viSegs + 5, MARKER_POOL_PRUNE_THRESHOLD)) viewerPolylinePool.removeAt(viewerPolylinePool.size - 1)
    }

    fun updateViolations(violations: List<ViolationPoint>, isViolationsVisible: Boolean, isGeofenceViolationsVisible: Boolean) {
        val visibilityPair = Pair(isViolationsVisible, isGeofenceViolationsVisible)
        if (lastViolationsSize == violations.size && lastViolationVisibility == visibilityPair) return

        violationMarkersFolder.items.clear()
        violationAccuracyFolder.items.clear()
        
        val filteredViolations = violations.filter { v -> 
            val isJump = v.type == ALERT_ID_JUMP_ALERT || v.type == ALERT_ID_VISUAL_JUMP
            val isGeo = v.type == ALERT_ID_TRACKER_GEOFENCE
            (isJump && isViolationsVisible) || (isGeo && isGeofenceViolationsVisible) 
        }

        if (violationMarkerPool.size > filteredViolations.size + MARKER_POOL_PRUNE_THRESHOLD) {
            while (violationMarkerPool.size > maxOf(filteredViolations.size + 5, MARKER_POOL_PRUNE_THRESHOLD)) { 
                violationMarkerPool.removeAt(violationMarkerPool.size - 1)
                violationCirclePool.removeAt(violationCirclePool.size - 1) 
            }
        }

        filteredViolations.forEachIndexed { index, v ->
            val m = if (index < violationMarkerPool.size) violationMarkerPool[index] else Marker(mapView).also { m -> m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); m.setInfoWindow(null); violationMarkerPool.add(m) }
            val isJump = v.type == ALERT_ID_JUMP_ALERT || v.type == ALERT_ID_VISUAL_JUMP
            m.position = v.point; m.icon = if (isJump) jumpIcon else geofenceIcon; violationMarkersFolder.add(m)
            
            val hAcc = if (v.maxAccuracy > 0.0) v.maxAccuracy else v.accuracy
            if (hAcc > 0.0) {
                val c = if (index < violationCirclePool.size) violationCirclePool[index] else Polygon(mapView).also { p -> p.fillPaint.color = 0; p.outlinePaint.strokeWidth = 2f; p.setInfoWindow(null); violationCirclePool.add(p) }
                c.points = Polygon.pointsAsCircle(v.point, hAcc).map { GeoPoint(it.latitude, it.longitude) }
                c.outlinePaint.color = (if (isJump) 0x60FF00FF else 0x60FF0000).toInt()
                violationAccuracyFolder.add(c)
            }
        }
        lastViolationsSize = violations.size
        lastViolationVisibility = visibilityPair
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
    ) {
        if (trackerValid && trackerPos != null) {
            val baseAcc = if (maxTrackerAccuracy > 0.0) maxTrackerAccuracy else trackerAccuracy
            if (baseAcc > 0.0) {
                val drift = if (isTrackerPending && trackerLastValidFixRt > 0) {
                    baseAcc + (if (trackerSpeed > 1.0) trackerSpeed.coerceIn(PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS, PENDING_UNCERTAINTY_SPEED_CAP_MPS) else PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS) * ((systemPulseRt - trackerLastValidFixRt) / 1000.0)
                } else baseAcc
                
                // Issue #639: Granular polygon reconstruction (threshold: 1.0m)
                if (trackerPos != lastTrackerPos || abs(drift - lastTrackerDrift) > 1.0) {
                    accuracyCirclesFolder.items.remove(trackerCircle)
                    trackerCircle.points = Polygon.pointsAsCircle(trackerPos, drift).map { GeoPoint(it.latitude, it.longitude) }
                    trackerCircle.outlinePaint.color = if (isTrackerFresh) BrandJd.copy(alpha = 0.7f).toArgb() else Slate500.copy(alpha = 0.7f).toArgb()
                    accuracyCirclesFolder.add(trackerCircle)
                    lastTrackerPos = trackerPos
                    lastTrackerDrift = drift
                } else if (!accuracyCirclesFolder.items.contains(trackerCircle)) {
                     accuracyCirclesFolder.add(trackerCircle)
                }
            }
            trackerMarker.position = trackerPos
            trackerMarker.icon = if (isTrackerFresh) trackerIconFresh else trackerIconStale
            if (!mapView.overlays.contains(trackerMarker)) mapView.overlays.add(trackerMarker) 
        } else {
            accuracyCirclesFolder.items.remove(trackerCircle)
            mapView.overlays.remove(trackerMarker)
        }

        if (viewerValid && viewerPos != null) {
            val baseMyAcc = if (viewerMaxAcc > 0.0) viewerMaxAcc else viewerAccuracy
            if (baseMyAcc > 0.0) {
                val drift = if (isViewerPending && viewerLastValidFixRt > 0) {
                    baseMyAcc + (if (viewerSpeed > 1.0) viewerSpeed.coerceIn(PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS, PENDING_UNCERTAINTY_SPEED_CAP_MPS) else PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS) * ((systemPulseRt - viewerLastValidFixRt) / 1000.0)
                } else baseMyAcc
                
                if (viewerPos != lastViewerPos || abs(drift - lastViewerDrift) > 1.0) {
                    accuracyCirclesFolder.items.remove(viewerCircle)
                    viewerCircle.points = Polygon.pointsAsCircle(viewerPos, drift).map { GeoPoint(it.latitude, it.longitude) }
                    viewerCircle.outlinePaint.color = if (isViewerFresh) ViewerCyan.copy(alpha = 0.7f).toArgb() else Slate500.copy(alpha = 0.7f).toArgb()
                    accuracyCirclesFolder.add(viewerCircle)
                    lastViewerPos = viewerPos
                    lastViewerDrift = drift
                } else if (!accuracyCirclesFolder.items.contains(viewerCircle)) {
                    accuracyCirclesFolder.add(viewerCircle)
                }
            }
            viewerMarker.position = viewerPos
            viewerMarker.icon = if (isViewerFresh) viewerIconFresh else viewerIconStale
            if (!mapView.overlays.contains(viewerMarker)) mapView.overlays.add(viewerMarker) 
        } else {
            accuracyCirclesFolder.items.remove(viewerCircle)
            mapView.overlays.remove(viewerMarker)
        }
    }

    private fun drawTrailToFolder(view: MapView, folder: FolderOverlay, trailPoints: List<TrailPoint>, color: Int, pool: MutableList<Polyline>): Int {
        if (trailPoints.isEmpty()) return 0
        var poolIdx = 0; var startIdx = 0
        while (startIdx < trailPoints.size) {
            val segmentPoints = mutableListOf<TrailPoint>(); var currentIdx = startIdx
            while (currentIdx < trailPoints.size) {
                val pt = trailPoints[currentIdx]; if (pt.status != SentinelStatus.VALID && currentIdx > startIdx) break
                segmentPoints.add(pt); currentIdx++
                if (pt.status != SentinelStatus.VALID) { startIdx = currentIdx; break }
            }
            if (segmentPoints.size > 1) {
                // Issue #548: Radial thinning (1.0m) to reduce redundant polyline points
                val simplified = PhysicsUtils.simplifyTrail(segmentPoints, 1.0, { it.lat }, { it.lng })
                
                if (simplified.size > 1) {
                    val line = if (poolIdx < pool.size) pool[poolIdx] else Polyline(view).also { l -> l.outlinePaint.strokeWidth = 4f; l.setInfoWindow(null); pool.add(l) }
                    line.setPoints(simplified.map { it.toGeoPoint() }); line.outlinePaint.color = color; folder.add(line); poolIdx++
                }
            }
            if (currentIdx == trailPoints.size) break
            startIdx = if (startIdx < currentIdx) (if (trailPoints[currentIdx - 1].status == SentinelStatus.VALID) currentIdx - 1 else currentIdx) else startIdx + 1
        }
        return poolIdx
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
}
