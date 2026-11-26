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

/** How an event is shown: ALARM and WARNING stand out, DETAIL is listed only under the All filter ([EventFilter]). */
@Serializable
enum class EventLevel { ALARM, WARNING, INFO, DETAIL }

/**
 * One event. [trackerId] is the Sensor a Monitor received a Sensor event from (ISSUE-NOV15.6-O100 part 7, Nov17.2), so
 * a re-paired Monitor can tell the Sensors apart; null for a phone's own events and for Sensor events received before.
 * [kind] is the violation type of a Sensor alarm (ISSUE-NOV23.4-O139), so a Monitor can tell which of its own alarms
 * answered it, or one of the event kinds below (SIMP-NOV23.4-S1, Nov23.9); null for any other event and for a Sensor
 * before Nov23.4.
 */
@Serializable
data class EventEntry(
    val id: String,
    val timestampMs: Long,
    val source: EventSource,
    val level: EventLevel,
    val message: String,
    val trackerId: String? = null,
    val kind: String? = null
)

/**
 * The start of the Sensor's service start and stop events (ISSUE-NOV15.4-O98); the Graphs read them to explain a gap in
 * the Sensor's samples (ISSUE-NOV15.6-O100 part 3), so they live here and not in the Sensor app.
 */
const val SERVICE_STARTED = "Sensor service started"
const val SERVICE_STOPPED = "Sensor service stopped"

/**
 * The start of the Sensor's event for a Monitor's new Home Points: a DETAIL event (ISSUE-NOV23.4-O139) that is still
 * sent to the Monitors, so theirs lists it under All too.
 */
const val HOME_POINTS_RECEIVED = "Home points received"

/**
 * The [EventEntry.kind] of the Sensor's service start, service stop and Home Points events (SIMP-NOV23.4-S1, Nov23.9),
 * so the code reads the kind and the wording stays free. An event without a kind (recorded before Nov23.9, kept 7 days,
 * or from an older Sensor) is still known by the start of its message.
 */
const val SERVICE_STARTED_KIND = "SERVICE_STARTED"
const val SERVICE_STOPPED_KIND = "SERVICE_STOPPED"
const val HOME_POINTS_RECEIVED_KIND = "HOME_POINTS_RECEIVED"

/**
 * The level of a saved file's event, both apps' `Settings exported` and the Monitor's `Trails saved`: a DETAIL, listed
 * under All (ISSUE-NOV25.12-O142); a failed save stays a WARNING.
 */
val FILE_SAVED_LEVEL = EventLevel.DETAIL

fun EventEntry.isServiceStart(): Boolean = isOfKind(SERVICE_STARTED_KIND, SERVICE_STARTED)
fun EventEntry.isServiceStop(): Boolean = isOfKind(SERVICE_STOPPED_KIND, SERVICE_STOPPED)
fun EventEntry.isServiceLifecycle(): Boolean = isServiceStart() || isServiceStop()
fun EventEntry.isHomePointsReceived(): Boolean = isOfKind(HOME_POINTS_RECEIVED_KIND, HOME_POINTS_RECEIVED)

private fun EventEntry.isOfKind(eventKind: String, legacyPrefix: String): Boolean =
    kind == eventKind || (kind == null && message.startsWith(legacyPrefix))

/**
 * The Sensor's service start and stop events a Monitor needs to shade the stops among the samples after [afterMs]
 * (ISSUE-NOV15.6-O100 part 7, Nov17.2): those after the last start at or before [afterMs], oldest first. A stop learned
 * after the fact may be dated before [afterMs], but never before the start of the run it ended.
 */
fun List<EventEntry>.serviceLifecycleSince(afterMs: Long): List<EventEntry> {
    val lifecycle = filter { it.isServiceLifecycle() }.sortedBy { it.timestampMs }
    val runStart = lifecycle.indexOfLast { it.timestampMs <= afterMs && it.isServiceStart() }
    return lifecycle.drop(runStart + 1)
}

/** An event learned after the fact, at the time it happened (ISSUE-NOV15.4-O98), of [kind] if given (see [EventEntry.kind]). */
data class PastEvent(val level: EventLevel, val message: String, val timestampMs: Long, val kind: String? = null)

/**
 * Sensor events as the Monitor receives them (ISSUE-NOV10.8-O84). [completeAfterMs], when set, says the batch holds all
 * of the Sensor's peer events (see [forPeer]) newer than it, up to its newest (ISSUE-NOV15.4-O98 part 4); null for the
 * start and stop events sent with graph samples and for a Sensor before Nov17.5.
 */
data class SensorEventBatch(val trackerId: String, val events: List<EventEntry>, val completeAfterMs: Long? = null)

/** Peer events to send, holding all of them newer than [afterMs] (ISSUE-NOV15.4-O98 part 4). */
data class PeerEvents(val afterMs: Long, val events: List<EventEntry>)

/**
 * The Events list's filter, one choice of three (ISSUE-NOV25.25-O156; until then two toggles, Alarms and More). The
 * DETAIL events, and the events of a list's `detailIds` whatever their level (ISSUE-NOV23.4-O139), are listed only
 * under [ALL].
 */
enum class EventFilter(val label: String) {
    /** Only the ALARM events (ISSUE-NOV25.12-O142). */
    ALARMS("Alarms"),

    /** Every event but the details: the list as it opens. */
    MAIN("Main"),

    /** Every event (named More, and Details before ISSUE-NOV25.12-O142). */
    ALL("All")
}

/** The events of [period] before [nowMs] that pass [filter], newest first; see [EventFilter] for [detailIds]. */
fun List<EventEntry>.shown(
    period: ViewPeriod,
    filter: EventFilter,
    nowMs: Long,
    detailIds: Set<String> = emptySet()
): List<EventEntry> =
    filter {
        it.timestampMs >= nowMs - period.durationMs && when (filter) {
            EventFilter.ALL -> true
            EventFilter.MAIN -> it.level != EventLevel.DETAIL && it.id !in detailIds
            EventFilter.ALARMS -> it.level == EventLevel.ALARM && it.id !in detailIds
        }
    }.sortedByDescending { it.timestampMs }

/**
 * The events the Sensor sends its Monitors (ISSUE-NOV10.8-O84): all but the DETAIL ones, oldest first; the Home Points
 * received are sent though DETAIL (ISSUE-NOV23.4-O139), rare and the Monitor's own change.
 */
fun List<EventEntry>.forPeer(): List<EventEntry> =
    filter { it.level != EventLevel.DETAIL || it.isHomePointsReceived() }

/**
 * The newest [EventLog.PEER_EVENTS_SENT] peer events, sent each time a Monitor comes online and at each new event while
 * one is (ISSUE-NOV10.8-O84), with the time of the newest one left out, so a Monitor that missed older ones asks for
 * them (ISSUE-NOV15.4-O98 part 4); 0 when none is left out.
 */
fun List<EventEntry>.newestForPeer(): PeerEvents {
    val peer = forPeer()
    val sent = peer.takeLast(EventLog.PEER_EVENTS_SENT)
    return PeerEvents(peer.getOrNull(peer.size - sent.size - 1)?.timestampMs ?: 0L, sent)
}

/**
 * The answer to a Monitor's request for the peer events newer than [afterMs] (ISSUE-NOV15.4-O98 part 4): oldest first,
 * [EventLog.PEER_EVENTS_SENT] a message, each chunk complete after the previous one's newest, so an answer cut short
 * still leaves the Monitor without holes.
 */
fun List<EventEntry>.peerEventsAfter(afterMs: Long): List<PeerEvents> {
    var after = afterMs
    return forPeer().filter { it.timestampMs > afterMs }.chunked(EventLog.PEER_EVENTS_SENT).map { chunk ->
        PeerEvents(after, chunk).also { after = chunk.last().timestampMs }
    }
}

/**
 * The Monitor's side of the Sensor events' catch-up (ISSUE-NOV15.4-O98 part 4). It holds [syncedMs], the time up to
 * which it has every peer event of the Sensor, on the Sensor's clock: at start, the newest Sensor event it keeps. A
 * batch complete after a time not later than that moves it on; one complete only after a later time shows a hole, and
 * [received] returns the time to ask the Sensor for the events after, at most once per [RETRY_MS], so the live batches
 * that arrive while an answer comes in do not ask again.
 */
class PeerEventCatchUp(private var syncedMs: Long, private val clock: () -> Long) {
    private var requestedAtMs: Long? = null

    fun received(batch: SensorEventBatch): Long? {
        val completeAfterMs = batch.completeAfterMs ?: return null
        if (batch.events.isEmpty()) return null
        if (completeAfterMs <= syncedMs) {
            syncedMs = maxOf(syncedMs, batch.events.maxOf { it.timestampMs })
            return null
        }
        val nowMs = clock()
        requestedAtMs?.let { if (nowMs - it in 0 until RETRY_MS) return null }
        requestedAtMs = nowMs
        return syncedMs
    }

    companion object {
        const val RETRY_MS = 30_000L

        /** Where a Monitor keeping [events] starts for the Sensor [trackerId]: its newest event, or 0 for all. */
        fun syncedFrom(events: List<EventEntry>, trackerId: String): Long =
            events.lastOrNull { it.source == EventSource.SENSOR && it.trackerId == trackerId }?.timestampMs ?: 0L
    }
}

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

    /** Adds an event of this phone now, of [kind] if given (see [EventEntry.kind]), and writes it to the debug log as well. */
    fun record(level: EventLevel, message: String, kind: String? = null) {
        val now = clock()
        SystemLogger.log(if (level == EventLevel.ALARM || level == EventLevel.WARNING) "W" else "I", "Event", message)
        val entry = EventEntry(newId(now), now, source, level, message, kind = kind)
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
            add(listOf(EventEntry(newId(event.timestampMs), event.timestampMs, source, event.level, event.message, kind = event.kind)))
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
