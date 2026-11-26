package com.jd.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.jd.core.shared.EventFilter
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
 * The menu's Events list of both apps (ISSUE-NOV10.8-O84): the [events] of the chosen period, newest first, through one
 * filter of three, Alarms (red), Main and All ([EventFilter], ISSUE-NOV25.25-O156), Export through the share sheet, and
 * Clear after a confirmation. [showSource] marks each event with
 * the phone it happened on, for the Monitor, whose list also holds the Sensor's events. A selected event's details
 * open the Graphs around its time with [onShowGraphs] (ISSUE-NOV15.6-O100 part 4). The events of [detailIds] are listed
 * only under All, like the DETAIL ones (ISSUE-NOV23.4-O139). Under Alarms and Main an event of [summaries] is shown, and
 * exported, with that text instead of its own (SIMP-NOV25.0-S1: one row per finished alarm). On a screen wider than
 * tall the header, periods and filters form a side column beside the list (ISSUE-NOV25.25-O156).
 */
@Composable
fun EventLogOverlay(
    events: List<EventEntry>,
    showSource: Boolean,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onShowGraphs: (timeMs: Long) -> Unit = {},
    detailIds: Set<String> = emptySet(),
    summaries: Map<String, String> = emptyMap()
) {
    val context = LocalContext.current
    var period by rememberSaveable { mutableStateOf(ViewPeriod.DAY) }
    var filter by rememberSaveable { mutableStateOf(EventFilter.MAIN) }
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
    val listed = remember(events, period, filter, endMs, detailIds, summaries) {
        events.shown(period, filter, endMs, detailIds)
            .map { event -> summaries[event.id]?.takeIf { filter != EventFilter.ALL }?.let { event.copy(message = it) } ?: event }
    }
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
            val isLandscape = maxWidth > maxHeight
            val actions: @Composable RowScope.() -> Unit = {
                IconButton(onClick = { shareEvents(context, listed.reversed(), period) }, enabled = listed.isNotEmpty()) {
                    Icon(Icons.Default.Share, contentDescription = "Export", tint = if (listed.isNotEmpty()) Color.White else Slate500)
                }
                IconButton(onClick = { isConfirmingClear = true }, enabled = events.isNotEmpty()) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = if (events.isNotEmpty()) Rose500 else Slate500)
                }
            }
            // Composed before the details, so Back closes the details first, then the list (ISSUE-NOV25.22-O151).
            val header = @Composable { headerActions: @Composable RowScope.() -> Unit ->
                OverlayHeader(
                    title = "EVENTS",
                    color = MonitorBrown,
                    onClose = onClose,
                    fontSize = 18.sp,
                    subtitle = eventCountText(listed.size),
                    actions = headerActions
                )
            }
            val list = @Composable { modifier: Modifier ->
                Box(modifier = modifier) {
                    if (listed.isEmpty()) {
                        Text("No ${if (filter == EventFilter.ALARMS) "alarms" else "events"} in the last ${period.label}", color = Slate500, modifier = Modifier.align(Alignment.Center))
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
            }
            val detail = @Composable {
                selected?.let { EventDetail(it, onClose = { selectedId = null }, onShowGraphs = { onShowGraphs(it.timestampMs) }) }
            }
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.width(SIDE_COLUMN_WIDTH).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        header {} // The narrow column has no room for the buttons beside the title.
                        Row(content = actions)
                        ViewPeriod.entries.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                pair.forEach { option ->
                                    EventChip(option.label, isOn = option == period, modifier = Modifier.weight(1f), onClick = { period = option })
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        EventFilter.entries.forEach { option ->
                            FilterChoice(option, filter, Modifier.fillMaxWidth(), onChoose = { filter = it })
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        list(Modifier.weight(1f).fillMaxWidth())
                        detail()
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    header(actions)
                    // Seven chips: 4 dp apart, so they fit one line on the narrow S21FE with its large font (ISSUE-NOV25.12-O142).
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        ViewPeriod.entries.forEach { option ->
                            EventChip(option.label, isOn = option == period, onClick = { period = option })
                        }
                        Spacer(Modifier.weight(1f))
                        EventFilter.entries.forEach { option -> FilterChoice(option, filter, onChoose = { filter = it }) }
                    }
                    Spacer(Modifier.height(6.dp))
                    list(Modifier.weight(1f).fillMaxWidth())
                    detail()
                }
            }
        }
    }
    }
}

/** One of the three filters, highlighted when chosen like the periods; Alarms is red (ISSUE-NOV25.25-O156). */
@Composable
private fun FilterChoice(option: EventFilter, chosen: EventFilter, modifier: Modifier = Modifier, onChoose: (EventFilter) -> Unit) {
    EventChip(
        option.label,
        isOn = option == chosen,
        color = if (option == EventFilter.ALARMS) Rose500 else MonitorBrown,
        modifier = modifier,
        onClick = { onChoose(option) }
    )
}

@Composable
private fun EventRow(event: EventEntry, time: String, showSource: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f))
            .clickable(onClick = onClick)
            // Rows without a gap between them (ISSUE-NOV25.25-O156; 1 dp apart and 3 dp inside before).
            .padding(horizontal = 4.dp, vertical = 1.dp),
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
    // Composed after the list's header, so Back closes the details first, then the list (ISSUE-NOV25.22-O151).
    BackHandler(onBack = onClose)
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f), contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMs)),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text("${event.source.label} phone - ${event.level.name}", color = levelColor(event.level), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(event.message, fontSize = 13.sp)
            TextButton(onClick = onShowGraphs, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(GraphsIcon, contentDescription = null, tint = MonitorBrown, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Graphs at this time", color = MonitorBrown, fontSize = 12.sp)
            }
        }
    }
}

/**
 * A period or filter chip of the Events list, also used by the Graphs (ISSUE-NOV10.8-O84 part 3). A [color] other than
 * the brand's marks the chip in it even when off, as the red Alarms filter (ISSUE-NOV25.12-O142).
 */
@Composable
internal fun EventChip(label: String, isOn: Boolean, modifier: Modifier = Modifier, color: Color = MonitorBrown, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val isMarked = color != MonitorBrown
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(if (isOn) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f), shape)
            .border(1.dp, if (isOn || isMarked) color else Color.White.copy(alpha = 0.15f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp)
    ) {
        // A label never wraps: a squeezed chip showed "More" as M / or / e on the S21FE (ISSUE-NOV25.12-O142).
        Text(
            label,
            color = if (isOn || isMarked) color else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

fun eventCountText(count: Int): String = if (count == 1) "1 event" else "$count events"

private fun levelColor(level: EventLevel): Color = when (level) {
    EventLevel.ALARM -> Rose500
    EventLevel.WARNING -> BrandGold
    EventLevel.INFO -> Color.White
    EventLevel.DETAIL -> Slate500
}

internal fun sourceColor(source: EventSource): Color = when (source) {
    EventSource.MONITOR -> MonitorBrown
    EventSource.SENSOR -> SensorGreen
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

/** The landscape side column's width (ISSUE-NOV25.25-O156): two period chips a row, the S21FE's large font included. */
private val SIDE_COLUMN_WIDTH = 150.dp
private const val DAY_MS = 24 * 3_600_000L
private const val EXPORT_DIRECTORY = "event_exports"

/** Matches the provider in this module's manifest. */
private const val EXPORT_AUTHORITY_SUFFIX = ".events"
