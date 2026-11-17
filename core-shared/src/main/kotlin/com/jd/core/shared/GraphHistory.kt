package com.jd.core.shared

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * The history behind the Graphs (ISSUE-NOV10.8-O84 part 3, user decisions 2026-11-15 and 2026-11-16): each phone takes
 * one [GraphSample] a minute, kept [GraphHistory.RETENTION_MS]. The Sensor samples its sensors, GNSS, relay link and
 * battery, with the peaks of the minute for vibration, tilt and noise; a Monitor keeps a copy of the Sensor's samples,
 * which it asks for while its Graphs screen is open, and samples its own relay link and the rows it receives.
 */

/** How the samples within one point of a graph are combined: peaks stay peaks, the rest is averaged. */
enum class GraphAggregate { MAX, MEAN }

/**
 * What a sample can hold. The order is the stored and sent order, so a new field is only ever added at the end; a
 * reader that does not know a field ignores it, and a field missing from an older sample is unknown.
 */
enum class GraphField(val decimals: Int, val aggregate: GraphAggregate) {
    /** The largest vibration of the minute, in g. */
    VIBRATION_G(2, GraphAggregate.MAX),
    VIBRATION_LIMIT_G(2, GraphAggregate.MAX),
    /** The largest tilt from the resting position of the minute; unknown while tilt detection is not armed. */
    TILT_DEG(1, GraphAggregate.MAX),
    TILT_LIMIT_DEG(1, GraphAggregate.MAX),
    /** The loudest sound level of the minute, in dBFS; unknown while the microphone does not listen. */
    NOISE_DBFS(1, GraphAggregate.MAX),
    /** The current fix's accuracy at the minute's end; unknown without a current fix. */
    ACCURACY_M(1, GraphAggregate.MAX),
    SATS_USED(0, GraphAggregate.MEAN),
    SATS_IN_VIEW(0, GraphAggregate.MEAN),
    SIGNAL_DBHZ(1, GraphAggregate.MEAN),
    /** The share of the minute the relay link was up. */
    LINK_UP_PERCENT(0, GraphAggregate.MEAN),
    LATENCY_MS(0, GraphAggregate.MAX),
    /** Sensor: rows held while offline, waiting to be sent. */
    QUEUED_ROWS(0, GraphAggregate.MAX),
    BATTERY_PERCENT(0, GraphAggregate.MEAN),
    /** 1 while charging, 0 while not. */
    CHARGING(0, GraphAggregate.MAX),
    TEMPERATURE_C(1, GraphAggregate.MAX),
    /** Monitor: the Sensor's rows received live in the minute. */
    ROWS_LIVE(0, GraphAggregate.MEAN),
    /** Monitor: the Sensor's rows received by backfill in the minute. */
    ROWS_BACKFILLED(0, GraphAggregate.MAX),
    /** Monitor: the Sensor's rows known to be missing at the minute's end. */
    ROWS_MISSING(0, GraphAggregate.MAX)
}

/** One minute of a phone's history; [timeMs] is the minute's end on that phone's clock. Unknown values are absent. */
class GraphSample private constructor(val timeMs: Long, private val values: FloatArray) {

    operator fun get(field: GraphField): Float? = values[field.ordinal].takeUnless { it.isNaN() }

    override fun equals(other: Any?): Boolean =
        other is GraphSample && other.timeMs == timeMs && other.values.contentEquals(values)

    override fun hashCode(): Int = 31 * timeMs.hashCode() + values.contentHashCode()

    override fun toString(): String =
        "GraphSample($timeMs, ${GraphField.entries.mapNotNull { field -> get(field)?.let { "$field=$it" } }})"

    /** The sample as a JSON array: the time, then each field in [GraphField] order, null when unknown, trailing nulls left out. */
    fun toJson(): JsonArray {
        val last = GraphField.entries.indexOfLast { get(it) != null }
        val items = ArrayList<JsonElement>(last + 2)
        items.add(JsonPrimitive(timeMs))
        for (index in 0..last) {
            val field = GraphField.entries[index]
            items.add(get(field)?.let { rounded(it, field.decimals) } ?: JsonNull)
        }
        return JsonArray(items)
    }

    companion object {
        /** A sample of [readings]; a null or not-a-number reading is unknown, and values are rounded as they are stored. */
        fun of(timeMs: Long, readings: Map<GraphField, Number?>): GraphSample {
            val values = FloatArray(GraphField.entries.size) { Float.NaN }
            readings.forEach { (field, value) ->
                value?.toDouble()?.takeUnless { it.isNaN() || it.isInfinite() }?.let { values[field.ordinal] = round(it, field.decimals).toFloat() }
            }
            return GraphSample(timeMs, values)
        }

        /** Reads [toJson]'s form; null when the time is missing or the array is malformed. */
        fun fromJson(element: JsonElement): GraphSample? = runCatching {
            val items = element.jsonArray
            val timeMs = items[0].jsonPrimitive.longOrNull ?: return null
            val values = FloatArray(GraphField.entries.size) { Float.NaN }
            for (index in 1 until minOf(items.size, GraphField.entries.size + 1)) {
                val item = items[index]
                if (item is JsonPrimitive) item.doubleOrNull?.let { values[index - 1] = it.toFloat() }
            }
            GraphSample(timeMs, values)
        }.getOrNull()

        private fun round(value: Double, decimals: Int): Double {
            val scale = 10.0.pow(decimals)
            return (value * scale).roundToLong() / scale
        }

        private fun rounded(value: Float, decimals: Int): JsonPrimitive =
            if (decimals == 0) JsonPrimitive(value.toDouble().roundToLong()) else JsonPrimitive(round(value.toDouble(), decimals))
    }
}

/** [samples] as one JSON text, for the wire. */
fun encodeGraphSamples(samples: List<GraphSample>): String = JsonArray(samples.map { it.toJson() }).toString()

/** Reads [encodeGraphSamples]' text; a malformed sample is skipped, a malformed text gives none. */
fun decodeGraphSamples(text: String): List<GraphSample> = runCatching {
    Json.parseToJsonElement(text).jsonArray.mapNotNull { GraphSample.fromJson(it) }
}.getOrDefault(emptyList())

/**
 * [encodeGraphSamples]' text gzipped, in Base64 (ISSUE-NOV15.6-O100 part 1, Nov17.2): about a fifth of the plain text,
 * since samples repeat their form and most values minute to minute (on the A15 gzip took 68.8 KB to 9.7 KB).
 */
fun encodeGraphSamplesCompressed(samples: List<GraphSample>): String {
    val bytes = ByteArrayOutputStream()
    GZIPOutputStream(bytes).use { it.write(encodeGraphSamples(samples).toByteArray(Charsets.UTF_8)) }
    return Base64.getEncoder().encodeToString(bytes.toByteArray())
}

/** Reads [encodeGraphSamplesCompressed]' text; a malformed text gives none. */
fun decodeGraphSamplesCompressed(text: String): List<GraphSample> = runCatching {
    GZIPInputStream(Base64.getDecoder().decode(text).inputStream()).use { String(it.readBytes(), Charsets.UTF_8) }
}.map(::decodeGraphSamples).getOrDefault(emptyList())

/**
 * A Monitor's ask for the Sensor's samples after [afterMs] on the Sensor's clock (ISSUE-NOV10.8-O84 part 3); with
 * [compressed] (Monitors since Nov17.2) it reads them in [encodeGraphSamplesCompressed]' form.
 */
data class GraphRequest(val afterMs: Long, val compressed: Boolean)

/** The Sensor's samples as the Monitor receives them (ISSUE-NOV10.8-O84 part 3). */
data class SensorGraphBatch(val trackerId: String, val samples: List<GraphSample>)

/** Phones sample once a minute, on the minute of their own clock. */
const val GRAPH_SAMPLE_INTERVAL_MS = 60_000L

/** A Monitor's Graphs screen asks the Sensor for its newer samples this often while it is open. */
const val GRAPH_REQUEST_INTERVAL_MS = 60_000L

/**
 * The most samples (12 hours) in one message: a week's first sync takes 14 messages of about 60 KB each, or about 10 KB
 * each compressed (ISSUE-NOV15.6-O100 part 1).
 */
const val GRAPH_SAMPLES_PER_MESSAGE = 720

/** The wall-clock time of the next whole minute after [nowMs]. */
fun nextGraphSampleTimeMs(nowMs: Long): Long = (nowMs / GRAPH_SAMPLE_INTERVAL_MS + 1) * GRAPH_SAMPLE_INTERVAL_MS

/** Emits each whole minute of [clock] as it passes, for as long as it is collected. */
fun graphSampleTimes(clock: () -> Long = System::currentTimeMillis): Flow<Long> = flow {
    while (true) {
        val next = nextGraphSampleTimeMs(clock())
        delay((next - clock()).coerceAtLeast(0L))
        emit(next)
    }
}

/** The time now, and again at each whole minute, for a Graphs screen whose period ends now. */
fun graphScreenClock(clock: () -> Long = System::currentTimeMillis): Flow<Long> = flow {
    emit(clock())
    graphSampleTimes(clock).collect { emit(clock()) }
}

/**
 * The largest value offered since the last [take] (ISSUE-NOV10.8-O84 part 3): a sensor thread offers each reading, the
 * minute's sampler takes the peak, so a knock between two samples is not lost.
 */
class PeakTracker {
    private var peak = Double.NaN

    @Synchronized
    fun offer(value: Double) {
        if (peak.isNaN() || value > peak) peak = value
    }

    /** The peak since the last take, or null when nothing was offered; starts the next period. */
    @Synchronized
    fun take(): Double? {
        val taken = peak.takeUnless { it.isNaN() }
        peak = Double.NaN
        return taken
    }
}

/** The link's down time up to [nowMs] since the app started, the current drop included; none before the first connect. */
fun LinkStats.downMsUntil(nowMs: Long): Long =
    totalDropMs + if (!isConnected && changedAtMs > 0L) (nowMs - changedAtMs).coerceAtLeast(0L) else 0L

/**
 * The share of the time from [beforeMs] to [afterMs] the link was up, in percent, from its statistics at both ends. A
 * link that never connected was down; without a [before] (the first sample of a run) only the current state counts.
 */
fun linkUpPercent(before: LinkStats?, beforeMs: Long, after: LinkStats, afterMs: Long): Float {
    if (!after.isConnected && after.changedAtMs == 0L) return 0f
    if (before == null || afterMs <= beforeMs) return if (after.isConnected) 100f else 0f
    val downMs = after.downMsUntil(afterMs) - before.downMsUntil(beforeMs)
    return (100f * (1f - downMs.toFloat() / (afterMs - beforeMs))).coerceIn(0f, 100f)
}

/**
 * A phone's samples (ISSUE-NOV10.8-O84 part 3), kept [RETENTION_MS] and across restarts in a file of one JSON array per
 * line. Like the [EventLog], writes run in order on one background thread and [samples] shows the result. The file is
 * appended each minute and rewritten only about once an hour, when the oldest samples have expired.
 */
class GraphHistory(
    file: File,
    private val clock: () -> Long = System::currentTimeMillis,
    writer: CoroutineDispatcher = Executors.newSingleThreadExecutor { Thread(it, "GraphHistory").apply { isDaemon = true } }
        .asCoroutineDispatcher()
) {
    private val scope = CoroutineScope(SupervisorJob() + writer)
    private val lines = JsonLinesFile(file, "GraphHistory")

    private val _samples = MutableStateFlow<List<GraphSample>>(emptyList())

    /** Every kept sample, oldest first, one per minute. */
    val samples: StateFlow<List<GraphSample>> = _samples.asStateFlow()

    /** The newest sample's time, or 0 without one: a Monitor asks the Sensor for the samples after it. */
    val newestTimeMs: Long get() = _samples.value.lastOrNull()?.timeMs ?: 0L

    init {
        scope.launch { load() } // The single writer thread runs it before any later change.
    }

    /** Adds [received] samples; a minute already kept is skipped, so the same samples may arrive again. */
    fun add(received: List<GraphSample>) {
        if (received.isEmpty()) return
        scope.launch {
            val current = _samples.value
            val known = current.mapTo(HashSet()) { it.timeMs }
            val new = received.filter { known.add(it.timeMs) }.sortedBy { it.timeMs }
            if (new.isEmpty()) return@launch
            val isAppend = current.isEmpty() || new.first().timeMs > current.last().timeMs
            val all = if (isAppend) current + new else (current + new).sortedBy { it.timeMs }
            val kept = retained(all, clock())
            _samples.value = kept
            if (isAppend && kept.size == all.size) lines.append(new.map { it.toJson().toString() }) else rewrite(kept)
        }
    }

    fun add(sample: GraphSample) = add(listOf(sample))

    fun clear() {
        scope.launch {
            _samples.value = emptyList()
            lines.delete()
        }
    }

    private fun load() {
        val stored = lines.readLines() ?: return
        val read = stored.mapNotNull { line -> runCatching { GraphSample.fromJson(Json.parseToJsonElement(line)) }.getOrNull() }
        val unique = read.associateBy { it.timeMs }.values.sortedBy { it.timeMs }
        val kept = retained(unique, clock(), pruneAll = true)
        _samples.value = kept
        if (kept.size != stored.size) rewrite(kept) // Drops expired samples and lines cut short.
    }

    private fun rewrite(samples: List<GraphSample>) = lines.rewrite(samples.map { it.toJson().toString() })

    companion object {
        /** The same 7 days as the events (user decision 2026-11-15). */
        const val RETENTION_MS = EventLog.RETENTION_MS

        /** Expired samples wait this long before the file is rewritten without them. */
        const val PRUNE_SLACK_MS = 3_600_000L

        /** A week of minutes and the slack, with room for a clock that was set back. */
        const val MAX_SAMPLES = 11_000

        /** The Sensor's samples: on the Sensor its own, on a Monitor the copy it received. */
        const val SENSOR_FILE_NAME = "graph_history_sensor.jsonl"

        /** A Monitor's own samples. */
        const val MONITOR_FILE_NAME = "graph_history_monitor.jsonl"

        /**
         * The samples to keep, oldest first: those of the last [RETENTION_MS], once the oldest is [PRUNE_SLACK_MS] past it
         * (or at once with [pruneAll]), and at most [MAX_SAMPLES].
         */
        fun retained(sortedSamples: List<GraphSample>, nowMs: Long, pruneAll: Boolean = false): List<GraphSample> {
            val oldest = sortedSamples.firstOrNull()?.timeMs ?: return sortedSamples
            val limitMs = nowMs - RETENTION_MS
            val pruned = if (oldest < limitMs - (if (pruneAll) 0L else PRUNE_SLACK_MS)) sortedSamples.filter { it.timeMs >= limitMs } else sortedSamples
            return if (pruned.size > MAX_SAMPLES) pruned.takeLast(MAX_SAMPLES) else pruned
        }
    }
}
