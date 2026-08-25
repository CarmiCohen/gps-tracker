package com.gps19.app

import javax.inject.Inject

/**
 * NavigationUseCase: Logic for handling UI navigation state transitions.
 * Aug.25.01:
 * - Issue #311: Fixed navigation regression by adding handler for SetPendingMode.
 *   This ensures the app preserves the target mode during permission lifecycle interruptions.
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor.
 * v9.5.0:
 * - Issue #503: Hilt Removal.
 */
class NavigationUseCase @Inject constructor() {
    fun handleNavigationEvent(event: UiEvent, currentState: MainUiState): NavigationState {
        val nav = currentState.navigation
        return when (event) {
            is UiEvent.ToggleMap -> nav.copy(isMapVisible = event.visible)
            is UiEvent.ToggleLog -> nav.copy(isLogVisible = event.visible)
            is UiEvent.ToggleSettings -> nav.copy(isSettingsOpen = event.visible, activeSubSettings = null)
            is UiEvent.TogglePhoneSetup -> nav.copy(isPhoneSetupVisible = event.visible)
            is UiEvent.ToggleRibbons -> nav.copy(isRibbonsVisible = event.visible)
            is UiEvent.SetDashboardExpanded -> nav.copy(isDashboardExpanded = event.expanded)
            is UiEvent.ToggleGnssDetail -> nav.copy(isGnssDetailVisible = event.visible)
            is UiEvent.SetSubSettings -> nav.copy(activeSubSettings = event.sub)
            is UiEvent.ShowStopTrackingConfirmation -> nav.copy(isStopTrackingConfirmationVisible = event.show)
            is UiEvent.NavigateToDiagnostics -> nav.copy(isDiagnosticsVisible = event.visible)
            is UiEvent.SetPendingMode -> nav.copy(pendingMode = event.mode)
            else -> nav
        }
    }
}
