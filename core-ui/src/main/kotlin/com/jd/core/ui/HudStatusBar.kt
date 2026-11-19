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
                    hudState.leds.forEach { led -> StatusBadge(label = led.label, state = led.state) }
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
        text = motionLabelText(trackerState),
        color = if (!active) Color.Gray else if (isMoving) SensorGreen.copy(alpha = movingAlpha) else SensorGreen,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
    )
}

/** The longest motion label, `STATIONARY`; every label is padded to it. */
const val MOTION_LABEL_CHARS = 10

/**
 * The label's text, MOVING between guillemets, centered in [MOTION_LABEL_CHARS] monospace characters: the menu bar
 * gives it its whole width (on the narrow S21FE it was cut to `STATION`, Nov19.5) and the icons beside it do not move
 * when the state changes.
 */
fun motionLabelText(trackerState: String): String {
    val text = if (trackerState == "MOVING") "»$trackerState«" else trackerState
    val pad = (MOTION_LABEL_CHARS - text.length).coerceAtLeast(0)
    return " ".repeat(pad / 2) + text + " ".repeat(pad - pad / 2)
}

/** The color of a HUD LED in both apps: green OK, red not OK, gray no information (Nov19.5). */
fun ledColor(state: LedState): Color = when (state) {
    LedState.OK -> StatusGreen
    LedState.NOT_OK -> StatusRed
    LedState.UNKNOWN -> StatusGrey
}

@Composable
fun StatusBadge(
    label: String,
    state: LedState,
    modifier: Modifier = Modifier
) {
    val textColor = ledColor(state)
    val bgColor = textColor.copy(alpha = 0.2f)

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
