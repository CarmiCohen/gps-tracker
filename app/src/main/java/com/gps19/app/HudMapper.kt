package com.gps19.app

import com.gps19.core.engine.*

/**
 * HudMapper: Pure mapping logic for HUD state transitions.
 * Oct.26.6:
 * - SIMP-OCT26.6-S1: Implemented SRV latch logic. Added isConnectionAborted 
 *   to support orange latching state for 5s after rejection.
 * Oct.26.5:
 * - GAP-OCT26.5-1: Integrated signaling conflation metrics into HudConnectivityState.
 */
object HudMapper {

    fun mapHudState(
        session: SessionUiState,
        settings: SettingsUiState,
        diag: DiagnosticState,
        rtt: Int,
        remoteSignal: Int,
        pulseRt: Long,
        kin: KinematicState,
        trackerState: TrackerState,
        lastAbortedRt: Long = 0L
    ): HudState {
        val mode = session.appMode
        val isMali = kin.localHealth.isMaliAnomaly
        val isUltra = if (mode == "viewer") kin.trackerHealth.isUltraLongStationary else kin.localHealth.isUltraLongStationary
        return HudState(
            connectivity = mapHudConnectivity(
                mode, settings.deviceId, settings.viewerId, session.isSystemActive, settings.isSafeMode, 
                session.permissions.performanceTier == PerformanceTier.STAGGERED, diag, rtt, remoteSignal, pulseRt, lastAbortedRt
            ),
            telemetry = mapHudTelemetry(mode, kin, pulseRt, trackerState, isUltra),
            health = mapHudHealth(diag, pulseRt, isMali)
        )
    }

    internal fun mapHudConnectivity(
        appMode: String?, 
        deviceId: String, 
        viewerId: String, 
        isSystemActive: Boolean, 
        isSafeMode: Boolean, 
        isStaggered: Boolean, 
        diag: DiagnosticState, 
        rtt: Int, 
        remoteSignal: Int, 
        nowRt: Long,
        lastAbortedRt: Long = 0L
    ): HudConnectivityState {
        val lastSeenTs = diag.connectivity.lastRemoteActivityTs
        val isTelemetryFresh = if (lastSeenTs > 0) (nowRt - lastSeenTs) < TELEMETRY_UI_STALE_THRESHOLD_MS else false
        val commIndex = if (isSystemActive && diag.connectivity.isRelayConnected) TelemetryUtils.calculateCommIndex(rtt, 10, 10) else 0
        val remoteCommIndex = if (appMode == "viewer" && isTelemetryFresh) TelemetryUtils.calculateCommIndex(rtt, remoteSignal, 10) else 0
        
        val isDataHealthy = if (appMode == "viewer") {
            isTelemetryFresh && diag.connectivity.isLocalOnline && diag.connectivity.isRelayConnected
        } else {
            diag.connectivity.isLocalOnline && diag.connectivity.isRelayConnected
        }
        
        val isLocalServiceAlive = (nowRt - diag.pulse) < TELEMETRY_UI_STALE_THRESHOLD_MS
        val throttled = if (appMode == "viewer") diag.trackerIsGnssThrottled else diag.isGnssThrottled
        
        // SIMP-OCT26.6-S1: 5s latch for connection rejections
        val isAborted = lastAbortedRt > 0 && (nowRt - lastAbortedRt < 5000L)

        return HudConnectivityState(
            appMode = appMode, isInternet = diag.connectivity.isLocalOnline, isRelayConnected = diag.connectivity.isRelayConnected, 
            isTelemetryFresh = isTelemetryFresh, isDataHealthy = isDataHealthy, commIndex = commIndex, remoteCommIndex = remoteCommIndex, 
            trackerId = deviceId, viewerId = viewerId, watchdogOk = isLocalServiceAlive, rtt = rtt, remoteSignal = remoteSignal, 
            isSystemActive = isSystemActive, isSafeMode = isSafeMode, isStaggered = isStaggered, isGnssThrottled = throttled, 
            systemPulse = nowRt, conflatedCount = diag.signalingMetrics.conflated, isConnectionAborted = isAborted
        )
    }

    internal fun mapHudTelemetry(
        appMode: String?, 
        kinematicState: KinematicState, 
        nowRt: Long, 
        trackerState: TrackerState, 
        isUltra: Boolean
    ): HudTelemetryState {
        val loc = if (appMode == "viewer") kinematicState.trackerLocation else kinematicState.localLocation
        
        val isPowerSave = if (appMode == "viewer") kinematicState.trackerHealth.isPowerSaveMode else kinematicState.localHealth.isPowerSaveMode
        val threshold = when {
            isUltra -> ULTRA_LONG_STATIONARY_GPS_POLLING_MS + 30000L
            trackerState == TrackerState.PARKING -> STATIONARY_GPS_POLLING_MS + 15000L
            isPowerSave -> SCREEN_OFF_GPS_POLLING_MS + 25000L
            else -> GPS_UI_FAIL_THRESHOLD_MS
        }
        val isGpsFresh = (nowRt - loc.kinetic.rt) < threshold && loc.kinetic.gpsTs > 0
        
        val isLocalFresh = if (appMode == "tracker") isGpsFresh else (nowRt - kinematicState.localLocation.kinetic.rt < GPS_UI_FAIL_THRESHOLD_MS && kinematicState.localLocation.kinetic.gpsTs > 0)

        return HudTelemetryState(
            isLocalGpsActive = isLocalFresh, 
            isGpsFresh = isGpsFresh, speedMps = (if (appMode == "viewer") kinematicState.trackerLocation.kinetic.speed else 0.0).toFloat(), 
            trackerAccuracy = kinematicState.trackerLocation.kinetic.accuracy.toFloat(), maxTrackerAccuracy = kinematicState.trackerLocation.kinetic.maxAccuracy.toFloat(), 
            viewerAccuracy = (if (kinematicState.localLocation.kinetic.lat != 0.0) kinematicState.localLocation.kinetic.accuracy.toFloat() else 0f), 
            maxViewerAccuracy = kinematicState.localLocation.kinetic.maxAccuracy.toFloat(), 
            satsUsed = kinematicState.trackerLocation.integrity.satsUsed, satsView = kinematicState.trackerLocation.integrity.satsView, 
            viewerSatsUsed = kinematicState.localLocation.integrity.satsUsed, viewerSatsView = kinematicState.localLocation.integrity.satsView, 
            distToHome = kinematicState.distanceTrackerToHome, distToViewer = kinematicState.distanceTrackerToViewer, 
            lastGpsTs = if (loc.kinetic.gpsTs > 0) loc.kinetic.rt else 0L, viewerGpsTs = if (kinematicState.localLocation.kinetic.gpsTs > 0) kinematicState.localLocation.kinetic.rt else 0L, 
            trackerState = trackerState, isTrackerLocPending = kinematicState.trackerHealth.isLocationPending, 
            locationPendingReason = kinematicState.trackerHealth.locationPendingReason, 
            isViewerLocPending = kinematicState.localHealth.isLocationPending, 
            viewerLocPendingReason = kinematicState.localHealth.locationPendingReason, isUltraLongStationary = isUltra, systemPulse = nowRt
        )
    }

    internal fun mapHudHealth(
        diag: DiagnosticState, 
        nowRt: Long, 
        isMaliAnomaly: Boolean
    ): HudHealthState {
        val rawPulse = diag.connectivity.lastRemoteActivityTs
        val age = if (rawPulse > 0) nowRt - rawPulse else Long.MAX_VALUE
        val progressValue = if (rawPulse > 0) maxOf(0f, minOf(1f, (TELEMETRY_UI_STALE_THRESHOLD_MS - age).toFloat() / TELEMETRY_UI_STALE_THRESHOLD_MS)) else 0f
        return HudHealthState(
            battery = diag.battery.level, remoteBattery = diag.trackerBattery.level, 
            isCharging = diag.battery.isChargingStable, remoteCharging = diag.trackerBattery.isChargingStable, 
            trackerTemp = diag.trackerBattery.temp.toFloat(), viewerTemp = diag.battery.temp.toFloat(), 
            hasActiveAlarms = diag.activeAlarms.any { !it.isResolved }, 
            isRedScreenSuppressed = (diag.activeAlarms.any { !it.isResolved } && !diag.isRedScreenVisible), 
            isRedScreenVisible = diag.isRedScreenVisible, isAlarmSilenced = diag.isAlarmSilenced, 
            isSirenPlaying = diag.isSirenPlaying, activeAlarms = diag.activeAlarms, 
            progressPulse = progressValue, systemPulse = nowRt, isMaliAnomaly = isMaliAnomaly
        )
    }
}
