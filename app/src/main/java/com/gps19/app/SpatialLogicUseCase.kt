package com.gps19.app

import com.gps19.core.engine.PhysicsUtils
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

/**
 * SpatialLogicUseCase: Consolidated business logic for spatial operations, 
 * geofence management, and map-related state transitions.
 * 
 * Sep.22.05:
 * - Issue #1181: Consolidated HomePointUseCase and MapUseCase to reduce 
 *   ViewModel dependency surface area (R-ID 402).
 */
class SpatialLogicUseCase @Inject constructor(
    private val repository: MainRepository
) {
    // --- Home Point Management (formerly HomePointUseCase) ---

    suspend fun addHomePoint(newPoint: GeoPoint): List<GeoPoint> {
        repository.addHomePoint(newPoint.latitude, newPoint.longitude)
        return repository.loadHomePoints()
    }

    suspend fun removeHomePoint(index: Int): List<GeoPoint> {
        repository.removeHomePoint(index)
        return repository.loadHomePoints()
    }

    suspend fun clearHomePoints(maxDistance: Double): List<GeoPoint> {
        repository.saveHomePoints(emptyList(), maxDistance)
        return emptyList()
    }

    fun findNearestPointIndex(currentPoints: List<GeoPoint>, target: GeoPoint, thresholdMeters: Double = 100.0): Int {
        if (currentPoints.isEmpty()) return -1
        var nearestIdx = -1
        var minDist = Double.MAX_VALUE
        currentPoints.forEachIndexed { index, geoPoint ->
            val d = PhysicsUtils.calculateDistance(target.latitude, target.longitude, geoPoint.latitude, geoPoint.longitude)
            if (d < minDist && d < thresholdMeters) {
                minDist = d
                nearestIdx = index
            }
        }
        return nearestIdx
    }

    // --- Map Event Handling (formerly MapUseCase) ---

    fun handleMapEvent(event: UiEvent, currentState: MainUiState): MainUiState {
        return when (event) {
            is UiEvent.SetFenceVisible -> currentState.copy(isFenceVisible = event.visible)
            is UiEvent.SetViolationsVisible -> currentState.copy(isViolationsVisible = event.visible)
            is UiEvent.SetGeofenceViolationsVisible -> currentState.copy(isGeofenceViolationsVisible = event.visible)
            is UiEvent.SetMapButtonsVisible -> currentState.copy(isMapButtonsVisible = event.visible)
            is UiEvent.SetMapLocked -> currentState.copy(isMapLocked = event.locked)
            is UiEvent.SetGeofenceMode -> currentState.copy(geofenceMode = event.mode)
            is UiEvent.MapZoomIn -> currentState.copy(zoomInTrigger = currentState.zoomInTrigger + 1)
            is UiEvent.MapZoomOut -> currentState.copy(zoomOutTrigger = currentState.zoomOutTrigger + 1)
            is UiEvent.CenterTracker -> currentState.copy(
                centeringTrackerTrigger = currentState.centeringTrackerTrigger + 1, 
                isMapLocked = true,
                mapFollowMode = MapFollowMode.AUTO
            )
            is UiEvent.CenterViewer -> currentState.copy(
                centeringViewerTrigger = currentState.centeringViewerTrigger + 1, 
                isMapLocked = true,
                mapFollowMode = MapFollowMode.VIEWER
            )
            else -> currentState
        }
    }
}
