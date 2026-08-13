package com.gps19.app

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
 * Aug.13.11:
 * - Issue #162: Phone Setup ANR Remediation. Modified HeaderBar to hide the 
 *   System Issues icon when the overlay is already visible, reducing 
 *   animation overhead and preventing redundant triggers. Fixed syntax error 
 *   in ForensicRibbonContainer.
 * Aug.11.21:
 * - Issue #148: Header Layout Inversion Fix. Explicitly forced LayoutDirection.Ltr 
 *   in HeaderBar to prevent unintended RTL inversions (R148).
 * Aug.10.27:
 * - Issue #132: Forensic UI Dashboard Refinement. Integrated CPU, I/O Wait, 
 *   and Latency trend ribbons into AnalyticalRibbons (R132).
 */

enum class RibbonRenderType { BAR, LINE }

@Composable
fun RibbonsOverlay(
    isStrictMode: Boolean,
    history4MFlow: StateFlow<List<ConnectionPoint>>,
    history16MFlow: StateFlow<List<ConnectionPoint>>,
    history1HFlow: StateFlow<List<ConnectionPoint>>,
    history4HFlow: StateFlow<List<ConnectionPoint>>,
    history24HFlow: StateFlow<List<ConnectionPoint>>,
    history7DFlow: StateFlow<List<ConnectionPoint>>,
    onToggleStrictMode: (Boolean) -> Unit,
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
                        history4MFlow = history4MFlow,
                        history16MFlow = history16MFlow,
                        history1HFlow = history1HFlow,
                        history4HFlow = history4HFlow,
                        history24HFlow = history24HFlow,
                        history7DFlow = history7DFlow,
                        onToggleStrictMode = onToggleStrictMode
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticalRibbons(
    isStrictMode: Boolean,
    history4MFlow: StateFlow<List<ConnectionPoint>>,
    history16MFlow: StateFlow<List<ConnectionPoint>>,
    history1HFlow: StateFlow<List<ConnectionPoint>>,
    history4HFlow: StateFlow<List<ConnectionPoint>>,
    history24HFlow: StateFlow<List<ConnectionPoint>>,
    history7DFlow: StateFlow<List<ConnectionPoint>>,
    onToggleStrictMode: (Boolean) -> Unit
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
    val barSelector = remember { { p: ConnectionPoint -> p.baroIdx.toFloat() } }
    val svzSelector = remember { { p: ConnectionPoint -> (kotlin.math.abs(p.sitVz).toFloat() / 2.0f).coerceIn(0f, 1f) } }
    val svzDriftSelector = remember { { p: ConnectionPoint -> if (p.sitVzTs > 0) kotlin.math.abs(p.ts - p.sitVzTs) else 0L } }
    val sdzSelector = remember { { p: ConnectionPoint -> (kotlin.math.abs(p.sitDz).toFloat() / 0.5f).coerceIn(0f, 1f) } }
    
    // Issue #132: Performance selectors
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

        ConnectionQualityRibbon(history, selectedScale, isStrictMode)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))

        GenericSensorRibbon(history, "SNR", selectedScale, lineColor = Color(0xFF38BDF8), isStrictMode = isStrictMode, valueSelector = snrSelector)
        GenericSensorRibbon(history, "NOI", selectedScale, lineColor = Amber500, isStrictMode = isStrictMode, valueSelector = noiseSelector)
        GenericSensorRibbon(history, "KNT", selectedScale, lineColor = Color(0xFF4ADE80), isStrictMode = isStrictMode, valueSelector = kineticSelector)
        GenericSensorRibbon(history, "LUX", selectedScale, lineColor = Color.White, isStrictMode = isStrictMode, valueSelector = luxSelector)
        GenericSensorRibbon(history, "VIB", selectedScale, lineColor = Color.Magenta, isStrictMode = isStrictMode, valueSelector = vibeSelector)
        GenericSensorRibbon(history, "PRX", selectedScale, lineColor = Rose500, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, valueSelector = proxSelector)
        GenericSensorRibbon(history, "LIF", selectedScale, lineColor = Color(0xFFFACC15), isStrictMode = isStrictMode, valueSelector = liftSelector)
        GenericSensorRibbon(history, "BAT", selectedScale, lineColor = Rose500, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, valueSelector = batSelector)
        GenericSensorRibbon(history, "THM", selectedScale, lineColor = Color.Red, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, valueSelector = thmSelector)
        GenericSensorRibbon(history, "CUR", selectedScale, lineColor = Color(0xFFFB923C), isStrictMode = isStrictMode, valueSelector = curSelector)
        GenericSensorRibbon(history, "SIT", selectedScale, lineColor = BrandJd, renderType = RibbonRenderType.BAR, isStrictMode = isStrictMode, valueSelector = sitSelector)
        GenericSensorRibbon(history, "TLT", selectedScale, lineColor = Color(0xFF818CF8), isStrictMode = isStrictMode, valueSelector = tltSelector)
        GenericSensorRibbon(history, "BAR", selectedScale, lineColor = Color(0xFF2DD4BF), isStrictMode = isStrictMode, valueSelector = barSelector)
        GenericSensorRibbon(history, "SVZ", selectedScale, lineColor = Violet500, isStrictMode = isStrictMode, valueSelector = svzSelector, driftSelector = svzDriftSelector)
        GenericSensorRibbon(history, "SDZ", selectedScale, lineColor = Violet500, isStrictMode = isStrictMode, valueSelector = sdzSelector)
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))
        
        // Issue #132: Performance Trends
        GenericSensorRibbon(history, "CPU", selectedScale, lineColor = Color(0xFF4ADE80), isStrictMode = isStrictMode, valueSelector = cpuSelector)
        GenericSensorRibbon(history, "IOW", selectedScale, lineColor = Amber500, isStrictMode = isStrictMode, valueSelector = iowSelector)
        GenericSensorRibbon(history, "LAT", selectedScale, lineColor = Rose500, isStrictMode = isStrictMode, valueSelector = latSelector)
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
    valueSelector: (ConnectionPoint) -> Float,
    driftSelector: ((ConnectionPoint) -> Long)? = null
) {
    ForensicRibbonContainer(title, lineColor, if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 40.dp else 24.dp, history, scale, isStrictMode) { totalPoints, pointWidth, baseLineY, maxHeight, landscape ->
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
fun ConnectionQualityRibbon(history: List<ConnectionPoint>, scale: String, isStrictMode: Boolean = false) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val alignMs = remember(scale) { when(scale) { "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 1L } }
    val intervalMs = remember(scale) { when(scale) { "7D" -> 2700 * 1000L; "24H" -> 360 * 1000L; "4H" -> 60 * 1000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 0L } }
    val density = LocalDensity.current
    val textPaint = remember(isLandscape, density) { android.graphics.Paint().apply { color = android.graphics.Color.WHITE; with(density) { textSize = (if (isLandscape) 10.sp.toPx() else 7.sp.toPx()) }; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.MONOSPACE } }

    ForensicRibbonContainer(scale, Color.Gray, if (isLandscape) 60.dp else 34.dp, history, scale, isStrictMode) { totalPoints, pointWidth, connectionBaseY, maxHeight, landscape ->
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
    isPhoneSetupVisible: Boolean = false, // Issue #162: Track setup visibility
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
    val alertAlpha by alertPulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse), label = "Alpha")

    // Issue #148: Force LayoutDirection.Ltr
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
                // Issue #162: Hide icon if PhoneSetupOverlay is already visible to reduce main-thread load
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
                    // Issue #162: Hide icon if PhoneSetupOverlay is already visible
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
    appMode: String?, isSystemActive: Boolean, deviceId: String, viewerId: String, isLocalOnline: Boolean, isRelayConnected: Boolean, 
    lastRemoteActivityTs: Long, isRedScreenVisible: Boolean, batteryLevel: Int, trackerBatteryLevel: Int, isChargingStable: Boolean, 
    trackerChargingStable: Boolean, activeAlarms: List<AlarmInfo>, trackerSatsView: Int, trackerSatsUsed: Int, trackerBatteryTemp: Double, 
    viewerBatteryTemp: Double, viewerSatsUsed: Int, viewerSatsView: Int, isSirenPlaying: Boolean, trackerGpsTs: Long, trackerTelemetryTs: Long, 
    trackerSpeedMps: Double, trackerAccuracy: Double, trackerMaxAccuracy: Double, localGpsTs: Long, localAccuracy: Double, 
    localMaxAccuracy: Double, localLat: Double, trackerLocPending: Boolean, trackerLocPendingReason: LocationPendingReason, 
    localLocPending: Boolean, localLocPendingReason: LocationPendingReason, distanceTrackerToHome: Double?, 
    distanceTrackerToViewer: Double?, isTelemetryFresh: Boolean, isGpsFresh: Boolean, watchdogOk: Boolean, trackerState: TrackerState, 
    systemPulse: Long, rttFlow: StateFlow<Int>, remoteSignalFlow: StateFlow<Int>, modifier: Modifier = Modifier
) {
    val mode = appMode ?: return
    val rtt by rttFlow.collectAsStateWithLifecycle()
    val remoteSignal by remoteSignalFlow.collectAsStateWithLifecycle()
    val commIndex = if (isSystemActive && isRelayConnected) TelemetryUtils.calculateCommIndex(rtt, 10, 10) else 0
    val remoteCommIndex = if (mode == "viewer" && isTelemetryFresh) TelemetryUtils.calculateCommIndex(rtt, remoteSignal, 10) else 0
    val lastGpsTs = if (mode == "viewer") trackerGpsTs else localGpsTs
    val isLocalGpsActive = if (mode == "tracker") isGpsFresh else (systemPulse - localGpsTs < GPS_UI_FAIL_THRESHOLD_MS)
    val lastTelemetryTs = if (mode == "viewer") maxOf(trackerGpsTs, trackerTelemetryTs) else localGpsTs
    val progressPulse = if (mode == "tracker") lastRemoteActivityTs else lastTelemetryTs
    val hasUnresolved = activeAlarms.any { !it.isResolved }

    StatusBar(
        modifier = modifier, 
        isInternet = isLocalOnline, 
        isRelay = isRelayConnected, 
        isPeerActive = isTelemetryFresh, 
        isDataHealthy = isTelemetryFresh && isLocalOnline && isRelayConnected, 
        isLocalGpsActive = isLocalGpsActive, 
        isTrackerGpsActive = isGpsFresh, 
        mode = mode, 
        battery = batteryLevel, 
        lastP = progressPulse, 
        commIndex = commIndex, 
        remoteCommIndex = remoteCommIndex, 
        remoteBattery = if (mode == "viewer") trackerBatteryLevel else -1, 
        isCharging = isChargingStable, 
        remoteCharging = if (mode == "viewer") trackerChargingStable else false, 
        speedMps = (if (mode == "viewer") trackerSpeedMps else 0.0).toFloat(), 
        trackerAccuracy = trackerAccuracy.toFloat(), 
        maxTrackerAccuracy = trackerMaxAccuracy.toFloat(), 
        viewerAccuracy = (if (localLat != 0.0) localAccuracy.toFloat() else 0f), 
        maxViewerAccuracy = localMaxAccuracy.toFloat(), 
        now = systemPulse, 
        satsView = trackerSatsView, 
        satsUsed = trackerSatsUsed, 
        trackerTemp = trackerBatteryTemp.toFloat(), 
        viewerTemp = viewerBatteryTemp.toFloat(), 
        distToHome = distanceTrackerToHome, 
        distToViewer = distanceTrackerToViewer, 
        viewerSatsUsed = viewerSatsUsed, 
        viewerSatsView = viewerSatsView, 
        viewerGpsTs = localGpsTs, 
        trackerId = deviceId, 
        viewerId = viewerId, 
        watchdogOk = watchdogOk, 
        trackerState = trackerState, 
        hasActiveAlarms = hasUnresolved, 
        isRedScreenSuppressed = (hasUnresolved && !isRedScreenVisible), 
        isSirenPlaying = isSirenPlaying, 
        isTrackerLocPending = trackerLocPending, 
        trackerLocPendingReason = trackerLocPendingReason, 
        isViewerLocPending = localLocPending, 
        viewerLocPendingReason = localLocPendingReason, 
        lastGpsTs = lastGpsTs, 
        isTelemetryFresh = isTelemetryFresh, 
        isGpsFresh = isGpsFresh
    )
}

@Composable
fun StatusBar(
    modifier: Modifier = Modifier, isInternet: Boolean, isRelay: Boolean, isPeerActive: Boolean, isDataHealthy: Boolean, 
    isLocalGpsActive: Boolean, isTrackerGpsActive: Boolean,
    mode: String?, battery: Int, lastP: Long, commIndex: Int = 10, remoteCommIndex: Int = 0, remoteBattery: Int = -1, isCharging: Boolean = false,
    remoteCharging: Boolean = false, speedMps: Float = 0f, trackerAccuracy: Float = 0f, maxTrackerAccuracy: Float = 0f,
    viewerAccuracy: Float = 0f, maxViewerAccuracy: Float = 0f, now: Long, satsView: Int = 0, satsUsed: Int = 0,
    trackerTemp: Float = 0f, viewerTemp: Float = 0f, distToHome: Double? = null, distToViewer: Double? = null,
    viewerSatsUsed: Int = 0, viewerSatsView: Int = 0, viewerGpsTs: Long = 0L, trackerId: String = "TRK", viewerId: String = "VIEW", watchdogOk: Boolean = true,
    trackerState: TrackerState = TrackerState.UNKNOWN, hasActiveAlarms: Boolean = false, isRedScreenSuppressed: Boolean = false,
    isSirenPlaying: Boolean = false, 
    isTrackerLocPending: Boolean = false, 
    trackerLocPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    isViewerLocPending: Boolean = false,
    viewerLocPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    lastGpsTs: Long = 0L,
    isTelemetryFresh: Boolean = true,
    isGpsFresh: Boolean = true
) {
    val age = if (lastP > 0) now - lastP else Long.MAX_VALUE 
    val progressValue = if (lastP > 0) maxOf(0f, minOf(1f, (TELEMETRY_UI_STALE_THRESHOLD_MS - age).toFloat() / TELEMETRY_UI_STALE_THRESHOLD_MS)) else 0f
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val trkIdLabel = trackerId.take(6).uppercase()
    val viewIdLabel = viewerId.take(6).uppercase()
    val infiniteTransition = rememberInfiniteTransition(label = "StatusBarAnimations")
    val alarmAlpha by infiniteTransition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "AlarmAlpha")
    val movingAlpha by infiniteTransition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "MovingAlpha")

    Card(modifier = modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isLandscape) 0.7f else 0.9f)), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 3.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(label = "INT", active = isInternet, isBold = true); StatusBadge(label = "SRV", active = isRelay, isBold = true); StatusBadge(label = "GPS", active = isLocalGpsActive)
                    StatusBadge(label = if (mode == "tracker") "VWR" else "TRK", active = isPeerActive, activeColor = BrandJd)
                    if (mode != "tracker") StatusBadge(label = "DAT", active = isDataHealthy)
                    if (hasActiveAlarms) StatusBadge(label = "ALM", active = true, activeColor = Rose500.copy(alpha = alarmAlpha), isBold = true)
                    if (isRedScreenSuppressed) {
                         Spacer(modifier = Modifier.width(2.dp))
                         Box(modifier = Modifier.background(if (isSirenPlaying) Rose500.copy(alpha = alarmAlpha) else Slate500, RoundedCornerShape(2.dp)).padding(horizontal = 2.dp)) {
                             Text(text = if (isSirenPlaying) "SIREN LOCKOUT" else "LOCKOUT", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, style = compactStyle)
                         }
                    }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp)) { 
                        CircularProgressIndicator(progress = { progressValue }, modifier = Modifier.size(16.dp), color = if (isDataHealthy) BrandJd else Rose500, strokeWidth = 2.dp)
                        Icon(imageVector = if (isDataHealthy) Icons.Default.CheckCircle else Icons.Default.Error, contentDescription = null, modifier = Modifier.size(8.dp), tint = if (isDataHealthy) BrandJd else Rose500)
                    }
                    Text(text = if(watchdogOk) "OK" else "FAIL", color = if(watchdogOk) BrandJd else Rose500, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (trackerState == TrackerState.MOVING && isTrackerGpsActive) "»\u2009${trackerState.name}\u2009«" else trackerState.name, color = (if (!isTrackerGpsActive) Slate500 else BrandJd).copy(alpha = if (trackerState == TrackerState.MOVING && isTrackerGpsActive) movingAlpha else 1f), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                Spacer(modifier = Modifier.weight(1f))
                val animatedSpeed by animateFloatAsState(if (isTrackerGpsActive && !speedMps.isNaN()) speedMps * 3.6f else 0f, tween(1000), label = "SpeedAnim")
                Text(text = "${if (animatedSpeed < 10.0f) String.format(Locale.getDefault(), "%.1f", animatedSpeed) else animatedSpeed.toInt().toString()}km/h", color = if (isTrackerGpsActive) BrandJd else Slate500, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle, textAlign = TextAlign.End)
            }
            Spacer(modifier = Modifier.height(3.dp))
            if (isLandscape && mode == "viewer") {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    val vAge = if(viewerGpsTs > 0) now - viewerGpsTs else -1L
                    Box(modifier = Modifier.weight(1f)) { 
                        StatusRowData(label = viewIdLabel, battery = battery, commIndex = commIndex, color = ViewerCyan, overrideDistanceColor = BrandJd, isCharging = isCharging, accuracy = viewerAccuracy, maxAccuracy = maxViewerAccuracy, temp = viewerTemp, distance = distToViewer, satsUsed = viewerSatsUsed, satsView = viewerSatsView, gpsAgeMs = vAge, isRemote = false, isLocPending = isViewerLocPending, locPendingReason = viewerLocPendingReason, isTelemetryFresh = viewerGpsTs > 0 && (now - viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS), isGpsFresh = vAge in 0..GPS_UI_FAIL_THRESHOLD_MS) 
                    }
                    val tAge = if(lastGpsTs > 0) now - lastGpsTs else -1L
                    Box(modifier = Modifier.weight(1f)) { 
                        StatusRowData(label = trkIdLabel, battery = battery, commIndex = if(isPeerActive) remoteCommIndex else 0, color = if(isPeerActive) BrandJd else Slate500, isCharging = remoteCharging, accuracy = trackerAccuracy, maxAccuracy = maxTrackerAccuracy, satsView = satsView, satsUsed = satsUsed, gpsAgeMs = tAge, temp = trackerTemp, distance = distToHome, isRemote = true, isPeerActive = isPeerActive, isLocPending = isTrackerLocPending, locPendingReason = trackerLocPendingReason, isTelemetryFresh = isTelemetryFresh, isGpsFresh = isGpsFresh) 
                    }
                }
            } else {
                if (mode == "viewer") {
                    val vAge = if(viewerGpsTs > 0) now - viewerGpsTs else -1L
                    StatusRowData(label = viewIdLabel, battery = battery, commIndex = commIndex, color = ViewerCyan, overrideDistanceColor = BrandJd, isCharging = isCharging, accuracy = viewerAccuracy, maxAccuracy = maxViewerAccuracy, temp = viewerTemp, distance = distToViewer, satsUsed = viewerSatsUsed, satsView = viewerSatsView, gpsAgeMs = vAge, horizontalPadding = 8.dp, isLocPending = isViewerLocPending, locPendingReason = viewerLocPendingReason, isTelemetryFresh = viewerGpsTs > 0 && (now - viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS), isGpsFresh = vAge in 0..GPS_UI_FAIL_THRESHOLD_MS)
                    Spacer(modifier = Modifier.height(3.dp))
                }
                val tAge = if(lastGpsTs > 0) now - lastGpsTs else -1L
                StatusRowData(label = trkIdLabel, battery = if (mode == "viewer") remoteBattery else battery, commIndex = if (mode == "viewer") (if(isPeerActive) remoteCommIndex else 0) else commIndex, color = if (mode == "viewer" && !isPeerActive) Slate500 else BrandJd, isCharging = if (mode == "viewer") remoteCharging else isCharging, accuracy = trackerAccuracy, maxAccuracy = maxTrackerAccuracy, satsView = satsView, satsUsed = satsUsed, gpsAgeMs = tAge, temp = trackerTemp, distance = distToHome, horizontalPadding = 8.dp, isRemote = mode == "viewer", isPeerActive = if(mode == "viewer") isPeerActive else true, isLocPending = isTrackerLocPending, locPendingReason = trackerLocPendingReason, isTelemetryFresh = if (mode == "tracker") (viewerGpsTs > 0 && (now - viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS)) else isTelemetryFresh, isGpsFresh = isGpsFresh)
            }
        }
    }
}

@Composable
fun StatusRowData(
    label: String, battery: Int, commIndex: Int, color: Color, isCharging: Boolean = false, accuracy: Float = 0f, maxAccuracy: Float = 0f, 
    satsView: Int = 0, satsUsed: Int = 0, gpsAgeMs: Long = -1L, temp: Float = 0f, distance: Double? = null, horizontalPadding: androidx.compose.ui.unit.Dp = 1.dp,
    isRemote: Boolean = false, isPeerActive: Boolean = true, overrideDistanceColor: Color? = null, 
    isLocPending: Boolean = false, locPendingReason: LocationPendingReason = LocationPendingReason.NONE, isTelemetryFresh: Boolean = true, isGpsFresh: Boolean = true
) {
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val isConnStale = isRemote && !isPeerActive
    val telemetryColor = if (isTelemetryFresh && !isConnStale) color else Slate500
    val contentColor = if (isConnStale) Slate500 else color
    val distColor = if (isTelemetryFresh && !isConnStale) (overrideDistanceColor ?: color) else Slate500
    val infiniteTransition = rememberInfiniteTransition(label = "HandshakeAnimations")
    val handshakeAlpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "HandshakeAlpha")
    val animatedBattery by animateIntAsState(battery, tween(1500), label = "BatteryAnim")

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.width(210.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.width(48.dp), verticalAlignment = Alignment.CenterVertically) {
                 val alpha by animateFloatAsState(if (isConnStale) 0.5f else 1f, label = "LabelAlpha")
                 Text(text = label, color = contentColor.copy(alpha = if (isConnStale) handshakeAlpha else alpha), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                 if (isLocPending) Box(modifier = Modifier.padding(start = 1.dp).background(Amber500, RoundedCornerShape(1.dp)).padding(horizontal = 1.dp)) { 
                     Text(text = "P", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold, style = compactStyle) 
                 }
            }
            if (isConnStale) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = ">>> WAITING FOR TELEMETRY <<<", color = Slate500.copy(alpha = handshakeAlpha), fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, style = compactStyle, modifier = Modifier.padding(horizontal = 2.dp))
            } else {
                Row(modifier = Modifier.width(54.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.width(10.dp)) { 
                        if (isCharging) Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(10.dp), tint = if(!isConnStale && isTelemetryFresh) Amber500 else Slate500) 
                    }
                    Icon(imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull, contentDescription = null, modifier = Modifier.size(10.dp), tint = if (isConnStale || !isTelemetryFresh) Slate500 else if (battery in 0..19) Rose500 else telemetryColor)
                    Spacer(modifier = Modifier.width(2.dp)); Text(text = if(battery >= 0) "$animatedBattery%" else "--%", color = telemetryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
                Row(modifier = Modifier.width(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "°", color = telemetryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp), style = compactStyle)
                    Text(text = String.format(Locale.getDefault(), "%.0f", temp), color = telemetryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.width(20.dp)) { CommBar(index = commIndex, color = if (isTelemetryFresh) contentColor else Slate500) }
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.width(34.dp)) { Text(text = "$satsUsed/$satsView", color = telemetryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle) }
                Box(modifier = Modifier.width(26.dp)) {
                    val ageStr = if (gpsAgeMs != -1L) { val ageSec = (maxOf(0L, gpsAgeMs) / 1000).toInt(); when { ageSec < 100 -> "${ageSec}s"; ageSec < 3600 -> "${ageSec/60}m"; else -> ">1h" } } else "--s"
                    Text(text = ageStr, color = if (!isGpsFresh) Slate500 else color, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
            }
        }
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            if (!isConnStale) {
                if (isLocPending && locPendingReason != LocationPendingReason.NONE) {
                    Text(text = locPendingReason.name.replace("_", " "), color = Amber500, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false), textAlign = TextAlign.End, style = compactStyle)
                } else {
                    fun formatAcc(v: Float): String = when { v >= 10000f -> "${(v / 1000).toInt()}k"; v >= 1000f -> String.format(Locale.getDefault(), "%.1fk", v / 1000f); else -> v.toInt().toString() }
                    val accColor = if (isTelemetryFresh) (if (!isGpsFresh) Slate500 else color) else Slate500
                    val accText = "${if (accuracy > 0) "±${formatAcc(accuracy)}" else ""} ${if (maxAccuracy > 0) "(±${formatAcc(maxAccuracy)})" else ""}".trim()
                    if (accText.isNotEmpty()) Text(text = accText, color = accColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false), textAlign = TextAlign.End, style = compactStyle)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.width(62.dp)) {
                    val animatedDistance by animateFloatAsState(if (distance == null || distance.isNaN()) 0f else distance.toFloat(), tween(1200), label = "DistAnim")
                    val distStr = when { distance == null || distance.isNaN() -> "--"; animatedDistance >= 9000 -> String.format(Locale.getDefault(), "%.0fkm", animatedDistance / 1000.0); animatedDistance >= 1000 -> String.format(Locale.getDefault(), "%.1fkm", animatedDistance / 1000.0); else -> "${animatedDistance.toInt()}m" }
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
