package com.jd.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.ViewPeriod
import com.jd.core.shared.EventSource
import com.jd.core.shared.eventLogText
import com.jd.core.shared.shown
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The menu's Events list of both apps (ISSUE-NOV10.8-O84): the [events] of the chosen period, newest first, with a
 * Details filter, Export through the share sheet, and Clear after a confirmation. [showSource] marks each event with
 * the phone it happened on, for the Monitor, whose list also holds the Sensor's events. A selected event's details
 * open the Graphs around its time with [onShowGraphs] (ISSUE-NOV15.6-O100 part 4).
 */
@Composable
fun EventLogOverlay(
    events: List<EventEntry>,
    showSource: Boolean,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onShowGraphs: (timeMs: Long) -> Unit = {}
) {
    val context = LocalContext.current
    var period by rememberSaveable { mutableStateOf(ViewPeriod.DAY) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var isConfirmingClear by rememberSaveable { mutableStateOf(false) }
    val nowMs by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(NOW_REFRESH_MS)
            value = System.currentTimeMillis()
        }
    }
    // A new event moves the period's end too, so it is listed at once.
    val endMs = maxOf(nowMs, events.lastOrNull()?.timestampMs ?: 0L)
    val listed = remember(events, period, showDetails, endMs) { events.shown(period, showDetails, endMs) }
    val selected = listed.find { it.id == selectedId }

    if (isConfirmingClear) {
        AlertDialog(
            onDismissRequest = { isConfirmingClear = false },
            title = { Text("Clear all events?") },
            text = { Text("Every event of the last 7 days on this phone is deleted. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        isConfirmingClear = false
                        selectedId = null
                        onClear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { isConfirmingClear = false }) { Text("Cancel") } }
        )
    }

    // The events are English: a right-to-left phone language must not mirror the list or turn "24 h" into "h 24".
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Surface(modifier = Modifier.fillMaxSize(), color = Slate900, contentColor = Color.White) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("EVENTS", color = ViewerCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(eventCountText(listed.size), color = Slate500, fontSize = 11.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { shareEvents(context, listed.reversed(), period) }, enabled = listed.isNotEmpty()) {
                    Icon(Icons.Default.Share, contentDescription = "Export", tint = if (listed.isNotEmpty()) Color.White else Slate500)
                }
                IconButton(onClick = { isConfirmingClear = true }, enabled = events.isNotEmpty()) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = if (events.isNotEmpty()) Rose500 else Slate500)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ViewPeriod.entries.forEach { option ->
                    EventChip(option.label, isOn = option == period, onClick = { period = option })
                }
                Spacer(Modifier.weight(1f))
                EventChip("Details", isOn = showDetails, onClick = { showDetails = !showDetails })
            }
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (listed.isEmpty()) {
                    Text("No events in the last ${period.label}", color = Slate500, modifier = Modifier.align(Alignment.Center))
                } else {
                    val timeFormat = remember(period) { SimpleDateFormat(if (period.durationMs > DAY_MS) "dd/MM HH:mm" else "HH:mm:ss", Locale.getDefault()) }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(listed, key = { it.id }) { event ->
                            EventRow(
                                event = event,
                                time = timeFormat.format(Date(event.timestampMs)),
                                showSource = showSource,
                                isSelected = event.id == selectedId,
                                onClick = { selectedId = if (event.id == selectedId) null else event.id }
                            )
                        }
                    }
                }
            }
            selected?.let { EventDetail(it, onClose = { selectedId = null }, onShowGraphs = { onShowGraphs(it.timestampMs) }) }
        }
    }
    }
}

@Composable
private fun EventRow(event: EventEntry, time: String, showSource: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(time, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width(6.dp))
        if (showSource) {
            Text(
                event.source.label.first().toString(),
                color = sourceColor(event.source),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            event.message,
            color = levelColor(event.level),
            fontSize = 12.sp,
            fontWeight = if (event.level == EventLevel.ALARM || event.level == EventLevel.WARNING) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EventDetail(event: EventEntry, onClose: () -> Unit, onShowGraphs: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f), contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMs)),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close details", modifier = Modifier.size(16.dp))
                }
            }
            Text("${event.source.label} phone - ${event.level.name}", color = levelColor(event.level), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(event.message, fontSize = 13.sp)
            TextButton(onClick = onShowGraphs, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(GraphsIcon, contentDescription = null, tint = ViewerCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Graphs at this time", color = ViewerCyan, fontSize = 12.sp)
            }
        }
    }
}

/** A period or filter chip of the Events list, also used by the Graphs (ISSUE-NOV10.8-O84 part 3). */
@Composable
internal fun EventChip(label: String, isOn: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .background(if (isOn) ViewerCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f), shape)
            .border(1.dp, if (isOn) ViewerCyan else Color.White.copy(alpha = 0.15f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (isOn) ViewerCyan else Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

fun eventCountText(count: Int): String = if (count == 1) "1 event" else "$count events"

private fun levelColor(level: EventLevel): Color = when (level) {
    EventLevel.ALARM -> Rose500
    EventLevel.WARNING -> BrandGold
    EventLevel.INFO -> Color.White
    EventLevel.DETAIL -> Slate500
}

private fun sourceColor(source: EventSource): Color = when (source) {
    EventSource.MONITOR -> ViewerCyan
    EventSource.SENSOR -> StatusGreen
}

/**
 * Writes [events], oldest first, to a text file in the app's cache and opens the share sheet for it, so the user can
 * save or send it. The file goes through a FileProvider, as a list of 7 days can exceed what an Intent may carry.
 */
private fun shareEvents(context: Context, events: List<EventEntry>, period: ViewPeriod) {
    try {
        val directory = File(context.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() } // Only the newest export is kept.
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(directory, "events-$stamp.txt").apply { writeText(eventLogText(events)) }
        val uri = FileProvider.getUriForFile(context, context.packageName + EXPORT_AUTHORITY_SUFFIX, file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "Events of the last ${period.label}")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Export events"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app can receive the events file", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * The key under which an app's `SaveableStateHolder` keeps the Events list's period and selection while the Graphs of
 * an event cover it (ISSUE-NOV15.6-O100 part 4).
 */
const val EVENTS_STATE_KEY = "events"

private const val NOW_REFRESH_MS = 30_000L
private const val DAY_MS = 24 * 3_600_000L
private const val EXPORT_DIRECTORY = "event_exports"

/** Matches the provider in this module's manifest. */
private const val EXPORT_AUTHORITY_SUFFIX = ".events"
