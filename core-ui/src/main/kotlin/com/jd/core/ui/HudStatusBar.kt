package com.jd.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * HudStatusBar: Standardized HUD status bar composable for Monitor and Sensor apps.
 * Implements Requirement R960 (Upper Row Global Status Bar & LED Grouping).
 */
@Composable
fun HudStatusBar(
    hudState: HudState,
    modifier: Modifier = Modifier,
) {
    val isMonitor = hudState.appMode == "monitor"
    val peerBadgeLabel = if (isMonitor) "TRK" else "VWR"
    val localColor = if (isMonitor) Color(0xFF06B6D4) else Color(0xFF10B981) // Cyan for Monitor, Green for Sensor
    val peerColor = if (isMonitor) Color(0xFF10B981) else Color(0xFF06B6D4)

    val infiniteTransition = rememberInfiniteTransition(label = "HudPulse")
    val movingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MovingAlpha"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.90f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Row 1: Status LEDs & Speed/Movement
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    StatusBadge(label = "INT", active = hudState.isInternet, activeColor = localColor)
                    StatusBadge(label = "SRV", active = hudState.isRelayConnected, activeColor = localColor)
                    StatusBadge(label = "GPS", active = hudState.isLocalGpsActive, activeColor = localColor)
                    StatusBadge(label = peerBadgeLabel, active = hudState.isPeerActive, activeColor = peerColor)
                    StatusBadge(label = "DAT", active = hudState.isDataHealthy, activeColor = localColor)
                    StatusBadge(label = "WDG", active = hudState.watchdogOk, activeColor = localColor)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isMoving = hudState.trackerState == "MOVING"
                    Text(
                        text = if (isMoving) "»${hudState.trackerState}«" else hudState.trackerState,
                        color = if (hudState.isPeerActive || !isMonitor) {
                            if (isMoving) Color(0xFF10B981).copy(alpha = movingAlpha) else Color(0xFF10B981)
                        } else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )

                    Text(
                        text = String.format(Locale.US, "%.1f km/h", hudState.speedKmh),
                        color = if (hudState.isPeerActive || !isMonitor) Color.White else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Row 2: Metadata / Telemetry
            val idDisplay = if (isMonitor) {
                "TRK:${hudState.targetId.take(6).uppercase()} | SELF:${hudState.selfId.take(6).uppercase()}"
            } else {
                "SELF:${hudState.selfId.take(6).uppercase()}"
            }
            val ageStr = if (hudState.telemetryAgeSec >= 0) "${hudState.telemetryAgeSec}s" else "--"
            val battStr = "${hudState.batteryLevel}%${if (hudState.isCharging) "⚡" else ""}"
            val tempStr = String.format(Locale.US, "%.0f°C", hudState.tempCelsius)
            val accStr = if (hudState.accuracyMeters > 0) String.format(Locale.US, "±%.1fm", hudState.accuracyMeters) else "--"

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = idDisplay,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Text(
                    text = "B:$battStr|T:$tempStr|A:$accStr|AGE:$ageStr",
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF10B981),
    inactiveColor: Color = Color(0xFFF43F5E)
) {
    val bgColor = if (active) activeColor.copy(alpha = 0.2f) else inactiveColor.copy(alpha = 0.2f)
    val textColor = if (active) activeColor else inactiveColor

    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(2.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
