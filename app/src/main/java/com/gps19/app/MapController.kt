package com.gps19.app

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.snapshots.Snapshot
import com.gps19.core.engine.*
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * MapController: Decouples imperative osmdroid manipulation from Compose UI.
 * Oct.4.5:
 * - Issue #1425: Unified Clock Authority. Migrated manual trigger pulse to 
 *   monotonic time (SystemClock.elapsedRealtime) to prevent follow-logic 
 *   lockout glitches during clock sync.
 * Sep.30.42:
 * - Issue #1390: Transitioned from cumulative triggers to explicit camera action methods.
 */
class MapController(
    private val context: Context,
    private val mapView: MapView,
    density: Float
) {
    private val overlayManager = MapOverlayManager(context, mapView, density)
    private var lastTriggerPulseRt = 0L

    fun update(state: MapViewState, onTap: (GeoPoint) -> Unit, onRemoveMarker: (Int) -> Unit) {
        Snapshot.withoutReadObservation {
            val isTrackerMode = state.appMode == "tracker"
            var changed = false

            // Overlay Hydration Layers
            if (state.hydrationLevel >= 4) {
                changed = overlayManager.updateHomePoints(state.homePoints, state.isFenceVisible, state.maxDistance, isTrackerMode, state.geofenceMode, onTap, onRemoveMarker) || changed
            }
            if (state.hydrationLevel >= 5) {
                changed = overlayManager.updateTrails(state.trackerSegments, state.viewerSegments, state.systemPulseRt) || changed
            }
            if (state.hydrationLevel >= 6) {
                changed = overlayManager.updateCurrentPositions(
                    trackerValid = state.isTrackerValid,
                    trackerPos = state.smoothedTrackerPos,
                    isTrackerFresh = state.isTrackerFresh,
                    trackerAccuracy = state.trackerAccuracy,
                    maxTrackerAccuracy = state.trackerMaxAccuracy,
                    trackerSpeed = state.trackerSpeed,
                    isTrackerPending = state.trackerLocPending,
                    trackerLastValidFixRt = state.trackerLastValidFixRt,
                    viewerValid = state.isViewerValid,
                    viewerPos = state.smoothedViewerPos,
                    isViewerFresh = state.isViewerFresh,
                    viewerAccuracy = state.viewerAccuracy,
                    viewerMaxAcc = state.viewerMaxAcc,
                    viewerSpeed = state.viewerSpeed,
                    isViewerPending = state.viewerLocPending,
                    viewerLastValidFixRt = state.viewerLastValidFixRt,
                    systemPulseRt = state.systemPulseRt
                ) || changed
            }
            if (state.hydrationLevel >= 7) {
                changed = overlayManager.updateViolations(state.violations, state.isViolationsVisible, state.isGeofenceViolationsVisible, state.systemPulseRt) || changed
            }
            if (state.hydrationLevel >= 8) {
                changed = overlayManager.updateReplayCursor(state.replayCursorPos) || changed
            }

            if (changed) mapView.invalidate()

            // Camera Coordination
            handleFollowLogic(state)
        }
    }

    fun centerTracker(pos: GeoPoint?) {
        if (pos != null) {
            lastTriggerPulseRt = SystemClock.elapsedRealtime() // Monotonic lockout
            mapView.controller.animateTo(pos)
            mapView.controller.setZoom(18.0)
        }
    }

    fun centerViewer(pos: GeoPoint?) {
        if (pos != null) {
            lastTriggerPulseRt = SystemClock.elapsedRealtime()
            mapView.controller.animateTo(pos)
            mapView.controller.setZoom(18.0)
        }
    }

    fun zoomIn() {
        mapView.controller.zoomIn()
    }

    fun zoomOut() {
        mapView.controller.zoomOut()
    }

    private fun handleFollowLogic(state: MapViewState) {
        if (!state.isMapLocked) return
        // Lockout following if a manual trigger happened recently (Issue #1425)
        if (SystemClock.elapsedRealtime() - lastTriggerPulseRt < 500) return

        val sTrk = state.smoothedTrackerPos
        val sVwr = state.smoothedViewerPos

        when (state.mapFollowMode) {
            MapFollowMode.VIEWER -> if (sVwr != null) mapView.controller.setCenter(sVwr)
            MapFollowMode.TRACKER -> if (sTrk != null) mapView.controller.setCenter(sTrk)
            MapFollowMode.AUTO -> {
                if (sTrk != null && sVwr != null && state.isTrackerFresh && state.isViewerFresh) {
                    val dist = PhysicsUtils.calculateDistance(sTrk.latitude, sTrk.longitude, sVwr.latitude, sVwr.longitude)
                    if (dist in 100.0..100000.0) {
                        val box = BoundingBox.fromGeoPoints(listOf(sTrk, sVwr))
                        mapView.zoomToBoundingBox(box.increaseByScale(1.4f), false)
                        if (mapView.zoomLevelDouble > 18.0) mapView.controller.setZoom(18.0)
                    } else mapView.controller.setCenter(sTrk)
                } else if (sTrk != null || sVwr != null) {
                    mapView.controller.setCenter(sTrk ?: sVwr!!)
                }
            }
            MapFollowMode.NONE -> {}
        }
    }

    fun trimMemory(level: Int) = overlayManager.trimMemory(level)

    fun detach() {
        overlayManager.onDetach()
        mapView.onDetach()
        mapView.tileProvider.tileCache.clear()
        mapView.tileProvider.detach()
    }
}
