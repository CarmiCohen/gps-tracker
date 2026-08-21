package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UiStateAggregator: Orchestrates the transformation of raw domain states into UI-ready models.
 * Aug.21.08:
 * - Issue #240: Extracted aggregation logic from MainViewModel to resolve parameter 
 *   limit concerns in combine blocks (Simplify Idea #1).
 */
interface UiStateAggregator {
    fun aggregateDashboard(
        ui: MainUiState,
        kin: KinematicState,
        diag: DiagnosticState,
        pulse: Long,
        trkState: TrackerState,
        lMax: Double,
        tMax: Double
    ): DashboardState

    fun aggregateHud(
        ui: MainUiState,
        kin: KinematicState,
        diag: DiagnosticState,
        pulse: Long,
        trkState: TrackerState,
        rtt: Int,
        sig: Int
    ): HudState
}

@Singleton
class UiStateAggregatorImpl @Inject constructor(
    private val dashboardStateProvider: DashboardStateProvider
) : UiStateAggregator {

    override fun aggregateDashboard(
        ui: MainUiState,
        kin: KinematicState,
        diag: DiagnosticState,
        pulse: Long,
        trkState: TrackerState,
        lMax: Double,
        tMax: Double
    ): DashboardState {
        return dashboardStateProvider.buildDashboardState(ui.appMode, kin, diag, pulse, trkState, lMax, tMax)
    }

    override fun aggregateHud(
        ui: MainUiState,
        kin: KinematicState,
        diag: DiagnosticState,
        pulse: Long,
        trkState: TrackerState,
        rtt: Int,
        sig: Int
    ): HudState {
        return dashboardStateProvider.buildHudState(ui, kin, diag, pulse, trkState, rtt, sig)
    }
}
