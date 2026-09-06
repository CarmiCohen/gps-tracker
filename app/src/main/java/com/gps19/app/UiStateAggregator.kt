package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UiStateAggregator: Orchestrates the transformation of raw domain states into UI-ready models.
 * Sep.06.06:
 * - Issue #924 RESOLVED (Part A): Watchdog Safe-Mode. Added isSafeMode 
 *   to aggregateHudConnectivity for visual safety status (R-ID 271).
 * Sep.05.25:
 * - Issue #266: Added isMaliAnomaly to HudHealth aggregation for UI-throttling.
 */
interface UiStateAggregator {
    fun aggregateDashboardConnectivity(
        appMode: String?,
        diag: DiagnosticState,
        pulse: Long
    ): DashboardConnectivityState

    fun aggregateDashboardTelemetry(
        appMode: String?,
        kinematicState: KinematicState,
        pulse: Long,
        trkState: TrackerState,
        isUltra: Boolean
    ): DashboardTelemetryState

    fun aggregateDashboardHealth(
        appMode: String?,
        kinematicState: KinematicState,
        diag: DiagnosticState,
        lMax: Double,
        tMax: Double
    ): DashboardHealthState

    fun aggregateHudConnectivity(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        isSafeMode: Boolean,
        diag: DiagnosticState,
        rtt: Int,
        sig: Int
    ): HudConnectivityState

    fun aggregateHudTelemetry(
        appMode: String?,
        kinematicState: KinematicState,
        pulse: Long,
        trkState: TrackerState,
        isUltra: Boolean
    ): HudTelemetryState

    fun aggregateHudHealth(
        diag: DiagnosticState,
        pulse: Long,
        isMaliAnomaly: Boolean
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
        kinematicState: KinematicState,
        pulse: Long,
        trkState: TrackerState,
        isUltra: Boolean
    ): DashboardTelemetryState {
        return dashboardStateProvider.buildDashboardTelemetryState(appMode, kinematicState, pulse, trkState, isUltra)
    }

    override fun aggregateDashboardHealth(
        appMode: String?,
        kinematicState: KinematicState,
        diag: DiagnosticState,
        lMax: Double,
        tMax: Double
    ): DashboardHealthState {
        return dashboardStateProvider.buildDashboardHealthState(appMode, kinematicState, diag, lMax, tMax)
    }

    override fun aggregateHudConnectivity(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        isSafeMode: Boolean,
        diag: DiagnosticState,
        rtt: Int,
        sig: Int
    ): HudConnectivityState {
        return dashboardStateProvider.buildHudConnectivityState(appMode, deviceId, viewerId, isSystemActive, isSafeMode, diag, rtt, sig)
    }

    override fun aggregateHudTelemetry(
        appMode: String?,
        kinematicState: KinematicState,
        pulse: Long,
        trkState: TrackerState,
        isUltra: Boolean
    ): HudTelemetryState {
        return dashboardStateProvider.buildHudTelemetryState(appMode, kinematicState, pulse, trkState, isUltra)
    }

    override fun aggregateHudHealth(
        diag: DiagnosticState,
        pulse: Long,
        isMaliAnomaly: Boolean
    ): HudHealthState {
        return dashboardStateProvider.buildHudHealthState(diag, pulse, isMaliAnomaly)
    }
}
