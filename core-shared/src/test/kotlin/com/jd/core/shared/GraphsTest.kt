package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 192 (ISSUE-NOV10.8-O84 part 3, Nov15.6): what the Graphs screens show of the samples. */
class GraphsTest {

    private val nowMs = 1_800_000_000_000L

    private fun sample(ageMinutes: Int, vararg readings: Pair<GraphField, Number?>) =
        GraphSample.of(nowMs - ageMinutes * GRAPH_SAMPLE_INTERVAL_MS, readings.toMap())

    @Test
    fun `a graph has one point a minute up to 360`() {
        assertEquals(60, graphPointCount(ViewPeriod.HOUR))
        assertEquals(360, graphPointCount(ViewPeriod.SIX_HOURS))
        assertEquals(360, graphPointCount(ViewPeriod.DAY))
        assertEquals(360, graphPointCount(ViewPeriod.WEEK))
    }

    @Test
    fun `peaks stay peaks, the rest is averaged, and a point without samples is empty`() {
        val samples = listOf(
            sample(10, GraphField.VIBRATION_G to 0.2, GraphField.BATTERY_PERCENT to 80),
            sample(9, GraphField.VIBRATION_G to 0.9, GraphField.BATTERY_PERCENT to 70),
            sample(1, GraphField.VIBRATION_G to 0.1)
        )
        val startMs = nowMs - 12 * GRAPH_SAMPLE_INTERVAL_MS
        // Four points of 3 minutes: the samples 10 and 9 minutes old share the first point.
        val vibration = graphPoints(samples, GraphField.VIBRATION_G, startMs, nowMs, 4)
        assertEquals(listOf(0.9f, null, null, 0.1f), vibration)
        val battery = graphPoints(samples, GraphField.BATTERY_PERCENT, startMs, nowMs, 4)
        assertEquals(75f, battery[0]!!, 0.001f)
        assertNull("A sample without the field leaves its point empty", battery[3])
    }

    @Test
    fun `a sample at the start is outside, one a little after the end still counts`() {
        val samples = listOf(sample(60, GraphField.LATENCY_MS to 100), sample(-1, GraphField.LATENCY_MS to 300))
        val points = graphPoints(samples, GraphField.LATENCY_MS, nowMs - 3_600_000L, nowMs, 60)
        assertNull("The sample at the start covers the minute before it", points.first())
        assertEquals("A Sensor clock a minute ahead still shows its newest sample", 300f, points.last())
        assertTrue(graphPoints(samples, GraphField.LATENCY_MS, nowMs, nowMs, 60).isEmpty())
    }

    @Test
    fun `a panel's scale keeps its floor and ceiling and says the newest value and range`() {
        val samples = listOf(
            sample(3, GraphField.BATTERY_PERCENT to 64, GraphField.CHARGING to 0),
            sample(2, GraphField.BATTERY_PERCENT to 66, GraphField.CHARGING to 1),
            sample(1, GraphField.BATTERY_PERCENT to 65, GraphField.CHARGING to 1)
        )
        val battery = sensorGraphSections().single { it.title == "PHONE HEALTH" }.panels.first()
        val panel = graphPanel(battery, samples, nowMs - 3_600_000L, nowMs, 60)
        assertEquals(0f, panel.minY)
        assertEquals(100f, panel.maxY)
        assertEquals("Newest 65 % · range 64 % to 66 %", panel.summary)
        // A sample covers the minute before its time: 3, 2 and 1 minutes old are points 56, 57 and 58 of 60.
        assertEquals(listOf(false, true, true, false), panel.band.subList(56, 60))
        assertTrue(panel.hasData)

        val noise = sensorGraphSections().single { it.title == "MOTION" }.panels.single { it.title == "Noise" }
        val quiet = graphPanel(noise, listOf(sample(1, GraphField.NOISE_DBFS to -70)), nowMs - 3_600_000L, nowMs, 60)
        assertEquals("A flat line still gets a scale", -70f, quiet.minY)
        assertEquals(-69f, quiet.maxY)
        val empty = graphPanel(noise, emptyList(), nowMs - 3_600_000L, nowMs, 60)
        assertFalse(empty.hasData)
        assertEquals("No data in this period", empty.summary)
    }

    @Test
    fun `vibration, tilt, noise and light are drawn with their limits`() {
        val motion = sensorGraphSections().single { it.title == "MOTION" }
        assertEquals(listOf("Vibration", "Tilt", "Noise", "Light"), motion.panels.map { it.title }) // Light: ISSUE-NOV22.4-O130
        assertEquals(GraphField.LIGHT_LUX, motion.panels[3].lines.first().field)
        assertEquals(GraphField.VIBRATION_LIMIT_G, motion.panels[0].lines.single { it.tone == GraphTone.LIMIT }.field)
        assertEquals(GraphField.TILT_LIMIT_DEG, motion.panels[1].lines.single { it.tone == GraphTone.LIMIT }.field)
        // ISSUE-NOV25.22-O144
        assertEquals(GraphField.NOISE_LIMIT_DBFS, motion.panels[2].lines.single { it.tone == GraphTone.LIMIT }.field)
        assertEquals(GraphField.LIGHT_LIMIT_LUX, motion.panels[3].lines.single { it.tone == GraphTone.LIMIT }.field)
    }

    @Test
    fun `an older Sensor's samples without a limit draw no limit line, and the scale holds a limit above the peaks`() {
        val noise = sensorGraphSections().single { it.title == "MOTION" }.panels.single { it.title == "Noise" }
        val old = graphPanel(noise, listOf(sample(1, GraphField.NOISE_DBFS to -70)), nowMs - 3_600_000L, nowMs, 60)
        assertTrue(old.lines.single { it.label == "Limit" }.points.all { it == null })
        val new = graphPanel(
            noise, listOf(sample(1, GraphField.NOISE_DBFS to -70, GraphField.NOISE_LIMIT_DBFS to -42)), nowMs - 3_600_000L, nowMs, 60
        )
        assertEquals(listOf(-42f), new.lines.single { it.label == "Limit" }.points.filterNotNull())
        assertEquals(-42f, new.maxY)
        assertEquals("Newest -70.0 dBFS · range -70.0 dBFS", new.summary)
    }

    @Test
    fun `the scale's middle value is halfway between its bottom and top`() {
        val battery = sensorGraphSections().single { it.title == "PHONE HEALTH" }.panels.first()
        assertEquals(50f, graphPanel(battery, listOf(sample(1, GraphField.BATTERY_PERCENT to 64)), nowMs - 3_600_000L, nowMs, 60).midY)
        val noise = sensorGraphSections().single { it.title == "MOTION" }.panels.single { it.title == "Noise" }
        val panel = graphPanel(
            noise, listOf(sample(2, GraphField.NOISE_DBFS to -70), sample(1, GraphField.NOISE_DBFS to -40)), nowMs - 3_600_000L, nowMs, 60
        )
        assertEquals(-55f, panel.midY)
    }

    @Test
    fun `the Sensor's screen has the four agreed sections`() {
        val screen = sensorGraphScreen(listOf(sample(1, GraphField.VIBRATION_G to 0.1)), ViewPeriod.DAY, nowMs)
        assertEquals(listOf("MOTION", "GNSS", "CONNECTION", "PHONE HEALTH"), screen.sections.map { it.title })
        assertEquals(nowMs - ViewPeriod.DAY.durationMs, screen.startMs)
        assertNull(screen.note)
        assertTrue(sensorGraphScreen(emptyList(), ViewPeriod.DAY, nowMs).note!!.startsWith("No samples yet"))
    }

    @Test
    fun `the Monitor shows the Sensor's graphs, then its own connection, and says when the Sensor's are old`() {
        val sensor = listOf(sample(10, GraphField.VIBRATION_G to 0.1))
        val own = listOf(sample(1, GraphField.ROWS_LIVE to 6, GraphField.ROWS_MISSING to 0))
        val screen = monitorGraphScreen(sensor, own, ViewPeriod.HOUR, nowMs)
        assertEquals(
            listOf("SENSOR - MOTION", "SENSOR - GNSS", "SENSOR - CONNECTION", "SENSOR - PHONE HEALTH", "THIS MONITOR - CONNECTION"),
            screen.sections.map { it.title }
        )
        assertEquals("Newest Sensor sample 10 min 0 s ago", screen.note)
        assertTrue(screen.sections.last().panels.single { it.title == "Sensor rows" }.hasData)
        assertNull(monitorGraphScreen(listOf(sample(1, GraphField.VIBRATION_G to 0.1)), own, ViewPeriod.HOUR, nowMs).note)
        assertTrue(monitorGraphScreen(emptyList(), own, ViewPeriod.HOUR, nowMs).note!!.startsWith("No Sensor samples yet"))
    }

    // Chapter 199 (ISSUE-NOV15.6-O100 parts 3 and 4, Nov16.8): why a gap, and the graphs of an event's time.

    private fun event(ageMinutes: Int, message: String, source: EventSource = EventSource.SENSOR) =
        EventEntry("e$ageMinutes-$message", nowMs - ageMinutes * GRAPH_SAMPLE_INTERVAL_MS, source, EventLevel.WARNING, message)

    private val minute = GRAPH_SAMPLE_INTERVAL_MS

    @Test
    fun `a service stop lasts to the next start, or to now while none came`() {
        val events = listOf(
            event(50, "$SERVICE_STARTED (version Nov16.7)"),
            event(40, "$SERVICE_STOPPED: app updated"),
            event(38, SERVICE_STOPPED), // The same stop, reported again: still the first one.
            event(30, "$SERVICE_STARTED (version Nov16.8)"),
            event(25, SERVICE_STOPPED, EventSource.MONITOR), // Not the Sensor's: ignored.
            event(10, SERVICE_STOPPED)
        )
        val stops = serviceStops(events, nowMs - 3_600_000L, nowMs, nowMs)
        assertEquals(
            listOf(
                ServiceStop(nowMs - 40 * minute, nowMs - 30 * minute, "app updated", isOngoing = false),
                ServiceStop(nowMs - 10 * minute, nowMs, "stopped on the Sensor phone", isOngoing = true)
            ),
            stops
        )
        assertTrue("A start without a stop before it is no stop", serviceStops(events.take(1), nowMs - 3_600_000L, nowMs, nowMs).isEmpty())
        assertEquals(
            "Only the stops overlapping the period",
            listOf(stops.first()),
            serviceStops(events, nowMs - 45 * minute, nowMs - 35 * minute, nowMs)
        )
        val format: (Long) -> String = { "${(nowMs - it) / minute}m" }
        assertEquals("40m for 10 min 0 s: app updated", stops[0].describe(format))
        assertEquals("10m until now: stopped on the Sensor phone", stops[1].describe(format))
    }

    @Test
    fun `a stop is shaded in the Sensor's graphs only, cut at the period's ends`() {
        val stop = ServiceStop(nowMs - 70 * minute, nowMs - 30 * minute, "app crashed", isOngoing = false)
        assertEquals(listOf(GraphSpan(0f, 0.5f)), stoppedSpans(listOf(stop), nowMs - 60 * minute, nowMs))
        assertTrue(stoppedSpans(listOf(stop), nowMs - 20 * minute, nowMs).isEmpty())

        val events = listOf(event(45, "$SERVICE_STOPPED: app crashed"), event(15, "$SERVICE_STARTED (version Nov16.8)"))
        val sensor = sensorGraphScreen(emptyList(), ViewPeriod.HOUR, nowMs, events)
        assertEquals(listOf("app crashed"), sensor.serviceStops.map { it.reason })
        assertTrue(sensor.sections.all { it.stoppedSpans == listOf(GraphSpan(0.25f, 0.75f)) })
        val monitor = monitorGraphScreen(emptyList(), emptyList(), ViewPeriod.HOUR, nowMs, events)
        assertTrue(monitor.sections.dropLast(1).all { it.stoppedSpans == listOf(GraphSpan(0.25f, 0.75f)) })
        assertTrue("The Monitor's own connection did not stop", monitor.sections.last().stoppedSpans.isEmpty())
    }

    @Test
    fun `the graphs of an event are centred on its time, but never run past now`() {
        assertEquals(nowMs, graphPeriodEndMs(ViewPeriod.HOUR, nowMs, null))
        assertEquals(nowMs - 90 * minute, graphPeriodEndMs(ViewPeriod.HOUR, nowMs, nowMs - 120 * minute))
        assertEquals(nowMs, graphPeriodEndMs(ViewPeriod.HOUR, nowMs, nowMs - 10 * minute))

        val focusMs = nowMs - 120 * minute
        val screen = sensorGraphScreen(listOf(sample(120, GraphField.VIBRATION_G to 0.8)), ViewPeriod.HOUR, nowMs, focusMs = focusMs)
        assertEquals(nowMs - 150 * minute, screen.startMs)
        assertEquals(nowMs - 90 * minute, screen.endMs)
        assertEquals(0.5f, screen.focusFraction!!, 0.0001f)
        assertTrue("The event's minute is drawn", screen.sections.first().panels.first().hasData)
        assertEquals(focusMs, monitorGraphScreen(emptyList(), emptyList(), ViewPeriod.HOUR, nowMs, focusMs = focusMs).focusMs)
        assertNull(sensorGraphScreen(emptyList(), ViewPeriod.HOUR, nowMs).focusFraction)
    }

    // Chapter 202 (ISSUE-NOV15.6-O100 part 7, Nov17.2): only the paired Sensor's stops, and all of them in its samples.

    @Test
    fun `a re-paired Monitor shades only the paired Sensor's stops`() {
        val events = listOf(
            event(50, "$SERVICE_STOPPED: app crashed").copy(trackerId = "OLD"),
            event(45, "$SERVICE_STARTED (version Nov17.1)").copy(trackerId = "OLD"),
            event(30, "$SERVICE_STOPPED: app updated").copy(trackerId = "T"),
            event(25, "$SERVICE_STARTED (version Nov17.2)").copy(trackerId = "T"),
            event(15, "$SERVICE_STOPPED: received before Nov17.2"), // Not marked: still counted.
            event(10, "$SERVICE_STARTED (version Nov17.2)")
        )
        val periodStart = nowMs - 3_600_000L
        assertEquals(listOf("app updated", "received before Nov17.2"), serviceStops(events, periodStart, nowMs, nowMs, "T").map { it.reason })
        assertEquals("Unpaired, or on the Sensor itself: every stop", 3, serviceStops(events, periodStart, nowMs, nowMs).size)
        val screen = monitorGraphScreen(emptyList(), emptyList(), ViewPeriod.HOUR, nowMs, events, trackerId = "T")
        assertEquals(listOf("app updated", "received before Nov17.2"), screen.serviceStops.map { it.reason })
    }

    @Test
    fun `the Sensor sends the starts and stops among the samples asked for`() {
        val events = listOf(
            event(300, "$SERVICE_STARTED (version Nov17.0)"),
            event(290, "Relay connected"),
            event(200, "$SERVICE_STARTED (version Nov17.1)"),
            event(120, "$SERVICE_STOPPED: reason unknown (after this, exact time unknown)"), // Learned at the next start.
            event(60, "$SERVICE_STARTED (version Nov17.2)"),
            event(30, "Relay connected")
        )
        fun ages(afterAgeMinutes: Int) = events.serviceLifecycleSince(nowMs - afterAgeMinutes * minute).map { (nowMs - it.timestampMs) / minute }
        assertEquals("A first sync: every start and stop", listOf(300L, 200L, 120L, 60L), events.serviceLifecycleSince(0L).map { (nowMs - it.timestampMs) / minute })
        assertEquals("The run going on at the newest sample held, then its stop dated before that sample", listOf(120L, 60L), ages(100))
        assertEquals(listOf(120L, 60L), ages(150))
        assertTrue("Nothing while the run that began before the request goes on", ages(50).isEmpty())
    }

    // SIMP-NOV23.4-S1 (Nov23.9): the kind tells a start or stop, whatever its wording.

    @Test
    fun `a start or stop is known by its kind, one without a kind still by its wording`() {
        val events = listOf(
            event(50, "$SERVICE_STOPPED: app crashed"), // Before Nov23.9: no kind.
            event(40, "Service up again").copy(kind = SERVICE_STARTED_KIND),
            event(30, "Service down").copy(kind = SERVICE_STOPPED_KIND),
            event(25, "$SERVICE_STARTED, but of another kind").copy(kind = "TILT_ALERT"), // Its kind decides: no start.
            event(20, "Service up").copy(kind = SERVICE_STARTED_KIND)
        )
        val stops = serviceStops(events, nowMs - 3_600_000L, nowMs, nowMs)
        assertEquals(listOf(50L to 40L, 30L to 20L), stops.map { (nowMs - it.startMs) / minute to (nowMs - it.endMs) / minute })
        assertEquals(listOf(50L, 40L, 30L, 20L), events.serviceLifecycleSince(0L).map { (nowMs - it.timestampMs) / minute })
    }
}
