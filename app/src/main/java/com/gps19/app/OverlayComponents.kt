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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import com.gps19.core.engine.*

/**
 * OverlayComponents: Dashboard and telemetry visualization components.
 * July.22.11:
 * - Issue #519: Dashboard UI Simplification & Telemetry Componentization.
 * - Renamed LegacyDashboardGrid to MainDashboardGrid.
 * - Decomposed into logical telemetry sections.
 */

@Composable
fun MainDashboardGrid(
    uiState: MainUiState,
    dashboard: DashboardState,
    systemPulse: Long,
    gpsIndexDataFlow: StateFlow<GpsIndexData>,
    rttFlow: StateFlow<Int>,
    onShowGnssDetail: () -> Unit = {}
) {
    val d = dashboard
    val isViewer = uiState.appMode == "viewer"
    val gpsIdx by gpsIndexDataFlow.collectAsStateWithLifecycle()
    val rttValue by rttFlow.collectAsStateWithLifecycle()

    val gpsColor = if (d.isGpsFresh) Color.White else Slate500
    val telemetryColor = if (d.isTelemetryFresh) Color.White else Slate500
    val masterColor = if (d.isLinkFresh) Color.White else Slate500

    val conn = uiState.connectivity
    val isRemoteActive = conn.lastRemoteActivityTs > 0 && (systemPulse - conn.lastRemoteActivityTs < TELEMETRY_UI_STALE_THRESHOLD_MS)
    val isConnStale = isViewer && !isRemoteActive
    val relayColor = if (conn.isRelayConnected && !isConnStale) Color.White else Slate500

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            DashboardHeader(d)
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(Modifier.height(4.dp))

            SystemHealthSection(d, uiState, isConnStale, relayColor, masterColor, rttValue)
            
            SectionDivider()

            PositionSection(d, uiState, gpsIdx, gpsColor, onShowGnssDetail)

            SectionDivider()

            ForensicSection(d, telemetryColor, isViewer, uiState)
        }
    }
}

@Composable
private fun DashboardHeader(d: DashboardState) {
    val infiniteTransition = rememberInfiniteTransition(label = "DashboardPulse")
    val movingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "MovingAlpha"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val stateColor = if (d.isGpsFresh) BrandJd else Slate500
            val isMoving = d.trackerState == TrackerState.MOVING
            val stateText = d.trackerState.name
            
            Text(
                text = if (isMoving && d.isGpsFresh) "»\u2009$stateText\u2009«" else stateText,
                color = stateColor.copy(alpha = if (isMoving && d.isGpsFresh) movingAlpha else 1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        
        if (d.isLocationPending && d.locationPendingReason != LocationPendingReason.NONE) {
            Text(
                text = "UNCERTAINTY: ${d.locationPendingReason.name.replace("_", " ")}",
                color = Amber500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (d.status == SentinelStatus.TAMPER) Badge("[TAMPER]", if (d.isTelemetryFresh) Rose500 else Slate500)
            if (d.isTamperDetected) Badge("[HW TAMPER]", if (d.isTelemetryFresh) Rose500 else Slate500)
            if (d.isBatterySteepDischarge) Badge("[BATT HEALTH]", if (d.isTelemetryFresh) Rose500 else Slate500)
        }
    }
}

@Composable
private fun SystemHealthSection(d: DashboardState, uiState: MainUiState, isConnStale: Boolean, relayColor: Color, masterColor: Color, rttValue: Int) {
    InfoRow(leftVal = d.maxDrop, leftLabel = stringResource(R.string.label_max_drop), leftColor = if (isConnStale) Slate500 else Rose500, rightVal = d.lastSeen, rightLabel = stringResource(R.string.label_last_seen), rightColor = relayColor)
    InfoRow(leftVal = d.totalDrop, leftLabel = stringResource(R.string.label_total_drop), leftColor = if (isConnStale) Slate500 else Rose500, rightVal = d.totalUptime, rightLabel = stringResource(R.string.label_app_bruto), rightColor = masterColor)
    
    val pingStr = if (rttValue > 0) "${rttValue}ms" else "--"
    InfoRow(leftVal = pingStr, leftLabel = stringResource(R.string.label_ping), leftColor = relayColor, rightVal = d.totalUptime, rightLabel = stringResource(R.string.label_total_monitor), rightColor = masterColor)
    
    val isUnrestricted = uiState.permissions.isBatteryWhitelisted
    InfoRow(leftVal = d.watchdogCountdown, leftLabel = stringResource(R.string.label_watchdog), leftColor = if (isConnStale) Slate500 else (if(d.watchdogOk) BrandJd else Rose500), rightVal = if(isUnrestricted) "UNREST" else "RESTR", rightLabel = "Batt", rightColor = if (isConnStale) Slate500 else (if(isUnrestricted) BrandJd else Amber500))
    
    val standbyText = when (d.standbyBucket) {
        10 -> "ACTIVE"; 20 -> "WORKING"; 30 -> "FREQUENT"; 40 -> "RARE"; 45 -> "RESTRICTED"; else -> "U-${d.standbyBucket}"
    }
    val standbyColor = when (d.standbyBucket) {
        10 -> BrandJd; 20 -> BrandJd; 30 -> Amber500; else -> Rose500
    }
    InfoRow(leftVal = if (d.isPowerSaveMode) "ON" else "OFF", leftLabel = "PwrSave", leftColor = if (d.isPowerSaveMode) Rose500 else BrandJd, rightVal = standbyText, rightLabel = "Standby", rightColor = if (isConnStale) Slate500 else standbyColor)
    
    val (storageText, storageColor) = when {
        d.isStorageCritical -> "CRITICAL" to Rose500
        d.isStorageLow -> "LOW" to Amber500
        else -> "OK" to BrandJd
    }
    InfoRow(leftVal = d.netInterface, leftLabel = "Network", leftColor = if (d.netInterface == "OFFLINE") Rose500 else Color.White, rightVal = storageText, rightLabel = "Storage", rightColor = if (isConnStale) Slate500 else storageColor)
    
    InfoRow(leftVal = d.totalUptime, leftLabel = "Uptime", leftColor = masterColor, rightVal = d.session, rightLabel = stringResource(R.string.label_session), rightColor = masterColor)
    InfoRow(leftVal = d.engineVersion, leftLabel = "Engine Ver", leftColor = if(isConnStale) Slate500 else BrandJd, rightVal = d.sinceConn, rightLabel = stringResource(R.string.label_since_conn), rightColor = if(isConnStale) Slate500 else BrandJd)
    InfoRow(leftVal = d.sinceDisco, leftLabel = "Disconnected", leftColor = if(isConnStale) Slate500 else BrandJd, rightVal = d.violationUptime, rightLabel = "Violation", rightColor = if (!d.isTelemetryFresh) Slate500 else Rose500)
}

@Composable
private fun PositionSection(d: DashboardState, uiState: MainUiState, gpsIdx: GpsIndexData, gpsColor: Color, onShowGnssDetail: () -> Unit) {
    val isViewer = uiState.appMode == "viewer"
    InfoRow(leftVal = d.distToHome, leftLabel = "Dist Home", leftColor = gpsColor, rightVal = d.distToViewer, rightLabel = "Dist Other", rightColor = gpsColor)
    InfoRow(leftVal = d.lat, leftLabel = "Lat", leftColor = gpsColor, rightVal = d.lng, rightLabel = "Long", rightColor = gpsColor)
    
    val gpsIdxStr = "%.2f".format(Locale.getDefault(), gpsIdx.totalIndex)
    InfoRow(leftVal = gpsIdxStr, leftLabel = "GPS-Index", leftColor = if(!d.isGpsFresh) Slate500 else BrandJd, rightVal = d.gpsSpeed, rightLabel = "GPS Speed", rightColor = if(!d.isGpsFresh) Slate500 else BrandJd, onLeftClick = onShowGnssDetail)
    
    val trkAccDisplay = "${d.trackerAccuracy} (${d.trackerMaxAcc})"
    val vwrAccDisplay = if (isViewer) "${d.viewerAccuracy} (${d.viewerMaxAcc})" else ""
    InfoRow(leftVal = vwrAccDisplay, leftLabel = if (isViewer) stringResource(R.string.label_accuracy) else "", leftColor = if (isViewer && !uiState.connectivity.isLocalOnline) Slate500 else ViewerCyan, rightVal = trkAccDisplay, rightLabel = "Tr Accuracy", rightColor = gpsColor)
    
    InfoRow(leftVal = d.satsIndex, leftLabel = "Satellites Index", leftColor = if(!d.isGpsFresh) Slate500 else if(d.isSatsIndexWarning) Rose500 else Color.White, rightVal = "%.2f".format(Locale.getDefault(), gpsIdx.ageIndex), rightLabel = "Age Index", rightColor = if(!d.isGpsFresh) Slate500 else Amber500)
    InfoRow(leftVal = "%.2f".format(Locale.getDefault(), gpsIdx.accIndex), leftLabel = "Acc Index", leftColor = if(!d.isGpsFresh) Slate500 else Color.White, rightVal = d.snr, rightLabel = "Avg SNR", rightColor = if(!d.isGpsFresh) Slate500 else Color(0xFF38BDF8), onRightClick = onShowGnssDetail)
}

@Composable
private fun ForensicSection(d: DashboardState, telemetryColor: Color, isViewer: Boolean, uiState: MainUiState) {
    val staleColor = Slate500
    val tFresh = d.isTelemetryFresh
    
    InfoRow(leftVal = d.vibration, leftLabel = "Vibration", leftColor = if (!tFresh) staleColor else Color(FORENSIC_PINK_COLOR), rightVal = d.heading, rightLabel = "Compass", rightColor = if(!tFresh) staleColor else Color(0xFFFB923C))
    InfoRow(leftVal = d.tilt, leftLabel = "Tilt", leftColor = if(!tFresh) staleColor else Violet500, rightVal = d.acoustic, rightLabel = "Noise Level", rightColor = if(!tFresh) staleColor else Color(0xFF38BDF8))
    InfoRow(leftVal = d.lift, leftLabel = "Lift", leftColor = if(!tFresh) staleColor else Color(0xFFFACC15), rightVal = d.lux, rightLabel = "Lux", rightColor = if(!tFresh) staleColor else Amber500)
    
    InfoRow(leftVal = d.proximity, leftLabel = "Proximity", leftColor = if (!tFresh) staleColor else BrandJd, rightVal = d.proximityCm, rightLabel = "Raw Prox", rightColor = if (!tFresh) staleColor else Color(FORENSIC_PINK_COLOR))
    InfoRow(leftVal = d.proximityDebounce, leftLabel = "Prox Debounce", leftColor = if (!tFresh) staleColor else BrandJd, rightVal = d.rollingVibration, rightLabel = "Rolling Vibe", rightColor = if (!tFresh) staleColor else Color(FORENSIC_PINK_COLOR))

    InfoRow(leftVal = d.trackerMaxTemp, leftLabel = if (isViewer) "Tracker Max" else "Max Temp", leftColor = if (!tFresh) staleColor else BrandJd, rightVal = if (isViewer) d.viewerMaxTemp else "", rightLabel = if (isViewer) "Viewer Max" else "", rightColor = if (isViewer && !uiState.connectivity.isLocalOnline) staleColor else ViewerCyan)

    SectionDivider()

    InfoRow(leftVal = d.peakShock, leftLabel = "Peak Shock", leftColor = if (!tFresh) staleColor else Rose500, rightVal = d.vibrationFloor, rightLabel = "Vibration Floor", rightColor = if (tFresh) Slate400 else staleColor)
    InfoRow(leftVal = d.luxBaseline, leftLabel = "Lux Baseline", leftColor = if(!tFresh) staleColor else Amber500, rightVal = d.acousticFloor, rightLabel = "Acoustic Floor", rightColor = if(!tFresh) staleColor else Color(0xFF38BDF8))
    InfoRow(leftVal = d.currentMa, leftLabel = stringResource(R.string.log_diag_battery), leftColor = if(!tFresh) staleColor else Color.White, rightVal = "", rightLabel = "")
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
    uiState: MainUiState, 
    dashboard: DashboardState, 
    systemPulse: Long, 
    gpsIndexDataFlow: StateFlow<GpsIndexData>,
    rttFlow: StateFlow<Int>,
    onShowGnssDetail: () -> Unit = {}
) { MainDashboardGrid(uiState, dashboard, systemPulse, gpsIndexDataFlow, rttFlow, onShowGnssDetail) }

@Composable
fun DebugTable(
    uiState: MainUiState, 
    dashboard: DashboardState, 
    systemPulse: Long,
    rttFlow: StateFlow<Int>,
    currentMaFlow: StateFlow<Int>
) {
    val d = dashboard
    val loc = if (uiState.appMode == "viewer") uiState.trackerLocation else uiState.localLocation
    val gpsAgeMs = if (loc.timestamp > 0) systemPulse - loc.timestamp else -1L
    val gpsAgeSec = if (gpsAgeMs >= 0) gpsAgeMs / 1000 else -1L

    val rtt by rttFlow.collectAsStateWithLifecycle()
    val currentMa by currentMaFlow.collectAsStateWithLifecycle()

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
                DebugItem(stringResource(R.string.log_diag_latency), "${rtt}ms", valueColor = if (d.isLinkFresh) Color.White else Slate500)
                DebugItem(stringResource(R.string.log_diag_battery), "${currentMa}mA", valueColor = if (d.isTelemetryFresh) Color.White else Slate500)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                DebugItem(stringResource(R.string.log_diag_gps_age), if (gpsAgeSec >= 0) "${gpsAgeSec}s" else "--", valueColor = if (d.isGpsFresh) Color.White else Slate500)
                DebugItem(stringResource(R.string.log_diag_sentinel), d.trackerState.name, valueColor = if (d.isGpsFresh) BrandJd else Slate500)
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
