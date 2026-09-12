package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UiStateMapper: Unified stateless mapper for transforming raw domain states into UI-ready models.
 * Consolidates logic previously split between UiStateAggregator and DashboardStateProvider (Idea #13).
 * Sep.12.00:
 * - Centralized HUD and Dashboard state construction (R-ID 286).
 */
interface UiStateMapper {
    fun mapDashboardConnectivity(
        appMode: String?,
        diag: DiagnosticState,
        nowRt: Long
    ): DashboardConnectivityState

    fun mapDashboardTelemetry(
        appMode: String?,
        kinematicState: KinematicState,
        nowRt: Long,
        trackerState: TrackerState,
        isUltra: Boolean
    ): DashboardTelemetryState

    fun mapDashboardHealth(
        appMode: String?,
        kinematicState: KinematicState,
        diag: DiagnosticState,
        localMaxTemp: Double,
        trackerMaxTemp: Double,
        nowRt: Long
    ): DashboardHealthState

    fun mapHudConnectivity(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        isSafeMode: Boolean,
        isA15: Boolean,
        diag: DiagnosticState,
        rtt: Int,
        remoteSignal: Int,
        nowRt: Long
    ): HudConnectivityState

    fun mapHudTelemetry(
        appMode: String?,
        kinematicState: KinematicState,
        nowRt: Long,
        trackerState: TrackerState,
        isUltra: Boolean
    ): HudTelemetryState

    fun mapHudHealth(
        diag: DiagnosticState,
        nowRt: Long,
        isMaliAnomaly: Boolean
    ): HudHealthState
}

@Singleton
class UiStateMapperImpl @Inject constructor() : UiStateMapper {

    override fun mapDashboardConnectivity(
        appMode: String?,
        diag: DiagnosticState,
        nowRt: Long
    ): DashboardConnectivityState {
        val isViewer = appMode == "viewer"
        val activeStats = if (isViewer) diag.trackerStats else diag.stats
        val lastSeenTs = diag.connectivity.lastRemoteActivityTs

        val isLocalServiceAlive = (nowRt - diag.pulse) < TELEMETRY_UI_STALE_THRESHOLD_MS
        
        val watchdogSec = if (isViewer && lastSeenTs > 0) {
            val remaining = (WATCH_TIMEOUT_MS - (nowRt - lastSeenTs)) / 1000
            maxOf(0L, remaining)
        } else 0L

        return DashboardConnectivityState(
            lastSeenTs = lastSeenTs,
            watchdogOk = isLocalServiceAlive,
            watchdogCountdownSec = if (isViewer) watchdogSec else 0L,
            totalUptimeMs = activeStats.uptimeMs,
            sessionMs = if (activeStats.lastConnTs > 0) activeStats.sessionConnectedMs else 0L,
            sinceConnMs = if (activeStats.lastConnTs > 0) (nowRt - activeStats.lastConnTs) else 0L,
            sinceDiscoMs = if (activeStats.lastDiscTs > 0) (nowRt - activeStats.lastDiscTs) else 0L,
            totalDropMs = activeStats.totalDropMs,
            maxDropMs = activeStats.maxDropMs,
            engineVersion = BuildConfig.VERSION_NAME,
            netInterface = diag.connectivity.netInterface,
            systemPulse = nowRt
        )
    }

    override fun mapDashboardTelemetry(
        appMode: String?,
        kinematicState: KinematicState,
        nowRt: Long,
        trackerState: TrackerState,
        isUltra: Boolean
    ): DashboardTelemetryState {
        val isViewer = appMode == "viewer"
        val loc = if (isViewer) kinematicState.trackerLocation else kinematicState.localLocation
        
        val telemetryAge = if (kinematicState.pulse > 0) nowRt - kinematicState.pulse else Long.MAX_VALUE
        val isTelemetryFresh = telemetryAge < TELEMETRY_UI_STALE_THRESHOLD_MS
        
        val isGpsActive = (nowRt - loc.kinetic.rt) < GPS_UI_FAIL_THRESHOLD_MS && loc.kinetic.gpsTs > 0

        val gnss = loc.integrity.gnssDetail
        val avgCn0 = gnss?.satellites?.map { it.cn0 }?.safeAverage() ?: 0.0

        return DashboardTelemetryState(
            lat = if (isGpsActive) loc.kinetic.lat else 0.0,
            lng = if (isGpsActive) loc.kinetic.lng else 0.0,
            gpsSpeedMps = loc.kinetic.speed,
            trackerAccuracy = loc.kinetic.accuracy,
            trackerMaxAcc = if (loc.kinetic.maxAccuracy > 0) loc.kinetic.maxAccuracy else loc.kinetic.accuracy,
            viewerAccuracy = if (appMode == "tracker") 0.0 else kinematicState.localLocation.kinetic.accuracy,
            viewerMaxAcc = if (appMode == "tracker") 0.0 else (if(kinematicState.localLocation.kinetic.maxAccuracy > 0) kinematicState.localLocation.kinetic.maxAccuracy else kinematicState.localLocation.kinetic.accuracy),
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
            tamperReason = if (isViewer) kinematicState.trackerHealth.tamperNote else kinematicState.localHealth.tamperNote,
            isUltraLongStationary = isUltra,
            systemPulse = nowRt
        )
    }

    override fun mapDashboardHealth(
        appMode: String?,
        kinematicState: KinematicState,
        diag: DiagnosticState,
        localMaxTemp: Double,
        trackerMaxTemp: Double,
        nowRt: Long
    ): DashboardHealthState {
        val isViewer = appMode == "viewer"
        val health = if (isViewer) kinematicState.trackerHealth else kinematicState.localHealth

        return DashboardHealthState(
            batteryLevel = if (isViewer) diag.trackerBattery.level else diag.battery.level,
            trackerTemp = diag.trackerBattery.temp,
            trackerMaxTemp = trackerMaxTemp,
            viewerTemp = diag.battery.temp,
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
            netInterface = health.netInterface,
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
            isSilentFailure = health.isSilentFailure,
            isMaliAnomaly = health.isMaliAnomaly,
            isGnssThrottled = health.isGnssThrottled,
            lastEnergyDeltaMa = health.lastEnergyDeltaMa,
            lastEnergyDeltaTemp = health.lastEnergyDeltaTemp,
            lastEnergyDurationMs = health.lastEnergyDurationMs,
            systemPulse = nowRt
        )
    }

    override fun mapHudConnectivity(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        isSafeMode: Boolean,
        isA15: Boolean,
        diag: DiagnosticState,
        rtt: Int,
        remoteSignal: Int,
        nowRt: Long
    ): HudConnectivityState {
        val lastSeenTs = diag.connectivity.lastRemoteActivityTs
        
        val isTelemetryFresh = if (lastSeenTs > 0) {
            (nowRt - lastSeenTs) < TELEMETRY_UI_STALE_THRESHOLD_MS
        } else false

        val commIndex = if (isSystemActive && diag.connectivity.isRelayConnected) {
            TelemetryUtils.calculateCommIndex(rtt, 10, 10)
        } else 0

        val remoteCommIndex = if (appMode == "viewer" && isTelemetryFresh) {
            TelemetryUtils.calculateCommIndex(rtt, remoteSignal, 10)
        } else 0

        val isDataHealthy = (appMode == "viewer") && 
                            isTelemetryFresh &&
                            diag.connectivity.isLocalOnline && 
                            diag.connectivity.isRelayConnected

        val isLocalServiceAlive = (nowRt - diag.pulse) < TELEMETRY_UI_STALE_THRESHOLD_MS

        val throttled = if (appMode == "viewer") diag.trackerIsGnssThrottled else diag.isGnssThrottled

        return HudConnectivityState(
            appMode = appMode,
            isInternet = diag.connectivity.isLocalOnline,
            isRelayConnected = diag.connectivity.isRelayConnected,
            isTelemetryFresh = isTelemetryFresh,
            isDataHealthy = isDataHealthy,
            commIndex = commIndex,
            remoteCommIndex = remoteCommIndex,
            trackerId = deviceId,
            viewerId = viewerId,
            watchdogOk = isLocalServiceAlive,
            rtt = rtt,
            remoteSignal = remoteSignal,
            isSystemActive = isSystemActive,
            isSafeMode = isSafeMode,
            isA15 = isA15,
            isGnssThrottled = throttled,
            systemPulse = nowRt
        )
    }

    override fun mapHudTelemetry(
        appMode: String?,
        kinematicState: KinematicState,
        nowRt: Long,
        trackerState: TrackerState,
        isUltra: Boolean
    ): HudTelemetryState {
        val loc = if (appMode == "viewer") kinematicState.trackerLocation else kinematicState.localLocation
        val isGpsFresh = (nowRt - loc.kinetic.rt) < GPS_UI_FAIL_THRESHOLD_MS && loc.kinetic.gpsTs > 0

        return HudTelemetryState(
            isLocalGpsActive = if (appMode == "tracker") isGpsFresh else (nowRt - kinematicState.localLocation.kinetic.rt < GPS_UI_FAIL_THRESHOLD_MS),
            isGpsFresh = isGpsFresh,
            speedMps = (if (appMode == "viewer") kinematicState.trackerLocation.kinetic.speed else 0.0).toFloat(),
            trackerAccuracy = kinematicState.trackerLocation.kinetic.accuracy.toFloat(),
            maxTrackerAccuracy = kinematicState.trackerLocation.kinetic.maxAccuracy.toFloat(),
            viewerAccuracy = (if (kinematicState.localLocation.kinetic.lat != 0.0) kinematicState.localLocation.kinetic.accuracy.toFloat() else 0f),
            maxViewerAccuracy = kinematicState.localLocation.kinetic.maxAccuracy.toFloat(),
            satsUsed = 0,
            distToHome = kinematicState.distanceTrackerToHome,
            distToViewer = kinematicState.distanceTrackerToViewer,
            lastGpsTs = loc.kinetic.gpsTs,
            viewerGpsTs = kinematicState.localLocation.kinetic.gpsTs,
            trackerState = trackerState,
            isTrackerLocPending = kinematicState.trackerHealth.isLocationPending,
            trackerLocPendingReason = kinematicState.trackerHealth.locationPendingReason,
            isViewerLocPending = kinematicState.localHealth.isLocationPending,
            viewerLocPendingReason = kinematicState.localHealth.locationPendingReason,
            isUltraLongStationary = isUltra,
            systemPulse = nowRt
        )
    }

    override fun mapHudHealth(
        diag: DiagnosticState,
        nowRt: Long,
        isMaliAnomaly: Boolean
    ): HudHealthState {
        val rawPulse = diag.connectivity.lastRemoteActivityTs
        val age = if (rawPulse > 0) nowRt - rawPulse else Long.MAX_VALUE
        val progressValue = if (rawPulse > 0) {
            maxOf(0f, minOf(1f, (TELEMETRY_UI_STALE_THRESHOLD_MS - age).toFloat() / TELEMETRY_UI_STALE_THRESHOLD_MS))
        } else 0f

        return HudHealthState(
            battery = diag.battery.level,
            remoteBattery = diag.trackerBattery.level,
            isCharging = diag.battery.isChargingStable,
            remoteCharging = diag.trackerBattery.isChargingStable,
            trackerTemp = diag.trackerBattery.temp.toFloat(),
            viewerTemp = diag.battery.temp.toFloat(),
            hasActiveAlarms = diag.activeAlarms.any { !it.isResolved },
            isRedScreenSuppressed = (diag.activeAlarms.any { !it.isResolved } && !diag.isRedScreenVisible),
            isSirenPlaying = diag.isSirenPlaying,
            activeAlarms = diag.activeAlarms,
            progressPulse = progressValue,
            systemPulse = nowRt,
            isMaliAnomaly = isMaliAnomaly
        )
    }
}
