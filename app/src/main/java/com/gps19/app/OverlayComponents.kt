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
 * v8.9.42:
 * - Issue #325: Authoritative Spatial Anchoring (Dual-Metric). Updated LegacyDashboardGrid 
 *   to display both raw accuracy and authoritative maxAccuracy side-by-side.
 * - Issue #326: Intelligent Uncertainty UX Mapping. Displaying locationPendingReason 
 *   in LegacyDashboardGrid. (Formerly #226)
 * - Issue #338: Unified UI Staleness Threshold. Consistently dimmed all forensic badges 
 *   and labels to Slate500 when telemetry is stale (>10s). (Formerly #193)
 * - Issue #337: Forensic Power Visibility. Added currentMa to LegacyDashboardGrid 
 *   for power parity. (Formerly #192)
 * v8.9.40:
 * - R865/R866: Swapped Lime500 for authoritative BrandJd (#367C2B).
 */

@Composable
fun LegacyDashboardGrid(
    uiState: MainUiState, 
    dashboard: DashboardState, 
    systemPulse: Long, 
    gpsIndexDataFlow: StateFlow<GpsIndexData>,
    rttFlow: StateFlow<Int>,
    onCalibrateChair: () -> Unit = {},
    onShowGnssDetail: () -> Unit = {}
) {
    val d = dashboard
    val mode = uiState.appMode
    val isViewer = mode == "viewer"
    val now = systemPulse

    val gpsColor = if (d.isGpsFresh) Color.White else Slate500
    val linkColor = if (d.isLinkFresh) Color.White else Slate500
    val telemetryColor = if (d.isTelemetryFresh) Color.White else Slate500
    val masterColor = if (d.isLinkFresh) Color.White else Slate500

    val conn = uiState.connectivity
    val isRelayOnline = conn.isRelayConnected
    val isRemoteActive = conn.lastRemoteActivityTs > 0 && (now - conn.lastRemoteActivityTs < WATCH_TIMEOUT_MS)
    
    val isConnStale = isViewer && !isRemoteActive
    val relayColor = if (isRelayOnline && !isConnStale) Color.White else Slate500

    val gpsIdx by gpsIndexDataFlow.collectAsStateWithLifecycle()
    val rttValue by rttFlow.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "DashboardPulse")
    val movingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "MovingAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                
                // Issue #326: Intelligent Uncertainty UX - Propagation to Dashboard
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
                    if (d.isSuspicious) {
                        Text(text = "[SUSPICIOUS]", color = if (d.isTelemetryFresh) Amber500 else Slate500, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    if (d.isTamperDetected) {
                        Text(text = "[TAMPER]", color = if (d.isTelemetryFresh) Rose500 else Slate500, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    if (d.isSitDetected) {
                        Text(text = "[SITTING]", color = if (d.isTelemetryFresh) Color(FORENSIC_PINK_COLOR) else Slate500, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    if (d.isBatterySteepDischarge) {
                        Text(text = "[BATT HEALTH]", color = if (d.isTelemetryFresh) Rose500 else Slate500, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(Modifier.height(4.dp))

            InfoRow(leftVal = d.maxDrop, leftLabel = stringResource(R.string.label_max_drop), leftColor = if (isConnStale) Slate500 else Rose500, rightVal = d.lastSeen, rightLabel = stringResource(R.string.label_last_seen), rightColor = relayColor)
            InfoRow(leftVal = d.totalDrop, leftLabel = stringResource(R.string.label_total_drop), leftColor = if (isConnStale) Slate500 else Rose500, rightVal = d.totalUptime, rightLabel = stringResource(R.string.label_app_bruto), rightColor = masterColor)
            
            val pingStr = if (rttValue > 0) "${rttValue}ms" else "--"
            InfoRow(leftVal = pingStr, leftLabel = stringResource(R.string.label_ping), leftColor = relayColor, rightVal = d.totalUptime, rightLabel = stringResource(R.string.label_total_monitor), rightColor = masterColor)
            
            val isUnrestricted = uiState.permissions.isBatteryWhitelisted
            InfoRow(leftVal = d.watchdogCountdown, leftLabel = stringResource(R.string.label_watchdog), leftColor = if (isConnStale) Slate500 else (if(d.watchdogOk) Emerald500 else Rose500), rightVal = if(isUnrestricted) "UNREST" else "RESTR", rightLabel = "Batt", rightColor = if (isConnStale) Slate500 else (if(isUnrestricted) Emerald500 else Amber500))
            
            val standbyText = when (d.standbyBucket) {
                10 -> "ACTIVE"; 20 -> "WORKING"; 30 -> "FREQUENT"; 40 -> "RARE"; 45 -> "RESTRICTED"; else -> "U-${d.standbyBucket}"
            }
            val standbyColor = when (d.standbyBucket) {
                10 -> Emerald500; 20 -> BrandJd; 30 -> Amber500; else -> Rose500
            }
            
            InfoRow(
                leftVal = if (d.isPowerSaveMode) "ON" else "OFF", leftLabel = "PwrSave", leftColor = if (d.isPowerSaveMode) Rose500 else Emerald500,
                rightVal = standbyText, rightLabel = "Standby", rightColor = if (isConnStale) Slate500 else standbyColor
            )
            
            val (storageText, storageColor) = when {
                d.isStorageCritical -> "CRITICAL" to Rose500
                d.isStorageLow -> "LOW" to Amber500
                else -> "OK" to Emerald500
            }

            InfoRow(
                leftVal = d.netInterface, leftLabel = "Network", leftColor = if (d.netInterface == "OFFLINE") Rose500 else Color.White,
                rightVal = storageText, rightLabel = "Storage", rightColor = if (isConnStale) Slate500 else storageColor
            )

            InfoRow(leftVal = d.totalUptime, leftLabel = "Uptime", leftColor = masterColor, rightVal = d.session, rightLabel = stringResource(R.string.label_session), rightColor = masterColor)
            InfoRow(leftVal = d.totalUptime, leftLabel = "Uptime", leftColor = masterColor, rightVal = d.session, rightLabel = stringResource(R.string.label_session), rightColor = masterColor)
            InfoRow(leftVal = d.engineVersion, leftLabel = "Engine Ver", leftColor = if(isConnStale) Slate500 else BrandJd, rightVal = d.sinceConn, rightLabel = stringResource(R.string.label_since_conn), rightColor = if(isConnStale) Slate500 else Emerald500)
            
            InfoRow(
                leftVal = d.lastChairSit, leftLabel = "Last Sit", leftColor = if(!d.isTelemetryFresh) Slate500 else Color(FORENSIC_PINK_COLOR), 
                rightVal = d.sinceDisco, rightLabel = stringResource(R.string.label_since_conn), rightColor = if(isConnStale) Slate500 else Emerald500
            )
            
            InfoRow(leftVal = d.violationUptime, leftLabel = "Violation", leftColor = if (!d.isTelemetryFresh) Slate500 else Rose500, rightVal = d.violationPercentage, rightLabel = "Stress Idx", rightColor = if (!d.isTelemetryFresh) Slate500 else Rose500)
            
            InfoRow(
                leftVal = d.distToHome, leftLabel = "Dist Home", leftColor = gpsColor,
                rightVal = d.distToViewer, rightLabel = "Dist Other", rightColor = gpsColor
            )

            Spacer(Modifier.height(6.dp)); HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp); Spacer(Modifier.height(6.dp))

            InfoRow(leftVal = d.lat, leftLabel = "Lat", leftColor = gpsColor, rightVal = d.lng, rightLabel = "Long", rightColor = gpsColor)
            
            val gpsIdxStr = "%.2f".format(Locale.getDefault(), gpsIdx.totalIndex)
            val ageIdxStr = "%.2f".format(Locale.getDefault(), gpsIdx.ageIndex)
            val accIdxStr = "%.2f".format(Locale.getDefault(), gpsIdx.accIndex)
            
            InfoRow(leftVal = gpsIdxStr, leftLabel = "GPS-Index", leftColor = if(!d.isGpsFresh) Slate500 else BrandJd, rightVal = d.gpsSpeed, rightLabel = "GPS Speed", rightColor = if(!d.isGpsFresh) Slate500 else BrandJd, onLeftClick = onShowGnssDetail)
            
            // R325: Displaying both raw and authoritative accuracy side-by-side
            val trkAccDisplay = "${d.trackerAccuracy} (${d.trackerMaxAcc})"
            val vwrAccDisplay = if (isViewer) "${d.viewerAccuracy} (${d.viewerMaxAcc})" else ""

            InfoRow(leftVal = d.satsIndex, leftLabel = "Satellites Index", leftColor = if(!d.isGpsFresh) Slate500 else if(d.isSatsIndexWarning) Rose500 else Color.White, rightVal = trkAccDisplay, rightLabel = "Tr Accuracy", rightColor = gpsColor)
            InfoRow(leftVal = vwrAccDisplay, leftLabel = if (isViewer) stringResource(R.string.label_accuracy) else "", leftColor = if (isViewer && !uiState.connectivity.isLocalOnline) Slate500 else ViewerOrange, rightVal = ageIdxStr, rightLabel = "Age Index", rightColor = if(!d.isGpsFresh) Slate500 else Amber500)
            InfoRow(leftVal = accIdxStr, leftLabel = "Acc Index", leftColor = if(!d.isGpsFresh) Slate500 else Color.White, rightVal = d.snr, rightLabel = "Avg SNR", rightColor = if(!d.isGpsFresh) Slate500 else Color(0xFF38BDF8), onRightClick = onShowGnssDetail)

            Spacer(Modifier.height(6.dp)); HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp); Spacer(Modifier.height(6.dp))

            InfoRow(leftVal = d.vibration, leftLabel = "Vibration", leftColor = if (!d.isTelemetryFresh) Slate500 else Color(FORENSIC_PINK_COLOR), rightVal = d.heading, rightLabel = "Compass", rightColor = if(!d.isTelemetryFresh) Slate500 else Color(0xFFFB923C))
            InfoRow(leftVal = d.tilt, leftLabel = "Tilt", leftColor = if(!d.isTelemetryFresh) Slate500 else Violet500, rightVal = d.acoustic, rightLabel = "Noise Level", rightColor = if(!d.isTelemetryFresh) Slate500 else Color(0xFF38BDF8))
            InfoRow(leftVal = d.lift, leftLabel = "Lift", leftColor = if(!d.isTelemetryFresh) Slate500 else Color(0xFFFACC15), rightVal = d.lux, rightLabel = "Lux", rightColor = if(!d.isTelemetryFresh) Slate500 else Amber500)
            
            InfoRow(leftVal = d.proximity, leftLabel = "Proximity", leftColor = if (!d.isTelemetryFresh) Slate500 else Emerald500, rightVal = d.proximityCm, rightLabel = "Raw Prox", rightColor = if (!d.isTelemetryFresh) Slate500 else Color(FORENSIC_PINK_COLOR))

            if (d.chairForensics != "--") {
                Spacer(Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Forensics:", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(55.dp))
                    Text(
                        text = d.chairForensics, 
                        color = if (d.isTelemetryFresh) Color(FORENSIC_PINK_COLOR) else Slate500, 
                        fontSize = 10.sp, 
                        fontFamily = FontFamily.Monospace, 
                        fontWeight = FontWeight.Bold,
                        softWrap = true
                    )
                }
            }
            
            InfoRow(leftVal = d.trackerMaxTemp, leftLabel = if (isViewer) "Tracker Max" else "Max Temp", leftColor = if (!d.isTelemetryFresh) Slate500 else BrandJd, rightVal = if (isViewer) d.viewerMaxTemp else "", rightLabel = if (isViewer) "Viewer Max" else "", rightColor = if (isViewer && !uiState.connectivity.isLocalOnline) Slate500 else ViewerOrange)

            Spacer(Modifier.height(6.dp)); HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp); Spacer(Modifier.height(6.dp))

            InfoRow(leftVal = d.peakShock, leftLabel = "Peak Shock", leftColor = if (!d.isTelemetryFresh) Slate500 else Rose500, rightVal = d.vibrationFloor, rightLabel = "Vibration Floor", rightColor = if (d.isTelemetryFresh) Slate400 else Slate500)
            InfoRow(leftVal = d.luxBaseline, leftLabel = "Lux Baseline", leftColor = if(!d.isTelemetryFresh) Slate500 else Amber500, rightVal = d.acousticFloor, rightLabel = "Acoustic Floor", rightColor = if(!d.isTelemetryFresh) Slate500 else Color(0xFF38BDF8))
            InfoRow(leftVal = d.currentMa, leftLabel = stringResource(R.string.log_diag_battery), leftColor = if(!d.isTelemetryFresh) Slate500 else Color.White, rightVal = "", rightLabel = "")
        }
    }
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
    onCalibrateChair: () -> Unit = {},
    onShowGnssDetail: () -> Unit = {}
) { LegacyDashboardGrid(uiState, dashboard, systemPulse, gpsIndexDataFlow, rttFlow, onCalibrateChair, onShowGnssDetail) }

@Composable
fun DebugTable(
    uiState: MainUiState, 
    dashboard: DashboardState, 
    systemPulse: Long,
    rttFlow: StateFlow<Int>,
    currentMaFlow: StateFlow<Int>
) {
    val d = dashboard
    val mode = uiState.appMode
    val isViewer = mode == "viewer"
    val loc = if (isViewer) uiState.trackerLocation else uiState.localLocation
    val now = systemPulse
    val gpsAgeMs = if (loc.timestamp > 0) now - loc.timestamp else -1L
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
