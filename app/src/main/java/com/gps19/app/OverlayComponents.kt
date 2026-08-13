package com.gps19.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import com.gps19.core.engine.*

/**
 * OverlayComponents: Dashboard and telemetry visualization components.
 * Aug.13.11:
 * - Issue #163: 1Hz Telemetry Path Optimization. Refactored all dashboard 
 *   components to handle primitive types and utilized memoized formatting 
 *   to eliminate object churn during 1Hz telemetry updates (R163).
 * Aug.10.26:
 * - Issue #132: Forensic UI Dashboard Refinement. Integrated cpuLoad, ioWait, 
 *   and maxIoLatency into ForensicSection for performance auditing (R132).
 */

@Composable
fun MainDashboardGrid(
    appMode: String,
    isBatteryWhitelisted: Boolean,
    isLocalOnline: Boolean,
    isRelayConnected: Boolean,
    lastRemoteActivityTs: Long,
    systemPulse: Long,
    // Decomposed DashboardState fields
    isGpsFresh: Boolean,
    isTelemetryFresh: Boolean,
    isLinkFresh: Boolean,
    trackerState: TrackerState,
    isLocationPending: Boolean,
    locationPendingReason: LocationPendingReason,
    status: SentinelStatus,
    isTamperDetected: Boolean,
    isBatterySteepDischarge: Boolean,
    isBatteryLow: Boolean,
    isBatteryCritical: Boolean,
    maxDropMs: Long,
    lastSeenTs: Long,
    totalDropMs: Long,
    totalUptimeMs: Long,
    sessionMs: Long,
    engineVersion: String,
    sinceConnMs: Long,
    sinceDiscoMs: Long,
    violationUptimeMs: Long,
    watchdogCountdownSec: Long,
    watchdogOk: Boolean,
    isPowerSaveMode: Boolean,
    standbyBucket: Int,
    netInterface: String,
    isStorageLow: Boolean,
    isStorageCritical: Boolean,
    distToHome: Double?,
    distToViewer: Double?,
    lat: Double,
    lng: Double,
    gpsSpeedMps: Double,
    trackerAccuracy: Double,
    trackerMaxAcc: Double,
    viewerAccuracy: Double,
    viewerMaxAcc: Double,
    satsUsed: Int,
    satsView: Int,
    isSatsIndexWarning: Boolean,
    snr: Double,
    vibration: Double,
    heading: Double,
    tilt: Double,
    acousticDb: Double,
    baroAlt: Double,
    lux: Double,
    proximityCm: Double,
    proximityDebounceMs: Long,
    rollingVibration: Double,
    trackerMaxTemp: Double,
    viewerMaxTemp: Double,
    peakShock: Double,
    vibrationFloor: Double,
    luxBaseline: Double,
    acousticFloorDb: Double,
    trackerCurrentMa: Int,
    gpsIdx: GpsIndexData,
    rttValue: Int,
    cpuLoad: Double,
    ioWait: Double,
    maxIoLatencyMs: Long,
    onShowGnssDetail: () -> Unit = {}
) {
    val isViewer = appMode == "viewer"
    val gpsColor = if (isGpsFresh) Color.White else Slate500
    val relayColor = if (isRelayConnected && !(isViewer && (lastRemoteActivityTs <= 0 || (systemPulse - lastRemoteActivityTs >= TELEMETRY_UI_STALE_THRESHOLD_MS)))) Color.White else Slate500
    val masterColor = if (isLinkFresh) Color.White else Slate500

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            DashboardHeader(
                isGpsFresh = isGpsFresh,
                trackerState = trackerState,
                isLocationPending = isLocationPending,
                locationPendingReason = locationPendingReason,
                status = status,
                isTelemetryFresh = isTelemetryFresh,
                isTamperDetected = isTamperDetected,
                isBatterySteepDischarge = isBatterySteepDischarge,
                isBatteryLow = isBatteryLow,
                isBatteryCritical = isBatteryCritical
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(Modifier.height(4.dp))

            SystemHealthSection(
                maxDropMs = maxDropMs,
                lastSeenTs = lastSeenTs,
                totalDropMs = totalDropMs,
                totalUptimeMs = totalUptimeMs,
                sessionMs = sessionMs,
                engineVersion = engineVersion,
                sinceConnMs = sinceConnMs,
                sinceDiscoMs = sinceDiscoMs,
                violationUptimeMs = violationUptimeMs,
                watchdogCountdownSec = watchdogCountdownSec,
                watchdogOk = watchdogOk,
                standbyBucket = standbyBucket,
                netInterface = netInterface,
                isStorageLow = isStorageLow,
                isStorageCritical = isStorageCritical,
                isBatteryWhitelisted = isBatteryWhitelisted,
                isTelemetryFresh = isTelemetryFresh,
                isConnStale = isViewer && (lastRemoteActivityTs <= 0 || (systemPulse - lastRemoteActivityTs >= TELEMETRY_UI_STALE_THRESHOLD_MS)),
                relayColor = relayColor,
                masterColor = masterColor,
                rttValue = rttValue,
                isPowerSaveMode = isPowerSaveMode,
                systemPulse = systemPulse
            )
            SectionDivider()
            PositionSection(
                distToHome = distToHome,
                distToViewer = distToViewer,
                lat = lat,
                lng = lng,
                gpsSpeedMps = gpsSpeedMps,
                trackerAccuracy = trackerAccuracy,
                trackerMaxAcc = trackerMaxAcc,
                viewerAccuracy = viewerAccuracy,
                viewerMaxAcc = viewerMaxAcc,
                satsUsed = satsUsed,
                satsView = satsView,
                isSatsIndexWarning = isSatsIndexWarning,
                snr = snr,
                isGpsFresh = isGpsFresh,
                gpsIdx = gpsIdx,
                isViewer = isViewer,
                isLocalOnline = isLocalOnline,
                gpsColor = gpsColor,
                onShowGnssDetail = onShowGnssDetail
            )
            SectionDivider()
            ForensicSection(
                vibration = vibration,
                heading = heading,
                tilt = tilt,
                acousticDb = acousticDb,
                baroAlt = baroAlt,
                lux = lux,
                proximityCm = proximityCm,
                proximityDebounceMs = proximityDebounceMs,
                rollingVibration = rollingVibration,
                trackerMaxTemp = trackerMaxTemp,
                viewerMaxTemp = viewerMaxTemp,
                peakShock = peakShock,
                vibrationFloor = vibrationFloor,
                luxBaseline = luxBaseline,
                acousticFloorDb = acousticFloorDb,
                trackerCurrentMa = trackerCurrentMa,
                isTelemetryFresh = isTelemetryFresh,
                isViewer = isViewer,
                isLocalOnline = isLocalOnline,
                cpuLoad = cpuLoad,
                ioWait = ioWait,
                maxIoLatencyMs = maxIoLatencyMs
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    isGpsFresh: Boolean,
    trackerState: TrackerState,
    isLocationPending: Boolean,
    locationPendingReason: LocationPendingReason,
    status: SentinelStatus,
    isTelemetryFresh: Boolean,
    isTamperDetected: Boolean,
    isBatterySteepDischarge: Boolean,
    isBatteryLow: Boolean,
    isBatteryCritical: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DashboardPulse")
    val movingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "MovingAlpha"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val stateColor = if (isGpsFresh) BrandJd else Slate500
            val isMoving = trackerState == TrackerState.MOVING
            val stateText = trackerState.name
            
            Text(
                text = if (isMoving && isGpsFresh) "»\u2009$stateText\u2009«" else stateText,
                color = stateColor.copy(alpha = if (isMoving && isGpsFresh) movingAlpha else 1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        
        if (isLocationPending && locationPendingReason != LocationPendingReason.NONE) {
            val reasonLabel = remember(locationPendingReason) { locationPendingReason.name.replace("_", " ") }
            Text(
                text = "UNCERTAINTY: $reasonLabel",
                color = Amber500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (status == SentinelStatus.TAMPER) Badge("[TAMPER]", if (isTelemetryFresh) Rose500 else Slate500)
            if (isTamperDetected) Badge("[HW TAMPER]", if (isTelemetryFresh) Rose500 else Slate500)
            if (isBatterySteepDischarge) Badge("[BATT HEALTH]", if (isTelemetryFresh) Rose500 else Slate500)
            if (isBatteryCritical) Badge("[BATT CRITICAL]", if (isTelemetryFresh) Rose500 else Slate500)
            else if (isBatteryLow) Badge("[BATT LOW]", if (isTelemetryFresh) Amber500 else Slate500)
        }
    }
}

@Composable
private fun SystemHealthSection(
    maxDropMs: Long,
    lastSeenTs: Long,
    totalDropMs: Long,
    totalUptimeMs: Long,
    sessionMs: Long,
    engineVersion: String,
    sinceConnMs: Long,
    sinceDiscoMs: Long,
    violationUptimeMs: Long,
    watchdogCountdownSec: Long,
    watchdogOk: Boolean,
    standbyBucket: Int,
    netInterface: String,
    isStorageLow: Boolean,
    isStorageCritical: Boolean,
    isBatteryWhitelisted: Boolean,
    isTelemetryFresh: Boolean,
    isConnStale: Boolean,
    relayColor: Color,
    masterColor: Color,
    rttValue: Int,
    isPowerSaveMode: Boolean,
    systemPulse: Long
) {
    val maxDropStr = remember(maxDropMs) { formatDuration(maxDropMs) }
    val lastSeenStr = remember(lastSeenTs, systemPulse) { 
        if (lastSeenTs > 0) {
            val delta = (systemPulse - lastSeenTs) / 1000
            if (delta < 60) "${delta}s" else "${delta / 60}m"
        } else "--"
    }
    InfoRow(leftVal = maxDropStr, leftLabel = stringResource(R.string.label_max_drop), leftColor = if (isConnStale) Slate500 else Rose500, rightVal = lastSeenStr, rightLabel = stringResource(R.string.label_last_seen), rightColor = relayColor)
    
    val totalDropStr = remember(totalDropMs) { formatDuration(totalDropMs) }
    val totalUptimeStr = remember(totalUptimeMs) { formatDuration(totalUptimeMs) }
    InfoRow(leftVal = totalDropStr, leftLabel = stringResource(R.string.label_total_drop), leftColor = if (isConnStale) Slate500 else Rose500, rightVal = totalUptimeStr, rightLabel = stringResource(R.string.label_app_bruto), rightColor = masterColor)
    
    val pingStr = if (rttValue > 0) "${rttValue}ms" else "--"
    InfoRow(leftVal = pingStr, leftLabel = stringResource(R.string.label_ping), leftColor = relayColor, rightVal = totalUptimeStr, rightLabel = stringResource(R.string.label_total_monitor), rightColor = masterColor)
    
    val watchdogStr = remember(watchdogCountdownSec) { if (watchdogCountdownSec > 0) "${watchdogCountdownSec}s" else "--" }
    InfoRow(leftVal = watchdogStr, leftLabel = stringResource(R.string.label_watchdog), leftColor = if (isConnStale) Slate500 else (if(watchdogOk) BrandJd else Rose500), rightVal = if(isBatteryWhitelisted) "UNREST" else "RESTR", rightLabel = "Batt", rightColor = if (isConnStale) Slate500 else (if(isBatteryWhitelisted) BrandJd else Amber500))
    
    val (standbyText, standbyColor) = remember(standbyBucket) {
        when (standbyBucket) {
            10 -> "ACTIVE" to BrandJd; 20 -> "WORKING" to BrandJd; 30 -> "FREQUENT" to Amber500; 40 -> "RARE" to Rose500; 45 -> "RESTRICTED" to Rose500; else -> "U-$standbyBucket" to Rose500
        }
    }
    InfoRow(leftVal = if (isPowerSaveMode) "ON" else "OFF", leftLabel = "PwrSave", leftColor = if (isPowerSaveMode) Rose500 else BrandJd, rightVal = standbyText, rightLabel = "Standby", rightColor = if (isConnStale) Slate500 else standbyColor)
    
    val (storageText, storageColor) = when {
        isStorageCritical -> "CRITICAL" to Rose500
        isStorageLow -> "LOW" to Amber500
        else -> "OK" to BrandJd
    }
    InfoRow(leftVal = netInterface, leftLabel = "Network", leftColor = if (netInterface == "OFFLINE") Rose500 else Color.White, rightVal = storageText, rightLabel = "Storage", rightColor = if (isConnStale) Slate500 else storageColor)
    
    val sessionStr = remember(sessionMs) { formatDuration(sessionMs) }
    InfoRow(leftVal = totalUptimeStr, leftLabel = "Uptime", leftColor = masterColor, rightVal = sessionStr, rightLabel = stringResource(R.string.label_session), rightColor = masterColor)
    
    val sinceConnStr = remember(sinceConnMs) { if (sinceConnMs > 0) formatDuration(sinceConnMs) else "--" }
    InfoRow(leftVal = engineVersion, leftLabel = "Engine Ver", leftColor = if(isConnStale) Slate500 else BrandJd, rightVal = sinceConnStr, rightLabel = stringResource(R.string.label_since_conn), rightColor = if(isConnStale) Slate500 else BrandJd)
    
    val sinceDiscoStr = remember(sinceDiscoMs) { if (sinceDiscoMs > 0) formatDuration(sinceDiscoMs) else "--" }
    val violationUptimeStr = remember(violationUptimeMs) { formatDuration(violationUptimeMs) }
    InfoRow(leftVal = sinceDiscoStr, leftLabel = "Disconnected", leftColor = if(isConnStale) Slate500 else BrandJd, rightVal = violationUptimeStr, rightLabel = "Violation", rightColor = if (!isTelemetryFresh) Slate500 else Rose500)
}

@Composable
private fun PositionSection(
    distToHome: Double?,
    distToViewer: Double?,
    lat: Double,
    lng: Double,
    gpsSpeedMps: Double,
    trackerAccuracy: Double,
    trackerMaxAcc: Double,
    viewerAccuracy: Double,
    viewerMaxAcc: Double,
    satsUsed: Int,
    satsView: Int,
    isSatsIndexWarning: Boolean,
    snr: Double,
    isGpsFresh: Boolean,
    gpsIdx: GpsIndexData,
    isViewer: Boolean,
    isLocalOnline: Boolean,
    gpsColor: Color,
    onShowGnssDetail: () -> Unit
) {
    val distToHomeStr = remember(distToHome) { formatDist(distToHome) }
    val distToViewerStr = remember(distToViewer) { formatDist(distToViewer) }
    InfoRow(leftVal = distToHomeStr, leftLabel = "Dist Home", leftColor = gpsColor, rightVal = distToViewerStr, rightLabel = "Dist Other", rightColor = gpsColor)
    
    val latStr = remember(lat, isGpsFresh) { if (isGpsFresh) "%.6f".format(Locale.getDefault(), lat) else "--" }
    val lngStr = remember(lng, isGpsFresh) { if (isGpsFresh) "%.6f".format(Locale.getDefault(), lng) else "--" }
    InfoRow(leftVal = latStr, leftLabel = "Lat", leftColor = gpsColor, rightVal = lngStr, rightLabel = "Long", rightColor = gpsColor)
    
    val gpsIdxStr = remember(gpsIdx.totalIndex) { "%.2f".format(Locale.getDefault(), gpsIdx.totalIndex) }
    val gpsSpeedStr = remember(gpsSpeedMps, isGpsFresh) { if (isGpsFresh) "%.1fkm/h".format(Locale.getDefault(), gpsSpeedMps * 3.6) else "--" }
    InfoRow(leftVal = gpsIdxStr, leftLabel = "GPS-Index", leftColor = if(!isGpsFresh) Slate500 else BrandJd, rightVal = gpsSpeedStr, rightLabel = "GPS Speed", rightColor = if(!isGpsFresh) Slate500 else BrandJd, onLeftClick = onShowGnssDetail)
    
    val trkAccDisplay = remember(trackerAccuracy, trackerMaxAcc, isGpsFresh) { if (isGpsFresh) "±%.1fm (±%.1fm)".format(Locale.getDefault(), trackerAccuracy, trackerMaxAcc) else "--" }
    val vwrAccDisplay = remember(viewerAccuracy, viewerMaxAcc, isViewer) { if (isViewer) "±%.1fm (±%.1fm)".format(Locale.getDefault(), viewerAccuracy, viewerMaxAcc) else "" }
    InfoRow(leftVal = vwrAccDisplay, leftLabel = if (isViewer) stringResource(R.string.label_accuracy) else "", leftColor = if (isViewer && !isLocalOnline) Slate500 else ViewerCyan, rightVal = trkAccDisplay, rightLabel = "Tr Accuracy", rightColor = gpsColor)
    
    val satsIndexStr = remember(satsUsed, satsView, isGpsFresh) { if (isGpsFresh) "$satsUsed/$satsView" else "--" }
    val ageIdxStr = remember(gpsIdx.ageIndex) { "%.2f".format(Locale.getDefault(), gpsIdx.ageIndex) }
    InfoRow(leftVal = satsIndexStr, leftLabel = "Satellites Index", leftColor = if(!isGpsFresh) Slate500 else if(isSatsIndexWarning) Rose500 else Color.White, rightVal = ageIdxStr, rightLabel = "Age Index", rightColor = if(!isGpsFresh) Slate500 else Amber500)
    
    val accIdxStr = remember(gpsIdx.accIndex) { "%.2f".format(Locale.getDefault(), gpsIdx.accIndex) }
    val snrStr = remember(snr, isGpsFresh) { if (isGpsFresh) "${snr.toInt()}dB" else "--" }
    InfoRow(leftVal = accIdxStr, leftLabel = "Acc Index", leftColor = if(!isGpsFresh) Slate500 else Color.White, rightVal = snrStr, rightLabel = "Avg SNR", rightColor = if(!isGpsFresh) Slate500 else Color(0xFF38BDF8), onRightClick = onShowGnssDetail)
}

@Composable
private fun ForensicSection(
    vibration: Double,
    heading: Double,
    tilt: Double,
    acousticDb: Double,
    baroAlt: Double,
    lux: Double,
    proximityCm: Double,
    proximityDebounceMs: Long,
    rollingVibration: Double,
    trackerMaxTemp: Double,
    viewerMaxTemp: Double,
    peakShock: Double,
    vibrationFloor: Double,
    luxBaseline: Double,
    acousticFloorDb: Double,
    trackerCurrentMa: Int,
    isTelemetryFresh: Boolean,
    isViewer: Boolean,
    isLocalOnline: Boolean,
    cpuLoad: Double,
    ioWait: Double,
    maxIoLatencyMs: Long
) {
    val staleColor = Slate500
    val tFresh = isTelemetryFresh
    
    val vibeStr = remember(vibration, tFresh) { if (tFresh) "%.2fG".format(Locale.getDefault(), vibration) else "--" }
    val headingStr = remember(heading, tFresh) { if (tFresh) "%.0f°".format(Locale.getDefault(), heading) else "--" }
    InfoRow(leftVal = vibeStr, leftLabel = "Vibration", leftColor = if (!tFresh) staleColor else Color(FORENSIC_PINK_COLOR), rightVal = headingStr, rightLabel = "Compass", rightColor = if(!tFresh) staleColor else Color(0xFFFB923C))
    
    val tiltStr = remember(tilt, tFresh) { if (tFresh) "%.1f°".format(Locale.getDefault(), tilt) else "--" }
    val acousticStr = remember(acousticDb, tFresh) { if (tFresh) "%.0fdB".format(Locale.getDefault(), acousticDb) else "--" }
    InfoRow(leftVal = tiltStr, leftLabel = "Tilt", leftColor = if(!tFresh) staleColor else Violet500, rightVal = acousticStr, rightLabel = "Noise Level", rightColor = if(!tFresh) staleColor else Color(0xFF38BDF8))
    
    val liftStr = remember(baroAlt, tFresh) { if (tFresh) "%.1fm".format(Locale.getDefault(), baroAlt) else "--" }
    val luxStr = remember(lux, tFresh) { if (tFresh) "%.0flx".format(Locale.getDefault(), lux) else "--" }
    InfoRow(leftVal = liftStr, leftLabel = "Lift", leftColor = if(!tFresh) staleColor else Color(0xFFFACC15), rightVal = luxStr, rightLabel = "Lux", rightColor = if(!tFresh) staleColor else Amber500)
    
    val proxStr = remember(proximityCm, tFresh) { if (tFresh) (if (proximityCm in 0.0..5.0) "NEAR" else "FAR") else "--" }
    val rawProxStr = remember(proximityCm, tFresh) { if (tFresh && proximityCm >= 0) "${proximityCm.toInt()}cm" else "--" }
    InfoRow(leftVal = proxStr, leftLabel = "Proximity", leftColor = if (!tFresh) staleColor else BrandJd, rightVal = rawProxStr, rightLabel = "Raw Prox", rightColor = if (!tFresh) staleColor else Color(FORENSIC_PINK_COLOR))
    
    val proxDebounceStr = remember(proximityDebounceMs, tFresh) { if (tFresh) "${proximityDebounceMs}ms" else "--" }
    val rollingVibeStr = remember(rollingVibration, tFresh) { if (tFresh) "%.3fG".format(Locale.getDefault(), rollingVibration) else "--" }
    InfoRow(leftVal = proxDebounceStr, leftLabel = "Prox Debounce", leftColor = if (!tFresh) staleColor else BrandJd, rightVal = rollingVibeStr, rightLabel = "Rolling Vibe", rightColor = if (!tFresh) staleColor else Color(FORENSIC_PINK_COLOR))

    val trkMaxTempStr = remember(trackerMaxTemp, tFresh) { if (tFresh) "%.1f°C".format(Locale.getDefault(), trackerMaxTemp) else "--" }
    val vwrMaxTempStr = remember(viewerMaxTemp, isViewer) { if (isViewer) "%.1f°C".format(Locale.getDefault(), viewerMaxTemp) else "" }
    InfoRow(leftVal = trkMaxTempStr, leftLabel = if (isViewer) "Tracker Max" else "Max Temp", leftColor = if (!tFresh) staleColor else BrandJd, rightVal = vwrMaxTempStr, rightLabel = if (isViewer) "Viewer Max" else "", rightColor = if (isViewer && !isLocalOnline) staleColor else ViewerCyan)

    SectionDivider()

    val peakShockStr = remember(peakShock, tFresh) { if (tFresh) "%.2fG".format(Locale.getDefault(), peakShock) else "--" }
    val vibeFloorStr = remember(vibrationFloor, tFresh) { if (tFresh) "%.2fG".format(Locale.getDefault(), vibrationFloor) else "--" }
    InfoRow(leftVal = peakShockStr, leftLabel = "Peak Shock", leftColor = if (!tFresh) staleColor else Rose500, rightVal = vibeFloorStr, rightLabel = "Vibration Floor", rightColor = if (tFresh) Slate400 else staleColor)
    
    val luxBaseStr = remember(luxBaseline, tFresh) { if (tFresh) "%.0flx".format(Locale.getDefault(), luxBaseline) else "--" }
    val acousticFloorStr = remember(acousticFloorDb, tFresh) { if (tFresh) "%.0fdB".format(Locale.getDefault(), acousticFloorDb) else "--" }
    InfoRow(leftVal = luxBaseStr, leftLabel = "Lux Baseline", leftColor = if(!tFresh) staleColor else Amber500, rightVal = acousticFloorStr, rightLabel = "Acoustic Floor", rightColor = if(!tFresh) staleColor else Color(0xFF38BDF8))
    
    val cpuLoadStr = remember(cpuLoad, tFresh) { if (tFresh) "%.1f%%".format(Locale.getDefault(), cpuLoad * 100.0) else "--" }
    val ioWaitStr = remember(ioWait, tFresh) { if (tFresh) "%.1f%%".format(Locale.getDefault(), ioWait * 100.0) else "--" }
    InfoRow(leftVal = cpuLoadStr, leftLabel = "CPU Load", leftColor = if (!tFresh) staleColor else Color.White, rightVal = ioWaitStr, rightLabel = "I/O Wait", rightColor = if (!tFresh) staleColor else Amber500)
    
    val maxLatencyStr = remember(maxIoLatencyMs, tFresh) { if (tFresh) "${maxIoLatencyMs}ms" else "--" }
    val currentMaStr = remember(trackerCurrentMa, tFresh) { if (tFresh) "${trackerCurrentMa}mA" else "--" }
    InfoRow(leftVal = maxLatencyStr, leftLabel = "Max Latency", leftColor = if (!tFresh) staleColor else Rose500, rightVal = currentMaStr, rightLabel = stringResource(R.string.log_diag_battery), rightColor = if(!tFresh) staleColor else Color.White)
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun InfoRow(
    leftVal: String, leftLabel: String, leftColor: Color = Color.White, 
    rightVal: String, rightLabel: String, rightColor: Color = Color.White, 
    onLeftClick: (() -> Unit)? = null,
    onRightClick: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp, horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(modifier = Modifier.weight(1.3f).clickable(enabled = onLeftClick != null) { onLeftClick?.invoke() }, verticalAlignment = Alignment.CenterVertically) { 
            if (leftVal.isNotEmpty()) {
                Text(leftVal, color = leftColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(115.dp))
                Spacer(Modifier.width(2.dp))
                Text(leftLabel, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                if (onLeftClick != null) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Refresh, null, tint = leftColor.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                }
            }
        }
        Row(modifier = Modifier.weight(1.2f).clickable(enabled = onRightClick != null) { onRightClick?.invoke() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
            if (rightVal.isNotEmpty()) {
                Text(rightVal, color = rightColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(105.dp))
                Spacer(Modifier.width(2.dp))
                Text(rightLabel, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                if (onRightClick != null) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Refresh, null, tint = rightColor.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

@Composable
fun TelemetryBox(
    appMode: String,
    isBatteryWhitelisted: Boolean,
    isLocalOnline: Boolean,
    isRelayConnected: Boolean,
    lastRemoteActivityTs: Long,
    systemPulse: Long,
    // Decomposed DashboardState fields
    isGpsFresh: Boolean,
    isTelemetryFresh: Boolean,
    isLinkFresh: Boolean,
    trackerState: TrackerState,
    isLocationPending: Boolean,
    locationPendingReason: LocationPendingReason,
    status: SentinelStatus,
    isTamperDetected: Boolean,
    isBatterySteepDischarge: Boolean,
    isBatteryLow: Boolean,
    isBatteryCritical: Boolean,
    maxDropMs: Long,
    lastSeenTs: Long,
    totalDropMs: Long,
    totalUptimeMs: Long,
    sessionMs: Long,
    engineVersion: String,
    sinceConnMs: Long,
    sinceDiscoMs: Long,
    violationUptimeMs: Long,
    watchdogCountdownSec: Long,
    watchdogOk: Boolean,
    isPowerSaveMode: Boolean,
    standbyBucket: Int,
    netInterface: String,
    isStorageLow: Boolean,
    isStorageCritical: Boolean,
    distToHome: Double?,
    distToViewer: Double?,
    lat: Double,
    lng: Double,
    gpsSpeedMps: Double,
    trackerAccuracy: Double,
    trackerMaxAcc: Double,
    viewerAccuracy: Double,
    viewerMaxAcc: Double,
    satsUsed: Int,
    satsView: Int,
    isSatsIndexWarning: Boolean,
    snr: Double,
    vibration: Double,
    heading: Double,
    tilt: Double,
    acousticDb: Double,
    baroAlt: Double,
    lux: Double,
    proximityCm: Double,
    proximityDebounceMs: Long,
    rollingVibration: Double,
    trackerMaxTemp: Double,
    viewerMaxTemp: Double,
    peakShock: Double,
    vibrationFloor: Double,
    luxBaseline: Double,
    acousticFloorDb: Double,
    trackerCurrentMa: Int,
    gpsIdx: GpsIndexData,
    rttValue: Int,
    cpuLoad: Double,
    ioWait: Double,
    maxIoLatencyMs: Long,
    onShowGnssDetail: () -> Unit = {}
) {
    MainDashboardGrid(
        appMode = appMode,
        isBatteryWhitelisted = isBatteryWhitelisted,
        isLocalOnline = isLocalOnline,
        isRelayConnected = isRelayConnected,
        lastRemoteActivityTs = lastRemoteActivityTs,
        systemPulse = systemPulse,
        isGpsFresh = isGpsFresh,
        isTelemetryFresh = isTelemetryFresh,
        isLinkFresh = isLinkFresh,
        trackerState = trackerState,
        isLocationPending = isLocationPending,
        locationPendingReason = locationPendingReason,
        status = status,
        isTamperDetected = isTamperDetected,
        isBatterySteepDischarge = isBatterySteepDischarge,
        isBatteryLow = isBatteryLow,
        isBatteryCritical = isBatteryCritical,
        maxDropMs = maxDropMs,
        lastSeenTs = lastSeenTs,
        totalDropMs = totalDropMs,
        totalUptimeMs = totalUptimeMs,
        sessionMs = sessionMs,
        engineVersion = engineVersion,
        sinceConnMs = sinceConnMs,
        sinceDiscoMs = sinceDiscoMs,
        violationUptimeMs = violationUptimeMs,
        watchdogCountdownSec = watchdogCountdownSec,
        watchdogOk = watchdogOk,
        isPowerSaveMode = isPowerSaveMode,
        standbyBucket = standbyBucket,
        netInterface = netInterface,
        isStorageLow = isStorageLow,
        isStorageCritical = isStorageCritical,
        distToHome = distToHome,
        distToViewer = distToViewer,
        lat = lat,
        lng = lng,
        gpsSpeedMps = gpsSpeedMps,
        trackerAccuracy = trackerAccuracy,
        trackerMaxAcc = trackerMaxAcc,
        viewerAccuracy = viewerAccuracy,
        viewerMaxAcc = viewerMaxAcc,
        satsUsed = satsUsed,
        satsView = satsView,
        isSatsIndexWarning = isSatsIndexWarning,
        snr = snr,
        vibration = vibration,
        heading = heading,
        tilt = tilt,
        acousticDb = acousticDb,
        baroAlt = baroAlt,
        lux = lux,
        proximityCm = proximityCm,
        proximityDebounceMs = proximityDebounceMs,
        rollingVibration = rollingVibration,
        trackerMaxTemp = trackerMaxTemp,
        viewerMaxTemp = viewerMaxTemp,
        peakShock = peakShock,
        vibrationFloor = vibrationFloor,
        luxBaseline = luxBaseline,
        acousticFloorDb = acousticFloorDb,
        trackerCurrentMa = trackerCurrentMa,
        gpsIdx = gpsIdx,
        rttValue = rttValue,
        cpuLoad = cpuLoad,
        ioWait = ioWait,
        maxIoLatencyMs = maxIoLatencyMs,
        onShowGnssDetail = onShowGnssDetail
    )
}

@Composable
fun DebugTable(
    isLinkFresh: Boolean,
    isTelemetryFresh: Boolean,
    isGpsFresh: Boolean,
    trackerStateName: String,
    gpsAgeSec: Long,
    rtt: Int,
    currentMa: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(stringResource(R.string.log_diagnostics_title), color = BrandJd, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                DebugItem(stringResource(R.string.log_diag_latency), "${rtt}ms", valueColor = if (isLinkFresh) Color.White else Slate500)
                DebugItem(stringResource(R.string.log_diag_battery), "${currentMa}mA", valueColor = if (isTelemetryFresh) Color.White else Slate500)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                DebugItem(stringResource(R.string.log_diag_gps_age), if (gpsAgeSec >= 0) "${gpsAgeSec}s" else "--", valueColor = if (isGpsFresh) Color.White else Slate500)
                DebugItem(stringResource(R.string.log_diag_sentinel), trackerStateName, valueColor = if (isGpsFresh) BrandJd else Slate500)
            }
        }
    }
}

@Composable
private fun DebugItem(label: String, value: String, valueColor: Color = Color.White) {
    Column(modifier = Modifier.width(140.dp)) {
        Text(label, color = Slate500, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
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
