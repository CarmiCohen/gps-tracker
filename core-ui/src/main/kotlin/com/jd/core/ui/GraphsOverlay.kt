package com.jd.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jd.core.shared.ViewPeriod
import com.jd.core.shared.GraphLine
import com.jd.core.shared.GraphPanel
import com.jd.core.shared.GraphScreenState
import com.jd.core.shared.GraphSpan
import com.jd.core.shared.ServiceStop
import com.jd.core.shared.GraphTone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The menu's Graphs screen of both apps (ISSUE-NOV10.8-O84 part 3): each section's graphs over the chosen [period],
 * built by core-shared (`sensorGraphScreen`, `monitorGraphScreen`). A gap in a line is a time without samples; where
 * the Sensor's service was stopped its graphs are shaded gray and the stops are listed with their reasons
 * (ISSUE-NOV15.6-O100 part 3), so a remaining gap means the Monitor did not receive those samples yet. A limit line is
 * dashed red, and the battery's green background marks charging. Opened from an event, the period is centred on the
 * event's time, marked by a dashed white line, and [onNow] returns to the period ending now (part 4).
 */
@Composable
fun GraphsOverlay(
    title: String,
    state: GraphScreenState,
    period: ViewPeriod,
    onPeriod: (ViewPeriod) -> Unit,
    onClose: () -> Unit,
    onNow: () -> Unit = {}
) {
    // English labels and a time axis running left to right, also on a right-to-left phone (as the Events list).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(modifier = Modifier.fillMaxSize(), color = Slate900, contentColor = Color.White) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
                OverlayHeader(title = title, color = MonitorBrown, onClose = onClose, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ViewPeriod.entries.forEach { option ->
                        EventChip(option.label, isOn = option == period, onClick = { onPeriod(option) })
                    }
                    if (state.focusMs != null) {
                        Spacer(Modifier.weight(1f))
                        EventChip("Now", isOn = false, onClick = onNow)
                    }
                }
                state.focusMs?.let {
                    Text(
                        "Around the event of " + SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(it)),
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                state.note?.let { Text(it, color = BrandGold, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) }
                // A period centred on an older event needs its date as well.
                val withDate = period.durationMs >= DAY_MS || state.focusMs != null
                val timeFormat = remember(withDate) { SimpleDateFormat(if (withDate) "dd/MM HH:mm" else "HH:mm", Locale.getDefault()) }
                val startText = timeFormat.format(Date(state.startMs))
                val endText = timeFormat.format(Date(state.endMs))
                ServiceStopList(state.serviceStops) { timeFormat.format(Date(it)) }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    state.sections.forEach { section ->
                        item(key = "section-${section.title}") {
                            Text(
                                section.title,
                                color = MonitorBrown,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }
                        items(section.panels, key = { "${section.title}-${it.title}" }) { panel ->
                            GraphPanelView(panel, section.stoppedSpans, state.focusFraction, startText, endText)
                        }
                    }
                }
            }
        }
    }
}

/** The Sensor's service stops of the period, newest first, at most [MAX_STOPS_LISTED] (ISSUE-NOV15.6-O100 part 3). */
@Composable
private fun ServiceStopList(stops: List<ServiceStop>, timeText: (Long) -> String) {
    if (stops.isEmpty()) return
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Legend(if (stops.size == 1) "Sensor service stopped:" else "Sensor service stopped ${stops.size} times, newest:", STOPPED_COLOR)
        }
        stops.takeLast(MAX_STOPS_LISTED).reversed().forEach {
            Text(it.describe(timeText), color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, modifier = Modifier.padding(start = 11.dp))
        }
    }
}

@Composable
private fun GraphPanelView(panel: GraphPanel, stoppedSpans: List<GraphSpan>, focusFraction: Float?, startText: String, endText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(panel.title + if (panel.unit.isEmpty()) "" else " (${panel.unit})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(panel.summary, color = Slate500, fontSize = 11.sp, modifier = Modifier.weight(1f))
        }
        if (panel.lines.size > 1 || panel.band.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                panel.lines.forEach { Legend(it.label, toneColor(it.tone)) }
                if (panel.band.isNotEmpty()) Legend(panel.bandLabel, BAND_COLOR)
            }
        }
        Row {
            Column(modifier = Modifier.height(GRAPH_HEIGHT), verticalArrangement = Arrangement.SpaceBetween) {
                Text(axisText(panel.maxY), color = Slate500, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(axisText(panel.minY), color = Slate500, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.width(4.dp))
            Box(modifier = Modifier.weight(1f).height(GRAPH_HEIGHT)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawStoppedSpans(stoppedSpans)
                    drawPanel(panel)
                    focusFraction?.let { drawFocus(it) }
                }
                if (!panel.hasData) {
                    Text("No data", color = Slate500, fontSize = 11.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(startText, color = Slate500, fontSize = 9.sp, modifier = Modifier.weight(1f).padding(start = 28.dp))
            Text(endText, color = Slate500, fontSize = 9.sp)
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(3.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

private fun DrawScope.drawPanel(panel: GraphPanel) {
    val count = panel.lines.firstOrNull()?.points?.size ?: return
    if (count == 0) return
    val step = size.width / count
    // Charging: a shaded strip behind each charging point.
    panel.band.forEachIndexed { index, isOn ->
        if (isOn) drawRect(BAND_COLOR.copy(alpha = 0.18f), topLeft = Offset(index * step, 0f), size = Size(step, size.height))
    }
    // Top, middle and bottom grid lines.
    listOf(0f, size.height / 2f, size.height).forEach { y ->
        drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    }
    val range = panel.maxY - panel.minY
    fun y(value: Float) = size.height - (value - panel.minY) / range * size.height
    panel.lines.forEach { line -> drawGraphLine(line, step, ::y) }
}

/** The times the Sensor's service was stopped, shaded gray; at least a pixel wide, so a short stop in a week shows. */
private fun DrawScope.drawStoppedSpans(spans: List<GraphSpan>) {
    spans.forEach { span ->
        val left = span.start * size.width
        drawRect(STOPPED_COLOR.copy(alpha = 0.3f), topLeft = Offset(left, 0f), size = Size(maxOf(1f, (span.end - span.start) * size.width), size.height))
    }
}

/** The time of the event the Graphs were opened from (ISSUE-NOV15.6-O100 part 4). */
private fun DrawScope.drawFocus(fraction: Float) {
    val x = fraction * size.width
    drawLine(
        Color.White.copy(alpha = 0.7f),
        Offset(x, 0f),
        Offset(x, size.height),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
    )
}

private fun DrawScope.drawGraphLine(line: GraphLine, step: Float, y: (Float) -> Float) {
    val color = toneColor(line.tone)
    val path = Path()
    var isDrawing = false
    line.points.forEachIndexed { index, value ->
        if (value == null) {
            isDrawing = false
            return@forEachIndexed
        }
        val x = (index + 0.5f) * step
        val isAlone = line.points.getOrNull(index - 1) == null && line.points.getOrNull(index + 1) == null
        if (isAlone) drawCircle(color, radius = 2.dp.toPx(), center = Offset(x, y(value)))
        if (isDrawing) path.lineTo(x, y(value)) else path.moveTo(x, y(value))
        isDrawing = true
    }
    drawPath(
        path,
        color,
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = if (line.tone == GraphTone.LIMIT) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null
        )
    )
}

internal fun toneColor(tone: GraphTone): Color = when (tone) {
    GraphTone.MAIN -> MonitorBrown
    GraphTone.SECOND -> Color(0xFF60A5FA) // light blue: gold is too close to the brown main line (ISSUE-NOV19.2-O123)
    GraphTone.LIMIT -> Rose500
}

private fun axisText(value: Float): String =
    when {
        value == value.toLong().toFloat() || kotlin.math.abs(value) >= 100f -> value.toLong().toString()
        kotlin.math.abs(value) >= 1f -> "%.1f".format(Locale.US, value)
        else -> "%.2f".format(Locale.US, value)
    }

/** The menu's Graphs button: Material's "show chart" line, which the core icon set lacks. */
val GraphsIcon: ImageVector = materialIcon(name = "Filled.ShowChart") {
    materialPath {
        moveTo(3.5f, 18.49f)
        lineToRelative(6.0f, -6.01f)
        lineToRelative(4.0f, 4.0f)
        lineTo(22.0f, 6.92f)
        lineToRelative(-1.41f, -1.41f)
        lineToRelative(-7.09f, 7.97f)
        lineToRelative(-4.0f, -4.0f)
        lineTo(2.0f, 16.99f)
        close()
    }
}

private val GRAPH_HEIGHT = 80.dp
private val BAND_COLOR = StatusGreen
private val STOPPED_COLOR = Slate500
private const val MAX_STOPS_LISTED = 3
private const val DAY_MS = 24 * 3_600_000L
