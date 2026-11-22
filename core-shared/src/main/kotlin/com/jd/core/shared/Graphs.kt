package com.jd.core.shared

import java.util.Locale

/**
 * What the menu's Graphs screen shows (ISSUE-NOV10.8-O84 part 3, user decisions 2026-11-15): Sensor motion (vibration
 * and tilt with their limits, noise, light since ISSUE-NOV22.4-O130), GNSS (accuracy, satellites, mean signal), Connection (relay link, latency, rows
 * held offline) and Phone health (battery with charging, temperature), each over the chosen [ViewPeriod]; a Monitor
 * adds its own connection (relay link, latency, the Sensor's rows live, backfilled and missing). Built here, drawn by
 * core-ui's `GraphsOverlay`.
 */

/** The colour role of a graph line; core-ui chooses the colour. */
enum class GraphTone { MAIN, SECOND, LIMIT }

data class GraphLineSpec(val field: GraphField, val label: String, val tone: GraphTone = GraphTone.MAIN)

/**
 * One graph: its [lines] share the unit and the vertical scale, which runs from [floor] and to [ceiling] when given and
 * otherwise follows the values. Where [bandField] is above 0 the background is shaded (charging).
 */
data class GraphPanelSpec(
    val title: String,
    val unit: String,
    val lines: List<GraphLineSpec>,
    val bandField: GraphField? = null,
    val bandLabel: String = "",
    val floor: Float? = null,
    val ceiling: Float? = null
)

data class GraphSectionSpec(val title: String, val panels: List<GraphPanelSpec>)

/** A line's value at each point of the period, oldest first; null where no sample fell. */
data class GraphLine(val label: String, val tone: GraphTone, val points: List<Float?>)

data class GraphPanel(
    val title: String,
    val unit: String,
    val lines: List<GraphLine>,
    /** Where the band is shaded, one entry per point; empty without a band. */
    val band: List<Boolean>,
    val bandLabel: String,
    val minY: Float,
    val maxY: Float,
    /** The first line's newest value and its range over the period, or a note that there is none. */
    val summary: String
) {
    val hasData: Boolean get() = lines.any { line -> line.points.any { it != null } }
}

/** A part of the period as shares of its width, from 0 (the start) to 1 (the end). */
data class GraphSpan(val start: Float, val end: Float)

/** A section's graphs; [stoppedSpans] are the times the Sensor's service did not run, shaded in each of its graphs. */
data class GraphSection(val title: String, val panels: List<GraphPanel>, val stoppedSpans: List<GraphSpan> = emptyList())

/**
 * A time the Sensor's service did not run, so it took no samples (ISSUE-NOV15.6-O100 part 3): from a stop event to the
 * next start event on the Sensor's clock, or to now while [isOngoing]. [reason] is the stop event's own, as recorded.
 */
data class ServiceStop(val startMs: Long, val endMs: Long, val reason: String, val isOngoing: Boolean) {
    /** The stop for the Graphs' list: its start written by [timeText], how long it lasted, and its reason. */
    fun describe(timeText: (Long) -> String): String =
        "${timeText(startMs)} ${if (isOngoing) "until now" else "for ${durationText(endMs - startMs)}"}: $reason"
}

/**
 * The Graphs screen: the period from [startMs] to [endMs], its sections, and a [note] above them when there is one.
 * [focusMs] is the event time the period is centred on (ISSUE-NOV15.6-O100 part 4), null for the period ending now;
 * [serviceStops] are the Sensor's service stops within the period, oldest first.
 */
data class GraphScreenState(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val sections: List<GraphSection> = emptyList(),
    val note: String? = null,
    val focusMs: Long? = null,
    val serviceStops: List<ServiceStop> = emptyList()
) {
    /** Where [focusMs] falls in the period, as a share of its width; null without one or outside the period. */
    val focusFraction: Float?
        get() = focusMs?.takeIf { endMs > startMs && it in startMs..endMs }?.let { (it - startMs).toFloat() / (endMs - startMs) }
}

/**
 * The end of the period the Graphs show: now, or with a [focusMs] the end of the period centred on it, but never past
 * now (ISSUE-NOV15.6-O100 part 4).
 */
fun graphPeriodEndMs(period: ViewPeriod, nowMs: Long, focusMs: Long?): Long =
    if (focusMs == null) nowMs else minOf(nowMs, focusMs + period.durationMs / 2)

/**
 * The Sensor's service stops that overlap the period from [startMs] to [endMs] (ISSUE-NOV15.6-O100 part 3), from its
 * [SERVICE_STOPPED] and [SERVICE_STARTED] events, oldest first: each stop lasts to the next start, or to [nowMs] when
 * none came yet. A second stop before the next start (a clean stop that Android's exit record reported again) belongs
 * to the first. Only the Sensor's events count, so a Monitor's own events are ignored; with a [trackerId] (a Monitor's
 * paired Sensor) the events received from another Sensor are ignored too (ISSUE-NOV15.6-O100 part 7, Nov17.2).
 */
fun serviceStops(events: List<EventEntry>, startMs: Long, endMs: Long, nowMs: Long, trackerId: String? = null): List<ServiceStop> {
    val stops = ArrayList<ServiceStop>()
    var open: EventEntry? = null
    val sensorEvents = events.filter {
        it.source == EventSource.SENSOR && (trackerId == null || it.trackerId == null || it.trackerId == trackerId)
    }
    sensorEvents.sortedBy { it.timestampMs }.forEach { event ->
        when {
            event.message.startsWith(SERVICE_STOPPED) -> if (open == null) open = event
            event.message.startsWith(SERVICE_STARTED) -> open?.let { stop ->
                stops += ServiceStop(stop.timestampMs, event.timestampMs, stopReason(stop.message), isOngoing = false)
                open = null
            }
        }
    }
    open?.let { stops += ServiceStop(it.timestampMs, maxOf(nowMs, it.timestampMs), stopReason(it.message), isOngoing = true) }
    return stops.filter { it.endMs > startMs && it.startMs < endMs }
}

/** The reason of a stop event: Android's when recorded after the fact, else a stop through the service's own end. */
private fun stopReason(message: String): String =
    message.removePrefix(SERVICE_STOPPED).removePrefix(":").trim().ifEmpty { "stopped on the Sensor phone" }

/** The [stops] as parts of the period from [startMs] to [endMs], cut at its ends. */
fun stoppedSpans(stops: List<ServiceStop>, startMs: Long, endMs: Long): List<GraphSpan> {
    if (endMs <= startMs) return emptyList()
    val width = (endMs - startMs).toFloat()
    return stops.map { GraphSpan(((it.startMs - startMs) / width).coerceIn(0f, 1f), ((it.endMs - startMs) / width).coerceIn(0f, 1f)) }
        .filter { it.end > it.start }
}

/** A graph has at most this many points, so a week is drawn as fast as an hour. */
const val MAX_GRAPH_POINTS = 360

/** One point a minute up to [MAX_GRAPH_POINTS]: 60 for an hour, 360 for 6 hours and more. */
fun graphPointCount(period: ViewPeriod): Int =
    (period.durationMs / GRAPH_SAMPLE_INTERVAL_MS).toInt().coerceIn(1, MAX_GRAPH_POINTS)

/**
 * [field]'s value at each of [count] equal points from [startMs] to [endMs], combined by the field's [GraphAggregate];
 * null where no sample fell. A sample covers the minute before its time; one up to a minute after [endMs] still counts,
 * since a Monitor's clock may lag the Sensor's.
 */
fun graphPoints(samples: List<GraphSample>, field: GraphField, startMs: Long, endMs: Long, count: Int): List<Float?> {
    if (count <= 0 || endMs <= startMs) return emptyList()
    val width = (endMs - startMs).toDouble() / count
    val sums = DoubleArray(count)
    val counts = IntArray(count)
    val peaks = DoubleArray(count) { Double.NEGATIVE_INFINITY }
    for (sample in samples) {
        if (sample.timeMs <= startMs || sample.timeMs > endMs + GRAPH_SAMPLE_INTERVAL_MS) continue
        val value = sample[field]?.toDouble() ?: continue
        val index = ((sample.timeMs - startMs - 1) / width).toInt().coerceIn(0, count - 1)
        sums[index] += value
        counts[index]++
        if (value > peaks[index]) peaks[index] = value
    }
    return List(count) { i ->
        when {
            counts[i] == 0 -> null
            field.aggregate == GraphAggregate.MAX -> peaks[i].toFloat()
            else -> (sums[i] / counts[i]).toFloat()
        }
    }
}

/** The Sensor's graphs, in both apps; the Monitor puts [titlePrefix] before the titles. */
fun sensorGraphSections(titlePrefix: String = ""): List<GraphSectionSpec> = listOf(
    GraphSectionSpec(
        "${titlePrefix}MOTION",
        listOf(
            GraphPanelSpec("Vibration", "g", listOf(GraphLineSpec(GraphField.VIBRATION_G, "Peak"), GraphLineSpec(GraphField.VIBRATION_LIMIT_G, "Limit", GraphTone.LIMIT)), floor = 0f),
            GraphPanelSpec("Tilt", "°", listOf(GraphLineSpec(GraphField.TILT_DEG, "Peak"), GraphLineSpec(GraphField.TILT_LIMIT_DEG, "Limit", GraphTone.LIMIT)), floor = 0f),
            GraphPanelSpec("Noise", "dBFS", listOf(GraphLineSpec(GraphField.NOISE_DBFS, "Peak"))),
            GraphPanelSpec("Light", "lx", listOf(GraphLineSpec(GraphField.LIGHT_LUX, "Peak")), floor = 0f)
        )
    ),
    GraphSectionSpec(
        "${titlePrefix}GNSS",
        listOf(
            GraphPanelSpec("Accuracy", "m", listOf(GraphLineSpec(GraphField.ACCURACY_M, "Accuracy")), floor = 0f),
            GraphPanelSpec("Satellites", "", listOf(GraphLineSpec(GraphField.SATS_USED, "Used"), GraphLineSpec(GraphField.SATS_IN_VIEW, "In view", GraphTone.SECOND)), floor = 0f),
            GraphPanelSpec("Mean signal", "dB-Hz", listOf(GraphLineSpec(GraphField.SIGNAL_DBHZ, "Used satellites")), floor = 0f)
        )
    ),
    GraphSectionSpec(
        "${titlePrefix}CONNECTION",
        listOf(
            GraphPanelSpec("Relay link up", "%", listOf(GraphLineSpec(GraphField.LINK_UP_PERCENT, "Up")), floor = 0f, ceiling = 100f),
            GraphPanelSpec("Latency", "ms", listOf(GraphLineSpec(GraphField.LATENCY_MS, "Round trip")), floor = 0f),
            GraphPanelSpec("Rows held offline", "", listOf(GraphLineSpec(GraphField.QUEUED_ROWS, "Held")), floor = 0f)
        )
    ),
    GraphSectionSpec(
        "${titlePrefix}PHONE HEALTH",
        listOf(
            GraphPanelSpec("Battery", "%", listOf(GraphLineSpec(GraphField.BATTERY_PERCENT, "Level")), GraphField.CHARGING, "Charging", floor = 0f, ceiling = 100f),
            GraphPanelSpec("Temperature", "°C", listOf(GraphLineSpec(GraphField.TEMPERATURE_C, "Battery")))
        )
    )
)

/** The Monitor's own connection, below the Sensor's graphs. */
val monitorGraphSection = GraphSectionSpec(
    "THIS MONITOR - CONNECTION",
    listOf(
        GraphPanelSpec("Relay link up", "%", listOf(GraphLineSpec(GraphField.LINK_UP_PERCENT, "Up")), floor = 0f, ceiling = 100f),
        GraphPanelSpec("Latency", "ms", listOf(GraphLineSpec(GraphField.LATENCY_MS, "Round trip")), floor = 0f),
        GraphPanelSpec(
            "Sensor rows",
            "",
            listOf(
                GraphLineSpec(GraphField.ROWS_LIVE, "Live a minute"),
                GraphLineSpec(GraphField.ROWS_BACKFILLED, "Backfilled a minute", GraphTone.SECOND),
                GraphLineSpec(GraphField.ROWS_MISSING, "Missing", GraphTone.LIMIT)
            ),
            floor = 0f
        )
    )
)

fun graphSection(
    spec: GraphSectionSpec,
    samples: List<GraphSample>,
    startMs: Long,
    endMs: Long,
    count: Int,
    stoppedSpans: List<GraphSpan> = emptyList()
): GraphSection = GraphSection(spec.title, spec.panels.map { graphPanel(it, samples, startMs, endMs, count) }, stoppedSpans)

fun graphPanel(spec: GraphPanelSpec, samples: List<GraphSample>, startMs: Long, endMs: Long, count: Int): GraphPanel {
    val lines = spec.lines.map { GraphLine(it.label, it.tone, graphPoints(samples, it.field, startMs, endMs, count)) }
    val values = lines.flatMap { line -> line.points.filterNotNull() }
    val minY = listOfNotNull(spec.floor, values.minOrNull()).minOrNull() ?: 0f
    val maxY = (listOfNotNull(spec.ceiling, values.maxOrNull()).maxOrNull() ?: minY).let { if (it > minY) it else minY + 1f }
    return GraphPanel(
        title = spec.title,
        unit = spec.unit,
        lines = lines,
        band = spec.bandField?.let { field -> graphPoints(samples, field, startMs, endMs, count).map { (it ?: 0f) > 0f } } ?: emptyList(),
        bandLabel = spec.bandLabel,
        minY = minY,
        maxY = maxY,
        summary = panelSummary(spec, lines.first())
    )
}

private fun panelSummary(spec: GraphPanelSpec, line: GraphLine): String {
    val values = line.points.filterNotNull()
    if (values.isEmpty()) return "No data in this period"
    val decimals = spec.lines.first().field.decimals
    fun text(value: Float) = "%.${decimals}f".format(Locale.US, value) + if (spec.unit.isEmpty()) "" else " ${spec.unit}"
    val low = values.min()
    val high = values.max()
    val range = if (low == high) text(low) else "${text(low)} to ${text(high)}"
    return "Newest ${text(line.points.last { it != null }!!)} · range $range"
}

/**
 * The Sensor's own Graphs screen: the period of [period] ending [nowMs], or centred on [focusMs]; its service stops
 * are taken from its [events].
 */
fun sensorGraphScreen(
    samples: List<GraphSample>,
    period: ViewPeriod,
    nowMs: Long,
    events: List<EventEntry> = emptyList(),
    focusMs: Long? = null
): GraphScreenState {
    val endMs = graphPeriodEndMs(period, nowMs, focusMs)
    val startMs = endMs - period.durationMs
    val count = graphPointCount(period)
    val stops = serviceStops(events, startMs, endMs, nowMs)
    val spans = stoppedSpans(stops, startMs, endMs)
    return GraphScreenState(
        startMs = startMs,
        endMs = endMs,
        sections = sensorGraphSections().map { graphSection(it, samples, startMs, endMs, count, spans) },
        note = if (samples.isEmpty()) "No samples yet: the Sensor takes one a minute while its service runs" else null,
        focusMs = focusMs,
        serviceStops = stops
    )
}

/**
 * A Monitor's Graphs screen: the paired Sensor's samples it received ([sensorSamples]), then its own connection, over
 * the period ending [nowMs] or centred on [focusMs]. The Sensor's service stops come from the events in this phone's
 * log ([events]) received from the paired Sensor [trackerId]: its newest ones, and with each answer of samples the
 * starts and stops among them (ISSUE-NOV15.6-O100 part 7). The note tells how old the newest Sensor sample is, since the
 * Sensor's samples arrive only while the screen is open and the Sensor is online.
 */
fun monitorGraphScreen(
    sensorSamples: List<GraphSample>,
    ownSamples: List<GraphSample>,
    period: ViewPeriod,
    nowMs: Long,
    events: List<EventEntry> = emptyList(),
    focusMs: Long? = null,
    trackerId: String? = null
): GraphScreenState {
    val endMs = graphPeriodEndMs(period, nowMs, focusMs)
    val startMs = endMs - period.durationMs
    val count = graphPointCount(period)
    val newestSensorMs = sensorSamples.lastOrNull()?.timeMs
    val stops = serviceStops(events, startMs, endMs, nowMs, trackerId)
    val spans = stoppedSpans(stops, startMs, endMs)
    return GraphScreenState(
        startMs = startMs,
        endMs = endMs,
        sections = sensorGraphSections("SENSOR - ").map { graphSection(it, sensorSamples, startMs, endMs, count, spans) } +
            graphSection(monitorGraphSection, ownSamples, startMs, endMs, count),
        note = when {
            newestSensorMs == null -> "No Sensor samples yet: they are asked for while this screen is open and the Sensor is online"
            nowMs - newestSensorMs > SENSOR_GRAPH_STALE_MS -> "Newest Sensor sample ${durationText(nowMs - newestSensorMs)} ago"
            else -> null
        },
        focusMs = focusMs,
        serviceStops = stops
    )
}

/** The Sensor samples once a minute and is asked once a minute, so its newest sample is old after this. */
const val SENSOR_GRAPH_STALE_MS = 3 * GRAPH_SAMPLE_INTERVAL_MS
