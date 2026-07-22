package com.gps19.app

import com.gps19.core.engine.*
import java.util.Locale
import javax.inject.Inject

/**
 * DashboardUseCase: Logic for computing the analytical dashboard state.
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor.
 * July.16.18:
 * - Issue #516: De-duplicate "Status" Logic. Use localHealth and trackerHealth.
 */
class DashboardUseCase @Inject constructor() {

    fun computeDashboardState(
        state: MainUiState,
        now: Long,
        trackerState: TrackerState,
        localMaxTemp: Double,
        trackerMaxTemp: Double
    ): DashboardState {
        val s = state.stats
        val ts = state.trackerStats
        val mode = state.appMode
        val isTrackerMode = mode == "tracker"
        val isViewer = mode == "viewer"

        val activeStats = if (isViewer) ts else s
        
        val totalUptime = formatDuration(activeStats.uptimeMs)
        val session = if (activeStats.lastConnTs > 0) formatDuration(activeStats.sessionConnectedMs) else "00:00:00"
        val sinceConn = if (activeStats.lastConnTs > 0) formatDuration(now - activeStats.lastConnTs) else "--"
        val sinceDisco = if (activeStats.lastDiscTs > 0) formatDuration(now - activeStats.lastDiscTs) else "--"
        
        val totalDrop = formatDuration(activeStats.totalDropMs)
        val maxDrop = formatDuration(activeStats.maxDropMs)
        
        val lastSeen = if (state.connectivity.lastRemoteActivityTs > 0) {
            val delta = (now - state.connectivity.lastRemoteActivityTs) / 1000
            if (delta < 60) "${delta}s" else "${delta / 60}m"
        } else "--"

        val watchdogSec = if (state.connectivity.lastRemoteActivityTs > 0) {
            val remaining = (WATCH_TIMEOUT_MS - (now - state.connectivity.lastRemoteActivityTs)) / 1000
            maxOf(0L, remaining)
        } else 0L

        val loc = if (isViewer) state.trackerLocation else state.localLocation
        val health = if (isViewer) state.trackerHealth else state.localHealth
        
        // Receipt-based freshness calculation
        val telemetryAge = if (loc.telemetryTs > 0) now - loc.telemetryTs else Long.MAX_VALUE
        val sourceGpsAge = if (loc.telemetryTs > 0 && loc.timestamp > 0) maxOf(0L, loc.telemetryTs - loc.timestamp) else 0L
        val totalGpsAge = telemetryAge + sourceGpsAge

        val isTelemetryFresh = telemetryAge < TELEMETRY_UI_STALE_THRESHOLD_MS
        val isGpsActive = totalGpsAge < GPS_UI_FAIL_THRESHOLD_MS && loc.timestamp > 0
        val isTelemetryVisible = telemetryAge < SENSOR_GRACE_PERIOD_MS
        val isForensicFresh = telemetryAge < WATCH_DOG_UI_GRACE_MS

        val bucket = health.standbyBucket
        val snrValue = if (loc.gnssDetail != null && isTelemetryVisible) {
            val avgCn0 = loc.gnssDetail?.satellites?.map { it.cn0 }?.average() ?: 0.0
            "${avgCn0.toInt()}dB"
        } else "--"

        fun gpsVal(value: String): String = if (isGpsActive) value else "--"
        fun sensorVal(value: String): String = if (isTelemetryVisible) value else "--"
        fun forensicVal(value: String): String = if (isForensicFresh) value else "--"

        val rawAcc = loc.accuracy
        val filteredAcc = if (loc.maxAccuracy > 0) loc.maxAccuracy else rawAcc
        
        return DashboardState(
            trackerState = trackerState,
            status = loc.status,
            isTamperDetected = health.isTamperDetected,
            maxDrop = maxDrop,
            lastSeen = lastSeen,
            totalDrop = totalDrop,
            watchdogCountdown = if (isTrackerMode) "--" else "${watchdogSec}s",
            watchdogOk = if (isTrackerMode) true else (watchdogSec > 0),
            totalUptime = totalUptime,
            session = session,
            engineVersion = BuildConfig.VERSION_NAME,
            sinceConn = sinceConn,
            sinceDisco = sinceDisco,
            violationUptime = formatDuration(health.violationUptimeMs),
            violationPercentage = "%.1f%%".format(Locale.getDefault(), health.violationPercentage * 100.0),
            lat = gpsVal("%.6f".format(Locale.getDefault(), loc.lat)),
            lng = gpsVal("%.6f".format(Locale.getDefault(), loc.lng)),
            trackerAccuracy = gpsVal("±%.1fm".format(Locale.getDefault(), rawAcc)),
            trackerMaxAcc = gpsVal("±%.1fm".format(Locale.getDefault(), filteredAcc)),
            satsIndex = gpsVal("${state.trackerSatsUsed}/${state.trackerSatsView}"),
            isSatsIndexWarning = (state.trackerSatsUsed < 4 && state.trackerSatsView > 0),
            viewerAccuracy = if (isTrackerMode) "--" else "±%.1fm".format(Locale.getDefault(), state.localLocation.accuracy),
            viewerMaxAcc = if (isTrackerMode) "--" else "±%.1fm".format(Locale.getDefault(), if(state.localLocation.maxAccuracy > 0) state.localLocation.maxAccuracy else state.localLocation.accuracy),
            vibration = sensorVal("%.2fG".format(Locale.getDefault(), health.vibration)),
            heading = sensorVal("%.0f°".format(Locale.getDefault(), health.heading)),
            tilt = sensorVal("%.1f°".format(Locale.getDefault(), health.tiltDegrees)),
            acoustic = sensorVal("%.0fdB".format(Locale.getDefault(), health.acousticDb)),
            lift = sensorVal("%.1fm".format(Locale.getDefault(), health.baroAlt)),
            lux = sensorVal("%.0flx".format(Locale.getDefault(), health.lux)),
            proximity = sensorVal(if (health.isNear) "NEAR" else "FAR"),
            proximityCm = sensorVal(if (health.proximityCm >= 0) "${health.proximityCm.toInt()}cm" else "--"),
            proximityDebounce = forensicVal("${health.proximityDebounceMs}ms"),
            rollingVibration = forensicVal("%.3fG".format(Locale.getDefault(), health.vibrationRollingSum)),
            gpsSpeed = gpsVal("%.1fkm/h".format(Locale.getDefault(), loc.speed * 3.6)),
            trackerMaxTemp = sensorVal("%.1f°C".format(Locale.getDefault(), trackerMaxTemp)),
            viewerMaxTemp = sensorVal("%.1f°C".format(Locale.getDefault(), localMaxTemp)),
            peakShock = sensorVal("%.2fG".format(Locale.getDefault(), health.peakVibrationShock)),
            vibrationFloor = sensorVal("%.2fG".format(Locale.getDefault(), health.adaptiveVibrationFloor)),
            luxBaseline = sensorVal("%.0flx".format(Locale.getDefault(), health.luxBaseline)),
            acousticFloor = sensorVal("%.0fdB".format(Locale.getDefault(), health.acousticFloorDb)),
            isMicPending = health.micPending,
            isPowerTamper = health.isPowerTamper,
            isPowerSaveMode = health.isPowerSaveMode,
            standbyBucket = bucket,
            netInterface = health.netInterface,
            isStorageLow = health.isStorageLow,
            isStorageCritical = health.isStorageCritical,
            snr = snrValue,
            distToHome = formatDist(state.distanceTrackerToHome),
            distToViewer = formatDist(state.distanceTrackerToViewer),
            isGpsFresh = isGpsActive,
            isLinkFresh = (telemetryAge < WATCH_DOG_UI_GRACE_MS),
            isTelemetryFresh = isTelemetryFresh,
            isGpsVisible = isTelemetryVisible,
            isLinkVisible = isTelemetryVisible,
            isBatterySteepDischarge = health.isBatterySteepDischarge,
            isCoolingModeActive = health.isCoolingModeActive,
            currentMa = sensorVal("${health.currentMa}mA"),
            isLocationPending = health.isLocationPending,
            locationPendingReason = health.locationPendingReason
        )
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private fun formatDist(d: Double?): String {
        if (d == null || d.isNaN() || d == 0.0) return "--"
        return when {
            d >= 9000 -> String.format(Locale.getDefault(), "%.0fkm", d / 1000.0)
            d >= 1000 -> String.format(Locale.getDefault(), "%.1fkm", d / 1000.0)
            else -> "${d.toInt()}m"
        }
    }
}
