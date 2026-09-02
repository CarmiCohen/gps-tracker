package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DashboardStateProvider: Dedicated provider for UI-ready dashboard and HUD states.
 * Sep.02.68:
 * - Idea #243: Flattened StatusBar indicator chain. Populated isSystemActive 
 *   in HudConnectivityState to support unified state propagation (R243).
 * Sep.03.01:
 * - Issue #238: Location Model Unification. Updated to use ts and gpsTs 
 *   from LocationUpdate instead of telemetryTs and timestamp (R-ID 238).
 */
interface DashboardStateProvider {
    fun buildDashboardConnectivityState(
        appMode: String?,
        diagnosticState: DiagnosticState,
        now: Long
    ): DashboardConnectivityState

    fun buildDashboardTelemetryState(
        appMode: String?,
        kinematicState: KinematicState,
        now: Long,
        trackerState: TrackerState,
        isUltra: Boolean
    ): DashboardTelemetryState

    fun buildDashboardHealthState(
        appMode: String?,
        kinematicState: KinematicState,
        diagnosticState: DiagnosticState,
        localMaxTemp: Double,
        trackerMaxTemp: Double
    ): DashboardHealthState

    fun buildHudConnectivityState(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        diagnosticState: DiagnosticState,
        rtt: Int,
        remoteSignal: Int
    ): HudConnectivityState

    fun buildHudTelemetryState(
        appMode: String?,
        kinematicState: KinematicState,
        systemPulse: Long,
        trackerState: TrackerState,
        isUltra: Boolean
    ): HudTelemetryState

    fun buildHudHealthState(
        diagnosticState: DiagnosticState,
        systemPulse: Long
    ): HudHealthState
}

@Singleton
class DashboardStateProviderImpl @Inject constructor() : DashboardStateProvider {

    override fun buildDashboardConnectivityState(
        appMode: String?,
        diagnosticState: DiagnosticState,
        now: Long
    ): DashboardConnectivityState {
        val isViewer = appMode == "viewer"
        val activeStats = if (isViewer) diagnosticState.trackerStats else diagnosticState.stats
        val lastSeenTs = diagnosticState.connectivity.lastRemoteActivityTs

        val watchdogSec = if (lastSeenTs > 0) {
            val remaining = (WATCH_TIMEOUT_MS - (now - lastSeenTs)) / 1000
            maxOf(0L, remaining)
        } else 0L

        return DashboardConnectivityState(
            lastSeenTs = lastSeenTs,
            watchdogOk = if (appMode == "tracker") true else (watchdogSec > 0),
            watchdogCountdownSec = if (appMode == "tracker") 0L else watchdogSec,
            totalUptimeMs = activeStats.uptimeMs,
            sessionMs = if (activeStats.lastConnTs > 0) activeStats.sessionConnectedMs else 0L,
            sinceConnMs = if (activeStats.lastConnTs > 0) (now - activeStats.lastConnTs) else 0L,
            sinceDiscoMs = if (activeStats.lastDiscTs > 0) (now - activeStats.lastDiscTs) else 0L,
            totalDropMs = activeStats.totalDropMs,
            maxDropMs = activeStats.maxDropMs,
            engineVersion = BuildConfig.VERSION_NAME,
            netInterface = diagnosticState.connectivity.netInterface
        )
    }

    override fun buildDashboardTelemetryState(
        appMode: String?,
        kinematicState: KinematicState,
        now: Long,
        trackerState: TrackerState,
        isUltra: Boolean
    ): DashboardTelemetryState {
        val isViewer = appMode == "viewer"
        val loc = if (isViewer) kinematicState.trackerLocation else kinematicState.localLocation
        
        val telemetryAge = if (loc.ts > 0) now - loc.ts else Long.MAX_VALUE
        val sourceGpsAge = if (loc.ts > 0 && loc.gpsTs > 0) maxOf(0L, loc.ts - loc.gpsTs) else 0L
        val totalGpsAge = telemetryAge + sourceGpsAge

        val isTelemetryFresh = telemetryAge < TELEMETRY_UI_STALE_THRESHOLD_MS
        val isGpsActive = totalGpsAge < GPS_UI_FAIL_THRESHOLD_MS && loc.gpsTs > 0

        val gnss = loc.gnssDetail
        var avgCn0 = 0.0
        gnss?.satellites?.let { sats ->
            if (sats.isNotEmpty()) avgCn0 = sats.map { it.cn0 }.average()
        }

        return DashboardTelemetryState(
            lat = if (isGpsActive) loc.lat else 0.0,
            lng = if (isGpsActive) loc.lng else 0.0,
            gpsSpeedMps = loc.speed,
            trackerAccuracy = loc.accuracy,
            trackerMaxAcc = if (loc.maxAccuracy > 0) loc.maxAccuracy else loc.accuracy,
            viewerAccuracy = if (appMode == "tracker") 0.0 else kinematicState.localLocation.accuracy,
            viewerMaxAcc = if (appMode == "tracker") 0.0 else (if(kinematicState.localLocation.maxAccuracy > 0) kinematicState.localLocation.maxAccuracy else kinematicState.localLocation.accuracy),
            satsUsed = 0,
            satsView = 0,
            snr = avgCn0,
            distToHome = kinematicState.distanceTrackerToHome,
            distToViewer = kinematicState.distanceTrackerToViewer,
            isGpsFresh = isGpsActive,
            isTelemetryFresh = isTelemetryFresh,
            isLocationPending = if (isViewer) kinematicState.trackerHealth.isLocationPending else kinematicState.localHealth.isLocationPending,
            locationPendingReason = if (isViewer) kinematicState.trackerHealth.locationPendingReason else kinematicState.localHealth.locationPendingReason,
            trackerState = trackerState,
            status = loc.status,
            isUltraLongStationary = isUltra
        )
    }

    override fun buildDashboardHealthState(
        appMode: String?,
        kinematicState: KinematicState,
        diagnosticState: DiagnosticState,
        localMaxTemp: Double,
        trackerMaxTemp: Double
    ): DashboardHealthState {
        val isViewer = appMode == "viewer"
        val health = if (isViewer) kinematicState.trackerHealth else kinematicState.localHealth

        return DashboardHealthState(
            batteryLevel = if (isViewer) diagnosticState.trackerBattery.level else diagnosticState.battery.level,
            trackerTemp = diagnosticState.trackerBattery.temp,
            trackerMaxTemp = trackerMaxTemp,
            viewerTemp = diagnosticState.battery.temp,
            viewerMaxTemp = localMaxTemp,
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
            peakShock = health.peakVibrationShock,
            luxBaseline = health.luxBaseline,
            acousticFloorDb = health.acousticFloorDb,
            vibrationFloor = health.adaptiveVibrationFloor,
            isMicPending = health.micPending,
            isPowerTamper = health.isPowerTamper,
            violationUptimeMs = health.violationUptimeMs,
            violationPercentage = health.violationPercentage,
            isPowerSaveMode = health.isPowerSaveMode,
            standbyBucket = health.standbyBucket,
            isStorageLow = health.isStorageLow,
            isStorageCritical = health.isStorageCritical,
            isBatterySteepDischarge = health.isBatterySteepDischarge,
            isCoolingModeActive = health.isCoolingModeActive,
            trackerCurrentMa = health.currentMa,
            isBatteryLow = health.isBatteryLow,
            isBatteryCritical = health.isBatteryCritical,
            cpuLoad = health.cpuLoad,
            ioWait = health.ioWait,
            maxIoLatency = health.maxIoLatency,
            isSilentFailure = health.isSilentFailure
        )
    }

    override fun buildHudConnectivityState(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        diagnosticState: DiagnosticState,
        rtt: Int,
        remoteSignal: Int
    ): HudConnectivityState {
        val isTelemetryFresh = if (appMode == "viewer") {
            (System.currentTimeMillis() - diagnosticState.connectivity.lastUpdateTs) < TELEMETRY_UI_STALE_THRESHOLD_MS
        } else true

        val commIndex = if (isSystemActive && diagnosticState.connectivity.isRelayConnected) {
            TelemetryUtils.calculateCommIndex(rtt, 10, 10)
        } else 0

        val remoteCommIndex = if (appMode == "viewer" && isTelemetryFresh) {
            TelemetryUtils.calculateCommIndex(rtt, remoteSignal, 10)
        } else 0

        return HudConnectivityState(
            appMode = appMode,
            isInternet = diagnosticState.connectivity.isLocalOnline,
            isRelayConnected = diagnosticState.connectivity.isRelayConnected,
            isTelemetryFresh = isTelemetryFresh,
            isDataHealthy = isTelemetryFresh && diagnosticState.connectivity.isLocalOnline && diagnosticState.connectivity.isRelayConnected,
            commIndex = commIndex,
            remoteCommIndex = remoteCommIndex,
            trackerId = deviceId,
            viewerId = viewerId,
            watchdogOk = if (appMode == "viewer") (isTelemetryFresh || (System.currentTimeMillis() - diagnosticState.connectivity.lastRemoteActivityTs < WATCH_DOG_UI_GRACE_MS)) else true,
            rtt = rtt,
            remoteSignal = remoteSignal,
            isSystemActive = isSystemActive
        )
    }

    override fun buildHudTelemetryState(
        appMode: String?,
        kinematicState: KinematicState,
        systemPulse: Long,
        trackerState: TrackerState,
        isUltra: Boolean
    ): HudTelemetryState {
        val loc = if (appMode == "viewer") kinematicState.trackerLocation else kinematicState.localLocation
        
        val telemetryAge = if (loc.ts > 0) systemPulse - loc.ts else Long.MAX_VALUE
        val sourceGpsAge = if (loc.ts > 0 && loc.gpsTs > 0) maxOf(0L, loc.ts - loc.gpsTs) else 0L
        val totalGpsAge = telemetryAge + sourceGpsAge
        val isGpsFresh = totalGpsAge < GPS_UI_FAIL_THRESHOLD_MS && loc.gpsTs > 0

        return HudTelemetryState(
            isLocalGpsActive = if (appMode == "tracker") isGpsFresh else (systemPulse - kinematicState.localLocation.gpsTs < GPS_UI_FAIL_THRESHOLD_MS),
            isGpsFresh = isGpsFresh,
            speedMps = (if (appMode == "viewer") kinematicState.trackerLocation.speed else 0.0).toFloat(),
            trackerAccuracy = kinematicState.trackerLocation.accuracy.toFloat(),
            maxTrackerAccuracy = kinematicState.trackerLocation.maxAccuracy.toFloat(),
            viewerAccuracy = (if (kinematicState.localLocation.lat != 0.0) kinematicState.localLocation.accuracy.toFloat() else 0f),
            maxViewerAccuracy = kinematicState.localLocation.maxAccuracy.toFloat(),
            satsUsed = 0,
            distToHome = kinematicState.distanceTrackerToHome,
            distToViewer = kinematicState.distanceTrackerToViewer,
            lastGpsTs = loc.gpsTs,
            viewerGpsTs = kinematicState.localLocation.gpsTs,
            trackerState = trackerState,
            isTrackerLocPending = kinematicState.trackerHealth.isLocationPending,
            trackerLocPendingReason = kinematicState.trackerHealth.locationPendingReason,
            isViewerLocPending = kinematicState.localHealth.isLocationPending,
            viewerLocPendingReason = kinematicState.localHealth.locationPendingReason,
            isUltraLongStationary = isUltra
        )
    }

    override fun buildHudHealthState(
        diagnosticState: DiagnosticState,
        systemPulse: Long
    ): HudHealthState {
        val rawPulse = diagnosticState.connectivity.lastRemoteActivityTs
        val age = if (rawPulse > 0) systemPulse - rawPulse else Long.MAX_VALUE
        val progressValue = if (rawPulse > 0) {
            maxOf(0f, minOf(1f, (TELEMETRY_UI_STALE_THRESHOLD_MS - age).toFloat() / TELEMETRY_UI_STALE_THRESHOLD_MS))
        } else 0f

        return HudHealthState(
            battery = diagnosticState.battery.level,
            remoteBattery = diagnosticState.trackerBattery.level,
            isCharging = diagnosticState.battery.isChargingStable,
            remoteCharging = diagnosticState.trackerBattery.isChargingStable,
            trackerTemp = diagnosticState.trackerBattery.temp.toFloat(),
            viewerTemp = diagnosticState.battery.temp.toFloat(),
            hasActiveAlarms = diagnosticState.activeAlarms.any { !it.isResolved },
            isRedScreenSuppressed = (diagnosticState.activeAlarms.any { !it.isResolved } && !diagnosticState.isRedScreenVisible),
            isSirenPlaying = diagnosticState.isSirenPlaying,
            activeAlarms = diagnosticState.activeAlarms,
            progressPulse = progressValue,
            systemPulse = systemPulse
        )
    }
}
