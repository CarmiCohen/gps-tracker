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
        assertEquals(60, graphPointCount(EventPeriod.HOUR))
        assertEquals(360, graphPointCount(EventPeriod.SIX_HOURS))
        assertEquals(360, graphPointCount(EventPeriod.DAY))
        assertEquals(360, graphPointCount(EventPeriod.WEEK))
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
    fun `vibration and tilt are drawn with their limits`() {
        val motion = sensorGraphSections().single { it.title == "MOTION" }
        assertEquals(listOf("Vibration", "Tilt", "Noise"), motion.panels.map { it.title })
        assertEquals(GraphField.VIBRATION_LIMIT_G, motion.panels[0].lines.single { it.tone == GraphTone.LIMIT }.field)
        assertEquals(GraphField.TILT_LIMIT_DEG, motion.panels[1].lines.single { it.tone == GraphTone.LIMIT }.field)
    }

    @Test
    fun `the Sensor's screen has the four agreed sections`() {
        val screen = sensorGraphScreen(listOf(sample(1, GraphField.VIBRATION_G to 0.1)), EventPeriod.DAY, nowMs)
        assertEquals(listOf("MOTION", "GNSS", "CONNECTION", "PHONE HEALTH"), screen.sections.map { it.title })
        assertEquals(nowMs - EventPeriod.DAY.durationMs, screen.startMs)
        assertNull(screen.note)
        assertTrue(sensorGraphScreen(emptyList(), EventPeriod.DAY, nowMs).note!!.startsWith("No samples yet"))
    }

    @Test
    fun `the Monitor shows the Sensor's graphs, then its own connection, and says when the Sensor's are old`() {
        val sensor = listOf(sample(10, GraphField.VIBRATION_G to 0.1))
        val own = listOf(sample(1, GraphField.ROWS_LIVE to 6, GraphField.ROWS_MISSING to 0))
        val screen = monitorGraphScreen(sensor, own, EventPeriod.HOUR, nowMs)
        assertEquals(
            listOf("SENSOR - MOTION", "SENSOR - GNSS", "SENSOR - CONNECTION", "SENSOR - PHONE HEALTH", "THIS MONITOR - CONNECTION"),
            screen.sections.map { it.title }
        )
        assertEquals("Newest Sensor sample 10 min 0 s ago", screen.note)
        assertTrue(screen.sections.last().panels.single { it.title == "Sensor rows" }.hasData)
        assertNull(monitorGraphScreen(listOf(sample(1, GraphField.VIBRATION_G to 0.1)), own, EventPeriod.HOUR, nowMs).note)
        assertTrue(monitorGraphScreen(emptyList(), own, EventPeriod.HOUR, nowMs).note!!.startsWith("No Sensor samples yet"))
    }
}
