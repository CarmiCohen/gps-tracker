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
 * Aug.10.24:
 * - Issue #130: Proto Health Parity. Integrated isBatteryLow and isBatteryCritical 
 *   badges into DashboardHeader (R130).
 * Aug.07.00:
 * - Issue #741: Dashboard & TelemetryBox Recomposition Audit. Refactored MainDashboardGrid,
 *   TelemetryBox, and DebugTable to take primitive parameters instead of monolithic 
 *   state objects (R736).
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
    maxDrop: String,
    lastSeen: String,
    totalDrop: String,
    totalUptime: String,
    session: String,
    engineVersion: String,
    sinceConn: String,
    sinceDisco: String,
    violationUptime: String,
    watchdogCountdown: String,
    watchdogOk: Boolean,
    isPowerSaveMode: Boolean,
    standbyBucket: Int,
    netInterface: String,
    isStorageLow: Boolean,
    isStorageCritical: Boolean,
    distToHome: String,
    distToViewer: String,
    lat: String,
    lng: String,
    gpsSpeed: String,
    trackerAccuracy: String,
    trackerMaxAcc: String,
    viewerAccuracy: String,
    viewerMaxAcc: String,
    satsIndex: String,
    isSatsIndexWarning: Boolean,
    snr: String,
    vibration: String,
    heading: String,
    tilt: String,
    acoustic: String,
    lift: String,
    lux: String,
    proximity: String,
    proximityCm: String,
    proximityDebounce: String,
    rollingVibration: String,
    trackerMaxTemp: String,
    viewerMaxTemp: String,
    peakShock: String,
    vibrationFloor: String,
    luxBaseline: String,
    acousticFloor: String,
    trackerCurrentMa: String,
    gpsIdx: GpsIndexData,
    rttValue: Int,
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
                maxDrop = maxDrop,
                lastSeen = lastSeen,
                totalDrop = totalDrop,
                totalUptime = totalUptime,
                session = session,
                engineVersion = engineVersion,
                sinceConn = sinceConn,
                sinceDisco = sinceDisco,
                violationUptime = violationUptime,
                watchdogCountdown = watchdogCountdown,
                watchdogOk = watchdogOk,
                isPowerSaveMode = isPowerSaveMode,
                standbyBucket = standbyBucket,
                netInterface = netInterface,
                isStorageLow = isStorageLow,
                isStorageCritical = isStorageCritical,
                isBatteryWhitelisted = isBatteryWhitelisted,
                isTelemetryFresh = isTelemetryFresh,
                isConnStale = isViewer && (lastRemoteActivityTs <= 0 || (systemPulse - lastRemoteActivityTs >= TELEMETRY_UI_STALE_THRESHOLD_MS)),
                relayColor = relayColor,
                masterColor = masterColor,
                rttValue = rttValue
            )
            SectionDivider()
            PositionSection(
                distToHome = distToHome,
                distToViewer = distToViewer,
                lat = lat,
                lng = lng,
                gpsSpeed = gpsSpeed,
                trackerAccuracy = trackerAccuracy,
                trackerMaxAcc = trackerMaxAcc,
                viewerAccuracy = viewerAccuracy,
                viewerMaxAcc = viewerMaxAcc,
                satsIndex = satsIndex,
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
                acoustic = acoustic,
                lift = lift,
                lux = lux,
                proximity = proximity,
                proximityCm = proximityCm,
                proximityDebounce = proximityDebounce,
                rollingVibration = rollingVibration,
                trackerMaxTemp = trackerMaxTemp,
                viewerMaxTemp = viewerMaxTemp,
                peakShock = peakShock,
                vibrationFloor = vibrationFloor,
                luxBaseline = luxBaseline,
                acousticFloor = acousticFloor,
                trackerCurrentMa = trackerCurrentMa,
                isTelemetryFresh = isTelemetryFresh,
                isViewer = isViewer,
                isLocalOnline = isLocalOnline
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
            Text(
                text = "UNCERTAINTY: ${locationPendingReason.name.replace("_", " ")}",
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
    maxDrop: String,
    lastSeen: String,
    totalDrop: String,
    totalUptime: String,
    session: String,
    engineVersion: String,
    sinceConn: String,
    sinceDisco: String,
    violationUptime: String,
    watchdogCountdown: String,
    watchdogOk: Boolean,
    isPowerSaveMode: Boolean,
    standbyBucket: Int,
    netInterface: String,
    isStorageLow: Boolean,
    isStorageCritical: Boolean,
    isBatteryWhitelisted: Boolean,
    isTelemetryFresh: Boolean,
    isConnStale: Boolean,
    relayColor: Color,
    masterColor: Color,
    rttValue: Int
) {
    InfoRow(leftVal = maxDrop, leftLabel = stringResource(R.string.label_max_drop), leftColor = if (isConnStale) Slate500 else Rose500, rightVal = lastSeen, rightLabel = stringResource(R.string.label_last_seen), rightColor = relayColor)
    InfoRow(leftVal = totalDrop, leftLabel = stringResource(R.string.label_total_drop), leftColor = if (isConnStale) Slate500 else Rose500, rightVal = totalUptime, rightLabel = stringResource(R.string.label_app_bruto), rightColor = masterColor)
    
    val pingStr = if (rttValue > 0) "${rttValue}ms" else "--"
    InfoRow(leftVal = pingStr, leftLabel = stringResource(R.string.label_ping), leftColor = relayColor, rightVal = totalUptime, rightLabel = stringResource(R.string.label_total_monitor), rightColor = masterColor)
    
    InfoRow(leftVal = watchdogCountdown, leftLabel = stringResource(R.string.label_watchdog), leftColor = if (isConnStale) Slate500 else (if(watchdogOk) BrandJd else Rose500), rightVal = if(isBatteryWhitelisted) "UNREST" else "RESTR", rightLabel = "Batt", rightColor = if (isConnStale) Slate500 else (if(isBatteryWhitelisted) BrandJd else Amber500))
    
    val standbyText = when (standbyBucket) {
        10 -> "ACTIVE"; 20 -> "WORKING"; 30 -> "FREQUENT"; 40 -> "RARE"; 45 -> "RESTRICTED"; else -> "U-$standbyBucket"
    }
    val standbyColor = when (standbyBucket) {
        10 -> BrandJd; 20 -> BrandJd; 30 -> Amber500; else -> Rose500
    }
    InfoRow(leftVal = if (isPowerSaveMode) "ON" else "OFF", leftLabel = "PwrSave", leftColor = if (isPowerSaveMode) Rose500 else BrandJd, rightVal = standbyText, rightLabel = "Standby", rightColor = if (isConnStale) Slate500 else standbyColor)
    
    val (storageText, storageColor) = when {
        isStorageCritical -> "CRITICAL" to Rose500
        isStorageLow -> "LOW" to Amber500
        else -> "OK" to BrandJd
    }
    InfoRow(leftVal = netInterface, leftLabel = "Network", leftColor = if (netInterface == "OFFLINE") Rose500 else Color.White, rightVal = storageText, rightLabel = "Storage", rightColor = if (isConnStale) Slate500 else storageColor)
    
    InfoRow(leftVal = totalUptime, leftLabel = "Uptime", leftColor = masterColor, rightVal = session, rightLabel = stringResource(R.string.label_session), rightColor = masterColor)
    InfoRow(leftVal = engineVersion, leftLabel = "Engine Ver", leftColor = if(isConnStale) Slate500 else BrandJd, rightVal = sinceConn, rightLabel = stringResource(R.string.label_since_conn), rightColor = if(isConnStale) Slate500 else BrandJd)
    InfoRow(leftVal = sinceDisco, leftLabel = "Disconnected", leftColor = if(isConnStale) Slate500 else BrandJd, rightVal = violationUptime, rightLabel = "Violation", rightColor = if (!isTelemetryFresh) Slate500 else Rose500)
}

@Composable
private fun PositionSection(
    distToHome: String,
    distToViewer: String,
    lat: String,
    lng: String,
    gpsSpeed: String,
    trackerAccuracy: String,
    trackerMaxAcc: String,
    viewerAccuracy: String,
    viewerMaxAcc: String,
    satsIndex: String,
    isSatsIndexWarning: Boolean,
    snr: String,
    isGpsFresh: Boolean,
    gpsIdx: GpsIndexData,
    isViewer: Boolean,
    isLocalOnline: Boolean,
    gpsColor: Color,
    onShowGnssDetail: () -> Unit
) {
    InfoRow(leftVal = distToHome, leftLabel = "Dist Home", leftColor = gpsColor, rightVal = distToViewer, rightLabel = "Dist Other", rightColor = gpsColor)
    InfoRow(leftVal = lat, leftLabel = "Lat", leftColor = gpsColor, rightVal = lng, rightLabel = "Long", rightColor = gpsColor)
    
    val gpsIdxStr = "%.2f".format(Locale.getDefault(), gpsIdx.totalIndex)
    InfoRow(leftVal = gpsIdxStr, leftLabel = "GPS-Index", leftColor = if(!isGpsFresh) Slate500 else BrandJd, rightVal = gpsSpeed, rightLabel = "GPS Speed", rightColor = if(!isGpsFresh) Slate500 else BrandJd, onLeftClick = onShowGnssDetail)
    
    val trkAccDisplay = "$trackerAccuracy ($trackerMaxAcc)"
    val vwrAccDisplay = if (isViewer) "$viewerAccuracy ($viewerMaxAcc)" else ""
    InfoRow(leftVal = vwrAccDisplay, leftLabel = if (isViewer) stringResource(R.string.label_accuracy) else "", leftColor = if (isViewer && !isLocalOnline) Slate500 else ViewerCyan, rightVal = trkAccDisplay, rightLabel = "Tr Accuracy", rightColor = gpsColor)
    
    InfoRow(leftVal = satsIndex, leftLabel = "Satellites Index", leftColor = if(!isGpsFresh) Slate500 else if(isSatsIndexWarning) Rose500 else Color.White, rightVal = "%.2f".format(Locale.getDefault(), gpsIdx.ageIndex), rightLabel = "Age Index", rightColor = if(!isGpsFresh) Slate500 else Amber500)
    InfoRow(leftVal = "%.2f".format(Locale.getDefault(), gpsIdx.accIndex), leftLabel = "Acc Index", leftColor = if(!isGpsFresh) Slate500 else Color.White, rightVal = snr, rightLabel = "Avg SNR", rightColor = if(!isGpsFresh) Slate500 else Color(0xFF38BDF8), onRightClick = onShowGnssDetail)
}

@Composable
private fun ForensicSection(
    vibration: String,
    heading: String,
    tilt: String,
    acoustic: String,
    lift: String,
    lux: String,
    proximity: String,
    proximityCm: String,
    proximityDebounce: String,
    rollingVibration: String,
    trackerMaxTemp: String,
    viewerMaxTemp: String,
    peakShock: String,
    vibrationFloor: String,
    luxBaseline: String,
    acousticFloor: String,
    trackerCurrentMa: String,
    isTelemetryFresh: Boolean,
    isViewer: Boolean,
    isLocalOnline: Boolean
) {
    val staleColor = Slate500
    val tFresh = isTelemetryFresh
    
    InfoRow(leftVal = vibration, leftLabel = "Vibration", leftColor = if (!tFresh) staleColor else Color(FORENSIC_PINK_COLOR), rightVal = heading, rightLabel = "Compass", rightColor = if(!tFresh) staleColor else Color(0xFFFB923C))
    InfoRow(leftVal = tilt, leftLabel = "Tilt", leftColor = if(!tFresh) staleColor else Violet500, rightVal = acoustic, rightLabel = "Noise Level", rightColor = if(!tFresh) staleColor else Color(0xFF38BDF8))
    InfoRow(leftVal = lift, leftLabel = "Lift", leftColor = if(!tFresh) staleColor else Color(0xFFFACC15), rightVal = lux, rightLabel = "Lux", rightColor = if(!tFresh) staleColor else Amber500)
    
    InfoRow(leftVal = proximity, leftLabel = "Proximity", leftColor = if (!tFresh) staleColor else BrandJd, rightVal = proximityCm, rightLabel = "Raw Prox", rightColor = if (!tFresh) staleColor else Color(FORENSIC_PINK_COLOR))
    InfoRow(leftVal = proximityDebounce, leftLabel = "Prox Debounce", leftColor = if (!tFresh) staleColor else BrandJd, rightVal = rollingVibration, rightLabel = "Rolling Vibe", rightColor = if (!tFresh) staleColor else Color(FORENSIC_PINK_COLOR))

    InfoRow(leftVal = trackerMaxTemp, leftLabel = if (isViewer) "Tracker Max" else "Max Temp", leftColor = if (!tFresh) staleColor else BrandJd, rightVal = if (isViewer) viewerMaxTemp else "", rightLabel = if (isViewer) "Viewer Max" else "", rightColor = if (isViewer && !isLocalOnline) staleColor else ViewerCyan)

    SectionDivider()

    InfoRow(leftVal = peakShock, leftLabel = "Peak Shock", leftColor = if (!tFresh) staleColor else Rose500, rightVal = vibrationFloor, rightLabel = "Vibration Floor", rightColor = if (tFresh) Slate400 else staleColor)
    InfoRow(leftVal = luxBaseline, leftLabel = "Lux Baseline", leftColor = if(!tFresh) staleColor else Amber500, rightVal = acousticFloor, rightLabel = "Acoustic Floor", rightColor = if(!tFresh) staleColor else Color(0xFF38BDF8))
    InfoRow(leftVal = trackerCurrentMa, leftLabel = stringResource(R.string.log_diag_battery), leftColor = if(!tFresh) staleColor else Color.White, rightVal = "", rightLabel = "")
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
    maxDrop: String,
    lastSeen: String,
    totalDrop: String,
    totalUptime: String,
    session: String,
    engineVersion: String,
    sinceConn: String,
    sinceDisco: String,
    violationUptime: String,
    watchdogCountdown: String,
    watchdogOk: Boolean,
    isPowerSaveMode: Boolean,
    standbyBucket: Int,
    netInterface: String,
    isStorageLow: Boolean,
    isStorageCritical: Boolean,
    distToHome: String,
    distToViewer: String,
    lat: String,
    lng: String,
    gpsSpeed: String,
    trackerAccuracy: String,
    trackerMaxAcc: String,
    viewerAccuracy: String,
    viewerMaxAcc: String,
    satsIndex: String,
    isSatsIndexWarning: Boolean,
    snr: String,
    vibration: String,
    heading: String,
    tilt: String,
    acoustic: String,
    lift: String,
    lux: String,
    proximity: String,
    proximityCm: String,
    proximityDebounce: String,
    rollingVibration: String,
    trackerMaxTemp: String,
    viewerMaxTemp: String,
    peakShock: String,
    vibrationFloor: String,
    luxBaseline: String,
    acousticFloor: String,
    trackerCurrentMa: String,
    gpsIdx: GpsIndexData,
    rttValue: Int,
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
        maxDrop = maxDrop,
        lastSeen = lastSeen,
        totalDrop = totalDrop,
        totalUptime = totalUptime,
        session = session,
        engineVersion = engineVersion,
        sinceConn = sinceConn,
        sinceDisco = sinceDisco,
        violationUptime = violationUptime,
        watchdogCountdown = watchdogCountdown,
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
        gpsSpeed = gpsSpeed,
        trackerAccuracy = trackerAccuracy,
        trackerMaxAcc = trackerMaxAcc,
        viewerAccuracy = viewerAccuracy,
        viewerMaxAcc = viewerMaxAcc,
        satsIndex = satsIndex,
        isSatsIndexWarning = isSatsIndexWarning,
        snr = snr,
        vibration = vibration,
        heading = heading,
        tilt = tilt,
        acoustic = acoustic,
        lift = lift,
        lux = lux,
        proximity = proximity,
        proximityCm = proximityCm,
        proximityDebounce = proximityDebounce,
        rollingVibration = rollingVibration,
        trackerMaxTemp = trackerMaxTemp,
        viewerMaxTemp = viewerMaxTemp,
        peakShock = peakShock,
        vibrationFloor = vibrationFloor,
        luxBaseline = luxBaseline,
        acousticFloor = acousticFloor,
        trackerCurrentMa = trackerCurrentMa,
        gpsIdx = gpsIdx,
        rttValue = rttValue,
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
