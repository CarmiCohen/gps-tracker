package com.gps19.app

import com.gps19.core.engine.PhysicsUtils
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

/**
 * SpatialLogicUseCase: Consolidated business logic for spatial operations, 
 * geofence management, and map-related state transitions.
 * 
 * Sep.22.08:
 * - Issue #1166: State Partitioning & Slicing. Updated handleMapEvent to 
 *   support the nested MainUiState structure (R-ID 405).
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
            is UiEvent.SetFenceVisible -> currentState.copy(spatial = currentState.spatial.copy(isFenceVisible = event.visible))
            is UiEvent.SetViolationsVisible -> currentState.copy(spatial = currentState.spatial.copy(isViolationsVisible = event.visible))
            is UiEvent.SetGeofenceViolationsVisible -> currentState.copy(spatial = currentState.spatial.copy(isGeofenceViolationsVisible = event.visible))
            is UiEvent.SetMapButtonsVisible -> currentState.copy(spatial = currentState.spatial.copy(isMapButtonsVisible = event.visible))
            is UiEvent.SetMapLocked -> currentState.copy(spatial = currentState.spatial.copy(isMapLocked = event.locked))
            is UiEvent.SetGeofenceMode -> currentState.copy(spatial = currentState.spatial.copy(geofenceMode = event.mode))
            is UiEvent.MapZoomIn -> currentState.copy(triggers = currentState.triggers.copy(zoomInTrigger = currentState.triggers.zoomInTrigger + 1))
            is UiEvent.MapZoomOut -> currentState.copy(triggers = currentState.triggers.copy(zoomOutTrigger = currentState.triggers.zoomOutTrigger + 1))
            is UiEvent.CenterTracker -> currentState.copy(
                triggers = currentState.triggers.copy(centeringTrackerTrigger = currentState.triggers.centeringTrackerTrigger + 1),
                spatial = currentState.spatial.copy(
                    isMapLocked = true,
                    mapFollowMode = MapFollowMode.AUTO
                )
            )
            is UiEvent.CenterViewer -> currentState.copy(
                triggers = currentState.triggers.copy(centeringViewerTrigger = currentState.triggers.centeringViewerTrigger + 1),
                spatial = currentState.spatial.copy(
                    isMapLocked = true,
                    mapFollowMode = MapFollowMode.VIEWER
                )
            )
            else -> currentState
        }
    }
}
