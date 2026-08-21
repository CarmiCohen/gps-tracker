package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DashboardStateProvider: Dedicated provider for UI-ready dashboard and HUD states.
 * Aug.20.09:
 * - Issue #226: HUD State Centralization. Added buildHudState to consolidate 
 *   telemetry logic for status badges and ribbons (R226). Fixed lambda 
 *   parameter inference for Samsung A15 compiler stability.
 * Aug.13.11:
 * - Issue #163: 1Hz Telemetry Path Optimization.
 */
interface DashboardStateProvider {
    fun buildDashboardState(
        appMode: String?,
        kinematicState: KinematicState,
        diagnosticState: DiagnosticState,
        now: Long,
        trackerState: TrackerState,
        localMaxTemp: Double,
        trackerMaxTemp: Double
    ): DashboardState

    fun buildHudState(
        uiState: MainUiState,
        kinematicState: KinematicState,
        diagnosticState: DiagnosticState,
        systemPulse: Long,
        trackerState: TrackerState,
        rtt: Int,
        remoteSignal: Int
    ): HudState
}

@Singleton
class DashboardStateProviderImpl @Inject constructor() : DashboardStateProvider {

    override fun buildDashboardState(
        appMode: String?,
        kinematicState: KinematicState,
        diagnosticState: DiagnosticState,
        now: Long,
        trackerState: TrackerState,
        localMaxTemp: Double,
        trackerMaxTemp: Double
    ): DashboardState {
        val mode = appMode
        val isTrackerMode = mode == "tracker"
        val isViewer = mode == "viewer"

        val activeStats = if (isViewer) diagnosticState.trackerStats else diagnosticState.stats
        
        val lastSeenTs = diagnosticState.connectivity.lastRemoteActivityTs

        val watchdogSec = if (lastSeenTs > 0) {
            val remaining = (WATCH_TIMEOUT_MS - (now - lastSeenTs)) / 1000
            maxOf(0L, remaining)
        } else 0L

        val loc = if (isViewer) kinematicState.trackerLocation else kinematicState.localLocation
        val health = if (isViewer) kinematicState.trackerHealth else kinematicState.localHealth
        
        // Receipt-based freshness calculation
        val telemetryAge = if (loc.telemetryTs > 0) now - loc.telemetryTs else Long.MAX_VALUE
        val sourceGpsAge = if (loc.telemetryTs > 0 && loc.timestamp > 0) maxOf(0L, loc.telemetryTs - loc.timestamp) else 0L
        val totalGpsAge = telemetryAge + sourceGpsAge

        val isTelemetryFresh = telemetryAge < TELEMETRY_UI_STALE_THRESHOLD_MS
        val isGpsActive = totalGpsAge < GPS_UI_FAIL_THRESHOLD_MS && loc.timestamp > 0

        val gnss = loc.gnssDetail
        var avgCn0 = 0.0
        if (gnss != null) {
            val satellites = gnss.satellites
            if (satellites.isNotEmpty()) {
                var sum = 0.0
                for (sat in satellites) {
                    sum += sat.cn0
                }
                avgCn0 = sum / satellites.size
            }
        }

        return DashboardState(
            trackerState = trackerState,
            status = loc.status,
            isTamperDetected = health.isTamperDetected,
            maxDropMs = activeStats.maxDropMs,
            lastSeenTs = lastSeenTs,
            totalDropMs = activeStats.totalDropMs,
            watchdogCountdownSec = if (isTrackerMode) 0L else watchdogSec,
            watchdogOk = if (isTrackerMode) true else (watchdogSec > 0),
            totalUptimeMs = activeStats.uptimeMs,
            sessionMs = if (activeStats.lastConnTs > 0) activeStats.sessionConnectedMs else 0L,
            engineVersion = BuildConfig.VERSION_NAME,
            sinceConnMs = if (activeStats.lastConnTs > 0) (now - activeStats.lastConnTs) else 0L,
            sinceDiscoMs = if (activeStats.lastDiscTs > 0) (now - activeStats.lastDiscTs) else 0L,
            violationUptimeMs = health.violationUptimeMs,
            violationPercentage = health.violationPercentage,
            lat = if (isGpsActive) loc.lat else 0.0,
            lng = if (isGpsActive) loc.lng else 0.0,
            trackerAccuracy = loc.accuracy,
            trackerMaxAcc = if (loc.maxAccuracy > 0) loc.maxAccuracy else loc.accuracy,
            satsUsed = diagnosticState.trackerSatsUsed,
            satsView = diagnosticState.trackerSatsView,
            isSatsIndexWarning = (diagnosticState.trackerSatsUsed < 4 && diagnosticState.trackerSatsView > 0),
            viewerAccuracy = if (isTrackerMode) 0.0 else kinematicState.localLocation.accuracy,
            viewerMaxAcc = if (isTrackerMode) 0.0 else (if(kinematicState.localLocation.maxAccuracy > 0) kinematicState.localLocation.maxAccuracy else kinematicState.localLocation.accuracy),
            vibration = health.vibration,
            heading = health.heading,
            tilt = health.tiltDegrees,
            acousticDb = health.acousticDb,
            baroAlt = health.baroAlt,
            lux = health.lux,
            isNear = health.isNear,
            proximityCm = health.proximityCm,
            proximityDebounceMs = health.proximityDebounceMs,
            rollingVibration = health.vibrationRollingSum,
            kineticEnergy = health.kineticEnergy,
            gpsSpeedMps = loc.speed,
            trackerMaxTemp = trackerMaxTemp,
            viewerMaxTemp = localMaxTemp,
            peakShock = health.peakVibrationShock,
            vibrationFloor = health.adaptiveVibrationFloor,
            luxBaseline = health.luxBaseline,
            acousticFloorDb = health.acousticFloorDb,
            isMicPending = health.micPending,
            isPowerTamper = health.isPowerTamper,
            isPowerSaveMode = health.isPowerSaveMode,
            standbyBucket = health.standbyBucket,
            netInterface = health.netInterface,
            isStorageLow = health.isStorageLow,
            isStorageCritical = health.isStorageCritical,
            snr = avgCn0,
            distToHome = kinematicState.distanceTrackerToHome,
            distToViewer = kinematicState.distanceTrackerToViewer,
            isGpsFresh = isGpsActive,
            isLinkFresh = (telemetryAge < WATCH_DOG_UI_GRACE_MS),
            isTelemetryFresh = isTelemetryFresh,
            isGpsVisible = isTelemetryFresh,
            isLinkVisible = isTelemetryFresh,
            isBatterySteepDischarge = health.isBatterySteepDischarge,
            isCoolingModeActive = health.isCoolingModeActive,
            trackerCurrentMa = health.currentMa,
            isLocationPending = health.isLocationPending,
            locationPendingReason = health.locationPendingReason,
            isBatteryLow = health.isBatteryLow,
            isBatteryCritical = health.isBatteryCritical,
            cpuLoad = health.cpuLoad,
            ioWait = health.ioWait,
            maxIoLatencyMs = health.maxIoLatency,
            isSilentFailure = health.isSilentFailure
        )
    }

    override fun buildHudState(
        uiState: MainUiState,
        kinematicState: KinematicState,
        diagnosticState: DiagnosticState,
        systemPulse: Long,
        trackerState: TrackerState,
        rtt: Int,
        remoteSignal: Int
    ): HudState {
        val mode = uiState.appMode
        
        // Freshness Logic
        val loc = if (mode == "viewer") kinematicState.trackerLocation else kinematicState.localLocation
        val telemetryAge = if (loc.telemetryTs > 0) systemPulse - loc.telemetryTs else Long.MAX_VALUE
        val sourceGpsAge = if (loc.telemetryTs > 0 && loc.timestamp > 0) maxOf(0L, loc.telemetryTs - loc.timestamp) else 0L
        val totalGpsAge = telemetryAge + sourceGpsAge

        val isTelemetryFresh = telemetryAge < TELEMETRY_UI_STALE_THRESHOLD_MS
        val isGpsFresh = totalGpsAge < GPS_UI_FAIL_THRESHOLD_MS && loc.timestamp > 0

        val commIndex = if (uiState.isSystemActive && diagnosticState.connectivity.isRelayConnected) {
            TelemetryUtils.calculateCommIndex(rtt, 10, 10)
        } else 0

        val remoteCommIndex = if (mode == "viewer" && isTelemetryFresh) {
            TelemetryUtils.calculateCommIndex(rtt, remoteSignal, 10)
        } else 0

        val lastGpsTs = if (mode == "viewer") kinematicState.trackerLocation.timestamp else kinematicState.localLocation.timestamp
        val lastTelemetryTs = if (mode == "viewer") maxOf(kinematicState.trackerLocation.timestamp, kinematicState.trackerLocation.telemetryTs) else kinematicState.localLocation.timestamp
        val rawPulse = if (mode == "tracker") diagnosticState.connectivity.lastRemoteActivityTs else lastTelemetryTs
        
        val age = if (rawPulse > 0) systemPulse - rawPulse else Long.MAX_VALUE
        val progressValue = if (rawPulse > 0) {
            maxOf(0f, minOf(1f, (TELEMETRY_UI_STALE_THRESHOLD_MS - age).toFloat() / TELEMETRY_UI_STALE_THRESHOLD_MS))
        } else 0f

        return HudState(
            appMode = mode,
            isInternet = diagnosticState.connectivity.isLocalOnline,
            isRelayConnected = diagnosticState.connectivity.isRelayConnected,
            isTelemetryFresh = isTelemetryFresh,
            isDataHealthy = isTelemetryFresh && diagnosticState.connectivity.isLocalOnline && diagnosticState.connectivity.isRelayConnected,
            isLocalGpsActive = if (mode == "tracker") isGpsFresh else (systemPulse - kinematicState.localLocation.timestamp < GPS_UI_FAIL_THRESHOLD_MS),
            isGpsFresh = isGpsFresh,
            battery = diagnosticState.battery.level,
            remoteBattery = if (mode == "viewer") diagnosticState.trackerBattery.level else -1,
            isCharging = diagnosticState.battery.isChargingStable,
            remoteCharging = if (mode == "viewer") diagnosticState.trackerBattery.isChargingStable else false,
            speedMps = (if (mode == "viewer") kinematicState.trackerLocation.speed else 0.0).toFloat(),
            trackerAccuracy = kinematicState.trackerLocation.accuracy.toFloat(),
            maxTrackerAccuracy = kinematicState.trackerLocation.maxAccuracy.toFloat(),
            viewerAccuracy = (if (kinematicState.localLocation.lat != 0.0) kinematicState.localLocation.accuracy.toFloat() else 0f),
            maxViewerAccuracy = kinematicState.localLocation.maxAccuracy.toFloat(),
            satsUsed = diagnosticState.trackerSatsUsed,
            satsView = diagnosticState.trackerSatsView,
            viewerSatsUsed = diagnosticState.viewerSatsUsed,
            viewerSatsView = diagnosticState.viewerSatsView,
            trackerTemp = diagnosticState.trackerBattery.temp.toFloat(),
            viewerTemp = diagnosticState.battery.temp.toFloat(),
            distToHome = kinematicState.distanceTrackerToHome,
            distToViewer = kinematicState.distanceTrackerToViewer,
            trackerId = uiState.deviceId,
            viewerId = uiState.viewerId,
            watchdogOk = if (mode == "viewer") (isTelemetryFresh || (systemPulse - diagnosticState.connectivity.lastRemoteActivityTs < WATCH_DOG_UI_GRACE_MS)) else true,
            trackerState = trackerState,
            hasActiveAlarms = diagnosticState.activeAlarms.any { alarm -> !alarm.isResolved },
            isRedScreenSuppressed = (diagnosticState.activeAlarms.any { alarm -> !alarm.isResolved } && !diagnosticState.isRedScreenVisible),
            isSirenPlaying = diagnosticState.isSirenPlaying,
            isTrackerLocPending = kinematicState.trackerHealth.isLocationPending,
            trackerLocPendingReason = kinematicState.trackerHealth.locationPendingReason,
            isViewerLocPending = kinematicState.localHealth.isLocationPending,
            viewerLocPendingReason = kinematicState.localHealth.locationPendingReason,
            commIndex = commIndex,
            remoteCommIndex = remoteCommIndex,
            lastGpsTs = lastGpsTs,
            viewerGpsTs = kinematicState.localLocation.timestamp,
            progressPulse = progressValue,
            systemPulse = systemPulse,
            activeAlarms = diagnosticState.activeAlarms
        )
    }
}
