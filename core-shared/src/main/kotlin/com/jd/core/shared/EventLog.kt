package com.jd.core.shared

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.random.Random

/** The phone an event happened on (ISSUE-NOV10.8-O84). */
@Serializable
enum class EventSource(val label: String) { MONITOR("Monitor"), SENSOR("Sensor") }

/** How an event is shown: ALARM and WARNING stand out, DETAIL is listed only with the Details filter on. */
@Serializable
enum class EventLevel { ALARM, WARNING, INFO, DETAIL }

@Serializable
data class EventEntry(
    val id: String,
    val timestampMs: Long,
    val source: EventSource,
    val level: EventLevel,
    val message: String
)

/**
 * The start of the Sensor's service start and stop events (ISSUE-NOV15.4-O98); the Graphs read them to explain a gap in
 * the Sensor's samples (ISSUE-NOV15.6-O100 part 3), so they live here and not in the Sensor app.
 */
const val SERVICE_STARTED = "Sensor service started"
const val SERVICE_STOPPED = "Sensor service stopped"

/** An event learned after the fact, at the time it happened (ISSUE-NOV15.4-O98). */
data class PastEvent(val level: EventLevel, val message: String, val timestampMs: Long)

/** The Sensor's newest events as the Monitor receives them (ISSUE-NOV10.8-O84). */
data class SensorEventBatch(val trackerId: String, val events: List<EventEntry>)

/** The events of [period] before [nowMs], newest first; DETAIL events only when [showDetails]. */
fun List<EventEntry>.shown(period: ViewPeriod, showDetails: Boolean, nowMs: Long): List<EventEntry> =
    filter { it.timestampMs >= nowMs - period.durationMs && (showDetails || it.level != EventLevel.DETAIL) }
        .sortedByDescending { it.timestampMs }

/**
 * What the Sensor sends its Monitors (ISSUE-NOV10.8-O84): its newest events without the DETAIL ones. A Monitor that was
 * offline receives the ones it missed, as far as they are among these, when it comes back.
 */
fun List<EventEntry>.newestForPeer(): List<EventEntry> =
    filter { it.level != EventLevel.DETAIL }.takeLast(EventLog.PEER_EVENTS_SENT)

/** Each change of a link from its first up on, so a phone that starts offline logs no loss before it ever connected. */
fun Flow<Boolean>.linkChanges(): Flow<Boolean> = distinctUntilChanged().dropWhile { !it }

/** One line per event, in the given order, for the Export button. */
fun eventLogText(events: List<EventEntry>): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    return events.joinToString(separator = "\n", postfix = if (events.isEmpty()) "" else "\n") {
        "${format.format(Date(it.timestampMs))}  ${it.source.label.padEnd(7)}  ${it.level.name.padEnd(7)}  ${it.message}"
    }
}

/**
 * The phone's event log (ISSUE-NOV10.8-O84): what happened, kept for [RETENTION_MS] and across restarts, in a file of
 * one JSON line per event. Writes run in order on one background thread, so callers on any thread never wait for
 * storage; [events] shows the result. A line cut short by a crash is skipped on reading.
 *
 * The Monitor's log also holds the Sensor's events, which it receives with [merge].
 */
class EventLog(
    file: File,
    private val source: EventSource,
    private val clock: () -> Long = System::currentTimeMillis,
    writer: CoroutineDispatcher = Executors.newSingleThreadExecutor { Thread(it, "EventLog").apply { isDaemon = true } }
        .asCoroutineDispatcher()
) {
    private val scope = CoroutineScope(SupervisorJob() + writer)
    private val json = Json { ignoreUnknownKeys = true }
    private val lines = JsonLinesFile(file, "EventLog")

    private val _events = MutableStateFlow<List<EventEntry>>(emptyList())

    /** Every kept event, oldest first. */
    val events: StateFlow<List<EventEntry>> = _events.asStateFlow()

    init {
        scope.launch { load() } // The single writer thread runs it before any later change.
    }

    /** Adds an event of this phone now, and writes it to the debug log as well. */
    fun record(level: EventLevel, message: String) {
        val now = clock()
        SystemLogger.log(if (level == EventLevel.ALARM || level == EventLevel.WARNING) "W" else "I", "Event", message)
        val entry = EventEntry(newId(now), now, source, level, message)
        scope.launch { add(listOf(entry)) }
    }

    /**
     * Adds the event [decide] finds from the kept events, if any (ISSUE-NOV15.4-O98). [decide] runs on the writer thread,
     * after the file has loaded and every earlier change is in, so it sees what the log held when this was called.
     */
    fun recordFrom(decide: (List<EventEntry>) -> PastEvent?) {
        scope.launch {
            val event = runCatching { decide(_events.value) }
                .onFailure { SystemLogger.w("Event", "Past event not decided: ${it.message}") }
                .getOrNull() ?: return@launch
            SystemLogger.log(if (event.level == EventLevel.ALARM || event.level == EventLevel.WARNING) "W" else "I", "Event", event.message)
            add(listOf(EventEntry(newId(event.timestampMs), event.timestampMs, source, event.level, event.message)))
        }
    }

    /** Adds the peer's events; an event already kept is skipped, so the peer may send the same ones again. */
    fun merge(received: List<EventEntry>) {
        scope.launch {
            val known = _events.value.mapTo(HashSet()) { it.id }
            add(received.filter { known.add(it.id) })
        }
    }

    fun clear() {
        scope.launch {
            _events.value = emptyList()
            lines.delete()
        }
    }

    private fun add(new: List<EventEntry>) {
        if (new.isEmpty()) return
        val all = (_events.value + new).sortedBy { it.timestampMs }
        val kept = retained(all, clock())
        _events.value = kept
        if (kept.size == all.size) lines.append(new.map(::encode)) else lines.rewrite(kept.map(::encode))
    }

    private fun load() {
        val stored = lines.readLines() ?: return
        val read = stored.mapNotNull { line -> runCatching { json.decodeFromString<EventEntry>(line) }.getOrNull() }
        val kept = retained(read.sortedBy { it.timestampMs }, clock())
        _events.value = kept
        if (kept.size != stored.size) lines.rewrite(kept.map(::encode)) // Drops expired events and lines cut short.
    }

    private fun encode(entry: EventEntry): String = json.encodeToString(entry)

    private fun newId(nowMs: Long): String =
        "${source.name.first()}${nowMs.toString(36)}-${Random.nextInt(0, Int.MAX_VALUE).toString(36)}"

    companion object {
        /** Events and graph history are kept 7 days (user decision 2026-11-15). */
        const val RETENTION_MS = 7 * 24 * 3_600_000L

        /** Bounds the file and the list should a phone log far more than expected. */
        const val MAX_EVENTS = 5_000

        const val FILE_NAME = "event_log.jsonl"

        /** About 6 KB a message: sent when a Monitor comes online and at each new event while one is. */
        const val PEER_EVENTS_SENT = 50

        fun retained(sortedEvents: List<EventEntry>, nowMs: Long): List<EventEntry> =
            sortedEvents.filter { it.timestampMs >= nowMs - RETENTION_MS }.takeLast(MAX_EVENTS)
    }
}
