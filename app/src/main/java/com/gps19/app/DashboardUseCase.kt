package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs

/**
 * DashboardUseCase: Logic for computing the complex dashboard display state.
 * v8.8.2: 
 * - R923 Fix: Dashboard now recovers immediately upon telemetry receipt, even without GPS fix,
 *   by utilizing both position timestamp and telemetry arrival timestamp.
 * v8.8.34: Forensic Simplification - Removed 'ver' from display logic.
 * v8.8.35: Updated to latest baseline following database schema cleanup (Issue 159).
 * v8.8.36: Issue 165 - Migrated to FormatterUtils.
 * v8.9.2: Cleaned up redundant 'v' prefix in version display.
 */
@Singleton
class DashboardUseCase @Inject constructor() {

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun computeDashboardState(
        uiState: MainUiState, 
        now: Long, 
        trackerState: TrackerState, 
        localMaxTemp: Float, 
        trackerMaxTemp: Float
    ): DashboardState {
        val s = uiState.stats
        val ts = uiState.trackerStats
        val mode = uiState.appMode
        val isTrackerMode = mode == "tracker"
        val isViewer = mode == "viewer"

        val activeStats = if (isViewer) ts else s
        
        val totalUptime = FormatterUtils.formatDurationUnified(activeStats.uptimeMs)
        val session = if (activeStats.lastConnTs > 0) FormatterUtils.formatDurationUnified(activeStats.sessionConnectedMs) else "00:00:00"
        val sinceConn = if (activeStats.lastConnTs > 0) FormatterUtils.formatDurationUnified(now - activeStats.lastConnTs) else "--"
        val sinceDisco = if (activeStats.lastDiscTs > 0) FormatterUtils.formatDurationUnified(now - activeStats.lastDiscTs) else "--"
        
        val totalDrop = FormatterUtils.formatDurationUnified(activeStats.totalDropMs)
        val maxDrop = FormatterUtils.formatDurationUnified(activeStats.maxDropMs)
        
        val lastSeen = if (uiState.connectivity.lastRemoteActivityTs > 0) {
            val delta = (now - uiState.connectivity.lastRemoteActivityTs) / 1000
            if (delta < 60) "${delta}s" else "${delta / 60}m"
        } else "--"

        val watchdogSec = if (uiState.connectivity.lastRemoteActivityTs > 0) {
            val remaining = (WATCH_TIMEOUT_MS - (now - uiState.connectivity.lastRemoteActivityTs)) / 1000
            maxOf(0L, remaining)
        } else 0L

        val loc = if (isViewer) uiState.trackerLocation else uiState.localLocation
        
        // R923 Fix: Use maximum of GPS timestamp and Telemetry arrival timestamp for visibility check.
        val effectiveLastActivityTs = maxOf(loc.timestamp, loc.telemetryTs)
        val telemetryAge = if (effectiveLastActivityTs > 0) now - effectiveLastActivityTs else Long.MAX_VALUE
        val isTelemetryVisible = telemetryAge < SENSOR_GRACE_PERIOD_MS
        
        // SoT: GPS_UI_FAIL_THRESHOLD_MS (7s) for position fresh check. R923: Recover immediately on telemetry.
        val effectiveGpsAge = if (effectiveLastActivityTs > 0) now - effectiveLastActivityTs else Long.MAX_VALUE
        val isGpsActive = effectiveGpsAge < GPS_UI_FAIL_THRESHOLD_MS && loc.timestamp > 0

        val chairTime = if (loc.lastSitTs > 0) {
            synchronized(timeFormatter) { timeFormatter.format(Date(loc.lastSitTs)) }
        } else "--"

        val forensics = if (loc.lastSitTs > 0) {
            "v${"%.1f".format(Locale.getDefault(), loc.sitVz)} d${"%.1f".format(Locale.getDefault(), loc.sitDz)} b${"%.1f".format(Locale.getDefault(), loc.sitBaro)} t${"%.0f".format(Locale.getDefault(), loc.sitTilt)} s${"%.1f".format(Locale.getDefault(), loc.sitShock)}"
        } else "--"

        val isSitActive = loc.isSitDetected || (loc.lastSitTs > 0 && (now - loc.lastSitTs < SUSPICIOUS_STATE_COOLDOWN_MS))

        val isTrackerOffline = isViewer && (trackerState == TrackerState.UNKNOWN)
        val engineVer = if (isViewer) {
            if (isTrackerOffline) "${BuildConfig.VERSION_NAME} (Last)" else BuildConfig.VERSION_NAME
        } else {
            BuildConfig.VERSION_NAME
        }

        val bucket = if (isViewer) uiState.trackerLocation.standbyBucket else uiState.integrity.standbyBucket
        
        // SoT: SNR Index is 0.0-1.0 mapped to core engine scale (standard GNSS CN0 range)
        val snrValue = if (loc.snrIdx > 0f && isTelemetryVisible) "${(loc.snrIdx * RIBBON_SNR_SCALE_DB).toInt()}dB" else "--"

        // Helper to hide stale coordinates (7s threshold)
        fun gpsVal(value: String): String = if (isGpsActive) value else "--"
        
        // Helper to hide expired sensors (10m threshold)
        fun sensorVal(value: String): String = if (isTelemetryVisible) value else "--"

        // R880: Format distances for display. Persist values, UI will handle gray-out via isGpsFresh.
        fun formatDist(d: Double?): String {
            if (d == null || d.isNaN() || d == 0.0) return "--"
            return when {
                d >= 9000 -> String.format(Locale.getDefault(), "%.0fkm", d / 1000.0)
                d >= 1000 -> String.format(Locale.getDefault(), "%.1fkm", d / 1000.0)
                else -> "${d.toInt()}m"
            }
        }

        return DashboardState(
            trackerState = trackerState,
            isSuspicious = loc.isSuspicious,
            isTamperDetected = loc.isTamperDetected,
            isSitDetected = isSitActive,
            lastSitTs = loc.lastSitTs,
            maxDrop = maxDrop,
            lastSeen = lastSeen,
            totalDrop = totalDrop,
            watchdogCountdown = if (isTrackerMode) "--" else "${watchdogSec}s",
            watchdogOk = if (isTrackerMode) true else (watchdogSec > 0),
            totalUptime = totalUptime,
            session = session,
            engineVersion = engineVer,
            sinceConn = sinceConn,
            sinceDisco = sinceDisco,
            violationUptime = FormatterUtils.formatDurationUnified(loc.violationUptimeMs),
            violationPercentage = "%.1f%%".format(Locale.getDefault(), loc.violationPercentage * 100f),
            lat = gpsVal("%.6f".format(Locale.getDefault(), loc.lat)),
            lng = gpsVal("%.6f".format(Locale.getDefault(), loc.lng)),
            trackerAccuracy = gpsVal("±%.1fm".format(Locale.getDefault(), loc.accuracy)),
            satsIndex = gpsVal("${uiState.trackerSatsUsed}/${uiState.trackerSatsView}"),
            isSatsIndexWarning = (uiState.trackerSatsUsed < 4 && uiState.trackerSatsView > 0),
            trackerMaxAcc = gpsVal("±%.1fm".format(Locale.getDefault(), uiState.maxTrackerAccuracy)),
            viewerAccuracy = if (isTrackerMode) "--" else "±%.1fm".format(Locale.getDefault(), uiState.localLocation.accuracy),
            vibration = sensorVal("%.2fG".format(Locale.getDefault(), loc.vibration)),
            heading = sensorVal("%.0f°".format(Locale.getDefault(), loc.heading)),
            tilt = sensorVal("%.1f°".format(Locale.getDefault(), loc.tiltDegrees)),
            acoustic = sensorVal("%.0fdB".format(Locale.getDefault(), loc.acousticDb)),
            lift = sensorVal("%.1fm".format(Locale.getDefault(), loc.baroAlt)),
            lux = sensorVal("%.0flx".format(Locale.getDefault(), loc.lux)),
            proximity = sensorVal(if (loc.isNear) "NEAR" else "FAR"),
            proximityCm = sensorVal(if (loc.proximityCm >= 0) "${loc.proximityCm.toInt()}cm" else "--"),
            gpsSpeed = gpsVal("%.1fkm/h".format(Locale.getDefault(), loc.speed * 3.6f)),
            trackerMaxTemp = sensorVal("%.1f°C".format(Locale.getDefault(), trackerMaxTemp)),
            viewerMaxTemp = sensorVal("%.1f°C".format(Locale.getDefault(), localMaxTemp)),
            peakShock = sensorVal("%.2fG".format(Locale.getDefault(), loc.peakVibrationShock)),
            vibrationFloor = sensorVal("%.2fG".format(Locale.getDefault(), loc.adaptiveVibrationFloor)),
            luxBaseline = sensorVal("%.0flx".format(Locale.getDefault(), loc.luxBaseline)),
            acousticFloor = sensorVal("%.0fdB".format(Locale.getDefault(), loc.acousticFloorDb)),
            lastChairSit = sensorVal(chairTime),
            plungeSpeed = sensorVal("%.2fm/s".format(Locale.getDefault(), abs(loc.verticalVelocity))),
            chairForensics = sensorVal(forensics),
            isPowerSaveMode = if (isViewer) uiState.trackerLocation.isPowerSaveMode else uiState.integrity.isPowerSaveMode,
            standbyBucket = bucket,
            netInterface = if (isViewer) uiState.trackerLocation.netInterface else uiState.integrity.netInterface,
            isStorageLow = if (isViewer) uiState.trackerLocation.isStorageLow else uiState.integrity.isStorageLow,
            isStorageCritical = if (isViewer) uiState.trackerLocation.isStorageCritical else uiState.integrity.isStorageCritical,
            snr = snrValue,
            distToHome = formatDist(uiState.distanceTrackerToHome),
            distToViewer = formatDist(uiState.distanceTrackerToViewer),
            isGpsFresh = isGpsActive,
            isLinkFresh = (telemetryAge < WATCH_DOG_UI_GRACE_MS),
            isGpsVisible = isTelemetryVisible,
            isLinkVisible = isTelemetryVisible,
            isBatterySteepDischarge = loc.isBatterySteepDischarge,
            isCoolingModeActive = loc.isCoolingModeActive
        )
    }
}
