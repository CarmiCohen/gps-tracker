package com.gps19.app

import javax.inject.Inject
import javax.inject.Singleton

/**
 * NavigationUseCase: Manages UI navigation state transitions and overlay logic.
 * v9.3.11:
 * - Issue #059: Added NavigateToDiagnostics handling for Permission Health Check.
 */
@Singleton
class NavigationUseCase @Inject constructor(
    private val repository: MainRepository
) {
    fun handleNavigationEvent(event: UiEvent, currentState: MainUiState): NavigationState {
        val nav = currentState.navigation
        val isAnyOverlayOpen = nav.isLogVisible || nav.isSettingsOpen || nav.isPhoneSetupVisible || 
                               nav.isRibbonsVisible || nav.isGnssDetailVisible || nav.isDiagnosticsVisible
        
        return when (event) {
            is UiEvent.ToggleMap -> {
                nav.copy(
                    isMapVisible = event.visible, 
                    isLogVisible = false, 
                    isSettingsOpen = false, 
                    isPhoneSetupVisible = false, 
                    isRibbonsVisible = false, 
                    isGnssDetailVisible = false,
                    isDiagnosticsVisible = false,
                    activeSubSettings = null, 
                    wasMapVisibleBeforeOverlay = if (event.visible) true else nav.wasMapVisibleBeforeOverlay
                )
            }
            is UiEvent.ToggleLog -> { 
                if (event.visible) {
                    repository.updateLogFilters(details = false, recovered = false)
                    val wasMap = if (isAnyOverlayOpen) nav.wasMapVisibleBeforeOverlay else nav.isMapVisible
                    nav.copy(isLogVisible = true, isMapVisible = false, isSettingsOpen = false, isPhoneSetupVisible = false, isRibbonsVisible = false, isGnssDetailVisible = false, isDiagnosticsVisible = false, activeSubSettings = null, wasMapVisibleBeforeOverlay = wasMap)
                } else {
                    nav.copy(isLogVisible = false, isMapVisible = nav.wasMapVisibleBeforeOverlay)
                }
            }
            is UiEvent.ToggleSettings -> { 
                if (event.visible) {
                    val wasMap = if (isAnyOverlayOpen) nav.wasMapVisibleBeforeOverlay else nav.isMapVisible
                    nav.copy(isSettingsOpen = true, isMapVisible = false, isLogVisible = false, isPhoneSetupVisible = false, isRibbonsVisible = false, isGnssDetailVisible = false, isDiagnosticsVisible = false, activeSubSettings = null, wasMapVisibleBeforeOverlay = wasMap)
                } else {
                    nav.copy(isSettingsOpen = false, isMapVisible = nav.wasMapVisibleBeforeOverlay)
                }
            }
            is UiEvent.TogglePhoneSetup -> {
                if (event.visible) {
                    val wasMap = if (isAnyOverlayOpen) nav.wasMapVisibleBeforeOverlay else nav.isMapVisible
                    nav.copy(isPhoneSetupVisible = true, isMapVisible = false, isLogVisible = false, isSettingsOpen = false, isRibbonsVisible = false, isGnssDetailVisible = false, isDiagnosticsVisible = false, activeSubSettings = null, wasMapVisibleBeforeOverlay = wasMap)
                } else {
                    nav.copy(isPhoneSetupVisible = false, isMapVisible = nav.wasMapVisibleBeforeOverlay)
                }
            }
            is UiEvent.NavigateToDiagnostics -> {
                if (event.visible) {
                    val wasMap = if (isAnyOverlayOpen) nav.wasMapVisibleBeforeOverlay else nav.isMapVisible
                    nav.copy(isDiagnosticsVisible = true, isMapVisible = false, isLogVisible = false, isSettingsOpen = false, isPhoneSetupVisible = false, isRibbonsVisible = false, isGnssDetailVisible = false, activeSubSettings = null, wasMapVisibleBeforeOverlay = wasMap)
                } else {
                    nav.copy(isDiagnosticsVisible = false, isMapVisible = nav.wasMapVisibleBeforeOverlay)
                }
            }
            is UiEvent.ToggleRibbons -> {
                if (event.visible) {
                    val wasMap = if (isAnyOverlayOpen) nav.wasMapVisibleBeforeOverlay else nav.isMapVisible
                    nav.copy(isRibbonsVisible = true, isMapVisible = false, isLogVisible = false, isSettingsOpen = false, isPhoneSetupVisible = false, isGnssDetailVisible = false, isDiagnosticsVisible = false, activeSubSettings = null, wasMapVisibleBeforeOverlay = wasMap)
                } else {
                    nav.copy(isRibbonsVisible = false, isMapVisible = nav.wasMapVisibleBeforeOverlay)
                }
            }
            is UiEvent.SetDashboardExpanded -> nav.copy(isDashboardExpanded = event.expanded)
            is UiEvent.ShowStopTrackingConfirmation -> nav.copy(isStopTrackingConfirmationVisible = event.show)
            is UiEvent.ToggleGnssDetail -> nav.copy(isGnssDetailVisible = event.visible)
            is UiEvent.SetSubSettings -> nav.copy(activeSubSettings = event.sub)
            else -> nav
        }
    }
}
