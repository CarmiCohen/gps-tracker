package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UiStateAggregator: Orchestrates the transformation of raw domain states into UI-ready models.
 * Aug.21.09:
 * - Issue #248 Performance Optimization: Refined HUD aggregation signatures to 
 *   accept granular parameters, enabling flow-level pruning and eliminating 
 *   Davey stalls on budget hardware (R248).
 */
interface UiStateAggregator {
    fun aggregateDashboardConnectivity(
        appMode: String?,
        diag: DiagnosticState,
        pulse: Long
    ): DashboardConnectivityState

    fun aggregateDashboardTelemetry(
        appMode: String?,
        kin: KinematicState,
        pulse: Long,
        trkState: TrackerState
    ): DashboardTelemetryState

    fun aggregateDashboardHealth(
        appMode: String?,
        kin: KinematicState,
        diag: DiagnosticState,
        lMax: Double,
        tMax: Double
    ): DashboardHealthState

    fun aggregateHudConnectivity(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        diag: DiagnosticState,
        rtt: Int,
        sig: Int
    ): HudConnectivityState

    fun aggregateHudTelemetry(
        appMode: String?,
        kin: KinematicState,
        pulse: Long,
        trkState: TrackerState
    ): HudTelemetryState

    fun aggregateHudHealth(
        diag: DiagnosticState,
        pulse: Long
    ): HudHealthState
}

@Singleton
class UiStateAggregatorImpl @Inject constructor(
    private val dashboardStateProvider: DashboardStateProvider
) : UiStateAggregator {

    override fun aggregateDashboardConnectivity(
        appMode: String?,
        diag: DiagnosticState,
        pulse: Long
    ): DashboardConnectivityState {
        return dashboardStateProvider.buildDashboardConnectivityState(appMode, diag, pulse)
    }

    override fun aggregateDashboardTelemetry(
        appMode: String?,
        kin: KinematicState,
        pulse: Long,
        trkState: TrackerState
    ): DashboardTelemetryState {
        return dashboardStateProvider.buildDashboardTelemetryState(appMode, kin, pulse, trkState)
    }

    override fun aggregateDashboardHealth(
        appMode: String?,
        kin: KinematicState,
        diag: DiagnosticState,
        lMax: Double,
        tMax: Double
    ): DashboardHealthState {
        return dashboardStateProvider.buildDashboardHealthState(appMode, kin, diag, lMax, tMax)
    }

    override fun aggregateHudConnectivity(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        diag: DiagnosticState,
        rtt: Int,
        sig: Int
    ): HudConnectivityState {
        return dashboardStateProvider.buildHudConnectivityState(appMode, deviceId, viewerId, isSystemActive, diag, rtt, sig)
    }

    override fun aggregateHudTelemetry(
        appMode: String?,
        kin: KinematicState,
        pulse: Long,
        trkState: TrackerState
    ): HudTelemetryState {
        return dashboardStateProvider.buildHudTelemetryState(appMode, kin, pulse, trkState)
    }

    override fun aggregateHudHealth(
        diag: DiagnosticState,
        pulse: Long
    ): HudHealthState {
        return dashboardStateProvider.buildHudHealthState(diag, pulse)
    }
}
