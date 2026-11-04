package com.jd.app.monitor

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.snapshots.Snapshot
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

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

            if (state.hydrationLevel >= 4) {
                changed = overlayManager.updateHomePoints(state.homePoints, state.isFenceVisible, state.maxDistance, isTrackerMode, state.geofenceMode, onTap, onRemoveMarker) || changed
            }
            if (state.hydrationLevel >= 5) {
                changed = overlayManager.updateTrails(state.trackerSegments, state.viewerSegments, state.systemPulseRt) || changed
            }
            if (state.hydrationLevel >= 6) {
                changed = overlayManager.updateCurrentPositions(
                    trackerValid = state.isTrackerValid,
                    trackerPos = GeoPoint(state.smoothedTrackerLat, state.smoothedTrackerLng),
                    isTrackerFresh = state.isTrackerFresh,
                    trackerAccuracy = state.trackerAccuracy,
                    maxTrackerAccuracy = state.trackerMaxAccuracy,
                    trackerSpeed = state.trackerSpeed,
                    isTrackerPending = state.trackerLocPending,
                    trackerLastValidFixRt = state.trackerLastValidFixRt,
                    viewerValid = state.isViewerValid,
                    viewerPos = GeoPoint(state.smoothedViewerLat, state.smoothedViewerLng),
                    isViewerFresh = state.isViewerFresh,
                    viewerAccuracy = state.viewerAccuracy,
                    viewerMaxAcc = state.viewerMaxAcc,
                    viewerSpeed = state.viewerSpeed,
                    isViewerPending = false,
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
            handleFollowLogic(state)
        }
    }

    fun centerTracker(pos: GeoPoint?) {
        if (pos != null) {
            lastTriggerPulseRt = SystemClock.elapsedRealtime()
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
        if (SystemClock.elapsedRealtime() - lastTriggerPulseRt < 500) return

        val sTrk = GeoPoint(state.smoothedTrackerLat, state.smoothedTrackerLng)
        val sVwr = GeoPoint(state.smoothedViewerLat, state.smoothedViewerLng)

        when (state.mapFollowMode) {
            MapFollowMode.VIEWER -> mapView.controller.setCenter(sVwr)
            MapFollowMode.TRACKER -> mapView.controller.setCenter(sTrk)
            MapFollowMode.AUTO -> {
                if (state.isTrackerFresh && state.isViewerFresh) {
                    val box = BoundingBox.fromGeoPoints(listOf(sTrk, sVwr))
                    mapView.zoomToBoundingBox(box.increaseByScale(1.4f), false)
                    if (mapView.zoomLevelDouble > 18.0) mapView.controller.setZoom(18.0)
                } else {
                    mapView.controller.setCenter(sTrk)
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
