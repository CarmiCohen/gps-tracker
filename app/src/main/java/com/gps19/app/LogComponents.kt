package com.gps19.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps19.core.engine.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * LogComponents: UI for system logs and diagnostic history.
 * v9.1.0:
 * - R799e: Swapped legacy BrandJd (#367C2B) for JD Vivid Green (#78BE20).
 * v9.0.4:
 * - R799d: Changed Viewer color to ViewerCyan.
 * v8.9.48:
 * - Issue #425: R865 Color Compliance. Swapped Emerald500 for authoritative 
 *   BrandJd (#367C2B) in log rendering for restored/connected events.
 */

@Composable
fun LogOverlay(
    logs: List<LogEntry>, onExport: () -> Unit, onToggle: () -> Unit, onClear: () -> Unit,
    showDetails: Boolean, showRecovered: Boolean, onSetShowDetails: (Boolean) -> Unit,
    onSetShowRecovered: (Boolean) -> Unit, appStartTime: Long, systemPulse: Long,
    isTelemetryFresh: Boolean = true
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val now = systemPulse
    val filteredLogs by remember(showDetails, showRecovered, logs, appStartTime) {
        derivedStateOf { 
            logs.filter { log -> 
                val isRecovered = (log.timestamp < appStartTime) || (log.timestamp < now - 43200000L)
                if (isRecovered && !showRecovered) return@filter false
                if (log.isSpecial) return@filter true 
                if (log.isImportant) return@filter true
                showDetails
            } 
        }
    }

    var selectedLog by remember { mutableStateOf<LogEntry?>(null) }

    Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = Slate950) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) { 
                    Text("${filteredLogs.size} / ${logs.size}", color = if (isTelemetryFresh) Slate500 else Slate500.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Normal) 
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LogFilterButton(stringResource(R.string.log_filter_details), showDetails, BrandJd, onSetShowDetails)
                    LogFilterButton(stringResource(R.string.log_filter_hist), showRecovered, Amber500, onSetShowRecovered)
                    IconButton(onClick = onExport, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Download, "Save", tint = Color.White, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.DeleteSweep, "Clear", tint = Rose500, modifier = Modifier.size(20.dp)) }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredLogs, key = { it.localId }) { log ->
                        val time = remember(log.timestamp) { try { timeFormatter.format(Date(log.timestamp)) } catch(e: Exception) { "--:--:--" } }
                        val isRecovered = remember(log.timestamp, appStartTime) { (log.timestamp < appStartTime) || (log.timestamp < now - 43200000L) }
                        val msgPrefix = if (isRecovered) stringResource(R.string.log_hist_prefix) else ""
                        val renderingConfig = remember(log.message, log.isImportant, log.isSpecial, log.specialColor, isTelemetryFresh) { 
                            getLogRenderingConfig(log, isTelemetryFresh) 
                        }
                        val isHebrewMsg = remember(log.message) { log.message.any { it in '\u0590'..'\u05FF' } }
                        val cleanMsg = remember(log.message) { FormatterUtils.cleanLogDisplayMessage(log.message) }
                        
                        val baseMsg = remember(log.count, log.durationMs, log.firstSeenTs, log.timestamp, cleanMsg, now, appStartTime) {
                            val countText = if (log.count > 1) " (x${log.count})" else ""
                            
                            val durationText = if (log.count > 1 && log.durationMs > 0) {
                                val windowMs = maxOf(1000L, log.timestamp - log.firstSeenTs)
                                val pct = (log.durationMs * 100.0 / windowMs).coerceIn(0.0, 100.0)
                                // Issue #428: Aligned with 15s jitter buffer
                                val generalized = if (log.durationMs < 15000L) "less than 15s" else FormatterUtils.formatDurationSimple(log.durationMs)
                                " - persistence was $generalized, ${String.format(Locale.getDefault(), "%.1f", pct)}% of window"
                            } else if (log.durationMs > 0) {
                                " [${FormatterUtils.formatDurationSimple(log.durationMs)}]"
                            } else ""

                            "$cleanMsg$countText$durationText"
                        }
                        val displayMessage = "$msgPrefix$baseMsg"

                        val isSelected = selectedLog?.localId == log.localId
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 0.5.dp, horizontal = 8.dp)
                            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.02f))
                            .clickable { selectedLog = if (isSelected) null else log }, 
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = time, 
                                color = if (isTelemetryFresh) Color.White.copy(alpha = 0.6f) else Slate500, 
                                fontSize = 10.sp, 
                                fontFamily = FontFamily.Monospace, 
                                modifier = Modifier.requiredWidth(95.dp), 
                                maxLines = 1, 
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )
                            Spacer(Modifier.width(4.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides if (isHebrewMsg) LayoutDirection.Rtl else LayoutDirection.Ltr) { 
                                Text(
                                    text = displayMessage, 
                                    color = renderingConfig.color, 
                                    fontSize = 11.sp, 
                                    fontWeight = renderingConfig.fontWeight, 
                                    fontFamily = FontFamily.Monospace, 
                                    modifier = Modifier.weight(1f)
                                ) 
                            }
                        }
                    }
                }
            }

            if (selectedLog != null) {
                LogDetailPane(log = selectedLog!!, onClose = { selectedLog = null })
            }
        }
    }
}

@Composable
fun LogDetailPane(log: LogEntry, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("FORENSIC DETAIL", color = Teal500, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(log.message, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))
            
            DetailRow(
                label1 = "SNR-SNAPSHOT", val1 = log.snrSnapshot?.let { "%.1f dB".format(it) } ?: "--", color1 = Color(0xFF38BDF8),
                label2 = "VIBE-SNAPSHOT", val2 = log.vibeSnapshot?.let { "%.2f g".format(it) } ?: "--", color2 = Color.Magenta
            )
            
            DetailRow(
                label1 = "LATITUDE", val1 = if (log.lat != 0.0) "%.6f".format(log.lat) else "--", color1 = Color.White,
                label2 = "LONGITUDE", val2 = if (log.lng != 0.0) "%.6f".format(log.lng) else "--", color2 = Color.White
            )
            
            // R325: Dual-Metric Accuracy Display
            val accText = if (log.accuracy > 0) "%.1fm".format(log.accuracy) else "--"
            val maxAccText = if (log.maxAccuracy > 0) "%.1fm".format(log.maxAccuracy) else "--"
            
            DetailRow(
                label1 = "RAW ACCURACY", val1 = accText, color1 = Amber500,
                label2 = "UNCERTAINTY (MAX)", val2 = maxAccText, color2 = Teal500
            )

            DetailRow(
                label1 = "ROLE", val1 = log.role.uppercase(), color1 = if(log.role == "tracker") BrandJd else ViewerCyan,
                label2 = "EXTREME VALUE", val2 = log.extremeValue?.let { "%.2f".format(it) } ?: "--", color2 = Rose500
            )
        }
    }
}

@Composable
private fun DetailRow(label1: String, val1: String, color1: Color, label2: String, val2: String, color2: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label1, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(val1, color = color1, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label2, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(val2, color = color2, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LogFilterButton(label: String, active: Boolean, activeColor: Color, onClick: (Boolean) -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (active) activeColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)).border(1.dp, if (active) activeColor else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).clickable { onClick(!active) }.padding(horizontal = 8.dp, vertical = 4.dp)) { Text(label, color = if (active) activeColor else Slate500, fontSize = 10.sp, fontWeight = FontWeight.Normal) }
}

@Stable
data class LogRenderingConfig(val color: Color, val fontWeight: FontWeight)

fun getLogRenderingConfig(log: LogEntry, isTelemetryFresh: Boolean = true): LogRenderingConfig {
    if (!isTelemetryFresh) {
        return LogRenderingConfig(Slate500, FontWeight.Normal)
    }

    if (log.isSpecial) {
        val color = log.specialColor?.let { Color(it) } ?: Color(FORENSIC_PINK_COLOR) 
        return LogRenderingConfig(color, FontWeight.Bold)
    }
    val message = log.message
    val isImportant = log.isImportant
    val msg = message.uppercase()
    if (msg.contains("CRITICAL") || msg.contains("ERROR") || msg.contains("[SIREN]") || msg.contains("VIOLATION")) return LogRenderingConfig(Rose500, FontWeight.Bold)
    if (msg.contains("CONNECTED") || msg.contains("RESTORED")) return LogRenderingConfig(BrandJd, FontWeight.Bold)
    if (msg.contains("USER ACTION") || msg.contains("VIEWER CONNECTED")) return LogRenderingConfig(ViewerCyan, FontWeight.Normal)
    if (msg.contains("TRACKER STATE") || msg.contains("TRACKER IS")) return LogRenderingConfig(Amber500, FontWeight.Bold)
    if (isImportant) return LogRenderingConfig(BrandJd, FontWeight.Bold)
    return LogRenderingConfig(BrandJd, FontWeight.Normal)
}
