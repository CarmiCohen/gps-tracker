package com.gps19.app

import javax.inject.Inject

/**
 * MapUseCase: Logic for map-related UI state transitions and triggers.
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor.
 * v9.5.0:
 * - Issue #503: Hilt Removal.
 */
class MapUseCase @Inject constructor() {
    fun handleMapEvent(event: UiEvent, currentState: MainUiState): MainUiState {
        return when (event) {
            is UiEvent.SetFenceVisible -> currentState.copy(isFenceVisible = event.visible)
            is UiEvent.SetViolationsVisible -> currentState.copy(isViolationsVisible = event.visible)
            is UiEvent.SetGeofenceViolationsVisible -> currentState.copy(isGeofenceViolationsVisible = event.visible)
            is UiEvent.SetMapButtonsVisible -> currentState.copy(isMapButtonsVisible = event.visible)
            is UiEvent.SetMapLocked -> currentState.copy(isMapLocked = event.locked)
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
