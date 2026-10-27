package com.gps19.app

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
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
import com.gps19.core.engine.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Shared UI Components for GPS Tracker.
 * Oct.27.5:
 * - GAP-OCT27.4-1: Unit-Based Badge Scaling. Transitioned HUD badges to use 
 *   a shared MeasurableBadge component to reduce redundant Compose declarations.
 * Oct.27.0:
 * - GAP-OCT26.5-1: Added conflation counter overlay to DAT badge and long-press 
 *   diagnostic feedback for signaling pipeline transparency.
 * Oct.26.6:
 * - SIMP-OCT26.6-S1: Diagnostic Latch for SRV Badge. SRV badge turns Orange 
 *   when a connection is aborted (isConnectionAborted) to provide 5s failure visibility.
 */

enum class RibbonRenderType { BAR, LINE }

@Stable
data class StatusRowState(
    val label: String, val battery: Int, val commIndex: Int, val color: Color,
    val isCharging: Boolean = false, val accuracy: Float = 0f, val maxAccuracy: Float = 0f,
    val satsView: Int = -1, val satsUsed: Int = -1, val gpsAgeMs: Long = -1L,
    val temp: Float = 0f, val distance: Double? = null,
    val horizontalPadding: androidx.compose.ui.unit.Dp = 1.dp,
    val isRemote: Boolean = false, val isPeerActive: Boolean = true,
    val overrideDistanceColor: Color? = null, val isLocPending: Boolean = false,
    val locPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isTelemetryFresh: Boolean = true, val isGpsFresh: Boolean = true,
    val isUltraLongStationary: Boolean = false
)

@Composable
fun RibbonsOverlay(
    stateProvider: UiStateProvider,
    onEvent: (UiEvent) -> Unit
) {
    val navState by stateProvider.navigation.collectAsStateWithLifecycle()
    
    Dialog(onDismissRequest = { onEvent(UiEvent.ToggleRibbons(false)) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalBackAction provides { onEvent(UiEvent.ToggleRibbons(false)) }) {
            BackPressInterceptor()
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.95f)) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        AnalyticalRibbons(
                            stateProvider = stateProvider,
                            isStrictMode = navState.isStrictMode,
                            replayCursorTs = navState.replayCursorTs,
                            onEvent = onEvent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticalRibbons(
    stateProvider: UiStateProvider,
    isStrictMode: Boolean,
    replayCursorTs: Long?,
    onEvent: (UiEvent) -> Unit
) {
    var selectedScale by remember { mutableStateOf("4M") }
    
    val activeHistoryFlow = when(selectedScale) {
        "16M" -> stateProvider.history16M
        "1H" -> stateProvider.history1H
        "4H" -> stateProvider.history4H
        "24H" -> stateProvider.history24H
        "7D" -> stateProvider.history7D
        else -> stateProvider.history4M
    }

    val history by activeHistoryFlow.collectAsStateWithLifecycle()

    val amberColor = Color(0xFFF59E0B)
    val roseColor = Color(0xFFF43F5E)
    val violetColor = Color(0xFF8B5CF6)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            listOf("4M", "16M", "1H", "4H", "24H", "7D").forEach { scale ->
                val isSelected = selectedScale == scale
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { selectedScale = scale }.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent
                ) {
                    Text(text = scale, color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium, fontFamily = FontFamily.Monospace)
                }
            }
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onEvent(UiEvent.ToggleStrictMode(!isStrictMode)) }.padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (isStrictMode) Color.Red.copy(alpha = 0.3f) else Color.Transparent
            ) {
                Text(text = "STRICT", color = if (isStrictMode) Color.White else Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
        }

        ConnectionQualityRibbon(history, "CON", selectedScale, isStrictMode, replayCursorTs, onEvent)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))

        GenericSensorRibbon(history, "SNR", selectedScale, lineColor = Color(0xFF38BDF8), isStrictMode = isStrictMode, normValues = history.snrNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "NOI", selectedScale, lineColor = amberColor, isStrictMode = isStrictMode, normValues = history.noiseNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "KNT", selectedScale, lineColor = Color(0xFF4ADE80), isStrictMode = isStrictMode, normValues = history.kineticNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "LUX", selectedScale, lineColor = Color.White, isStrictMode = isStrictMode, normValues = history.luxNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "VIB", selectedScale, lineColor = Color.Magenta, isStrictMode = isStrictMode, normValues = history.vibeNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "PRX", selectedScale, lineColor = roseColor, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, normValues = history.proxNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "LIF", selectedScale, lineColor = Color(0xFFFACC15), isStrictMode = isStrictMode, normValues = history.liftNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "BAT", selectedScale, lineColor = roseColor, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, normValues = history.batNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "THM", selectedScale, lineColor = Color.Red, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, normValues = history.thmNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "CUR", selectedScale, lineColor = Color(0xFFFB923C), isStrictMode = isStrictMode, normValues = history.curNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "SIT", selectedScale, lineColor = Color(0xFFF97316), renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, normValues = history.sitNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "TLT", selectedScale, lineColor = Color(0xFF818CF8), isStrictMode = isStrictMode, normValues = history.tltNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "BAR", selectedScale, lineColor = Color(0xFF2DD4BF), isStrictMode = isStrictMode, normValues = history.baroNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "SVZ", selectedScale, lineColor = violetColor, isStrictMode = isStrictMode, normValues = history.svzNorm, driftSelector = { b, i -> if (b.sitVzTs[i] > 0) kotlin.math.abs(b.ts[i] - b.sitVzTs[i]) else 0L }, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "SDZ", selectedScale, lineColor = violetColor, isStrictMode = isStrictMode, normValues = history.sdzNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))
        GenericSensorRibbon(history, "CPU", selectedScale, lineColor = Color(0xFF4ADE80), isStrictMode = isStrictMode, normValues = history.cpuNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "IOW", selectedScale, lineColor = amberColor, isStrictMode = isStrictMode, normValues = history.iowNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
        GenericSensorRibbon(history, "LAT", selectedScale, lineColor = roseColor, isStrictMode = isStrictMode, normValues = history.latNorm, replayCursorTs = replayCursorTs, onEvent = onEvent)
    }
}

@Composable
fun ForensicRibbonContainer(
    title: String, 
    titleColor: Color, 
    height: androidx.compose.ui.unit.Dp, 
    history: HistoryBuffer, 
    scale: String, 
    isStrictMode: Boolean = false, 
    replayCursorTs: Long? = null, 
    onEvent: (UiEvent) -> Unit = {},
    onDrawRibbon: DrawScope.(totalPoints: Float, pointWidth: Float, baseLineY: Float, maxHeight: Float, isLandscape: Boolean) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val tickIntervalMs = remember(scale) { 
        when(scale) { 
            "7D" -> 24 * 3600 * 1000L; "24H" -> 6 * 3600 * 1000L; "4H" -> 3600 * 1000L; 
            "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 60000L; else -> 1000L 
        } 
    }
    val tickAlignMs = remember(scale) { 
        when(scale) { 
            "7D" -> 24 * 3600 * 1000L; "24H" -> 6 * 3600 * 1000L; "4H" -> 3600 * 1000L; 
            "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 60000L; else -> 1L 
        } 
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title, 
            color = titleColor.copy(alpha = 0.7f), 
            fontSize = 9.sp, 
            fontWeight = FontWeight.Black, 
            modifier = Modifier.width(34.dp).padding(start = 2.dp), 
            maxLines = 1
        )
        Box(
            modifier = Modifier
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
                            if (index in 0 until history.size) onEvent(UiEvent.SetReplayCursor(history.ts[index])) 
                        }, 
                        onDrag = { change, _ -> 
                            val totalPoints = MAX_HISTORY_POINTS_PER_RIBBONS.toFloat()
                            val pointWidth = size.width / totalPoints
                            val startOffset = totalPoints - history.size
                            val index = ((change.position.x / pointWidth) - startOffset).toInt()
                            if (index in 0 until history.size) onEvent(UiEvent.SetReplayCursor(history.ts[index])) 
                        }, 
                        onDragEnd = { onEvent(UiEvent.SetReplayCursor(null)) }, 
                        onDragCancel = { onEvent(UiEvent.SetReplayCursor(null)) }
                    ) 
                }
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithCache {
                    val totalPoints = MAX_HISTORY_POINTS_PER_RIBBONS.toFloat()
                    val pointWidth = size.width / totalPoints
                    val isTimeRuler = title == "CON"
                    val baseLineY = size.height - (if (isTimeRuler) (if (isLandscape) 22.dp.toPx() else 18.dp.toPx()) else size.height * 0.1f)
                    val maxHeight = if (isTimeRuler) baseLineY * 0.75f else size.height * 0.8f
                    val tickHeightPx = if (isTimeRuler) 5.dp.toPx() else 2.dp.toPx()
                    val gaps = Path()
                    val strictGaps = Path()
                    val ticks = Path()
                    if (history.size > 0) {
                        val firstTs = history.ts[0]
                        val baseTickTs = ((firstTs + tickAlignMs - 1) / tickAlignMs) * tickAlignMs
                        val startOffset = totalPoints - history.size
                        for (index in 0 until history.size) {
                            val pTs = history.ts[index]
                            val pIsGap = history.isGap[index]
                            val xPos = (startOffset + index) * pointWidth
                            if (pTs >= baseTickTs) {
                                val tickCount = (pTs - baseTickTs) / tickIntervalMs
                                val prevTickCount = if (index > 0) { 
                                    if (history.ts[index - 1] >= baseTickTs) (history.ts[index - 1] - baseTickTs) / tickIntervalMs else -1L 
                                } else -2L
                                if (tickCount >= 0 && tickCount > prevTickCount) { 
                                    ticks.moveTo(xPos, baseLineY - maxHeight)
                                    ticks.lineTo(xPos, baseLineY + tickHeightPx) 
                                }
                            }
                            if (pIsGap) {
                                gaps.addRect(Rect(Offset(xPos, baseLineY - maxHeight), Size(maxOf(1f, pointWidth), maxHeight + tickHeightPx))) 
                            } else if (isStrictMode && index > 0) { 
                                val prevTs = history.ts[index - 1]
                                val prevIsGap = history.isGap[index - 1]
                                if (!prevIsGap && (pTs - prevTs > tickIntervalMs * 2)) {
                                    strictGaps.addRect(Rect(Offset(xPos - pointWidth, baseLineY - maxHeight), Size(maxOf(1f, pointWidth), maxHeight))) 
                                }
                            }
                        }
                    }
                    onDrawWithContent {
                        drawContent()
                        drawLine(color = Color.White.copy(alpha = 0.1f), start = Offset(0f, baseLineY), end = Offset(size.width, baseLineY), strokeWidth = 0.5.dp.toPx())
                        drawPath(ticks, Color.White.copy(alpha = 0.2f), style = Stroke(width = 1.dp.toPx().coerceAtLeast(1f)))
                        drawPath(gaps, Color.Black)
                        drawPath(strictGaps, Color.Red.copy(alpha = 0.4f))
                        onDrawRibbon(totalPoints, pointWidth, baseLineY, maxHeight, isLandscape)
                        replayCursorTs?.let { cursorTs -> 
                            val startOffset = totalPoints - history.size
                            val rawIndex = history.ts.binarySearch(cursorTs)
                            val index = if (rawIndex >= 0) rawIndex else -(rawIndex + 1)
                            if (index in 0 until history.size) { 
                                val xPos = (startOffset + index) * pointWidth
                                drawLine(color = Color.White, start = Offset(xPos, 0f), end = Offset(xPos, size.height), strokeWidth = 1.5.dp.toPx()) 
                            } 
                        }
                    }
                }
        )
    }
}

@Composable
fun GenericSensorRibbon(
    history: HistoryBuffer, 
    title: String, 
    scale: String, 
    lineColor: Color, 
    renderType: RibbonRenderType = RibbonRenderType.LINE, 
    isStrictMode: Boolean = false, 
    replayCursorTs: Long? = null, 
    onEvent: (UiEvent) -> Unit = {},
    normValues: FloatArray, 
    driftSelector: ((HistoryBuffer, Int) -> Long)? = null
) {
    val sensorHeight = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 40.dp else 24.dp
    ForensicRibbonContainer(title, lineColor, sensorHeight, history, scale, isStrictMode, replayCursorTs, onEvent) { totalPoints, pointWidth, baseLineY, maxHeight, landscape ->
        if (history.size == 0 || normValues.size < history.size) return@ForensicRibbonContainer
        val startOffset = totalPoints - history.size
        val rectW = maxOf(1f, pointWidth)
        if (renderType == RibbonRenderType.BAR) {
            val barPath = Path()
            for (index in 0 until history.size) { 
                if (history.isGap[index]) continue
                val value = normValues[index]
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
            for (index in 0 until history.size) { 
                val xPos = (startOffset + index) * pointWidth
                if (history.isGap[index]) { 
                    lastPos = null
                    continue 
                }
                val value = normValues[index]
                val currentPos = Offset(xPos + (rectW / 2f), baseLineY - (value * maxHeight))
                if (isStrictMode && driftSelector != null && driftSelector(history, index) > 2000L) {
                    drawRect(Color.Yellow.copy(alpha = 0.5f), Offset((startOffset + index) * pointWidth, baseLineY - maxHeight), Size(maxOf(2f, pointWidth), maxHeight * 0.1f))
                }
                if (lastPos != null && (currentPos.x - lastPos.x < gapLimitWidth)) {
                    linePath.lineTo(currentPos.x, currentPos.y) 
                } else {
                    linePath.moveTo(currentPos.x, currentPos.y)
                }
                if (value > 0.05f) points.add(currentPos)
                lastPos = currentPos 
            }
            val lineWidth = if (landscape) 1.5.dp.toPx() else 1.dp.toPx()
            drawPath(linePath, lineColor, style = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (points.isNotEmpty()) {
                val pointRadius = (if (landscape) 1.2.dp.toPx() else 0.8.dp.toPx()) * 2
                drawPoints(points, PointMode.Points, lineColor, pointRadius, StrokeCap.Round)
            }
        }
    }
}

@Composable
fun ConnectionQualityRibbon(
    history: HistoryBuffer, 
    title: String, 
    scale: String, 
    isStrictMode: Boolean = false, 
    replayCursorTs: Long? = null, 
    onEvent: (UiEvent) -> Unit = {}
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val alignMs = remember(scale) { 
        when(scale) { 
            "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 3600000L; 
            "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 60000L; else -> 1L 
        } 
    }
    val intervalMs = remember(scale) { 
        when(scale) { 
            "7D" -> 24 * 3600 * 1000L; "24H" -> 6 * 3600 * 1000L; "4H" -> 3600 * 1000L; 
            "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 60000L; else -> 0L 
        } 
    }
    val density = LocalDensity.current
    val textPaint = remember(isLandscape, density) { 
        android.graphics.Paint().apply { 
            color = android.graphics.Color.WHITE
            with(density) { textSize = (if (isLandscape) 10.sp.toPx() else 7.sp.toPx()) }
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE 
        } 
    }
    val ribbonHeight = if (isLandscape) 68.dp else 50.dp
    ForensicRibbonContainer(title, Color.Gray, ribbonHeight, history, scale, isStrictMode, replayCursorTs, onEvent) { totalPoints, pointWidth, connectionBaseY, maxHeight, landscape ->
        if (history.size == 0) return@ForensicRibbonContainer
        val ribbonMaxHeight = if (landscape) 14.dp.toPx() else 8.dp.toPx()
        val effectiveBaseY = connectionBaseY - (if (landscape) 6.dp.toPx() else 4.dp.toPx())
        val startOffset = totalPoints - history.size
        val rectW = maxOf(1f, pointWidth)
        val firstTs = history.ts[0]
        val baseTickTs = ((firstTs + alignMs - 1) / alignMs) * alignMs
        val connectedPath = Path()
        val disconnectedPath = Path()
        val gpsPath = Path()
        val gpsPoints = mutableListOf<Offset>()
        var lastGpsPos: Offset? = null
        for (index in 0 until history.size) { 
            val pTs = history.ts[index]
            val pRt = history.rt[index]
            val pIsConnected = history.isConnected[index]
            val pIsGap = history.isGap[index]
            val pIsRecoveryEvent = history.isRecoveryEvent[index]
            val pHasGps = history.hasGps[index]
            val pGpsIndex = history.gpsIndex[index]

            val xPos = (startOffset + index) * pointWidth
            if (!pIsGap) { 
                // SIMP-OCT15.8-F1: Consume pre-calculated normalization from HistoryBuffer directly.
                val hFactor = history.commNorm[index]
                val r = Rect(Offset(xPos, effectiveBaseY - (ribbonMaxHeight * hFactor)), Size(rectW, ribbonMaxHeight * hFactor))
                if (pIsConnected) connectedPath.addRect(r) else disconnectedPath.addRect(r)
                
                if (isStrictMode && index > 0) {
                    val prevTs = history.ts[index - 1]
                    val prevRt = history.rt[index - 1]
                    if (kotlin.math.abs((pTs - pRt) - (prevTs - prevRt)) > 2000L) {
                        drawRect(Color.Yellow.copy(alpha = 0.5f), Offset(xPos, effectiveBaseY - (ribbonMaxHeight * 1.5f)), Size(maxOf(2f, pointWidth), ribbonMaxHeight * 0.5f))
                    }
                }
                
                if (pIsRecoveryEvent) {
                    drawRect(Color.White.copy(alpha = 0.8f), Offset(xPos - (rectW * 0.5f), effectiveBaseY - maxHeight), Size(maxOf(2f, rectW), maxHeight + 4.dp.toPx()))
                }
                
                if (pHasGps && pGpsIndex > 0.0) { 
                    val gpsOffset = (pGpsIndex.toFloat().coerceIn(0f, 1f) * (if (landscape) 24.dp.toPx() else 14.dp.toPx()))
                    val yPos = effectiveBaseY - ribbonMaxHeight - (if (landscape) 4.dp.toPx() else 2.dp.toPx()) - gpsOffset
                    val currentPos = Offset(xPos + (rectW / 2f), yPos)
                    if (lastGpsPos != null && (currentPos.x - lastGpsPos.x < pointWidth * 10)) {
                        gpsPath.lineTo(currentPos.x, currentPos.y) 
                    } else {
                        gpsPath.moveTo(currentPos.x, currentPos.y)
                    }
                    gpsPoints.add(currentPos)
                    lastGpsPos = currentPos 
                } 
            } else {
                lastGpsPos = null
            }
            
            if (intervalMs > 0 && pTs >= baseTickTs) { 
                val tickCount = (pTs - baseTickTs) / intervalMs
                val prevTickCount = if (index > 0 && history.ts[index-1] >= baseTickTs) (history.ts[index - 1] - baseTickTs) / intervalMs else -1L
                if (tickCount >= 0 && tickCount > prevTickCount) { 
                    val timeStr = if (scale == "7D") dateFormatter.format(Date(pTs)) else timeFormatter.format(Date(pTs))
                    drawIntoCanvas { 
                        it.nativeCanvas.drawText(
                            timeStr, 
                            xPos.coerceIn(20.dp.toPx(), size.width - 20.dp.toPx()), 
                            size.height - (if (landscape) 6.dp.toPx() else 4.dp.toPx()), 
                            textPaint
                        ) 
                    } 
                } 
            } 
        }
        drawPath(connectedPath, BrandJd)
        drawPath(disconnectedPath, Color(0xFFF43F5E))
        drawPath(gpsPath, Color(0xFF00FFFF), style = Stroke(width = if (landscape) 1.dp.toPx() else 0.5.dp.toPx()))
        if (gpsPoints.isNotEmpty()) {
            val gpsPointRadius = (if (landscape) 1.2.dp.toPx() else 0.8.dp.toPx()) * 2
            drawPoints(gpsPoints, PointMode.Points, Color(0xFF00FFFF), gpsPointRadius, StrokeCap.Round)
        }
    }
}

@Composable
fun HeaderBar(
    isLogVisible: Boolean, isSettingsOpen: Boolean, isRibbonsVisible: Boolean, isMapVisible: Boolean, isPhoneSetupVisible: Boolean = false, 
    requiresExtraTopPadding: Boolean, isSystemReady: Boolean, systemIssuesCount: Int, 
    onEvent: (UiEvent) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isDashboardActive = !isMapVisible && !(isLogVisible || isSettingsOpen || isRibbonsVisible)
    val commitAnd = { event: UiEvent -> if (isSettingsOpen) onEvent(UiEvent.CommitSettings); onEvent(event) }
    
    val alertAlpha by gatedInfinitePulse(
        initialValue = 0.4f, 
        targetValue = 1f, 
        animationSpec = infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse), 
        label = "AlertPulse"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (isLandscape) {
            Column(modifier = Modifier.fillMaxHeight().width(48.dp).background(Color.Black.copy(alpha = 0.4f)), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(8.dp)); IconButton(onClick = { commitAnd(UiEvent.ToggleSettings(!isSettingsOpen)); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Settings button clicked", false)) }) { Icon(Icons.Default.Settings, null, tint = if (isSettingsOpen) Color.Gray else Color.White, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.height(16.dp)); IconButton(onClick = { commitAnd(UiEvent.NavigateToDashboard); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Dashboard button clicked", false)) }, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Info, "Dashboard", tint = if (isDashboardActive) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) }
                IconButton(onClick = { commitAnd(UiEvent.ToggleRibbons(!isRibbonsVisible)) }, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.BarChart, null, tint = if (isRibbonsVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.weight(1f)); if (!isSystemReady && !isPhoneSetupVisible) IconButton(onClick = { onEvent(UiEvent.TogglePhoneSetup(true)) }) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ReportProblem, "System Issues", tint = Color(0xFFF43F5E).copy(alpha = alertAlpha), modifier = Modifier.size(28.dp)); Text(text = systemIssuesCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) } }
                IconButton(onClick = { commitAnd(UiEvent.ToggleLog(!isLogVisible)); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Log button clicked", false)) }) { Icon(Icons.AutoMirrored.Filled.List, null, tint = if (isLogVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) }
                IconButton(onClick = { commitAnd(UiEvent.ToggleMap(!isMapVisible)) }) { Icon(Icons.Default.Map, null, tint = if (isMapVisible && !(isLogVisible || isSettingsOpen || isRibbonsVisible)) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.height(8.dp))
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(top = if (requiresExtraTopPadding) 12.dp else 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) { 
                    IconButton(onClick = { commitAnd(UiEvent.ToggleSettings(!isSettingsOpen)); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Settings button clicked", false)) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Settings, null, tint = if (isSettingsOpen) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) }
                    IconButton(onClick = { commitAnd(UiEvent.NavigateToDashboard); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Dashboard button clicked", false)) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Info, "Dashboard", tint = if (isDashboardActive) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) }
                    IconButton(onClick = { commitAnd(UiEvent.ToggleRibbons(!isRibbonsVisible)) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.BarChart, null, tint = if (isRibbonsVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.weight(1f)); if (!isSystemReady && !isPhoneSetupVisible) IconButton(onClick = { onEvent(UiEvent.TogglePhoneSetup(true)) }, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ReportProblem, "System Issues", tint = Color(0xFFF43F5E).copy(alpha = alertAlpha), modifier = Modifier.size(26.dp)); Text(text = systemIssuesCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) } }
                    IconButton(onClick = { commitAnd(UiEvent.ToggleLog(!isLogVisible)); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Log button clicked", false)) }, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.List, null, tint = if (isLogVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) }
                    IconButton(onClick = { commitAnd(UiEvent.ToggleMap(!isMapVisible)) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Map, null, tint = if (isMapVisible && !(isLogVisible || isSettingsOpen || isRibbonsVisible)) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) }
                }
            }
        }
    }
}

@Composable
fun GlobalStatusBar(stateProvider: UiStateProvider, onEvent: (UiEvent) -> Unit, modifier: Modifier = Modifier) {
    val hudState by stateProvider.hudState.collectAsStateWithLifecycle()
    StatusBar(connectivity = hudState.connectivity, telemetry = hudState.telemetry, health = hudState.health, onEvent = onEvent, modifier = modifier)
}

@Composable
fun StatusBar(connectivity: HudConnectivityState, telemetry: HudTelemetryState, health: HudHealthState, onEvent: (UiEvent) -> Unit, modifier: Modifier = Modifier) {
    val mode = connectivity.appMode ?: return
    val lastGpsTs = if (mode == "viewer") telemetry.lastGpsTs else telemetry.viewerGpsTs
    val isPeerActive = connectivity.isTelemetryFresh
    val isTrackerGpsActive = if (mode == "viewer") telemetry.isGpsFresh else telemetry.isLocalGpsActive
    val progressValue = if (health.progressPulse > 0f) health.progressPulse else 0f
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val trkIdLabel = connectivity.trackerId.take(6).uppercase()
    val viewIdLabel = connectivity.viewerId.take(6).uppercase()
    val isTracker = mode == "tracker"
    
    val alarmAlpha by gatedInfinitePulse(
        0.4f, 1f, 
        infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse), 
        label = "AlarmAlpha"
    )
    
    val movingAlpha by gatedInfinitePulse(
        0.5f, 1f, 
        infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse), 
        label = "MovingAlpha"
    )
    
    val localColor = if (mode == "viewer") ViewerCyan else BrandJd
    val peerColor = if (mode == "viewer") BrandJd else ViewerCyan
    val trackerStateActive = isTrackerGpsActive && telemetry.trackerState != TrackerState.UNKNOWN
    val stateColor = if (trackerStateActive) BrandJd else Slate500
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(modifier = modifier.fillMaxWidth(), shape = androidx.compose.ui.graphics.RectangleShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        StatusBadge(label = "SYS", active = connectivity.isSystemActive, activeColor = localColor, isBold = true)
                        StatusBadge(label = "INT", active = connectivity.isInternet, activeColor = localColor, isBold = true)
                        
                        // SIMP-OCT26.6-S1: SRV Latch Logic. Orange indicates aborted attempt.
                        StatusBadge(label = "SRV", active = connectivity.isRelayConnected, activeColor = if (connectivity.isConnectionAborted) Color(0xFFF59E0B) else localColor, isBold = true)
                        
                        StatusBadge(label = "GPS", active = telemetry.isLocalGpsActive, activeColor = localColor)
                        StatusBadge(label = if (isTracker) "VWR" else "TRK", active = isPeerActive, activeColor = peerColor)
                        
                        // GAP-OCT26.5-1: Conflation UI Feedback / GAP-OCT27.4-1: Unit-Based Badge Scaling.
                        MeasurableBadge(
                            label = "DAT",
                            contentColor = if (connectivity.isDataHealthy) (if (connectivity.conflatedCount > 0) Color(0xFF38BDF8) else localColor) else Color(0xFFF43F5E),
                            overlayText = if (connectivity.conflatedCount > 0) (if (connectivity.conflatedCount > 99) "99+" else connectivity.conflatedCount.toString()) else null,
                            modifier = Modifier.pointerInput(connectivity.conflatedCount) {
                                detectTapGestures(onLongPress = { 
                                    if (connectivity.conflatedCount > 0) onEvent(UiEvent.ShowConflationStats(connectivity.conflatedCount)) 
                                })
                            }
                        )

                        StatusBadge(label = "WDG", active = connectivity.watchdogOk, activeColor = localColor, isBold = true)
                        if (connectivity.isSafeMode) { StatusBadge(label = "SAF", active = false, isBold = true) }
                        if (connectivity.isStaggered) { StatusBadge(label = "STG", active = true, activeColor = localColor, isBold = true) }
                        if (connectivity.isGnssThrottled) { StatusBadge(label = "THR", active = true, activeColor = Amber500, isBold = true) }
                        if (health.isMaliAnomaly) { StatusBadge(label = "MAL", active = true, activeColor = Color(0xFFF43F5E), isBold = true) }
                        
                        // R872: Absolute Stealth. Trackers must never show alarm/siren status badges.
                        if (!isTracker) {
                            if (health.hasActiveAlarms) { StatusBadge(label = "ALM", active = true, activeColor = Color(0xFFF43F5E).copy(alpha = alarmAlpha), isBold = true) }
                            if (health.isRedScreenSuppressed) {
                                MeasurableBadge(
                                    label = if (health.isSirenPlaying) "SIREN" else "LOCK",
                                    contentColor = Color.White,
                                    backgroundColor = if (health.isSirenPlaying) Color(0xFFF43F5E).copy(alpha = alarmAlpha) else Slate500,
                                    fontSize = 7.sp
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = telemetry.trackerState.name, color = stateColor.copy(alpha = if (telemetry.trackerState == TrackerState.MOVING && isTrackerGpsActive) movingAlpha else 1f), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                        val animatedSpeed by gatedAnimateFloatAsState(
                            if (isTrackerGpsActive && !telemetry.speedMps.isNaN()) telemetry.speedMps * 3.6f else 0f, 
                            tween(1000), 
                            label = "SpeedAnim"
                        )
                        Text(
                            text = "${if (animatedSpeed < 10.0f) String.format(Locale.getDefault(), "%.1f", animatedSpeed) else animatedSpeed.toInt().toString()}km/h", 
                            color = stateColor, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold, 
                            fontFamily = FontFamily.Monospace, 
                            style = compactStyle
                        )
                        
                        // ISSUE-OCT12.14-3: HUD Data Health Label for forensic clarity.
                        if (!connectivity.isDataHealthy) {
                            Text(
                                text = "DATA ERR",
                                color = Color(0xFFF43F5E),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                style = compactStyle,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) { 
                            if (!LocalAnimationGating.current) {
                                CircularProgressIndicator(
                                    progress = { progressValue }, 
                                    modifier = Modifier.size(14.dp), 
                                    color = if (connectivity.isDataHealthy) localColor else Color(0xFFF43F5E), 
                                    strokeWidth = 2.dp
                                )
                            }
                            Icon(
                                imageVector = if (connectivity.isDataHealthy) Icons.Default.CheckCircle else Icons.Default.Error, 
                                contentDescription = if (connectivity.isDataHealthy) "Data Healthy" else "Data Health Error", 
                                modifier = Modifier.size(9.dp), 
                                tint = if (connectivity.isDataHealthy) localColor else Color(0xFFF43F5E)
                            ) 
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (mode == "viewer") { 
                        val vAge = if(telemetry.viewerGpsTs > 0) health.systemPulse - telemetry.viewerGpsTs else -1L
                        StatusRowData(
                            StatusRowState(
                                label = viewIdLabel, battery = health.battery, commIndex = connectivity.commIndex, 
                                color = ViewerCyan, overrideDistanceColor = BrandJd, isCharging = health.isCharging, 
                                accuracy = telemetry.viewerAccuracy, maxAccuracy = telemetry.maxViewerAccuracy, 
                                temp = health.viewerTemp, distance = telemetry.distToViewer, satsUsed = telemetry.viewerSatsUsed, 
                                satsView = telemetry.viewerSatsView, gpsAgeMs = vAge, isLocPending = telemetry.isViewerLocPending, 
                                locPendingReason = telemetry.viewerLocPendingReason, 
                                isTelemetryFresh = telemetry.viewerGpsTs > 0 && (health.systemPulse - telemetry.viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS), 
                                isGpsFresh = vAge in 0..GPS_UI_FAIL_THRESHOLD_MS
                            )
                        ) 
                    }
                    val tAge = if(lastGpsTs > 0) health.systemPulse - lastGpsTs else -1L
                    StatusRowData(
                        StatusRowState(
                            label = trkIdLabel, 
                            battery = if (mode == "viewer") health.remoteBattery else health.battery, 
                            commIndex = if (mode == "viewer") (if(isPeerActive) connectivity.remoteCommIndex else 0) else connectivity.commIndex, 
                            color = if (mode == "viewer" && !isPeerActive) Slate500 else BrandJd, 
                            isCharging = if (mode == "viewer") health.remoteCharging else health.isCharging, 
                            accuracy = telemetry.trackerAccuracy, maxAccuracy = telemetry.maxTrackerAccuracy, 
                            satsView = telemetry.satsView, satsUsed = telemetry.satsUsed, gpsAgeMs = tAge, 
                            temp = health.trackerTemp, distance = telemetry.distToHome, isRemote = mode == "viewer", 
                            isPeerActive = if(mode == "viewer") isPeerActive else true, 
                            isLocPending = telemetry.isTrackerLocPending, locPendingReason = telemetry.locationPendingReason, 
                            isTelemetryFresh = if (mode == "tracker") (telemetry.viewerGpsTs > 0 && (health.systemPulse - telemetry.viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS)) else isPeerActive, 
                            isGpsFresh = isTrackerGpsActive, isUltraLongStationary = telemetry.isUltraLongStationary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatusRowData(state: StatusRowState) {
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val isConnStale = state.isRemote && !state.isPeerActive
    val telemetryColor = if (state.isTelemetryFresh && !isConnStale) state.color else Slate500
    val contentColor = if (isConnStale) Slate500 else state.color
    val distColor = if (state.isTelemetryFresh && !isConnStale) (state.overrideDistanceColor ?: state.color) else Slate500
    
    val handshakeAlpha by gatedInfinitePulse(
        0.3f, 1f, 
        infiniteRepeatable(tween(1200), repeatMode = RepeatMode.Reverse),
        label = "HandshakeAlpha"
    )
    
    val animatedBattery by gatedAnimateFloatAsState(
        state.battery.toFloat(), 
        tween(1500), 
        label = "BatteryAnim"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = state.horizontalPadding, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
             val labelAlpha by animateFloatAsState(if (isConnStale) 0.5f else 1f, label = "LabelAlpha")
             Text(
                 text = if (isConnStale) "${state.label} (OFFLINE)" else state.label,
                 color = contentColor.copy(alpha = if (isConnStale) handshakeAlpha else labelAlpha),
                 fontSize = 8.sp,
                 fontWeight = FontWeight.Bold,
                 fontFamily = FontFamily.Monospace,
                 style = compactStyle,
                 maxLines = 1,
                 overflow = TextOverflow.Ellipsis
             )
             if (state.isLocPending) {
                 Box(modifier = Modifier.padding(start = 1.dp).background(Amber500, RoundedCornerShape(1.dp)).padding(horizontal = 1.dp)) {
                     Text(text = "P", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold, style = compactStyle)
                 }
             }
             if (!isConnStale) {
                 Spacer(Modifier.width(3.dp))
                 Icon(
                     imageVector = if (state.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                     contentDescription = null,
                     modifier = Modifier.size(8.dp),
                     tint = if (!state.isTelemetryFresh) Slate500 else if (state.battery in 0..19) Color(0xFFF43F5E) else telemetryColor
                 )
                 Text(
                     text = if(state.battery >= 0) "${animatedBattery.toInt()}%" else "--%",
                     color = telemetryColor,
                     fontSize = 8.sp,
                     fontWeight = FontWeight.Bold,
                     fontFamily = FontFamily.Monospace,
                     style = compactStyle
                 )
                 Spacer(Modifier.width(2.dp))
                 Text(
                     text = String.format(Locale.getDefault(), "%.0f°", state.temp),
                     color = telemetryColor,
                     fontSize = 8.sp,
                     fontWeight = FontWeight.Bold,
                     fontFamily = FontFamily.Monospace,
                     style = compactStyle
                 )
                 Spacer(Modifier.width(2.dp))
                 Box(contentAlignment = Alignment.Center, modifier = Modifier.width(12.dp)) {
                     CommBar(index = state.commIndex, color = if (state.isTelemetryFresh) contentColor else Slate500)
                 }
             }
        }
        if (!isConnStale) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text(
                    text = "${if (state.satsUsed == -1) "--" else state.satsUsed}/${if (state.satsView == -1) "--" else state.satsView}",
                    color = telemetryColor,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = compactStyle
                )
                Spacer(Modifier.width(2.dp))
                val ageStr = if (state.gpsAgeMs != -1L) {
                    val ageSec = (maxOf(0L, state.gpsAgeMs) / 1000).toInt()
                    when {
                        ageSec < 100 -> "${ageSec}s"
                        ageSec < 3600 -> "${ageSec/60}m"
                        else -> ">1h"
                    }
                } else "--s"
                Text(
                    text = ageStr,
                    color = if (!state.isGpsFresh) Slate500 else state.color,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = compactStyle
                )
                Spacer(Modifier.width(3.dp))
                if (state.isLocPending && state.locPendingReason != LocationPendingReason.NONE) {
                    Text(
                        text = state.locPendingReason.name.take(4),
                        color = Amber500,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        style = compactStyle
                    )
                } else {
                    fun formatAcc(v: Float): String = when {
                        v >= 1000f -> "${(v / 1000).toInt()}k"
                        else -> v.toInt().toString()
                    }
                    Text(
                        text = "±${formatAcc(state.accuracy)}",
                        color = if (state.isTelemetryFresh && state.isGpsFresh) state.color else Slate500,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        style = compactStyle
                    )
                }
                Spacer(Modifier.width(3.dp))
                val animatedDistance by gatedAnimateFloatAsState(
                    if (state.distance == null || state.distance.isNaN()) 0f else state.distance.toFloat(),
                    tween(1200),
                    label = "DistAnim"
                )
                val distStr = when {
                    state.distance == null || state.distance.isNaN() -> "--"
                    animatedDistance >= 1000 -> String.format(Locale.getDefault(), "%.1fk", animatedDistance / 1000.0)
                    else -> "${animatedDistance.toInt()}m"
                }
                Text(
                    text = distStr,
                    color = distColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = compactStyle
                )
            }
        }
    }
}

@Composable
fun CommBar(index: Int, color: Color) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(0.5.dp)) { repeat(8) { i -> Box(modifier = Modifier.width(1.dp).height((2.0 + (i * 0.6)).dp).background(if (i < index) color else Slate500.copy(alpha = 0.4f))) } }
}

/**
 * GAP-OCT27.4-1: Shared Measurable Badge Component.
 * Reduces redundant Compose declarations by unifying label, active state, 
 * optional background, and optional overlay logic.
 */
@Composable
fun MeasurableBadge(
    label: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    isBold: Boolean = true,
    overlayText: String? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 8.sp
) {
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    Box(contentAlignment = Alignment.TopEnd, modifier = modifier) {
        Box(
            modifier = Modifier
                .then(if (backgroundColor != Color.Transparent) Modifier.background(backgroundColor, RoundedCornerShape(2.dp)).padding(horizontal = 2.dp) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                style = compactStyle
            )
        }
        if (overlayText != null) {
            Box(
                modifier = Modifier
                    .offset(x = 2.dp, y = (-2).dp)
                    .background(Color.Red, RoundedCornerShape(1.dp))
                    .padding(horizontal = 1.dp)
            ) {
                Text(
                    text = overlayText,
                    color = Color.White,
                    fontSize = 5.sp,
                    fontWeight = FontWeight.Black,
                    style = compactStyle
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
    activeColor: Color = BrandJd, 
    inactiveColor: Color = Color(0xFFF43F5E),
    isBold: Boolean = true
) {
    // GAP-OCT27.4-1: Refactored to use shared MeasurableBadge.
    MeasurableBadge(
        label = label,
        contentColor = if (active) activeColor else inactiveColor,
        isBold = isBold,
        modifier = modifier
    )
}

@Composable
fun SessionTerminationButton(appMode: String, onEvent: (UiEvent) -> Unit, modifier: Modifier = Modifier) {
    val label = if (appMode == "tracker") "TERMINATE TRACKING SESSION" else "TERMINATE MONITORING SESSION"
    Button(onClick = { onEvent(UiEvent.ShowStopTrackingConfirmation(true)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E).copy(alpha = 0.1f)), border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth().height(48.dp)) { Icon(Icons.Default.StopCircle, null, tint = Color(0xFFF43F5E), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(text = label, color = Color(0xFFF43F5E), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}
