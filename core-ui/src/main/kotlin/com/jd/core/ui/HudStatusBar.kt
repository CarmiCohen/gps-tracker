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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow

/**
 * HudStatusBar: Standardized HUD status bar composable for Monitor and Sensor apps.
 * Implements Requirement R960 (Upper Row Global Status Bar & LED Grouping).
 */
@Composable
fun HudStatusBar(
    hudState: HudState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    // Telemetry is positional, so it must not mirror or bidi-reorder under RTL system languages.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        CappedFontScale {
            ProvideTextStyle(if (compact) CompactHudTextStyle else HudTextStyle) {
                HudStatusBarContent(hudState, modifier, verticalGap = if (compact) 1.dp else 2.dp)
            }
        }
    }
}

private val HudTextStyle = TextStyle(textDirection = TextDirection.Ltr)

// Text otherwise inherits bodyLarge's 24.sp lineHeight, which dominates the row height at 8-10.sp.
private val CompactHudTextStyle = HudTextStyle.copy(
    lineHeight = 11.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

@Composable
private fun HudStatusBarContent(
    hudState: HudState,
    modifier: Modifier,
    verticalGap: Dp,
) {
    val isMonitor = hudState.appMode == "monitor"
    val peerBadgeLabel = if (isMonitor) "TRK" else "VWR"
    val localColor = if (isMonitor) MonitorBrown else SensorGreen
    val peerColor = if (isMonitor) SensorGreen else MonitorBrown

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.90f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = verticalGap)
        ) {
            // Row 1: Status LEDs & Speed (the Sensor's STATIONARY/MOVING is in the menu bar since Nov19.3: SensorMotionLabel)
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

                Text(
                    text = hudState.speedText,
                    color = if (hudState.isPeerActive || !isMonitor) Color.White else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(verticalGap))

            // Row 2: Metadata / Telemetry
            val idDisplay = hudState.idText

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // The ID is a few characters and always shown whole; a long telemetry line wraps instead
                // (ISSUE-NOV18.9-O117: the ID beside `38%⚡|29°|A:±96.0m|AGE:465s`).
                Text(
                    text = idDisplay,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = hudState.telemetryText,
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * The Sensor's STATIONARY / MOVING in the menu bar of both apps, where the `MONITOR: V -> TRK: T` and `SENSOR: T`
 * headers were (Nov19.3). MOVING pulses between guillemets; gray when the Sensor is not heard ([active] false).
 */
@Composable
fun SensorMotionLabel(trackerState: String, active: Boolean, modifier: Modifier = Modifier) {
    val isMoving = trackerState == "MOVING"
    val pulse = rememberInfiniteTransition(label = "MotionPulse")
    val movingAlpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "MovingAlpha"
    )
    Text(
        text = if (isMoving) "»$trackerState«" else trackerState,
        color = if (!active) Color.Gray else if (isMoving) SensorGreen.copy(alpha = movingAlpha) else SensorGreen,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
    )
}

@Composable
fun StatusBadge(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = SensorGreen,
    inactiveColor: Color = Rose500
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
