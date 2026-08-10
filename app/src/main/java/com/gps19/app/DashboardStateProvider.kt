package com.gps19.app

import com.gps19.core.engine.*
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DashboardStateProvider: Dedicated provider for UI-ready dashboard states.
 * Aug.10.24:
 * - Issue #130: Proto Health Parity. Integrated isBatteryLow and isBatteryCritical 
 *   into DashboardState mapping (R130).
 * Aug.01.10:
 * - Issue #668: Performance: Object Churn. Fixed smart-cast issue with mutable gnssDetail.
 * July.28.24:
 * - Issue #621: UseCase Internalization Audit.
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
        
        val totalUptime = formatDuration(activeStats.uptimeMs)
        val session = if (activeStats.lastConnTs > 0) formatDuration(activeStats.sessionConnectedMs) else "00:00:00"
        val sinceConn = if (activeStats.lastConnTs > 0) formatDuration(now - activeStats.lastConnTs) else "--"
        val sinceDisco = if (activeStats.lastDiscTs > 0) formatDuration(now - activeStats.lastDiscTs) else "--"
        
        val totalDrop = formatDuration(activeStats.totalDropMs)
        val maxDrop = formatDuration(activeStats.maxDropMs)
        
        val lastSeen = if (diagnosticState.connectivity.lastRemoteActivityTs > 0) {
            val delta = (now - diagnosticState.connectivity.lastRemoteActivityTs) / 1000
            if (delta < 60) "${delta}s" else "${delta / 60}m"
        } else "--"

        val watchdogSec = if (diagnosticState.connectivity.lastRemoteActivityTs > 0) {
            val remaining = (WATCH_TIMEOUT_MS - (now - diagnosticState.connectivity.lastRemoteActivityTs)) / 1000
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
        val isTelemetryVisible = telemetryAge < SENSOR_GRACE_PERIOD_MS
        val isForensicFresh = telemetryAge < WATCH_DOG_UI_GRACE_MS

        val gnss = loc.gnssDetail
        val snrValue = if (gnss != null && isTelemetryVisible) {
            val satellites = gnss.satellites
            if (satellites.isEmpty()) "--"
            else {
                var sum = 0.0
                for (sat in satellites) {
                    sum += sat.cn0
                }
                val avgCn0 = sum / satellites.size
                "${avgCn0.toInt()}dB"
            }
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
            satsIndex = gpsVal("${diagnosticState.trackerSatsUsed}/${diagnosticState.trackerSatsView}"),
            isSatsIndexWarning = (diagnosticState.trackerSatsUsed < 4 && diagnosticState.trackerSatsView > 0),
            viewerAccuracy = if (isTrackerMode) "--" else "±%.1fm".format(Locale.getDefault(), kinematicState.localLocation.accuracy),
            viewerMaxAcc = if (isTrackerMode) "--" else "±%.1fm".format(Locale.getDefault(), if(kinematicState.localLocation.maxAccuracy > 0) kinematicState.localLocation.maxAccuracy else kinematicState.localLocation.accuracy),
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
            kineticEnergy = forensicVal("%.3fG".format(Locale.getDefault(), health.kineticEnergy)),
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
            standbyBucket = health.standbyBucket,
            netInterface = health.netInterface,
            isStorageLow = health.isStorageLow,
            isStorageCritical = health.isStorageCritical,
            snr = snrValue,
            distToHome = formatDist(kinematicState.distanceTrackerToHome),
            distToViewer = formatDist(kinematicState.distanceTrackerToViewer),
            isGpsFresh = isGpsActive,
            isLinkFresh = (telemetryAge < WATCH_DOG_UI_GRACE_MS),
            isTelemetryFresh = isTelemetryFresh,
            isGpsVisible = isTelemetryVisible,
            isLinkVisible = isTelemetryVisible,
            isBatterySteepDischarge = health.isBatterySteepDischarge,
            isCoolingModeActive = health.isCoolingModeActive,
            trackerCurrentMa = sensorVal("${health.currentMa}mA"),
            isLocationPending = health.isLocationPending,
            locationPendingReason = health.locationPendingReason,
            isBatteryLow = health.isBatteryLow,
            isBatteryCritical = health.isBatteryCritical
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
