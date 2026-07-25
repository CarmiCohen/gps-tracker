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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*
import com.gps19.core.engine.*

/**
 * Shared UI Components for GPS Tracker.
 * July.25.01:
 * - Issue #547c: State Decomposition Refinement. Updated GlobalStatusBar to 
 *   consume isRedScreenVisible from TelemetryState for zero-latency surfacing.
 */

enum class RibbonRenderType { BAR, LINE }

@Composable
fun RibbonsOverlay(viewModel: MainViewModel, onDismiss: () -> Unit) {
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
                    AnalyticalRibbons(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AnalyticalRibbons(viewModel: MainViewModel) {
    var selectedScale by remember { mutableStateOf("4M") }
    
    val activeHistoryFlow = when(selectedScale) {
        "16M" -> viewModel.history16MFlow
        "1H" -> viewModel.history1HFlow
        "4H" -> viewModel.history4HFlow
        "24H" -> viewModel.history24HFlow
        "7D" -> viewModel.history7DFlow
        else -> viewModel.history4MFlow
    }

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
        }

        // Consolidated Connection & Health Ribbon
        StatefulConnectionRibbon(activeHistoryFlow, selectedScale)
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))

        // Synchronized Sensor Stack
        StatefulSensorRibbon(activeHistoryFlow, "SNR", selectedScale, lineColor = Color(0xFF38BDF8), valueSelector = { it.snrIdx.toFloat() })
        StatefulSensorRibbon(activeHistoryFlow, "NOI", selectedScale, lineColor = Amber500, valueSelector = { it.noiseIdx.toFloat() })
        StatefulSensorRibbon(activeHistoryFlow, "LUX", selectedScale, lineColor = Color.White, valueSelector = { it.luxIdx.toFloat() })
        StatefulSensorRibbon(activeHistoryFlow, "VIB", selectedScale, lineColor = Color.Magenta, valueSelector = { it.vibeIdx.toFloat() })
        StatefulSensorRibbon(activeHistoryFlow, "PRX", selectedScale, lineColor = Rose500, renderType = RibbonRenderType.BAR, valueSelector = { it.proxIdx.toFloat() })
        StatefulSensorRibbon(activeHistoryFlow, "LIF", selectedScale, lineColor = Color(0xFFFACC15), valueSelector = { it.liftIdx.toFloat() })
        StatefulSensorRibbon(activeHistoryFlow, "BAT", selectedScale, lineColor = Rose500, renderType = RibbonRenderType.BAR, valueSelector = { if (it.isBatterySteepDischarge) 1f else 0f })
        StatefulSensorRibbon(activeHistoryFlow, "THM", selectedScale, lineColor = Color.Red, renderType = RibbonRenderType.BAR, valueSelector = { if (it.isCoolingModeActive) 1f else 0f })
        StatefulSensorRibbon(activeHistoryFlow, "CUR", selectedScale, lineColor = Color(0xFFFB923C), valueSelector = { (kotlin.math.abs(it.currentMa).toFloat() / RIBBON_CURRENT_SCALE_MA.toFloat()).coerceIn(0f, 1f) })
        StatefulSensorRibbon(activeHistoryFlow, "SIT", selectedScale, lineColor = BrandJd, renderType = RibbonRenderType.BAR, valueSelector = { if (it.isSitActive) 1f else 0f })
        StatefulSensorRibbon(activeHistoryFlow, "TLT", selectedScale, lineColor = Color(0xFF818CF8), valueSelector = { it.tiltIdx.toFloat() })
        StatefulSensorRibbon(activeHistoryFlow, "BAR", selectedScale, lineColor = Color(0xFF2DD4BF), valueSelector = { it.baroIdx.toFloat() })
        StatefulSensorRibbon(activeHistoryFlow, "SVZ", selectedScale, lineColor = Violet500, valueSelector = { (kotlin.math.abs(it.sitVz).toFloat() / 2.0f).coerceIn(0f, 1f) })
        StatefulSensorRibbon(activeHistoryFlow, "SDZ", selectedScale, lineColor = Violet500, valueSelector = { (kotlin.math.abs(it.sitDz).toFloat() / 0.5f).coerceIn(0f, 1f) })
    }
}

@Composable
fun ForensicRibbonContainer(
    title: String,
    titleColor: Color,
    height: androidx.compose.ui.unit.Dp,
    history: List<ConnectionPoint>,
    scale: String,
    content: androidx.compose.ui.graphics.drawscope.DrawScope.(
        totalPoints: Float,
        pointWidth: Float,
        baseLineY: Float,
        maxHeight: Float,
        isLandscape: Boolean
    ) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
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
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    val totalPoints = MAX_HISTORY_POINTS_PER_RIBBONS.toFloat()
                    val pointWidth = size.width / totalPoints
                    val maxHeight = size.height * 0.8f
                    val baseLineY = size.height * 0.9f
                    
                    // Unified Grid/Baseline
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(0f, baseLineY),
                        end = Offset(size.width, baseLineY),
                        strokeWidth = 0.5.dp.toPx()
                    )

                    // Draw Scale Ticks
                    val intervalMs = when(scale) {
                        "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 1 * 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 0L
                    }
                    val alignMs = when(scale) {
                        "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 1L
                    }
                    val firstTs = history.firstOrNull()?.ts ?: 0L
                    val baseTickTs = ((firstTs + alignMs - 1) / alignMs) * alignMs

                    history.forEachIndexed { index, p ->
                        val xPos = (totalPoints - history.size + index) * pointWidth
                        
                        // Scale Ticks
                        if (intervalMs > 0 && p.ts >= baseTickTs) {
                            val tickCount = (p.ts - baseTickTs) / intervalMs
                            val prevTickCount = if (index > 0) {
                                if (history[index - 1].ts >= baseTickTs) (history[index - 1].ts - baseTickTs) / intervalMs else -1L
                            } else -2L
                            if (tickCount >= 0 && tickCount > prevTickCount) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.2f),
                                    start = Offset(xPos, baseLineY - maxHeight),
                                    end = Offset(xPos, baseLineY + 2.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }

                        // Explicit Black Gap Visualization (R106)
                        if (p.isGap) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(xPos, baseLineY - maxHeight),
                                size = Size(maxOf(1f, pointWidth), maxHeight + 2.dp.toPx())
                            )
                        }
                    }

                    // Delegate Content Rendering
                    content(totalPoints, pointWidth, baseLineY, maxHeight, isLandscape)
                }
            }
        )
    }
}

@Composable
fun StatefulSensorRibbon(
    flow: StateFlow<List<ConnectionPoint>>,
    title: String,
    scale: String,
    lineColor: Color,
    renderType: RibbonRenderType = RibbonRenderType.LINE,
    valueSelector: (ConnectionPoint) -> Float
) {
    val history by flow.collectAsStateWithLifecycle()
    GenericSensorRibbon(history, title, scale, lineColor, renderType, valueSelector)
}

@Composable
fun GenericSensorRibbon(
    history: List<ConnectionPoint>, 
    title: String, 
    scale: String,
    lineColor: Color, 
    renderType: RibbonRenderType = RibbonRenderType.LINE,
    valueSelector: (ConnectionPoint) -> Float
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val boxHeight = if (isLandscape) 40.dp else 24.dp
    
    ForensicRibbonContainer(
        title = title,
        titleColor = lineColor,
        height = boxHeight,
        history = history,
        scale = scale
    ) { totalPoints, pointWidth, baseLineY, maxHeight, landscape ->
        var lastPos: Offset? = null
        history.forEachIndexed { index, p ->
            if (p.isGap) {
                lastPos = null
                return@forEachIndexed
            }

            val xPos = (totalPoints - history.size + index) * pointWidth
            val value = valueSelector(p).coerceIn(0f, 1f)
            
            if (renderType == RibbonRenderType.BAR) {
                val barHeight = value * maxHeight
                drawRect(lineColor.copy(alpha = 0.6f), Offset(xPos, baseLineY - barHeight), Size(maxOf(1f, pointWidth), barHeight))
            } else {
                val yPos = baseLineY - (value * maxHeight)
                val currentPos = Offset(xPos + (maxOf(1f, pointWidth) / 2f), yPos)
                
                lastPos?.let { lp ->
                    if (currentPos.x - lp.x < pointWidth * 10) {
                        drawLine(
                            color = lineColor, 
                            start = lp, 
                            end = currentPos, 
                            strokeWidth = (if (landscape) 1.5.dp.toPx() else 1.dp.toPx())
                        )
                    }
                }
                if (value > 0.05f) {
                    drawCircle(
                        color = lineColor, 
                        radius = (if (landscape) 1.5.dp.toPx() else 1.dp.toPx()), 
                        center = currentPos
                    )
                }
                lastPos = currentPos
            }
        }
    }
}

@Composable
fun StatefulConnectionRibbon(
    flow: StateFlow<List<ConnectionPoint>>,
    scale: String
) {
    val history by flow.collectAsStateWithLifecycle()
    ConnectionQualityRibbon(history, scale)
}

@Composable
fun ConnectionQualityRibbon(history: List<ConnectionPoint>, scale: String) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val boxHeight = if (isLandscape) 60.dp else 34.dp
    
    ForensicRibbonContainer(
        title = scale,
        titleColor = Color.Gray,
        height = boxHeight,
        history = history,
        scale = scale
    ) { totalPoints, pointWidth, connectionBaseY, _, landscape ->
        val ribbonMaxHeight = if (landscape) 16.dp.toPx() else 10.dp.toPx()
        // Connection baseline is slightly above the container's baseline to allow for labels
        val effectiveBaseY = connectionBaseY - (if (landscape) 4.dp.toPx() else 2.dp.toPx())
        var lastGpsPos: Offset? = null
        val cyanColor = Color(0xFF00FFFF)

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = (if (landscape) 10.sp.toPx() else 7.sp.toPx())
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }

        val intervalMs = when(scale) {
            "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 1 * 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 0L
        }
        val alignMs = when(scale) {
            "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 1L
        }
        val firstTs = history.firstOrNull()?.ts ?: 0L
        val baseTickTs = ((firstTs + alignMs - 1) / alignMs) * alignMs

        history.forEachIndexed { index, p ->
            val xPos = (totalPoints - history.size + index) * pointWidth
            
            if (!p.isGap) {
                val pColor = if (p.isConnected) BrandJd else Rose500
                val commIdx = if (p.isConnected) TelemetryUtils.calculateCommIndex(p.rtt, p.remoteSig, p.localSig) else 0
                val hFactor = if (p.isConnected) (commIdx.toFloat() / 10f).coerceIn(0.1f, 1f) else 1f
                
                drawRect(
                    color = pColor, 
                    topLeft = Offset(xPos, effectiveBaseY - (ribbonMaxHeight * hFactor)), 
                    size = Size(maxOf(1f, pointWidth), ribbonMaxHeight * hFactor)
                )
                
                // Chain GPS Overlay
                if (p.hasGps && p.gpsIndex > 0.0) {
                    val dotRadius = (if (landscape) 1.2.dp.toPx() else 0.8.dp.toPx())
                    val baseHeight = if (landscape) 24.dp.toPx() else 14.dp.toPx()
                    val normalizedHeight = (p.gpsIndex.toFloat().coerceIn(0f, 1f) * baseHeight)
                    val yPos = effectiveBaseY - ribbonMaxHeight - (if (landscape) 4.dp.toPx() else 2.dp.toPx()) - normalizedHeight
                    val currentPos = Offset(xPos + (maxOf(1f, pointWidth) / 2f), yPos)
                    
                    lastGpsPos?.let { lastPos ->
                        if (currentPos.x - lastPos.x < pointWidth * 10) {
                            drawLine(
                                color = cyanColor, 
                                start = lastPos, 
                                end = currentPos, 
                                strokeWidth = (if (landscape) 1.dp.toPx() else 0.5.dp.toPx())
                            )
                        }
                    }
                    drawCircle(color = cyanColor, radius = dotRadius, center = currentPos)
                    lastGpsPos = currentPos
                }
            } else {
                lastGpsPos = null
            }

            // Time Labels on Connection Ribbon
            if (intervalMs > 0 && history.isNotEmpty() && p.ts >= baseTickTs) {
                val tickCount = (p.ts - baseTickTs) / intervalMs
                val prevTickCount = if (index > 0) {
                    if (history[index - 1].ts >= baseTickTs) (history[index - 1].ts - baseTickTs) / intervalMs else -1L
                } else -2L

                if (tickCount >= 0 && tickCount > prevTickCount) {
                    val timeStr = if (scale == "7D") dateFormatter.format(Date(p.ts)) else timeFormatter.format(Date(p.ts))
                    drawIntoCanvas { canvas ->
                        val finalX = xPos.coerceIn(18.dp.toPx(), size.width - 18.dp.toPx())
                        canvas.nativeCanvas.drawText(
                            timeStr, 
                            finalX, 
                            size.height - (if (landscape) 4.dp.toPx() else 2.dp.toPx()), 
                            textPaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalStatusBar(
    uiState: MainUiState, 
    telemetryState: TelemetryState,
    dashboardState: DashboardState, 
    systemPulse: Long, 
    rttFlow: StateFlow<Int>,
    remoteSignalFlow: StateFlow<Int>,
    modifier: Modifier = Modifier
) {
    val mode = uiState.appMode ?: return
    val isLocalOnline = telemetryState.connectivity.isLocalOnline
    val isRelayConnected = telemetryState.connectivity.isRelayConnected
    val lastIncomingActivity = telemetryState.connectivity.lastRemoteActivityTs
    
    // Rationale: isPeerActive uses receipt time already, but we align it with isTelemetryFresh.
    val isPeerActive = dashboardState.isTelemetryFresh
    
    val rtt by rttFlow.collectAsStateWithLifecycle()
    val remoteSignal by remoteSignalFlow.collectAsStateWithLifecycle()
    val redScreenVisible = telemetryState.isRedScreenVisible

    val commIndex = if (uiState.isSystemActive && isRelayConnected) {
        TelemetryUtils.calculateCommIndex(rtt, 10, 10)
    } else 0
    
    val remoteCommIndex = if (mode == "viewer" && isPeerActive) {
        TelemetryUtils.calculateCommIndex(rtt, remoteSignal, 10)
    } else 0

    val loc = if (mode == "viewer") telemetryState.trackerLocation else telemetryState.localLocation
    val health = if (mode == "viewer") telemetryState.trackerHealth else telemetryState.localHealth
    val lastGpsTs = loc.timestamp
    
    // Issue #044: Differentiate Local vs Tracker GPS Health for HUD top-level badges.
    // Fixed: Now relies on dashboardState skew-immune logic.
    val isLocalGpsActive = if (mode == "tracker") dashboardState.isGpsFresh else (systemPulse - telemetryState.localLocation.timestamp < GPS_UI_FAIL_THRESHOLD_MS)

    // Issue #049: Ensure Tracker health badge follows active context (local or remote).
    val isTrackerGpsActive = dashboardState.isGpsFresh

    val isDataHealthy = dashboardState.isTelemetryFresh && isLocalOnline && isRelayConnected

    val lastTelemetryTs = maxOf(loc.timestamp, loc.telemetryTs)
    val progressPulse = if (mode == "tracker") lastIncomingActivity else lastTelemetryTs

    val speedValueMps = if (mode == "viewer") telemetryState.trackerLocation.speed else telemetryState.localLocation.speed
    val hasUnresolved = telemetryState.activeAlarms.any { !it.isResolved }

    StatusBar(
        modifier = modifier, isInternet = isLocalOnline, isRelay = isRelayConnected, isPeerActive = isPeerActive, isDataHealthy = isDataHealthy, 
        isLocalGpsActive = isLocalGpsActive, isTrackerGpsActive = isTrackerGpsActive,
        mode = mode, battery = telemetryState.battery.level, lastP = progressPulse, 
        commIndex = commIndex, remoteCommIndex = remoteCommIndex, remoteBattery = if (mode == "viewer") telemetryState.trackerBattery.level else -1, 
        isCharging = telemetryState.battery.isChargingStable, remoteCharging = if (mode == "viewer") telemetryState.trackerBattery.isChargingStable else false,
        speedMps = speedValueMps.toFloat(), trackerAccuracy = loc.accuracy.toFloat(),
        maxTrackerAccuracy = loc.maxAccuracy.toFloat(), 
        viewerAccuracy = if (telemetryState.localLocation.lat != 0.0) telemetryState.localLocation.accuracy.toFloat() else 0f,
        maxViewerAccuracy = telemetryState.localLocation.maxAccuracy.toFloat(), now = systemPulse, satsView = telemetryState.trackerSatsView, satsUsed = telemetryState.trackerSatsUsed,
        trackerTemp = telemetryState.trackerBattery.temp.toFloat(), viewerTemp = telemetryState.battery.temp.toFloat(), distToHome = telemetryState.distanceTrackerToHome, distToViewer = telemetryState.distanceTrackerToViewer,
        viewerSatsUsed = if (mode == "viewer") telemetryState.viewerSatsUsed else 0, viewerSatsView = if (mode == "viewer") telemetryState.viewerSatsView else 0,
        viewerGpsTs = telemetryState.localLocation.timestamp, trackerId = uiState.deviceId, viewerId = uiState.viewerId, watchdogOk = dashboardState.watchdogOk,
        trackerState = dashboardState.trackerState, hasActiveAlarms = hasUnresolved, isRedScreenSuppressed = (hasUnresolved && !redScreenVisible),
        isSirenPlaying = telemetryState.isSirenPlaying,
        isTrackerLocPending = health.isLocationPending, 
        trackerLocPendingReason = health.locationPendingReason,
        isViewerLocPending = telemetryState.localHealth.isLocationPending,
        viewerLocPendingReason = telemetryState.localHealth.locationPendingReason,
        lastGpsTs = lastGpsTs,
        isTelemetryFresh = dashboardState.isTelemetryFresh,
        isGpsFresh = dashboardState.isGpsFresh
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
    val alarmAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
        label = "AlarmAlpha"
    )
    val movingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "MovingAlpha"
    )

    Card(modifier = modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isLandscape) 0.7f else 0.9f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 3.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // R960: Form "Local Capability" Block (INT, SRV, GPS)
                    StatusBadge("INT", isInternet, isBold = true)
                    StatusBadge("SRV", isRelay, isBold = true)
                    StatusBadge("GPS", isLocalGpsActive)
                    
                    val peerLabel = if (mode == "tracker") "VWR" else "TRK"
                    StatusBadge(peerLabel, isPeerActive, activeColor = BrandJd)
                    
                    if (mode != "tracker") {
                        StatusBadge("DAT", isDataHealthy)
                    }
                    
                    if (hasActiveAlarms) {
                        StatusBadge("ALM", true, activeColor = Rose500.copy(alpha = alarmAlpha), isBold = true)
                    }

                    if (isRedScreenSuppressed) {
                         Spacer(Modifier.width(2.dp))
                         val badgeColor = if (isSirenPlaying) Rose500.copy(alpha = alarmAlpha) else Slate500
                         val label = if (isSirenPlaying) "SIREN LOCKOUT" else "LOCKOUT"
                         Box(Modifier.background(badgeColor, RoundedCornerShape(2.dp)).padding(horizontal = 2.dp)) {
                             Text(label, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, style = compactStyle)
                         }
                    }

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp)) { 
                        CircularProgressIndicator(progress = { progressValue }, color = if (isDataHealthy) BrandJd else Rose500, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Icon(imageVector = if (isDataHealthy) Icons.Default.CheckCircle else Icons.Default.Error, tint = if (isDataHealthy) BrandJd else Rose500, contentDescription = null, modifier = Modifier.size(8.dp))
                    }
                    Text(text = if(watchdogOk) "OK" else "FAIL", color = if(watchdogOk) BrandJd else Rose500, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
                Spacer(Modifier.width(8.dp))
                
                // Issue #044: State label reflects Tracker health.
                val isMoving = trackerState == TrackerState.MOVING
                val stateColor = if (!isTrackerGpsActive) Slate500 else BrandJd 
                
                Text(
                    text = if (isMoving && isTrackerGpsActive) "»\u2009${trackerState.name}\u2009«" else trackerState.name,
                    color = stateColor.copy(alpha = if (isMoving && isTrackerGpsActive) movingAlpha else 1f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = compactStyle
                )
                
                Spacer(Modifier.weight(1f))

                // Issue #044: Speed reflects Tracker health.
                val speedTargetKph = if (isTrackerGpsActive && !speedMps.isNaN()) speedMps * 3.6f else 0f
                val animatedSpeed by animateFloatAsState(targetValue = speedTargetKph, animationSpec = tween(1000), label = "SpeedAnim")
                
                val speedVal = if (animatedSpeed < 10.0f) String.format(Locale.getDefault(), "%.1f", animatedSpeed) else animatedSpeed.toInt().toString()
                val speedColor = if (isTrackerGpsActive) BrandJd else Slate500
                Text(text = "${speedVal}km/h", color = speedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle, textAlign = TextAlign.End)
            }
            Spacer(Modifier.height(3.dp))

            if (isLandscape && mode == "viewer") {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    val vAge = if(viewerGpsTs > 0) now - viewerGpsTs else -1L
                    val isLocalTelemetryFresh = viewerGpsTs > 0 && (now - viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS)
                    
                    Box(modifier = Modifier.weight(1f)) { StatusRowData(label = viewIdLabel, battery = battery, commIndex = commIndex, color = ViewerCyan, overrideDistanceColor = BrandJd, isCharging = isCharging, accuracy = viewerAccuracy, maxAccuracy = maxViewerAccuracy, temp = viewerTemp, distance = distToViewer, satsUsed = viewerSatsUsed, satsView = viewerSatsView, gpsAgeMs = vAge, isRemote = false, isLocPending = isViewerLocPending, locPendingReason = viewerLocPendingReason, isTelemetryFresh = isLocalTelemetryFresh, isGpsFresh = vAge in 0..GPS_UI_FAIL_THRESHOLD_MS) }
                    
                    val tAge = if(lastGpsTs > 0) now - lastGpsTs else -1L
                    Box(modifier = Modifier.weight(1f)) { StatusRowData(label = trkIdLabel, battery = battery, commIndex = if(isPeerActive) remoteCommIndex else 0, color = if(isPeerActive) BrandJd else Slate500, isCharging = remoteCharging, accuracy = trackerAccuracy, maxAccuracy = maxViewerAccuracy, satsView = satsView, satsUsed = satsUsed, gpsAgeMs = tAge, temp = trackerTemp, distance = distToHome, isRemote = true, isPeerActive = isPeerActive, isLocPending = isTrackerLocPending, locPendingReason = trackerLocPendingReason, isTelemetryFresh = isTelemetryFresh, isGpsFresh = isGpsFresh) }
                }
            } else {
                if (mode == "viewer") {
                    val vAge = if(viewerGpsTs > 0) now - viewerGpsTs else -1L
                    val isLocalTelemetryFresh = viewerGpsTs > 0 && (now - viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS)
                    StatusRowData(label = viewIdLabel, battery = battery, commIndex = commIndex, color = ViewerCyan, overrideDistanceColor = BrandJd, isCharging = isCharging, accuracy = viewerAccuracy, maxAccuracy = maxViewerAccuracy, temp = viewerTemp, distance = distToViewer, satsUsed = viewerSatsUsed, satsView = viewerSatsView, gpsAgeMs = vAge, horizontalPadding = 8.dp, isRemote = false, isLocPending = isViewerLocPending, locPendingReason = viewerLocPendingReason, isTelemetryFresh = isLocalTelemetryFresh, isGpsFresh = vAge in 0..GPS_UI_FAIL_THRESHOLD_MS)
                    Spacer(Modifier.height(3.dp))
                }
                val trkColor = if (mode == "viewer" && !isPeerActive) Slate500 else BrandJd
                val tAge = if(lastGpsTs > 0) now - lastGpsTs else -1L
                val effectiveTrkTelemetryFresh = if (mode == "tracker") (viewerGpsTs > 0 && (now - viewerGpsTs < TELEMETRY_UI_STALE_THRESHOLD_MS)) else isTelemetryFresh

                StatusRowData(label = trkIdLabel, battery = if (mode == "viewer") remoteBattery else battery, commIndex = if (mode == "viewer") (if(isPeerActive) remoteCommIndex else 0) else commIndex, color = trkColor, isCharging = if (mode == "viewer") remoteCharging else isCharging, accuracy = trackerAccuracy, maxAccuracy = maxTrackerAccuracy, satsView = satsView, satsUsed = satsUsed, gpsAgeMs = tAge, temp = trackerTemp, distance = distToHome, horizontalPadding = 8.dp, isRemote = mode == "viewer", isPeerActive = if(mode == "viewer") isPeerActive else true, isLocPending = isTrackerLocPending, locPendingReason = trackerLocPendingReason, isTelemetryFresh = effectiveTrkTelemetryFresh, isGpsFresh = isGpsFresh)
            }
        }
    }
}

@Composable
fun StatusRowData(
    label: String, battery: Int, commIndex: Int, color: Color, isCharging: Boolean = false, accuracy: Float = 0f, maxAccuracy: Float = 0f, 
    satsView: Int = 0, satsUsed: Int = 0, gpsAgeMs: Long = -1L, temp: Float = 0f, distance: Double? = null, horizontalPadding: androidx.compose.ui.unit.Dp = 1.dp,
    isRemote: Boolean = false, isPeerActive: Boolean = true, overrideDistanceColor: Color? = null, 
    isLocPending: Boolean = false,
    locPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    isTelemetryFresh: Boolean = true,
    isGpsFresh: Boolean = true
) {
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    
    val isConnStale = isRemote && !isPeerActive
    val telemetryColor = if (isTelemetryFresh && !isConnStale) color else Slate500
    val contentColor = if (isConnStale) Slate500 else color
    val gpsColor = if (!isGpsFresh) Slate500 else color
    val distColor = if (isTelemetryFresh && !isConnStale) (overrideDistanceColor ?: color) else Slate500

    val infiniteTransition = rememberInfiniteTransition(label = "HandshakeAnimations")
    val handshakeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "HandshakeAlpha"
    )

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.width(210.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.width(48.dp), verticalAlignment = Alignment.CenterVertically) {
                 val animatedLabelAlpha by animateFloatAsState(targetValue = if (isConnStale) 0.5f else 1f, label = "LabelAlpha")
                 Text(text = label, color = contentColor.copy(alpha = if (isConnStale) handshakeAlpha else animatedLabelAlpha), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                 if (isLocPending) {
                     Box(Modifier.padding(start = 1.dp).background(Amber500, RoundedCornerShape(1.dp)).padding(horizontal = 1.dp)) {
                         Text("P", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold, style = compactStyle)
                     }
                 }
            }
            if (isConnStale) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = ">>> WAITING FOR TELEMETRY <<<", 
                    color = Slate500.copy(alpha = handshakeAlpha), 
                    fontSize = 8.sp, 
                    fontWeight = FontWeight.Black, 
                    fontFamily = FontFamily.Monospace, 
                    style = compactStyle,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            } else {
                val animatedBattery by animateIntAsState(targetValue = battery, animationSpec = tween(1500), label = "BatteryAnim")
                Row(modifier = Modifier.width(54.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(10.dp), contentAlignment = Alignment.Center) { if (isCharging) Icon(Icons.Default.Bolt, null, tint = if(!isConnStale && isTelemetryFresh) Amber500 else Slate500, modifier = Modifier.size(10.dp)) }
                    Icon(if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull, null, tint = if (isConnStale || !isTelemetryFresh) Slate500 else if (battery in 0..19) Rose500 else telemetryColor, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(2.dp)); Text(text = if(battery >= 0) "$animatedBattery%" else "--%", color = telemetryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
                Row(modifier = Modifier.width(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "°", color = telemetryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp), style = compactStyle)
                    Text(text = String.format(Locale.getDefault(), "%.0f", temp), color = telemetryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
                Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) { CommBar(commIndex, if (isTelemetryFresh) contentColor else Slate500) }
                Spacer(Modifier.width(4.dp))
                Box(modifier = Modifier.width(34.dp)) { Text(text = "$satsUsed/$satsView", color = telemetryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle) }
                Box(modifier = Modifier.width(26.dp)) {
                    val ageStr = if (gpsAgeMs != -1L) {
                        val ageSec = (maxOf(0L, gpsAgeMs) / 1000).toInt()
                        when { ageSec < 100 -> "${ageSec}s"; ageSec < 3600 -> "${ageSec/60}m"; else -> ">1h" }
                    } else "--s"
                    Text(text = ageStr, color = gpsColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle)
                }
            }
        }
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            if (!isConnStale) {
                if (isLocPending && locPendingReason != LocationPendingReason.NONE) {
                    Text(
                        text = locPendingReason.name.replace("_", " "),
                        color = Amber500,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        textAlign = TextAlign.End,
                        style = compactStyle
                    )
                } else {
                    fun formatAcc(v: Float): String = when { v >= 10000f -> "${(v / 1000).toInt()}k"; v >= 1000f -> String.format(Locale.getDefault(), "%.1fk", v / 1000f); else -> v.toInt().toString() }
                    val accColor = if (isTelemetryFresh) gpsColor else Slate500
                    
                    val rawText = if (accuracy > 0) "±${formatAcc(accuracy)}" else ""
                    val maxText = if (maxAccuracy > 0) "(±${formatAcc(maxAccuracy)})" else ""
                    val accText = "$rawText $maxText".trim()

                    if (accText.isNotEmpty()) Text(
                        text = accText, 
                        color = accColor, 
                        fontSize = 8.sp, 
                        fontWeight = FontWeight.Bold, 
                        fontFamily = FontFamily.Monospace, 
                        maxLines = 1,  softWrap = false, overflow = TextOverflow.Ellipsis, 
                        modifier = Modifier.weight(1f, fill = false),
                        textAlign = TextAlign.End,
                        style = compactStyle
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.width(62.dp), contentAlignment = Alignment.CenterEnd) {
                    val animatedDistance by animateFloatAsState(targetValue = if (distance == null || distance.isNaN()) 0f else distance.toFloat(), animationSpec = tween(1200), label = "DistAnim")
                    val distStr = when { 
                        distance == null || distance.isNaN() -> "--"
                        animatedDistance >= 9000 -> String.format(Locale.getDefault(), "%.0fkm", animatedDistance / 1000.0)
                        animatedDistance >= 1000 -> String.format(Locale.getDefault(), "%.1fkm", animatedDistance / 1000.0)
                        else -> "${animatedDistance.toInt()}m" 
                    }
                    Text(text = distStr, color = distColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, style = compactStyle, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
fun CommBar(index: Int, color: Color) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(0.6.dp)) { repeat(10) { i -> Box(Modifier.width(1.2.dp).height((2.5 + (i * 0.8)).dp).background(if (i < index) color else Slate500.copy(alpha = 0.6f))) } }
}

@Composable
fun StatusBadge(label: String, active: Boolean, activeColor: Color = BrandJd, isBold: Boolean = true) {
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val textColor = if (active) activeColor else Rose500
    
    Text(
        text = label, 
        color = textColor, 
        fontSize = 9.sp, 
        fontWeight = if(isBold) FontWeight.ExtraBold else FontWeight.Bold, 
        fontFamily = FontFamily.Monospace, 
        maxLines = 1, 
        style = compactStyle
    )
}

@Composable
fun HeaderBar(
    uiState: MainUiState, 
    onDashboard: () -> Unit = {}, 
    onS: () -> Unit = {}, 
    onL: () -> Unit = {}, 
    onM: () -> Unit = {},
    onR: () -> Unit = {}, 
    onEvent: (UiEvent) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val nav = uiState.navigation
    val isAnyOverlayOpen = nav.isLogVisible || nav.isSettingsOpen || nav.isRibbonsVisible
    val isDashboardActive = !nav.isMapVisible && !isAnyOverlayOpen
    val topPadding = if (uiState.permissions.requiresExtraTopPadding) 8.dp else 2.dp
    
    val commitAnd = { action: () -> Unit ->
        if (uiState.navigation.isSettingsOpen) {
            onEvent(UiEvent.CommitSettings)
        }
        action()
    }

    val alertPulse = rememberInfiniteTransition(label = "AlertPulse")
    val alertAlpha by alertPulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "Alpha"
    )

    if (isLandscape) {
        Column(
            modifier = Modifier.fillMaxHeight().width(48.dp).background(Color.Black.copy(alpha = 0.4f)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally) {
            
            Spacer(Modifier.height(8.dp))
            IconButton(onClick = { 
                commitAnd(onS)
                onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Settings button clicked", false)) 
            }) { 
                Icon(Icons.Default.Settings, null, tint = if (nav.isSettingsOpen) Color.Gray else Color.White, modifier = Modifier.size(24.dp)) 
            }
            Spacer(Modifier.height(16.dp))
            IconButton(onClick = { commitAnd(onDashboard); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Dashboard button clicked", false)) }, modifier = Modifier.size(44.dp)) { 
                Icon(Icons.Default.Info, "Dashboard", tint = if (isDashboardActive) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
            }
            IconButton(onClick = { commitAnd(onR) }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.BarChart, null, tint = if (nav.isRibbonsVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            if (!uiState.isSystemReady) {
                IconButton(onClick = { onEvent(UiEvent.TogglePhoneSetup(true)) }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ReportProblem, "System Issues", tint = Rose500.copy(alpha = alertAlpha), modifier = Modifier.size(28.dp))
                        Text(uiState.systemIssuesCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            IconButton(onClick = { 
                commitAnd(onL)
                onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Log button clicked", false)) 
            }) { 
                Icon(Icons.AutoMirrored.Filled.List, null, tint = if (nav.isLogVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { commitAnd(onM) }) { Icon(Icons.Default.Map, null, tint = if (nav.isMapVisible && !isAnyOverlayOpen) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(8.dp))
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth().padding(top = topPadding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) { 
                IconButton(onClick = { 
                    commitAnd(onS)
                    onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Settings button clicked", false)) 
                }, modifier = Modifier.size(44.dp)) { 
                    Icon(Icons.Default.Settings, null, tint = if (nav.isSettingsOpen) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                }
                Spacer(Modifier.width(12.dp))
                IconButton(onClick = { commitAnd(onDashboard); onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Dashboard button clicked", false)) }, modifier = Modifier.size(44.dp)) { 
                    Icon(Icons.Default.Info, "Dashboard", tint = if (isDashboardActive) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                }
                IconButton(onClick = { commitAnd(onR) }, modifier = Modifier.size(44.dp)) { 
                    Icon(Icons.Default.BarChart, null, tint = if (nav.isRibbonsVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.weight(1f))
                if (!uiState.isSystemReady) {
                    IconButton(onClick = { onEvent(UiEvent.TogglePhoneSetup(true)) }, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ReportProblem, "System Issues", tint = Rose500.copy(alpha = alertAlpha), modifier = Modifier.size(26.dp))
                            Text(uiState.systemIssuesCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                IconButton(onClick = { 
                    commitAnd(onL)
                    onEvent(UiEvent.LogAction("hidden", "USER ACTION: Header - Log button clicked", false))
                }, modifier = Modifier.size(44.dp)) { 
                    Icon(Icons.AutoMirrored.Filled.List, null, tint = if (nav.isLogVisible) Color.Gray else Color.White, modifier = Modifier.size(22.dp)) 
                }
                IconButton(onClick = { commitAnd(onM) }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Map, null, tint = if (nav.isMapVisible && !isAnyOverlayOpen) Color.Gray else Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun HeaderBarPreview() {
    HeaderBar(
        uiState = MainUiState(),
        onEvent = {}
    )
}
