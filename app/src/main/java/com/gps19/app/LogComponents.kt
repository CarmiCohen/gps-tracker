package com.gps19.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
 * Extracted from OverlayComponents for Issue 115 modularization.
 */

@Composable
fun LogOverlay(
    logs: List<LogEntry>, onExport: () -> Unit, onToggle: () -> Unit, onClear: () -> Unit,
    showDetails: Boolean, showRecovered: Boolean, onSetShowDetails: (Boolean) -> Unit,
    onSetShowRecovered: (Boolean) -> Unit, appStartTime: Long, systemPulse: Long
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
    Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = Slate950) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) { Text("${filteredLogs.size} / ${logs.size}", color = Slate500, fontSize = 9.sp, fontWeight = FontWeight.Normal) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LogFilterButton(stringResource(R.string.log_filter_details), showDetails, Lime500, onSetShowDetails)
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
                        val renderingConfig = remember(log.message, log.isImportant, log.isSpecial, log.specialColor) { getLogRenderingConfig(log) }
                        val isHebrewMsg = remember(log.message) { log.message.any { it in '\u0590'..'\u05FF' } }
                        val cleanMsg = remember(log.message) { cleanLogDisplayMessage(log.message) }
                        
                        val baseMsg = remember(log.count, log.durationMs, log.firstSeenTs, log.timestamp, cleanMsg, now, appStartTime) {
                            val countText = if (log.count > 1) " (x${log.count})" else ""
                            
                            val durationText = if (log.count > 1 && log.durationMs > 0) {
                                val windowMs = maxOf(1000L, log.timestamp - log.firstSeenTs)
                                val pct = (log.durationMs * 100.0 / windowMs).coerceIn(0.0, 100.0)
                                val generalized = if (log.durationMs < 10000L) "less than 10s" else formatDurationSimple(log.durationMs)
                                " - persistence was $generalized, ${String.format(Locale.getDefault(), "%.1f", pct)}% of window"
                            } else if (log.durationMs > 0) {
                                " [${formatDurationSimple(log.durationMs)}]"
                            } else ""

                            "$cleanMsg$countText$durationText"
                        }
                        val displayMessage = "$msgPrefix$baseMsg"

                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 0.5.dp, horizontal = 8.dp).background(Color.White.copy(alpha = 0.02f)), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = time, 
                                color = Color.White.copy(alpha = 0.6f), 
                                fontSize = 10.sp, 
                                fontFamily = FontFamily.Monospace, 
                                modifier = Modifier.requiredWidth(95.dp), 
                                maxLines = 1, 
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )
                            Spacer(Modifier.width(4.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides if (isHebrewMsg) LayoutDirection.Rtl else LayoutDirection.Ltr) { Text(text = displayMessage, color = renderingConfig.color, fontSize = 11.sp, fontWeight = renderingConfig.fontWeight, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogFilterButton(label: String, active: Boolean, activeColor: Color, onClick: (Boolean) -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (active) activeColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)).border(1.dp, if (active) activeColor else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).clickable { onClick(!active) }.padding(horizontal = 8.dp, vertical = 4.dp)) { Text(label, color = if (active) activeColor else Slate500, fontSize = 10.sp, fontWeight = FontWeight.Normal) }
}

@Stable
data class LogRenderingConfig(val color: Color, val fontWeight: FontWeight)

fun getLogRenderingConfig(log: LogEntry): LogRenderingConfig {
    if (log.isSpecial) {
        val color = log.specialColor?.let { Color(it) } ?: Color(FORENSIC_PINK_COLOR) 
        return LogRenderingConfig(color, FontWeight.Bold)
    }
    val message = log.message
    val isImportant = log.isImportant
    val msg = message.uppercase()
    if (msg.contains("CRITICAL") || msg.contains("ERROR") || msg.contains("[SIREN]") || msg.contains("VIOLATION")) return LogRenderingConfig(Rose500, FontWeight.Bold)
    if (msg.contains("CONNECTED") || msg.contains("RESTORED")) return LogRenderingConfig(Emerald500, FontWeight.Bold)
    if (msg.contains("USER ACTION") || msg.contains("VIEWER CONNECTED")) return LogRenderingConfig(ViewerOrange, FontWeight.Normal)
    if (msg.contains("TRACKER STATE") || msg.contains("TRACKER IS")) return LogRenderingConfig(Amber500, FontWeight.Bold)
    if (isImportant) return LogRenderingConfig(Emerald500, FontWeight.Bold)
    return LogRenderingConfig(Lime500, FontWeight.Normal)
}
