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
 * v9.3.12:
 * - Temporal Authority (#075): Finalized skew-immune receipt-based freshness logic.
 * v9.3.8:
 * - Clock Skew Hardening: Link health (isTelemetryFresh) and GPS health (isGpsFresh) 
 *   now use a receipt-based skew-immune formula.
 */
@Singleton
class DashboardUseCase @Inject constructor() {
    // ... (rest of the file content remains the same)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun computeDashboardState(
        uiState: MainUiState, 
        now: Long, 
        trackerState: TrackerState, 
        localMaxTemp: Double, 
        trackerMaxTemp: Double
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
        
        // Skew-Immune Freshness Calculation
        // 1. Link Age: How long since we last heard from the remote device (receipt time)
        val telemetryAge = if (loc.telemetryTs > 0) now - loc.telemetryTs else Long.MAX_VALUE
        
        // 2. Data Age: How old was the GPS fix relative to the source clock at transmission time
        val sourceGpsAge = if (loc.telemetryTs > 0 && loc.timestamp > 0) maxOf(0L, loc.telemetryTs - loc.timestamp) else 0L
        
        // 3. Total Effective Age: Local arrival delay + Source acquisition delay
        val totalGpsAge = telemetryAge + sourceGpsAge

        val isTelemetryFresh = telemetryAge < TELEMETRY_UI_STALE_THRESHOLD_MS
        val isGpsActive = totalGpsAge < GPS_UI_FAIL_THRESHOLD_MS && loc.timestamp > 0
        val isTelemetryVisible = telemetryAge < SENSOR_GRACE_PERIOD_MS
        val isForensicFresh = telemetryAge < WATCH_DOG_UI_GRACE_MS

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
        val snrValue = if (loc.snrIdx > 0.0 && isTelemetryVisible) "${(loc.snrIdx * RIBBON_SNR_SCALE_DB).toInt()}dB" else "--"

        fun gpsVal(value: String): String = if (isGpsActive) value else "--"
        fun sensorVal(value: String): String = if (isTelemetryVisible) value else "--"
        fun forensicVal(value: String): String = if (isForensicFresh) value else "--"

        fun formatDist(d: Double?): String {
            if (d == null || d.isNaN() || d == 0.0) return "--"
            return when {
                d >= 9000 -> String.format(Locale.getDefault(), "%.0fkm", d / 1000.0)
                d >= 1000 -> String.format(Locale.getDefault(), "%.1fkm", d / 1000.0)
                else -> "${d.toInt()}m"
            }
        }

        val rawAcc = loc.accuracy
        val filteredAcc = if (loc.maxAccuracy > 0) loc.maxAccuracy else rawAcc
        
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
            violationPercentage = "%.1f%%".format(Locale.getDefault(), loc.violationPercentage * 100.0),
            lat = gpsVal("%.6f".format(Locale.getDefault(), loc.lat)),
            lng = gpsVal("%.6f".format(Locale.getDefault(), loc.lng)),
            trackerAccuracy = gpsVal("±%.1fm".format(Locale.getDefault(), rawAcc)),
            trackerMaxAcc = gpsVal("±%.1fm".format(Locale.getDefault(), filteredAcc)),
            satsIndex = gpsVal("${uiState.trackerSatsUsed}/${uiState.trackerSatsView}"),
            isSatsIndexWarning = (uiState.trackerSatsUsed < 4 && uiState.trackerSatsView > 0),
            viewerAccuracy = if (isTrackerMode) "--" else "±%.1fm".format(Locale.getDefault(), uiState.localLocation.accuracy),
            viewerMaxAcc = if (isTrackerMode) "--" else "±%.1fm".format(Locale.getDefault(), if(uiState.localLocation.maxAccuracy > 0) uiState.localLocation.maxAccuracy else uiState.localLocation.accuracy),
            vibration = sensorVal("%.2fG".format(Locale.getDefault(), loc.vibration)),
            heading = sensorVal("%.0f°".format(Locale.getDefault(), loc.heading)),
            tilt = sensorVal("%.1f°".format(Locale.getDefault(), loc.tiltDegrees)),
            acoustic = sensorVal("%.0fdB".format(Locale.getDefault(), loc.acousticDb)),
            lift = sensorVal("%.1fm".format(Locale.getDefault(), loc.baroAlt)),
            lux = sensorVal("%.0flx".format(Locale.getDefault(), loc.lux)),
            proximity = sensorVal(if (loc.isNear) "NEAR" else "FAR"),
            proximityCm = sensorVal(if (loc.proximityCm >= 0) "${loc.proximityCm.toInt()}cm" else "--"),
            proximityDebounce = forensicVal("${loc.proximityDebounceMs}ms"),
            rollingVibration = forensicVal("%.3fG".format(Locale.getDefault(), loc.vibrationRollingSum)),
            gpsSpeed = gpsVal("%.1fkm/h".format(Locale.getDefault(), loc.speed * 3.6)),
            trackerMaxTemp = sensorVal("%.1f°C".format(Locale.getDefault(), trackerMaxTemp)),
            viewerMaxTemp = sensorVal("%.1f°C".format(Locale.getDefault(), localMaxTemp)),
            peakShock = sensorVal("%.2fG".format(Locale.getDefault(), loc.peakVibrationShock)),
            vibrationFloor = sensorVal("%.2fG".format(Locale.getDefault(), loc.adaptiveVibrationFloor)),
            luxBaseline = sensorVal("%.0flx".format(Locale.getDefault(), loc.luxBaseline)),
            acousticFloor = sensorVal("%.0fdB".format(Locale.getDefault(), loc.acousticFloorDb)),
            lastChairSit = sensorVal(chairTime),
            plungeSpeed = sensorVal("%.2fm/s".format(Locale.getDefault(), abs(loc.verticalVelocity))),
            chairForensics = forensicVal(forensics),
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
            isTelemetryFresh = isTelemetryFresh,
            isGpsVisible = isTelemetryVisible,
            isLinkVisible = isTelemetryVisible,
            isBatterySteepDischarge = loc.isBatterySteepDischarge,
            isCoolingModeActive = loc.isCoolingModeActive,
            currentMa = sensorVal("${loc.currentMa}mA"),
            isLocationPending = loc.isLocationPending,
            locationPendingReason = loc.locationPendingReason,
            isAnchorLocked = loc.isAnchorLocked
        )
    }
}
