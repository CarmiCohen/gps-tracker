package com.gps19.app

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*
import com.gps19.core.engine.*

/**
 * Shared UI Components for GPS Tracker.
 * Sep.05.25:
 * - Issue #266 RESOLVED: Automated Mali Driver Mitigation. Added MAL badge 
 *   and implemented UI-throttling to suppress high-frequency animations 
 *   during driver instability, preventing process-level ANRs (R-ID 266).
 * Sep.05.15:
 * - Issue #917 RESOLVED: Exact Actual Colors.
 */

enum class RibbonRenderType { BAR, LINE }

/**
 * StatusRowState: Grouped state for StatusRowData to reduce JIT compilation load (R883).
 */
@Stable
data class StatusRowState(
    val label: String,
    val battery: Int,
    val commIndex: Int,
    val color: Color,
    val isCharging: Boolean = false,
    val accuracy: Float = 0f,
    val maxAccuracy: Float = 0f,
    val satsView: Int = 0,
    val satsUsed: Int = 0,
    val gpsAgeMs: Long = -1L,
    val temp: Float = 0f,
    val distance: Double? = null,
    val horizontalPadding: androidx.compose.ui.unit.Dp = 1.dp,
    val isRemote: Boolean = false,
    val isPeerActive: Boolean = true,
    val overrideDistanceColor: Color? = null,
    val isLocPending: Boolean = false,
    val locPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isTelemetryFresh: Boolean = true,
    val isGpsFresh: Boolean = true,
    val isUltraLongStationary: Boolean = false,
    val isThrottled: Boolean = false
)

@Composable
fun RibbonsOverlay(
    isStrictMode: Boolean,
    replayCursorTs: Long?,
    history4MFlow: StateFlow<List<ConnectionPoint>>,
    history16MFlow: StateFlow<List<ConnectionPoint>>,
    history1HFlow: StateFlow<List<ConnectionPoint>>,
    history4HFlow: StateFlow<List<ConnectionPoint>>,
    history24HFlow: StateFlow<List<ConnectionPoint>>,
    history7DFlow: StateFlow<List<ConnectionPoint>>,
    onToggleStrictMode: (Boolean) -> Unit,
    onScrub: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    AnalyticalRibbons(
                        isStrictMode = isStrictMode,
                        replayCursorTs = replayCursorTs,
                        history4MFlow = history4MFlow,
                        history16MFlow = history16MFlow,
                        history1HFlow = history1HFlow,
                        history4HFlow = history4HFlow,
                        history24HFlow = history24HFlow,
                        history7DFlow = history7DFlow,
                        onToggleStrictMode = onToggleStrictMode,
                        onScrub = onScrub
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticalRibbons(
    isStrictMode: Boolean,
    replayCursorTs: Long?,
    history4MFlow: StateFlow<List<ConnectionPoint>>,
    history16MFlow: StateFlow<List<ConnectionPoint>>,
    history1HFlow: StateFlow<List<ConnectionPoint>>,
    history4HFlow: StateFlow<List<ConnectionPoint>>,
    history24HFlow: StateFlow<List<ConnectionPoint>>,
    history7DFlow: StateFlow<List<ConnectionPoint>>,
    onToggleStrictMode: (Boolean) -> Unit,
    onScrub: (Long?) -> Unit
) {
    var selectedScale by remember { mutableStateOf("4M") }
    
    val activeHistoryFlow = when(selectedScale) {
        "16M" -> history16MFlow
        "1H" -> history1HFlow
        "4H" -> history4HFlow
        "24H" -> history24HFlow
        "7D" -> history7DFlow
        else -> history4MFlow
    }

    val history by activeHistoryFlow.collectAsStateWithLifecycle()

    val snrSelector = remember { { p: ConnectionPoint -> p.snrIdx.toFloat() } }
    val noiseSelector = remember { { p: ConnectionPoint -> p.noiseIdx.toFloat() } }
    val kineticSelector = remember { { p: ConnectionPoint -> (p.kineticEnergy.toFloat() / RIBBON_KINETIC_ENERGY_SCALE.toFloat()).coerceIn(0f, 1f) } }
    val luxSelector = remember { { p: ConnectionPoint -> p.luxIdx.toFloat() } }
    val vibeSelector = remember { { p: ConnectionPoint -> p.vibeIdx.toFloat() } }
    val proxSelector = remember { { p: ConnectionPoint -> p.proxIdx.toFloat() } }
    val liftSelector = remember { { p: ConnectionPoint -> p.liftIdx.toFloat() } }
    val batSelector = remember { { p: ConnectionPoint -> if (p.isBatterySteepDischarge) 1f else 0f } }
    val thmSelector = remember { { p: ConnectionPoint -> if (p.isCoolingModeActive) 1f else 0f } }
    val curSelector = remember { { p: ConnectionPoint -> (kotlin.math.abs(p.currentMa).toFloat() / RIBBON_CURRENT_SCALE_MA.toFloat()).coerceIn(0f, 1f) } }
    val sitSelector = remember { { p: ConnectionPoint -> if (p.isSitActive) 1f else 0f } }
    val tltSelector = remember { { p: ConnectionPoint -> p.tiltIdx.toFloat() } }
    val baroSelector = remember { { p: ConnectionPoint -> p.baroIdx.toFloat() } }
    val svzSelector = remember { { p: ConnectionPoint -> (kotlin.math.abs(p.sitVz).toFloat() / 2.0f).coerceIn(0f, 1f) } }
    val svzDriftSelector = remember { { p: ConnectionPoint -> if (p.sitVzTs > 0) kotlin.math.abs(p.ts - p.sitVzTs) else 0L } }
    val sdzSelector = remember { { p: ConnectionPoint -> (kotlin.math.abs(p.sitDz).toFloat() / 0.5f).coerceIn(0f, 1f) } }
    
    val cpuSelector = remember { { p: ConnectionPoint -> (p.cpuLoad.toFloat() / RIBBON_CPU_LOAD_SCALE.toFloat()).coerceIn(0f, 1f) } }
    val iowSelector = remember { { p: ConnectionPoint -> (p.ioWait.toFloat() / RIBBON_IO_WAIT_SCALE.toFloat()).coerceIn(0f, 1f) } }
    val latSelector = remember { { p: ConnectionPoint -> (p.maxIoLatency.toFloat() / RIBBON_LATENCY_SCALE_MS.toFloat()).coerceIn(0f, 1f) } }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("4M", "16M", "1H", "4H", "24H", "7D").forEach { scale ->
                val isSelected = selectedScale == scale
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { selectedScale = scale }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent
                ) {
                    Text(
                        text = scale,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onToggleStrictMode(!isStrictMode) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (isStrictMode) Color.Red.copy(alpha = 0.3f) else Color.Transparent
            ) {
                Text(
                    text = "STRICT",
                    color = if (isStrictMode) Color.White else Color.Gray.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        ConnectionQualityRibbon(history, selectedScale, isStrictMode, replayCursorTs, onScrub)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))

        GenericSensorRibbon(history, "SNR", selectedScale, lineColor = Color(0xFF38BDF8), isStrictMode = isStrictMode, valueSelector = snrSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "NOI", selectedScale, lineColor = Amber500, isStrictMode = isStrictMode, valueSelector = noiseSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "KNT", selectedScale, lineColor = Color(0xFF4ADE80), isStrictMode = isStrictMode, valueSelector = kineticSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "LUX", selectedScale, lineColor = Color.White, isStrictMode = isStrictMode, valueSelector = luxSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "VIB", selectedScale, lineColor = Color.Magenta, isStrictMode = isStrictMode, valueSelector = vibeSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "PRX", selectedScale, lineColor = Rose500, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, valueSelector = proxSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "LIF", selectedScale, lineColor = Color(0xFFFACC15), isStrictMode = isStrictMode, valueSelector = liftSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "BAT", selectedScale, lineColor = Rose500, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, valueSelector = batSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "THM", selectedScale, lineColor = Color.Red, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, valueSelector = thmSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "CUR", selectedScale, lineColor = Color(0xFFFB923C), isStrictMode = isStrictMode, valueSelector = curSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "SIT", selectedScale, lineColor = BrandJd, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, valueSelector = sitSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "TLT", selectedScale, lineColor = Color(0xFF818CF8), isStrictMode = isStrictMode, valueSelector = tltSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "BAR", selectedScale, lineColor = Color(0xFF2DD4BF), isStrictMode = isStrictMode, valueSelector = baroSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "SVZ", selectedScale, lineColor = Violet500, isStrictMode = isStrictMode, valueSelector = svzSelector, driftSelector = svzDriftSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "SDZ", selectedScale, lineColor = Violet500, isStrictMode = isStrictMode, valueSelector = sdzSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))
        
        GenericSensorRibbon(history, "CPU", selectedScale, lineColor = Color(0xFF4ADE80), isStrictMode = isStrictMode, valueSelector = cpuSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "IOW", selectedScale, lineColor = Amber500, isStrictMode = isStrictMode, valueSelector = iowSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
        GenericSensorRibbon(history, "LAT", selectedScale, lineColor = Rose500, isStrictMode = isStrictMode, valueSelector = latSelector, replayCursorTs = replayCursorTs, onScrub = onScrub)
    }
}

@Composable
fun ForensicRibbonContainer(
    title: String,
    titleColor: Color,
    height: androidx.compose.ui.unit.Dp,
    history: List<ConnectionPoint>,
    scale: String,
    isStrictMode: Boolean = false,
    replayCursorTs: Long? = null,
    onScrub: (Long?) -> Unit = {},
    onDrawRibbon: DrawScope.(
        totalPoints: Float,
        pointWidth: Float,
        baseLineY: Float,
        maxHeight: Float,
        isLandscape: Boolean
    ) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val tickIntervalMs = remember(scale) {
        when(scale) {
            "7D" -> 2700 * 1000L; "24H" -> 360 * 1000L; "4H" -> 60 * 1000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 1000L
        }
    }
    val tickAlignMs = remember(scale) {
        when(scale) {
            "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 1L
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val density = LocalDensity.current
        Text(
            text = title, 
            color = titleColor.copy(alpha = 0.7f), 
            fontSize = 9.sp, 
            fontWeight = FontWeight.Black, 
            modifier = Modifier.width(28.dp).padding(start = 2.dp)
        )
        Box(modifier = Modifier
            .weight(1f)
            .height(height)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .pointerInput(history, scale) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val totalPoints = MAX_HISTORY_POINTS_PER_RIBBONS.toFloat()
                        val pointWidth = size.width / totalPoints
                        val startOffset = totalPoints - history.size
                        val index = ((offset.x / pointWidth) - startOffset).toInt()
                        if (index in history.indices) onScrub(history[index].ts)
                    },
                    onDrag = { change, _ ->
                        val totalPoints = MAX_HISTORY_POINTS_PER_RIBBONS.toFloat()
                        val pointWidth = size.width / totalPoints
                        val startOffset = totalPoints - history.size
                        val index = ((change.position.x / pointWidth) - startOffset).toInt()
                        if (index in history.indices) onScrub(history[index].ts)
                    },
                    onDragEnd = { onScrub(null) },
                    onDragCancel = { onScrub(null) }
                )
            }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                val totalPoints = MAX_HISTORY_POINTS_PER_RIBBONS.toFloat()
                val pointWidth = size.width / totalPoints
                val maxHeight = size.height * 0.8f
                val baseLineY = size.height * 0.9f
                val tickHeightPx = 2.dp.toPx()
                
                val gaps = Path()
                val strictGaps = Path()
                val ticks = Path()

                if (history.isNotEmpty()) {
                    val firstTs = history[0].ts
                    val baseTickTs = ((firstTs + tickAlignMs - 1) / tickAlignMs) * tickAlignMs
                    val startOffset = totalPoints - history.size

                    for (index in history.indices) {
                        val p = history[index]
                        val xPos = (startOffset + index) * pointWidth
                        
                        if (p.ts >= baseTickTs) {
                            val tickCount = (p.ts - baseTickTs) / tickIntervalMs
                            val prevTickCount = if (index > 0) {
                                if (history[index - 1].ts >= baseTickTs) (history[index - 1].ts - baseTickTs) / tickIntervalMs else -1L
                            } else -2L
                            
                            if (tickCount >= 0 && tickCount > prevTickCount) {
                                ticks.moveTo(xPos, baseLineY - maxHeight)
                                ticks.lineTo(xPos, baseLineY + tickHeightPx)
                            }
                        }

                        if (p.isGap) {
                            gaps.addRect(Rect(Offset(xPos, baseLineY - maxHeight), Size(maxOf(1f, pointWidth), maxHeight + tickHeightPx)))
                        } else if (isStrictMode && index > 0) {
                            val prev = history[index - 1]
                            if (!prev.isGap && (p.ts - prev.ts > tickIntervalMs * 2)) {
                                strictGaps.addRect(Rect(Offset(xPos - pointWidth, baseLineY - maxHeight), Size(maxOf(1f, pointWidth), maxHeight)))
                            }
                        }
                    }
                }

                onDrawWithContent {
                    drawContent()
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(0f, baseLineY),
                        end = Offset(size.width, baseLineY),
                        strokeWidth = 0.5.dp.toPx()
                    )
                    drawPath(ticks, Color.White.copy(alpha = 0.2f), style = Stroke(width = 1.dp.toPx()))
                    drawPath(gaps, Color.Black)
                    drawPath(strictGaps, Color.Red.copy(alpha = 0.4f))
                    
                    onDrawRibbon(totalPoints, pointWidth, baseLineY, maxHeight, isLandscape)

                    replayCursorTs?.let { cursorTs ->
                        val startOffset = totalPoints - history.size
                        val rawIndex = history.binarySearch { it.ts.compareTo(cursorTs) }
                        val index = if (rawIndex >= 0) rawIndex else -(rawIndex + 1)
                        
                        if (index in history.indices) {
                            val xPos = (startOffset + index) * pointWidth
                            drawLine(
                                color = Color.White,
                                start = Offset(xPos, 0f),
                                end = Offset(xPos, size.height),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun GenericSensorRibbon(
    history: List<ConnectionPoint>, 
    title: String, 
    scale: String,
    lineColor: Color, 
    renderType: RibbonRenderType = RibbonRenderType.LINE,
    isStrictMode: Boolean = false,
    replayCursorTs: Long? = null,
    onScrub: (Long?) -> Unit = {},
    valueSelector: (ConnectionPoint) -> Float,
    driftSelector: ((ConnectionPoint) -> Long)? = null
) {
    ForensicRibbonContainer(title, lineColor, if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 40.dp else 24.dp, history, scale, isStrictMode, replayCursorTs, onScrub) { totalPoints, pointWidth, baseLineY, maxHeight, landscape ->
        if (history.isEmpty()) return@ForensicRibbonContainer
        
        val startOffset = totalPoints - history.size
        val rectW = maxOf(1f, pointWidth)

        if (renderType == RibbonRenderType.BAR) {
            val barPath = Path()
            for (index in history.indices) {
                val p = history[index]
                if (p.isGap) continue
                val value = valueSelector(p).coerceIn(0f, 1f)
                if (value > 0.01f) {
                    val barHeight = value * maxHeight
                    barPath.addRect(Rect(Offset((startOffset + index) * pointWidth, baseLineY - barHeight), Size(rectW, barHeight)))
                }
            }
            drawPath(barPath, lineColor.copy(alpha = 0.6f))
        } else {
            val linePath = Path()
            val points = mutableListOf<Offset>()
            var lastPos: Offset? = null
            val gapLimitWidth = pointWidth * 10

            for (index in history.indices) {
                val p = history[index]
                if (p.isGap) { lastPos = null; continue }

                val value = valueSelector(p).coerceIn(0f, 1f)
                val currentPos = Offset((startOffset + index) * pointWidth + (rectW / 2f), baseLineY - (value * maxHeight))

                if (isStrictMode && driftSelector != null && driftSelector(p) > 2000L) {
                    drawRect(Color.Yellow.copy(alpha = 0.5f), Offset((startOffset + index) * pointWidth, baseLineY - maxHeight), Size(maxOf(2f, pointWidth), maxHeight * 0.1f))
                }

                if (lastPos != null && (currentPos.x - lastPos.x < gapLimitWidth)) linePath.lineTo(currentPos.x, currentPos.y)
                else linePath.moveTo(currentPos.x, currentPos.y)
                
                if (value > 0.05f) points.add(currentPos)
                lastPos = currentPos
            }
            drawPath(linePath, lineColor, style = Stroke(width = if (landscape) 1.5.dp.toPx() else 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (points.isNotEmpty()) drawPoints(points, PointMode.Points, lineColor, (if (landscape) 1.2.dp.toPx() else 0.8.dp.toPx()) * 2, StrokeCap.Round)
        }
    }
}

@Composable
fun ConnectionQualityRibbon(history: List<ConnectionPoint>, scale: String, isStrictMode: Boolean = false, replayCursorTs: Long? = null, onScrub: (Long?) -> Unit = {}) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val alignMs = remember(scale) { when(scale) { "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 1L } }
    val intervalMs = remember(scale) { when(scale) { "7D" -> 2700 * 1000L; "24H" -> 360 * 1000L; "4H" -> 60 * 1000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 0L } }
    val density = LocalDensity.current
    val textPaint = remember(isLandscape, density) { android.graphics.Paint().apply { color = android.graphics.Color.WHITE; with(density) { textSize = (if (isLandscape) 10.sp.toPx() else 7.sp.toPx()) }; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.MONOSPACE } }

    ForensicRibbonContainer(scale, Color.Gray, if (isLandscape) 60.dp else 34.dp, history, scale, isStrictMode, replayCursorTs, onScrub) { totalPoints, pointWidth, connectionBaseY, maxHeight, landscape ->
        if (history.isEmpty()) return@ForensicRibbonContainer
        val ribbonMaxHeight = if (landscape) 16.dp.toPx() else 10.dp.toPx()
        val effectiveBaseY = connectionBaseY - (if (landscape) 4.dp.toPx() else 2.dp.toPx())
        val startOffset = totalPoints - history.size
        val rectW = maxOf(1f, pointWidth)
        val firstTs = history[0].ts
        val baseTickTs = ((firstTs + alignMs - 1) / alignMs) * alignMs
        
        val connectedPath = Path()
        val disconnectedPath = Path()
        val gpsPath = Path()
        val gpsPoints = mutableListOf<Offset>()
        var lastGpsPos: Offset? = null

        for (index in history.indices) {
            val p = history[index]
            val xPos = (startOffset + index) * pointWidth
            if (!p.isGap) {
                val hFactor = if (p.isConnected) (TelemetryUtils.calculateCommIndex(p.rtt, p.remoteSig, p.localSig).toFloat() / 10f).coerceIn(0.1f, 1f) else 1f
                val r = Rect(Offset(xPos, effectiveBaseY - (ribbonMaxHeight * hFactor)), Size(rectW, ribbonMaxHeight * hFactor))
                if (p.isConnected) connectedPath.addRect(r) else disconnectedPath.addRect(r)
                if (isStrictMode && index > 0 && kotlin.math.abs((p.ts - p.rt) - (history[index-1].ts - history[index-1].rt)) > 2000L) {
                    drawRect(Color.Yellow.copy(alpha = 0.5f), Offset(xPos, effectiveBaseY - (ribbonMaxHeight * 1.5f)), Size(maxOf(2f, pointWidth), ribbonMaxHeight * 0.5f))
                }
                if (p.isRecoveryEvent) drawRect(Color.White.copy(alpha = 0.8f), Offset(xPos - (rectW * 0.5f), effectiveBaseY - maxHeight), Size(maxOf(2f, rectW), maxHeight + 4.dp.toPx()))
                if (p.hasGps && p.gpsIndex > 0.0) {
                    val yPos = effectiveBaseY - ribbonMaxHeight - (if (landscape) 4.dp.toPx() else 2.dp.toPx()) - (p.gpsIndex.toFloat().coerceIn(0f, 1f) * (if (landscape) 24.dp.toPx() else 14.dp.toPx()))
                    val currentPos = Offset(xPos + (rectW / 2f), yPos)
                    if (lastGpsPos != null && (currentPos.x - lastGpsPos.x < pointWidth * 10)) gpsPath.lineTo(currentPos.x, currentPos.y) else gpsPath.moveTo(currentPos.x, currentPos.y)
                    gpsPoints.add(currentPos); lastGpsPos = currentPos
                }
            } else lastGpsPos = null
            if (intervalMs > 0 && p.ts >= baseTickTs) {
                val tickCount = (p.ts - baseTickTs) / intervalMs
                if (tickCount >= 0 && tickCount > (if (index > 0 && history[index-1].ts >= baseTickTs) (history[index - 1].ts - baseTickTs) / intervalMs else -1L)) {
                    val timeStr = if (scale == "7D") dateFormatter.format(Date(p.ts)) else timeFormatter.format(Date(p.ts))
                    drawIntoCanvas { it.nativeCanvas.drawText(timeStr, xPos.coerceIn(18.dp.toPx(), size.width - 18.dp.toPx()), size.height - (if (landscape) 4.dp.toPx() else 2.dp.toPx()), textPaint) }
                }
            }
        }
        drawPath(connectedPath, BrandJd); drawPath(disconnectedPath, Rose500)
        drawPath(gpsPath, Color(0xFF00FFFF), style = Stroke(width = if (landscape) 1.dp.toPx() else 0.5.dp.toPx()))
        if (gpsPoints.isNotEmpty()) drawPoints(gpsPoints, PointMode.Points, Color(0xFF00FFFF), (if (landscape) 1.2.dp.toPx() else 0.8.dp.toPx()) * 2, StrokeCap.Round)
    }
}

@Composable
fun HeaderBar(
    isLogVisible: Boolean, isSettingsOpen: Boolean, isRibbonsVisible: Boolean, isMapVisible: Boolean, 
    isPhoneSetupVisible: Boolean = false, 
    requiresExtraTopPadding: Boolean, isSystemReady: Boolean, systemIssuesCount: Int, 
    onDashboard: () -> Unit = {}, onS: () -> Unit = {}, onL: () -> Unit = {}, 
    onM: () -> Unit = {}, onR: () -> Unit = {}, onEvent: (UiEvent) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isAnyOverlayOpen = isLogVisible || isSettingsOpen || isRibbonsVisible
    val isDashboardActive = !isMapVisible && !isAnyOverlayOpen
    val topPadding = if (requiresExtraTopPadding) 8.dp else 2.dp
    val commitAnd = { action: () -> Unit -> if (isSettingsOpen) onEvent(UiEvent.CommitSettings); action() }
    val alertPulse = rememberInfiniteTransition(label = "AlertPulse")
    
    // Issue #266: Animation Throttling for Mali Anomaly
    // Note: HeaderBar doesn't have hudState directly, but we use isSystemReady 
    // to gate critical animations if needed. In R-ID 266, we primarily throttle 
    // status animations in StatusBar.
    val alertAlpha by alertPulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse), label = "Alpha")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (isLandscape) {
            Column(
                modifier = Modifier.fillMaxHeight().width(48.dp).background(Color.Black.copy(alpha = 0.4f)), 
                verticalArrangement = Arrangement.Top, 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(onClick = { commitAnd(onS); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Settings button clicked", false)) }) { 
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = if (isSettingsOpen) Color.Gray else Color.White, modifier = Modifier.size(24.dp)) 
                }
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(onClick = { commitAnd(onDashboard); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Dashboard button clicked", false)) }, modifier = Modifier.size(44.dp)) { 
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Dashboard", tint = if (isDashboardActive) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                }
                IconButton(onClick = { commitAnd(onR) }, modifier = Modifier.size(44.dp)) { 
                    Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = if (isRibbonsVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!isSystemReady && !isPhoneSetupVisible) IconButton(onClick = { onEvent(UiEvent.TogglePhoneSetup(true)) }) { 
                    Box(contentAlignment = Alignment.Center) { 
                        Icon(imageVector = Icons.Default.ReportProblem, contentDescription = "System Issues", tint = Rose500.copy(alpha = alertAlpha), modifier = Modifier.size(28.dp))
                        Text(text = systemIssuesCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) 
                    } 
                }
                IconButton(onClick = { commitAnd(onL); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Log button clicked", false)) }) { 
                    Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null, tint = if (isLogVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                }
                IconButton(onClick = { commitAnd(onM) }) { 
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = if (isMapVisible && !isAnyOverlayOpen) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(top = topPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), 
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.Start
                ) { 
                    IconButton(onClick = { commitAnd(onS); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Settings button clicked", false)) }, modifier = Modifier.size(44.dp)) { 
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = if (isSettingsOpen) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = { commitAnd(onDashboard); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Dashboard button clicked", false)) }, modifier = Modifier.size(44.dp)) { 
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Dashboard", tint = if (isDashboardActive) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                    }
                    IconButton(onClick = { commitAnd(onR) }, modifier = Modifier.size(44.dp)) { 
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = if (isRibbonsVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (!isSystemReady && !isPhoneSetupVisible) IconButton(onClick = { onEvent(UiEvent.TogglePhoneSetup(true)) }, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { 
                            Icon(imageVector = Icons.Default.ReportProblem, contentDescription = "System Issues", tint = Rose500.copy(alpha = alertAlpha), modifier = Modifier.size(26.dp))
                            Text(text = systemIssuesCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) 
                        } 
                    }
                    IconButton(onClick = { commitAnd(onL); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Log button clicked", false)) }, modifier = Modifier.size(44.dp)) { 
                        Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null, tint = if (isLogVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                    }
                    IconButton(onClick = { commitAnd(onM) }, modifier = Modifier.size(44.dp)) { 
                        Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = if (isMapVisible && !isAnyOverlayOpen) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalStatusBar(
    hudState: HudState,
    modifier: Modifier = Modifier
) {
    StatusBar(hudState = hudState, modifier = modifier)
}

@Composable
fun StatusBar(
    hudState: HudState,
    modifier: Modifier = Modifier
) {
    val mode = hudState.appMode ?: return
    val lastGpsTs = if (mode == "viewer") hudState.lastGpsTs else hudState.viewerGpsTs
    
    val isPeerActive = hudState.isTelemetryFresh
    val isTrackerGpsActive = if (mode == "viewer") hudState.isGpsFresh else hudState.isLocalGpsActive

    val progressValue = if (hudState.health.progressPulse > 0f) hudState.health.progressPulse else 0f
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val trkIdLabel = hudState.trackerId.take(6).uppercase()
    val viewIdLabel = hudState.viewerId.take(6).uppercase()
    
    // Issue #266: Animation Throttling
    val isThrottled = hudState.isMaliAnomaly
    val infiniteTransition = rememberInfiniteTransition(label = "StatusBarAnimations")
    val alarmAlpha by if (isThrottled) remember { mutableStateOf(1f) } else infiniteTransition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse), label = "AlarmAlpha")
    val movingAlpha by if (isThrottled) remember { mutableStateOf(1f) } else infiniteTransition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse), label = "MovingAlpha")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(modifier = modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isLandscape) 0.7f else 0.9f)), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 3.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusBadge(label = "SYS", active = hudState.isSystemActive, isBold = true)
                        StatusBadge(label = "INT", active = hudState.isInternet, isBold = true)
                        StatusBadge(label = "SRV", active = hudState.isRelayConnected, isBold = true)
                        StatusBadge(label = "GPS", active = hudState.isLocalGpsActive)
                        StatusBadge(label = if (mode == "tracker") "VWR" else "TRK", active = isPeerActive, activeColor = BrandJd)
                        StatusBadge(label = "DAT", active = hudState.isDataHealthy)
                        StatusBadge(label = "WDG", active = hudState.watchdogOk, isBold = true)
                        
                        // Issue #266: Mali Anomaly Indicator
                        if (hudState.isMaliAnomaly) StatusBadge(label = "MAL", active = true, activeColor = Rose500, isBold = true)
                        
                        if (hudState.hasActiveAlarms) StatusBadge(label = "ALM", active = true, activeColor = Rose500.copy(alpha = alarmAlpha), isBold = true)
                        if (hudState.isRedScreenSuppressed) {
                             Spacer(modifier = Modifier.width(2.dp))
                             Box(modifier = Modifier.background(if (hudState.isSirenPlaying) Rose500.copy(alpha = alarmAlpha) else Slate500, RoundedCornerShape(2.dp)).padding(horizontal = 2.dp)) {
                                 Text(text = if (hudState.isSirenPlaying) "SIREN LOCKOUT" else "LOCKOUT", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, style = compactStyle)
                             }
                        }
                        
                        // Issue #266: Throttle circular progress indicator
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp)) { 
                            if (!isThrottled) {
                                CircularProgressIndicator(progress = { progressValue }, modifier = Modifier.size(16.dp), color = if (hudState.isDataHealthy) BrandJd else Rose500, strokeWidth = 2.dp)
                            }
                            Icon(imageVector = if (hudState.isDataHealthy) Icons.Default.CheckCircle else Icons.Default.Error, contentDescription = null, modifier = Modifier.size(8.dp), tint = if (hudState.isDataHealthy) BrandJd else Rose500)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (hudState.trackerState == TrackerState.MOVING && isTrackerGpsActive) "»\u2009${hudState.trackerState.name}\u2009«" else hudState.trackerState.name, color = (if (!isTrackerGpsActive) Slate500 else BrandJd).copy(alpha = if (hudState.trackerState == TrackerState.MOVING && isTrackerGpsActive) movingAlpha else 1f), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                    Spacer(modifier = Modifier.weight(1f))
                    val animatedSpeed by animateFloatAsState(if (isTrackerGpsActive && !hudState.speedMps.isNaN()) hudState.speedMps * 3.6f else 0f, if (isThrottled) snap() else tween(1000), label = "SpeedAnim")
                    Text(text = "${if (animatedSpeed < 10.0f) String.format(Locale.getDefault(), "%.1f", animatedSpeed) else animatedSpeed.toInt().toString()}km/h", color = if (isTrackerGpsActive) BrandJd else Slate500, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle, textAlign = TextAlign.End)
                }
                Spacer(modifier = Modifier.height(3.dp))
                if (isLandscape && mode == "viewer") {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        val vAge = if(hudState.viewerGpsTs > 0) hudState.systemPulse - hudState.viewerGpsTs else -1L
                        Box(modifier = Modifier.weight(1f)) { 
                            StatusRowData(StatusRowState(label = viewIdLabel, battery = hudState.battery, commIndex = hudState.commIndex, color = ViewerCyan, overrideDistanceColor = BrandJd, isCharging = hudState.isCharging, accuracy = hudState.viewerAccuracy, maxAccuracy = hudState.maxViewerAccuracy, temp = hudState.viewerTemp, distance = hudState.distToViewer, satsUsed = hudState.viewerSatsUsed, satsView = hudState.viewerSatsView, gpsAgeMs = vAge, isRemote = false, isLocPending = hudState.isViewerLocPending, locPendingReason = hudState.viewerLocPendingReason, isTelemetryFresh = hudState.viewerGpsTs > 0 && (hudState.systemPulse - hudState.viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS), isGpsFresh = vAge in 0..GPS_UI_FAIL_THRESHOLD_MS, isThrottled = isThrottled)) 
                        }
                        val tAge = if(lastGpsTs > 0) hudState.systemPulse - lastGpsTs else -1L
                        Box(modifier = Modifier.weight(1f)) { 
                            StatusRowData(StatusRowState(label = trkIdLabel, battery = hudState.battery, commIndex = if(isPeerActive) hudState.remoteCommIndex else 0, color = if(isPeerActive) BrandJd else Slate500, isCharging = hudState.remoteCharging, accuracy = hudState.trackerAccuracy, maxAccuracy = hudState.maxTrackerAccuracy, satsView = hudState.satsView, satsUsed = hudState.satsUsed, gpsAgeMs = tAge, temp = hudState.trackerTemp, distance = hudState.distToHome, isRemote = true, isPeerActive = isPeerActive, isLocPending = hudState.isTrackerLocPending, locPendingReason = hudState.trackerLocPendingReason, isTelemetryFresh = isPeerActive, isGpsFresh = isTrackerGpsActive, isUltraLongStationary = hudState.isUltraLongStationary, isThrottled = isThrottled)) 
                        }
                    }
                } else {
                    if (mode == "viewer") {
                        val vAge = if(hudState.viewerGpsTs > 0) hudState.systemPulse - hudState.viewerGpsTs else -1L
                        StatusRowData(StatusRowState(label = viewIdLabel, battery = hudState.battery, commIndex = hudState.commIndex, color = ViewerCyan, overrideDistanceColor = BrandJd, isCharging = hudState.isCharging, accuracy = hudState.viewerAccuracy, maxAccuracy = hudState.maxViewerAccuracy, temp = hudState.viewerTemp, distance = hudState.distToViewer, satsUsed = hudState.viewerSatsUsed, satsView = hudState.viewerSatsView, gpsAgeMs = vAge, horizontalPadding = 8.dp, isLocPending = hudState.isViewerLocPending, locPendingReason = hudState.viewerLocPendingReason, isTelemetryFresh = hudState.viewerGpsTs > 0 && (hudState.systemPulse - hudState.viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS), isGpsFresh = vAge in 0..GPS_UI_FAIL_THRESHOLD_MS, isThrottled = isThrottled))
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    val tAge = if(lastGpsTs > 0) hudState.systemPulse - lastGpsTs else -1L
                    StatusRowData(StatusRowState(label = trkIdLabel, battery = if (mode == "viewer") hudState.remoteBattery else hudState.battery, commIndex = if (mode == "viewer") (if(isPeerActive) hudState.remoteCommIndex else 0) else hudState.commIndex, color = if (mode == "viewer" && !isPeerActive) Slate500 else BrandJd, isCharging = if (mode == "viewer") hudState.remoteCharging else hudState.isCharging, accuracy = if (mode == "viewer") hudState.trackerAccuracy else hudState.trackerAccuracy, maxAccuracy = if (mode == "viewer") hudState.maxTrackerAccuracy else hudState.maxTrackerAccuracy, satsView = hudState.satsView, satsUsed = hudState.satsUsed, gpsAgeMs = tAge, temp = hudState.trackerTemp, distance = hudState.distToHome, horizontalPadding = 8.dp, isRemote = mode == "viewer", isPeerActive = if(mode == "viewer") isPeerActive else true, isLocPending = hudState.isTrackerLocPending, locPendingReason = hudState.trackerLocPendingReason, isTelemetryFresh = if (mode == "tracker") (hudState.viewerGpsTs > 0 && (hudState.systemPulse - hudState.viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS)) else isPeerActive, isGpsFresh = isTrackerGpsActive, isUltraLongStationary = hudState.isUltraLongStationary, isThrottled = isThrottled))
                }
            }
        }
    }
}

/**
 * Issue #883: Refactored to StatusRowState to group 22 parameters and reduce JIT 
 * compilation load during Level 8 hydration.
 */
@Composable
fun StatusRowData(state: StatusRowState) {
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val isConnStale = state.isRemote && !state.isPeerActive
    val telemetryColor = if (state.isTelemetryFresh && !isConnStale) state.color else Slate500
    val contentColor = if (isConnStale) Slate500 else state.color
    val distColor = if (state.isTelemetryFresh && !isConnStale) (state.overrideDistanceColor ?: state.color) else Slate500
    
    // Issue #266: Throttling
    val infiniteTransition = rememberInfiniteTransition(label = "HandshakeAnimations")
    val handshakeAlpha by if (state.isThrottled) remember { mutableStateOf(1f) } else infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(1200), repeatMode = RepeatMode.Reverse), label = "HandshakeAlpha")
    val animatedBattery by animateIntAsState(state.battery, if (state.isThrottled) snap() else tween(1500), label = "BatteryAnim")

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = state.horizontalPadding), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.width(204.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.width(42.dp), verticalAlignment = Alignment.CenterVertically) {
                 val alpha by animateFloatAsState(if (isConnStale) 0.5f else 1f, label = "LabelAlpha")
                 Text(text = state.label, color = contentColor.copy(alpha = if (isConnStale) handshakeAlpha else alpha), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                 if (state.isLocPending) Box(modifier = Modifier.padding(start = 1.dp).background(Amber500, RoundedCornerShape(1.dp)).padding(horizontal = 1.dp)) { 
                     Text(text = "P", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold, style = compactStyle) 
                 }
            }
            if (isConnStale) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = ">>> WAITING FOR TELEMETRY <<<", color = Slate500.copy(alpha = handshakeAlpha), fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = compactStyle, modifier = Modifier.padding(horizontal = 2.dp))
            } else {
                Row(modifier = Modifier.width(54.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.width(10.dp)) { 
                        if (state.isCharging) Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(10.dp), tint = if(!isConnStale && state.isTelemetryFresh) Amber500 else Slate500) 
                    }
                    Icon(imageVector = if (state.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull, contentDescription = null, modifier = Modifier.size(10.dp), tint = if (isConnStale || !state.isTelemetryFresh) Slate500 else if (state.battery in 0..19) Rose500 else telemetryColor)
                    Spacer(modifier = Modifier.width(2.dp)); Text(text = if(state.battery >= 0) "$animatedBattery%" else "--%", color = telemetryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
                Row(modifier = Modifier.width(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "°", color = telemetryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp), style = compactStyle)
                    Text(text = String.format(Locale.getDefault(), "%.0f", state.temp), color = telemetryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.width(20.dp)) { CommBar(index = state.commIndex, color = if (state.isTelemetryFresh) contentColor else Slate500) }
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.width(34.dp)) { Text(text = "${state.satsUsed}/${state.satsView}", color = telemetryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle) }
                Box(modifier = Modifier.width(26.dp)) {
                    val ageStr = if (state.gpsAgeMs != -1L) { val ageSec = (maxOf(0L, state.gpsAgeMs) / 1000).toInt(); when { ageSec < 100 -> "${ageSec}s"; ageSec < 3600 -> "${ageSec/60}m"; else -> ">1h" } } else "--s"
                    Text(text = ageStr, color = if (!state.isGpsFresh) Slate500 else state.color, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
            }
        }
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            if (!isConnStale) {
                if (state.isUltraLongStationary) {
                    Box(modifier = Modifier.background(BrandJd.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).padding(horizontal = 2.dp)) {
                        Text(text = "[ULTRA]", color = BrandJd, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = compactStyle)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (state.isLocPending && state.locPendingReason != LocationPendingReason.NONE) {
                    Text(text = state.locPendingReason.name.replace("_", " "), color = Amber500, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible, modifier = Modifier.weight(1f, fill = false), textAlign = TextAlign.End, style = compactStyle)
                } else {
                    fun formatAcc(v: Float): String = when { v >= 10000f -> "${(v / 1000).toInt()}k"; v >= 1000f -> String.format(Locale.getDefault(), "%.1fk", v / 1000f); else -> v.toInt().toString() }
                    val accColor = if (state.isTelemetryFresh) (if (!state.isGpsFresh) Slate500 else state.color) else Slate500
                    val accText = "${if (state.accuracy > 0) "±${formatAcc(state.accuracy)}" else ""} ${if (state.maxAccuracy > 0) "(±${formatAcc(state.maxAccuracy)})" else ""}".trim()
                    if (accText.isNotEmpty()) Text(text = accText, color = accColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false), textAlign = TextAlign.End, style = compactStyle)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.width(62.dp)) {
                    val animatedDistance by animateFloatAsState(if (state.distance == null || state.distance.isNaN()) 0f else state.distance.toFloat(), if (state.isThrottled) snap() else tween(1200), label = "DistAnim")
                    val distStr = when { state.distance == null || state.distance.isNaN() -> "--"; animatedDistance >= 9000 -> String.format(Locale.getDefault(), "%.0fkm", animatedDistance / 1000.0); animatedDistance >= 1000 -> String.format(Locale.getDefault(), "%.1fkm", animatedDistance / 1000.0); else -> "${animatedDistance.toInt()}m" }
                    Text(text = distStr, color = distColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, style = compactStyle, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
fun CommBar(index: Int, color: Color) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(0.6.dp)) { repeat(10) { i -> Box(modifier = Modifier.width(1.2.dp).height((2.5 + (i * 0.8)).dp).background(if (i < index) color else Slate500.copy(alpha = 0.6f))) } }
}

@Composable
fun StatusBadge(label: String, active: Boolean, activeColor: Color = BrandJd, isBold: Boolean = true) {
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    Text(text = label, color = if (active) activeColor else Rose500, fontSize = 9.sp, fontWeight = if(isBold) FontWeight.ExtraBold else FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, style = compactStyle)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun HeaderBarPreview() {
    HeaderBar(isLogVisible = false, isSettingsOpen = false, isRibbonsVisible = false, isMapVisible = true, requiresExtraTopPadding = false, isSystemReady = true, systemIssuesCount = 0, onEvent = {})
}
