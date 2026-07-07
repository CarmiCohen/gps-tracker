package com.gps19.app

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
 * v9.1.9:
 * - Issue #051: Binary Parity Gap support.
 * - Issue #048 Fix: Differentiated Telemetry vs GPS Freshness in StatusRowData. 
 *   Connectivity and health indicators (Battery, Temp, Comm, Sats, Distance) now 
 *   remain colorized as long as telemetry is fresh, even if GPS fix is stale.
 * v9.1.8:
 * - Issue #047 Fix: Standardized speed target to m/s. Implemented GPS freshness 
 *   gate in StatusBar to zero out speed and suppress animations during signal loss.
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
    
    val sensorFlow = when(selectedScale) {
        "16M" -> viewModel.history16MFlow
        "1H" -> viewModel.history1HFlow
        "4H" -> viewModel.history4HFlow
        "24H" -> viewModel.history24HFlow
        "7D" -> viewModel.history7DFlow
        else -> viewModel.history4MFlow
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("4M", "16M", "1H", "4H", "24H", "7D").forEach { scale ->
                val isSelected = selectedScale == scale
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { selectedScale = scale }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent
                ) {
                    Text(
                        text = scale,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        
        StatefulSensorRibbon(sensorFlow, "SNR", selectedScale, lineColor = Color(0xFF38BDF8), valueSelector = { it.snrIdx.toFloat() })
        StatefulSensorRibbon(sensorFlow, "NOI", selectedScale, lineColor = Amber500, valueSelector = { it.noiseIdx.toFloat() })
        StatefulSensorRibbon(sensorFlow, "LUX", selectedScale, lineColor = Color.White, valueSelector = { it.luxIdx.toFloat() })
        StatefulSensorRibbon(sensorFlow, "VIB", selectedScale, lineColor = Color.Magenta, valueSelector = { it.vibeIdx.toFloat() })
        StatefulSensorRibbon(sensorFlow, "PRX", selectedScale, lineColor = Rose500, renderType = RibbonRenderType.BAR, valueSelector = { it.proxIdx.toFloat() })
        StatefulSensorRibbon(sensorFlow, "LIF", selectedScale, lineColor = Color(0xFFFACC15), valueSelector = { it.liftIdx.toFloat() })
        StatefulSensorRibbon(sensorFlow, "BAT", selectedScale, lineColor = Rose500, renderType = RibbonRenderType.BAR, valueSelector = { if (it.isBatterySteepDischarge) 1f else 0f })
        StatefulSensorRibbon(sensorFlow, "THM", selectedScale, lineColor = Color.Red, renderType = RibbonRenderType.BAR, valueSelector = { if (it.isCoolingModeActive) 1f else 0f })
        StatefulSensorRibbon(sensorFlow, "CUR", selectedScale, lineColor = Color(0xFFFB923C), valueSelector = { (kotlin.math.abs(it.currentMa.toDouble()).toFloat() / RIBBON_CURRENT_SCALE_MA.toFloat()).coerceIn(0f, 1f) })
        StatefulSensorRibbon(sensorFlow, "SIT", selectedScale, lineColor = BrandJd, renderType = RibbonRenderType.BAR, valueSelector = { if (it.isSitActive) 1f else 0f })
        StatefulSensorRibbon(sensorFlow, "TLT", selectedScale, lineColor = Color(0xFF818CF8), valueSelector = { it.tiltIdx.toFloat() })
        StatefulSensorRibbon(sensorFlow, "BAR", selectedScale, lineColor = Color(0xFF2DD4BF), valueSelector = { it.baroIdx.toFloat() })
        StatefulSensorRibbon(sensorFlow, "SVZ", selectedScale, lineColor = Violet500, valueSelector = { (kotlin.math.abs(it.sitVz).toFloat() / 2.0f).coerceIn(0f, 1f) })
        StatefulSensorRibbon(sensorFlow, "SDZ", selectedScale, lineColor = Violet500, valueSelector = { (kotlin.math.abs(it.sitDz).toFloat() / 0.5f).coerceIn(0f, 1f) })
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))

        StatefulConnectionRibbon(viewModel.history4MFlow, "4M")
        StatefulConnectionRibbon(viewModel.history16MFlow, "16M")
        StatefulConnectionRibbon(viewModel.history1HFlow, "1H")
        StatefulConnectionRibbon(viewModel.history4HFlow, "4H")
        StatefulConnectionRibbon(viewModel.history24HFlow, "24H")
        StatefulConnectionRibbon(viewModel.history7DFlow, "7D")
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
fun StatefulConnectionRibbon(
    flow: StateFlow<List<ConnectionPoint>>,
    title: String
) {
    val history by flow.collectAsStateWithLifecycle()
    ConnectionQualityRibbon(history, title)
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
    
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = lineColor.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(28.dp).padding(start = 2.dp))
        Box(modifier = Modifier.weight(1f).height(boxHeight).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)).drawWithCache {
            onDrawWithContent {
                drawContent()
                val totalPoints = MAX_HISTORY_POINTS_PER_RIBBONS.toFloat()
                val pointWidth = size.width / totalPoints
                val maxHeight = size.height * 0.8f
                val baseLineY = size.height * 0.9f
                
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, baseLineY),
                    end = Offset(size.width, baseLineY),
                    strokeWidth = 0.5.dp.toPx()
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(0f, baseLineY - maxHeight),
                    end = Offset(size.width, baseLineY - maxHeight),
                    strokeWidth = 0.5.dp.toPx()
                )

                val intervalMs = when(scale) {
                    "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 1 * 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 0L
                }
                val alignMs = when(scale) {
                    "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 1L
                }
                val firstTs = history.firstOrNull()?.ts ?: 0L
                val baseTickTs = ((firstTs + alignMs - 1) / alignMs) * alignMs

                var lastPos: Offset? = null
                history.forEachIndexed { index, p ->
                    val xPos = (totalPoints - history.size + index) * pointWidth
                    
                    if (intervalMs > 0 && p.ts >= baseTickTs) {
                        val tickCount = (p.ts - baseTickTs) / intervalMs
                        val prevTickCount = if (index > 0) {
                            if (history[index - 1].ts >= baseTickTs) (history[index - 1].ts - baseTickTs) / intervalMs else -1L
                        } else -2L
                        if (tickCount >= 0 && tickCount > prevTickCount) {
                            drawLine(color = Color.White.copy(alpha = 0.2f), start = Offset(xPos, baseLineY - maxHeight), end = Offset(xPos, baseLineY + 2.dp.toPx()), strokeWidth = 1.dp.toPx())
                        }
                    }

                    val value = valueSelector(p).coerceIn(0f, 1f)
                    
                    if (p.isGap) {
                        lastPos = null
                        return@forEachIndexed
                    }

                    if (renderType == RibbonRenderType.BAR) {
                        val barHeight = value * maxHeight
                        drawRect(lineColor.copy(alpha = 0.6f), Offset(xPos, baseLineY - barHeight), Size(maxOf(1f, pointWidth), barHeight))
                    } else {
                        val yPos = baseLineY - (value * maxHeight)
                        val currentPos = Offset(xPos + (maxOf(1f, pointWidth) / 2f), yPos)
                        
                        lastPos?.let { lp ->
                            if (currentPos.x - lp.x < pointWidth * 10) {
                                drawLine(color = lineColor, start = lp, end = currentPos, strokeWidth = (if (isLandscape) { 1.5.dp.toPx() } else { 1.dp.toPx() }))
                            }
                        }
                        if (value > 0.05f) {
                            drawCircle(color = lineColor, radius = (if (isLandscape) { 1.5.dp.toPx() } else { 1.dp.toPx() }), center = currentPos)
                        }
                        lastPos = currentPos
                    }
                }
            }
        })
    }
}

@Composable
fun ConnectionQualityRibbon(history: List<ConnectionPoint>, title: String) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val boxHeight = if (isLandscape) 60.dp else 30.dp
    
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp).padding(start = 2.dp))
        Box(modifier = Modifier.weight(1f).height(boxHeight).background(MaterialTheme.colorScheme.surface).drawWithCache {
                onDrawWithContent {
                    drawContent()
                    val totalPoints = MAX_HISTORY_POINTS_PER_RIBBONS.toFloat()
                    val pointWidth = size.width / totalPoints
                    val ribbonMaxHeight = if (isLandscape) 16.dp.toPx() else 8.dp.toPx()
                    val baseLineY = if (isLandscape) 44.dp.toPx() else 22.dp.toPx() 

                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(0f, baseLineY),
                        end = Offset(size.width, baseLineY),
                        strokeWidth = 0.5.dp.toPx()
                    )

                    var lastGpsPos: Offset? = null
                    val cyanColor = Color(0xFF00FFFF)

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = (if (isLandscape) { 10.sp.toPx() } else { 6.sp.toPx() })
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.MONOSPACE
                    }

                    val intervalMs = when(title) {
                        "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 1 * 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 0L
                    }
                    val alignMs = when(title) {
                        "7D" -> 24 * 3600000L; "24H" -> 6 * 3600000L; "4H" -> 3600000L; "1H" -> 15 * 60000L; "16M" -> 4 * 60000L; "4M" -> 1 * 60000L; else -> 1L
                    }

                    val firstTs = history.firstOrNull()?.ts ?: 0L
                    val baseTickTs = ((firstTs + alignMs - 1) / alignMs) * alignMs

                    history.forEachIndexed { index, p ->
                        val xPos = (totalPoints - history.size + index) * pointWidth
                        val pColor = when { 
                            p.isGap -> Color.Black
                            p.isConnected -> BrandJd
                            else -> Rose500
                        }
                        
                        val commIdx = if (p.isConnected) TelemetryUtils.calculateCommIndex(p.rtt, p.remoteSig, p.localSig) else 0
                        val hFactor = if (p.isConnected) (commIdx.toFloat() / 10f).coerceIn(0.1f, 1f) else 1f
                        
                        drawRect(pColor, Offset(xPos, baseLineY - (ribbonMaxHeight * hFactor)), Size(maxOf(1f, pointWidth), ribbonMaxHeight * hFactor))
                        
                        if (p.hasGps && p.gpsIndex > 0.0) {
                            val dotRadius = (if (isLandscape) 1.dp.toPx() else 0.5.dp.toPx())
                            val baseHeight = if (isLandscape) 20.dp.toPx() else 10.dp.toPx()
                            val normalizedHeight = (p.gpsIndex.toFloat().coerceIn(0f, 1f) * baseHeight)
                            val yPos = baseLineY - ribbonMaxHeight - (if (isLandscape) { 3.dp.toPx() } else { 1.5.dp.toPx() }) - normalizedHeight
                            val currentPos = Offset(xPos + (maxOf(1f, pointWidth) / 2f), yPos)
                            lastGpsPos?.let { lastPos ->
                                if (currentPos.x - lastPos.x < pointWidth * 10) {
                                    drawLine(color = cyanColor, start = lastPos, end = currentPos, strokeWidth = (if (isLandscape) { 1.dp.toPx() } else { 0.5.dp.toPx() }))
                                }
                            }
                            drawCircle(color = cyanColor, radius = dotRadius, center = currentPos)
                            lastGpsPos = currentPos
                        }

                        if (intervalMs > 0 && history.isNotEmpty() && p.ts >= baseTickTs) {
                            val tickCount = (p.ts - baseTickTs) / intervalMs
                            val prevTickCount = if (index > 0) {
                                if (history[index - 1].ts >= baseTickTs) (history[index - 1].ts - baseTickTs) / intervalMs else -1L
                            } else -2L

                            if (tickCount >= 0 && tickCount > prevTickCount) {
                                drawLine(color = Color.White, start = Offset(xPos, baseLineY - ribbonMaxHeight), end = Offset(xPos, baseLineY + (if (isLandscape) 4.dp.toPx() else 2.dp.toPx())), strokeWidth = (if (isLandscape) 2.dp.toPx() else 1.dp.toPx()))
                                val timeStr = if (title == "7D") dateFormatter.format(Date(p.ts)) else timeFormatter.format(Date(p.ts))
                                drawIntoCanvas { canvas ->
                                    val finalX = xPos.coerceIn(15.dp.toPx(), size.width - 15.dp.toPx())
                                    canvas.nativeCanvas.drawText(timeStr, finalX, baseLineY + (if (isLandscape) { 14.dp.toPx() } else { 8.dp.toPx() }), textPaint)
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun GlobalStatusBar(
    uiState: MainUiState, 
    dashboardState: DashboardState, 
    systemPulse: Long, 
    rttFlow: StateFlow<Int>,
    remoteSignalFlow: StateFlow<Int>,
    redScreenVisibleFlow: StateFlow<Boolean>,
    modifier: Modifier = Modifier
) {
    val mode = uiState.appMode ?: return
    val isLocalOnline = uiState.connectivity.isLocalOnline
    val isRelayConnected = uiState.connectivity.isRelayConnected
    val lastIncomingActivity = uiState.connectivity.lastRemoteActivityTs
    val activityAge = if (lastIncomingActivity > 0) systemPulse - lastIncomingActivity else Long.MAX_VALUE
    
    val isPeerActive = activityAge < TELEMETRY_UI_STALE_THRESHOLD_MS
    
    val rtt by rttFlow.collectAsStateWithLifecycle()
    val remoteSignal by remoteSignalFlow.collectAsStateWithLifecycle()
    val redScreenVisible by redScreenVisibleFlow.collectAsStateWithLifecycle()

    val commIndex = if (uiState.isSystemActive && isRelayConnected) {
        TelemetryUtils.calculateCommIndex(rtt, 10, 10)
    } else 0
    
    val remoteCommIndex = if (mode == "viewer" && isPeerActive) {
        TelemetryUtils.calculateCommIndex(rtt, remoteSignal, 10)
    } else 0

    val loc = if (mode == "viewer") uiState.trackerLocation else uiState.localLocation
    val lastGpsTs = loc.timestamp
    
    val gpsAge = if (lastGpsTs > 0) systemPulse - lastGpsTs else Long.MAX_VALUE
    val isGpsActive = gpsAge < GPS_UI_FAIL_THRESHOLD_MS

    val isDataHealthy = dashboardState.isTelemetryFresh && isLocalOnline && isRelayConnected

    val lastTelemetryTs = maxOf(loc.timestamp, loc.telemetryTs)
    val progressPulse = if (mode == "tracker") lastIncomingActivity else lastTelemetryTs

    val speedValueMps = if (mode == "viewer") uiState.trackerLocation.speed else uiState.localLocation.speed
    val hasUnresolved = uiState.activeAlarms.any { !it.isResolved }

    StatusBar(
        modifier = modifier, isInternet = isLocalOnline, isRelay = isRelayConnected, isPeerActive = isPeerActive, isDataHealthy = isDataHealthy, isGpsActive = isGpsActive,
        mode = mode, battery = uiState.battery.level, lastP = progressPulse, 
        commIndex = commIndex, remoteCommIndex = remoteCommIndex, remoteBattery = if (mode == "viewer") uiState.trackerBattery.level else -1, 
        isCharging = uiState.battery.isChargingStable, remoteCharging = if (mode == "viewer") uiState.trackerBattery.isChargingStable else false,
        speedMps = speedValueMps.toFloat(), trackerAccuracy = uiState.trackerLocation.accuracy.toFloat(),
        maxTrackerAccuracy = uiState.trackerLocation.maxAccuracy.toFloat(), 
        viewerAccuracy = if (uiState.localLocation.lat != 0.0) uiState.localLocation.accuracy.toFloat() else 0f,
        maxViewerAccuracy = uiState.localLocation.maxAccuracy.toFloat(), now = systemPulse, satsView = uiState.trackerSatsView, satsUsed = uiState.trackerSatsUsed,
        trackerTemp = uiState.trackerBattery.temp.toFloat(), viewerTemp = uiState.battery.temp.toFloat(), distToHome = uiState.distanceTrackerToHome, distToViewer = uiState.distanceTrackerToViewer,
        viewerSatsUsed = if (mode == "viewer") uiState.viewerSatsUsed else 0, viewerSatsView = if (mode == "viewer") uiState.viewerSatsView else 0,
        viewerGpsTs = uiState.localLocation.timestamp, trackerId = uiState.deviceId, viewerId = uiState.viewerId, watchdogOk = dashboardState.watchdogOk,
        trackerState = dashboardState.trackerState, hasActiveAlarms = hasUnresolved, isRedScreenSuppressed = (hasUnresolved && !redScreenVisible),
        isSirenPlaying = uiState.isSirenPlaying,
        isTrackerLocPending = uiState.trackerLocation.isLocationPending, 
        trackerLocPendingReason = uiState.trackerLocation.locationPendingReason,
        isViewerLocPending = uiState.localLocation.isLocationPending,
        viewerLocPendingReason = uiState.localLocation.locationPendingReason,
        lastGpsTs = lastGpsTs,
        isTelemetryFresh = dashboardState.isTelemetryFresh
    )
}

@Composable
fun StatusBar(
    modifier: Modifier = Modifier, isInternet: Boolean, isRelay: Boolean, isPeerActive: Boolean, isDataHealthy: Boolean, isGpsActive: Boolean,
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
    isTelemetryFresh: Boolean = true
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
                    StatusBadge("INT", isInternet, isBold = true)
                    StatusBadge("SRV", isRelay, isBold = true)
                    
                    val peerLabel = if (mode == "tracker") "VWR" else "TRK"
                    StatusBadge(peerLabel, isPeerActive, activeColor = BrandJd)
                    
                    if (mode != "tracker") {
                        StatusBadge("DAT", isDataHealthy)
                    }

                    StatusBadge("GPS", isGpsActive)
                    
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
                
                val isMoving = trackerState == TrackerState.MOVING
                val stateColor = if (!isGpsActive) Slate500 else BrandJd 
                
                Text(
                    text = if (isMoving && isGpsActive) "»\u2009${trackerState.name}\u2009«" else trackerState.name,
                    color = stateColor.copy(alpha = if (isMoving && isGpsActive) movingAlpha else 1f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = compactStyle
                )
                
                Spacer(Modifier.weight(1f))

                // Issue #047 Fix: Suppress animation and zero out speed when GPS is stale.
                val speedTargetKph = if (isGpsActive && !speedMps.isNaN()) speedMps * 3.6f else 0f
                val animatedSpeed by animateFloatAsState(targetValue = speedTargetKph, animationSpec = tween(1000), label = "SpeedAnim")
                
                val speedVal = if (animatedSpeed < 10.0f) String.format(Locale.getDefault(), "%.1f", animatedSpeed) else animatedSpeed.toInt().toString()
                val speedColor = if (isGpsActive) BrandJd else Slate500
                Text(text = "${speedVal}km/h", color = speedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle, textAlign = TextAlign.End)
            }
            Spacer(Modifier.height(3.dp))

            val localTelemetryAge = now - viewerGpsTs
            val isLocalTelemetryFresh = viewerGpsTs > 0 && localTelemetryAge < TELEMETRY_UI_STALE_THRESHOLD_MS

            if (isLandscape && mode == "viewer") {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    val vAge = now - viewerGpsTs
                    Box(modifier = Modifier.weight(1f)) { StatusRowData(label = viewIdLabel, battery = battery, commIndex = commIndex, color = ViewerCyan, overrideDistanceColor = BrandJd, isCharging = isCharging, accuracy = viewerAccuracy, maxAccuracy = maxViewerAccuracy, temp = viewerTemp, distance = distToViewer, satsUsed = viewerSatsUsed, satsView = viewerSatsView, gpsAgeMs = if(viewerGpsTs > 0) vAge else -1L, isRemote = false, isLocPending = isViewerLocPending, locPendingReason = viewerLocPendingReason, isTelemetryFresh = isLocalTelemetryFresh) }
                    
                    val tAge = now - lastGpsTs
                    Box(modifier = Modifier.weight(1f)) { StatusRowData(label = trkIdLabel, battery = battery, commIndex = if(isPeerActive) remoteCommIndex else 0, color = if(isPeerActive) BrandJd else Slate500, isCharging = remoteCharging, accuracy = trackerAccuracy, maxAccuracy = maxTrackerAccuracy, satsView = satsView, satsUsed = satsUsed, gpsAgeMs = if(lastGpsTs > 0) tAge else -1L, temp = trackerTemp, distance = distToHome, isRemote = true, isPeerActive = isPeerActive, isLocPending = isTrackerLocPending, locPendingReason = trackerLocPendingReason, isTelemetryFresh = isTelemetryFresh) }
                }
            } else {
                if (mode == "viewer") {
                    val vAge = now - viewerGpsTs
                    StatusRowData(label = viewIdLabel, battery = battery, commIndex = commIndex, color = ViewerCyan, overrideDistanceColor = BrandJd, isCharging = isCharging, accuracy = viewerAccuracy, maxAccuracy = maxViewerAccuracy, temp = viewerTemp, distance = distToViewer, satsUsed = viewerSatsUsed, satsView = viewerSatsView, gpsAgeMs = if(viewerGpsTs > 0) vAge else -1L, horizontalPadding = 8.dp, isRemote = false, isLocPending = isViewerLocPending, locPendingReason = viewerLocPendingReason, isTelemetryFresh = isLocalTelemetryFresh)
                    Spacer(Modifier.height(3.dp))
                }
                val trkColor = if (mode == "viewer" && !isPeerActive) Slate500 else BrandJd
                val tAge = now - lastGpsTs
                
                val effectiveTrkTelemetryFresh = if (mode == "tracker") isLocalTelemetryFresh else isTelemetryFresh

                StatusRowData(label = trkIdLabel, battery = if (mode == "viewer") remoteBattery else battery, commIndex = if (mode == "viewer") (if(isPeerActive) remoteCommIndex else 0) else commIndex, color = trkColor, isCharging = if (mode == "viewer") remoteCharging else isCharging, accuracy = trackerAccuracy, maxAccuracy = maxTrackerAccuracy, satsView = satsView, satsUsed = satsUsed, gpsAgeMs = if(lastGpsTs > 0) tAge else -1L, temp = trackerTemp, distance = distToHome, horizontalPadding = 8.dp, isRemote = mode == "viewer", isPeerActive = if(mode == "viewer") isPeerActive else true, isLocPending = isTrackerLocPending, locPendingReason = trackerLocPendingReason, isTelemetryFresh = effectiveTrkTelemetryFresh)
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
    isTelemetryFresh: Boolean = true
) {
    val compactStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    
    val isGpsFresh = gpsAgeMs != -1L && gpsAgeMs < GPS_UI_FAIL_THRESHOLD_MS
    val isConnStale = isRemote && !isPeerActive
    val isGpsStale = !isGpsFresh
    
    val telemetryColor = if (isTelemetryFresh && !isConnStale) color else Slate500

    val isHandshaking = isConnStale

    val contentColor = if (isConnStale) Slate500 else color
    // GPS color is strictly for position-fix-dependent precision indicators (Age, Accuracy)
    val gpsColor = if (isGpsStale) Slate500 else color
    // Connectivity & Distance should remain colorized as long as the telemetry link is fresh
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
                 Text(text = label, color = contentColor.copy(alpha = if (isHandshaking) handshakeAlpha else animatedLabelAlpha), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = compactStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                 if (isLocPending) {
                     Box(Modifier.padding(start = 1.dp).background(Amber500, RoundedCornerShape(1.dp)).padding(horizontal = 1.dp)) {
                         Text("P", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold, style = compactStyle)
                     }
                 }
            }
            if (isHandshaking) {
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
            if (!isHandshaking) {
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
    val topPadding = if (isXiaomiDevice()) 8.dp else 2.dp
    
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
