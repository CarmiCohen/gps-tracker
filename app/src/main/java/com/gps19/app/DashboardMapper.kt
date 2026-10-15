package com.gps19.app

import com.gps19.core.engine.*

/**
 * DashboardMapper: Centralized authority for dashboard state transformations.
 * Oct.15.12:
 * - ISSUE-OCT15.11-F1: Vibration Floor Jitter Hardening. Updated engine version string to Oct15.12.
 * Oct.15.11:
 * - ISSUE-OCT15.10-F2: Proximity Debounce Hardening. Updated engine version string to Oct15.11.
 * Oct.14.21: Extracted from MainViewModel to reduce complexity and improve testability.
 */
object DashboardMapper {

    fun mapDashboardConnectivity(
        appMode: String?, 
        diag: DiagnosticState, 
        nowRt: Long
    ): DashboardConnectivityState {
        val isViewer = appMode == "viewer"
        val activeStats = if (isViewer) diag.trackerStats else diag.stats
        val lastSeenTs = diag.connectivity.lastRemoteActivityTs
        val isLocalServiceAlive = (nowRt - diag.pulse) < TELEMETRY_UI_STALE_THRESHOLD_MS
        val watchdogSec = if (isViewer && lastSeenTs > 0) {
            maxOf(0L, (WATCH_TIMEOUT_MS - (nowRt - lastSeenTs)) / 1000)
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
            engineVersion = "Oct15.12",
            netInterface = diag.connectivity.netInterface,
            systemPulse = nowRt,
            isLocalOnline = diag.connectivity.isLocalOnline,
            isRelayConnected = diag.connectivity.isRelayConnected
        )
    }

    fun mapDashboardTelemetry(
        appMode: String?, 
        kinematicState: KinematicState, 
        nowRt: Long, 
        trackerState: TrackerState, 
        isUltra: Boolean
    ): DashboardTelemetryState {
        val isViewer = appMode == "viewer"
        val loc = if (isViewer) kinematicState.trackerLocation else kinematicState.localLocation
        val fixAgeRt = if (loc.kinetic.rt > 0) nowRt - loc.kinetic.rt else Long.MAX_VALUE
        
        val isPowerSave = if (isViewer) kinematicState.trackerHealth.isPowerSaveMode else kinematicState.localHealth.isPowerSaveMode
        val threshold = when {
            isUltra -> ULTRA_LONG_STATIONARY_GPS_POLLING_MS + 30000L
            trackerState == TrackerState.PARKING -> STATIONARY_GPS_POLLING_MS + 15000L
            isPowerSave -> SCREEN_OFF_GPS_POLLING_MS + 25000L
            else -> GPS_UI_FAIL_THRESHOLD_MS
        }
        val isGpsActive = fixAgeRt < threshold && loc.kinetic.gpsTs > 0
        
        val telemetryAge = if (kinematicState.pulse > 0) nowRt - kinematicState.pulse else Long.MAX_VALUE
        val isTelemetryFresh = telemetryAge < TELEMETRY_UI_STALE_THRESHOLD_MS
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
            satsUsed = loc.integrity.satsUsed,
            satsView = loc.integrity.satsView,
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
            systemPulse = nowRt,
            gpsTs = loc.kinetic.gpsTs
        )
    }

    fun mapDashboardHealth(
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
            systemPulse = nowRt,
            thermalHeadroom = health.thermalHeadroom,
            heapAllocatedMb = health.heapAllocatedMb,
            isSuspiciousNoise = health.isSuspiciousNoise,
            isMemoryPressureThrottled = health.isMemoryPressureThrottled
        )
    }
}
