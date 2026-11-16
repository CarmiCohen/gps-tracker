package com.jd.core.shared

import java.util.Locale

/**
 * What the menu's Graphs screen shows (ISSUE-NOV10.8-O84 part 3, user decisions 2026-11-15): Sensor motion (vibration
 * and tilt with their limits, noise), GNSS (accuracy, satellites, mean signal), Connection (relay link, latency, rows
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

data class GraphSection(val title: String, val panels: List<GraphPanel>)

/** The Graphs screen: the period from [startMs] to [endMs], its sections, and a [note] above them when there is one. */
data class GraphScreenState(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val sections: List<GraphSection> = emptyList(),
    val note: String? = null
)

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
            GraphPanelSpec("Noise", "dBFS", listOf(GraphLineSpec(GraphField.NOISE_DBFS, "Peak")))
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

fun graphSection(spec: GraphSectionSpec, samples: List<GraphSample>, startMs: Long, endMs: Long, count: Int): GraphSection =
    GraphSection(spec.title, spec.panels.map { graphPanel(it, samples, startMs, endMs, count) })

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

/** The Sensor's own Graphs screen: the period of [period] ending [nowMs]. */
fun sensorGraphScreen(samples: List<GraphSample>, period: ViewPeriod, nowMs: Long): GraphScreenState {
    val startMs = nowMs - period.durationMs
    val count = graphPointCount(period)
    return GraphScreenState(
        startMs = startMs,
        endMs = nowMs,
        sections = sensorGraphSections().map { graphSection(it, samples, startMs, nowMs, count) },
        note = if (samples.isEmpty()) "No samples yet: the Sensor takes one a minute while its service runs" else null
    )
}

/**
 * A Monitor's Graphs screen: the paired Sensor's samples it received ([sensorSamples]), then its own connection. The
 * note tells how old the newest Sensor sample is, since the Sensor's samples arrive only while the screen is open and
 * the Sensor is online.
 */
fun monitorGraphScreen(sensorSamples: List<GraphSample>, ownSamples: List<GraphSample>, period: ViewPeriod, nowMs: Long): GraphScreenState {
    val startMs = nowMs - period.durationMs
    val count = graphPointCount(period)
    val newestSensorMs = sensorSamples.lastOrNull()?.timeMs
    return GraphScreenState(
        startMs = startMs,
        endMs = nowMs,
        sections = sensorGraphSections("SENSOR - ").map { graphSection(it, sensorSamples, startMs, nowMs, count) } +
            graphSection(monitorGraphSection, ownSamples, startMs, nowMs, count),
        note = when {
            newestSensorMs == null -> "No Sensor samples yet: they are asked for while this screen is open and the Sensor is online"
            nowMs - newestSensorMs > SENSOR_GRAPH_STALE_MS -> "Newest Sensor sample ${durationText(nowMs - newestSensorMs)} ago"
            else -> null
        }
    )
}

/** The Sensor samples once a minute and is asked once a minute, so its newest sample is old after this. */
const val SENSOR_GRAPH_STALE_MS = 3 * GRAPH_SAMPLE_INTERVAL_MS
