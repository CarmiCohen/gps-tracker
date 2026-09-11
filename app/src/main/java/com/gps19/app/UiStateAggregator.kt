package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UiStateAggregator: Orchestrates the transformation of raw domain states into UI-ready models.
 * Sep.11.48:
 * - Issue #946: Vitality Pulse Standardization. Added pulse to all 
 *   aggregation methods to ensure flow freshness (R-ID 289).
 * Sep.06.50:
 * - Issue #932: HUD Synchronization. Added isA15 to aggregateHudConnectivity 
 *   to provide visual confirmation of hardware adaptations (R-ID 276).
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
        tMax: Double,
        pulse: Long
    ): DashboardHealthState

    fun aggregateHudConnectivity(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        isSafeMode: Boolean,
        isA15: Boolean,
        diag: DiagnosticState,
        rtt: Int,
        sig: Int,
        pulse: Long
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
        tMax: Double,
        pulse: Long
    ): DashboardHealthState {
        return dashboardStateProvider.buildDashboardHealthState(appMode, kinematicState, diag, lMax, tMax, pulse)
    }

    override fun aggregateHudConnectivity(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        isSafeMode: Boolean,
        isA15: Boolean,
        diag: DiagnosticState,
        rtt: Int,
        sig: Int,
        pulse: Long
    ): HudConnectivityState {
        return dashboardStateProvider.buildHudConnectivityState(appMode, deviceId, viewerId, isSystemActive, isSafeMode, isA15, diag, rtt, sig, pulse)
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
