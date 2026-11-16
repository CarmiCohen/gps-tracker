package com.jd.core.shared

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Chapter 192 (ISSUE-NOV10.8-O84 part 3, Nov15.6): one sample a minute, kept 7 days, compact on file and wire. */
@OptIn(ExperimentalCoroutinesApi::class)
class GraphHistoryTest {

    private val directory: File = Files.createTempDirectory("graph-history").toFile()
    private val file = File(directory, GraphHistory.SENSOR_FILE_NAME)
    private var nowMs = 1_800_000_000_000L

    @After
    fun cleanUp() {
        directory.deleteRecursively()
    }

    private fun TestScope.history() = GraphHistory(file, clock = { nowMs }, writer = StandardTestDispatcher(testScheduler))

    private fun sample(ageMs: Long, battery: Int = 80) =
        GraphSample.of(nowMs - ageMs, mapOf(GraphField.BATTERY_PERCENT to battery, GraphField.VIBRATION_G to 0.123456))

    @Test
    fun `a sample keeps its order, rounds its values and leaves unknown ones out`() {
        val sample = GraphSample.of(
            60_000L,
            mapOf(GraphField.VIBRATION_G to 0.123456, GraphField.TILT_DEG to null, GraphField.SATS_USED to 26.4, GraphField.NOISE_DBFS to Double.NaN)
        )
        // Time, vibration, (limit), (tilt), (limit), (noise), (accuracy), satellites used; nothing after the last known value.
        assertEquals("[60000,0.12,null,null,null,null,null,26]", sample.toJson().toString())
        val read = GraphSample.fromJson(Json.parseToJsonElement(sample.toJson().toString()))!!
        assertEquals(sample, read)
        assertEquals(0.12f, read[GraphField.VIBRATION_G])
        assertNull(read[GraphField.TILT_DEG])
        assertNull(read[GraphField.ROWS_MISSING])
    }

    @Test
    fun `a newer phone's extra fields are ignored and a malformed sample is skipped`() {
        val fromNewer = "[[60000,0.5,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,99,98]]"
        val read = decodeGraphSamples(fromNewer).single()
        assertEquals(17f, read[GraphField.ROWS_MISSING])
        assertEquals(2, decodeGraphSamples("[[60000],\"x\",[],[120000,1]]").size)
        assertTrue(decodeGraphSamples("not json").isEmpty())
    }

    @Test
    fun `the wire form of a busy minute is small`() {
        val busy = GraphSample.of(
            1_800_000_000_000L,
            mapOf(
                GraphField.VIBRATION_G to 0.123, GraphField.VIBRATION_LIMIT_G to 0.5, GraphField.TILT_DEG to 12.34,
                GraphField.TILT_LIMIT_DEG to 15.0, GraphField.NOISE_DBFS to -61.27, GraphField.ACCURACY_M to 14.1,
                GraphField.SATS_USED to 26, GraphField.SATS_IN_VIEW to 55, GraphField.SIGNAL_DBHZ to 23.91,
                GraphField.LINK_UP_PERCENT to 100, GraphField.LATENCY_MS to 361, GraphField.QUEUED_ROWS to 0,
                GraphField.BATTERY_PERCENT to 87, GraphField.CHARGING to 1, GraphField.TEMPERATURE_C to 31.5
            )
        )
        val oneMessage = encodeGraphSamples(List(GRAPH_SAMPLES_PER_MESSAGE) { busy })
        assertTrue("A message of 12 hours stays far below the relay's 1 MB: ${oneMessage.length}", oneMessage.length < 100_000)
    }

    @Test
    fun `samples are kept across a restart and a minute is kept once`() = runTest {
        val history = history()
        history.add(sample(ageMs = 120_000))
        history.add(listOf(sample(ageMs = 60_000), sample(ageMs = 120_000, battery = 10)))
        runCurrent()
        assertEquals(listOf(80f, 80f), history.samples.value.map { it[GraphField.BATTERY_PERCENT] })
        assertEquals(nowMs - 60_000, history.newestTimeMs)
        assertEquals("Appended, never rewritten", 2, file.readLines().size)

        val restarted = history()
        runCurrent()
        assertEquals(history.samples.value, restarted.samples.value)
    }

    @Test
    fun `an older sample received later is put in its place`() = runTest {
        val history = history()
        history.add(sample(ageMs = 60_000))
        history.add(sample(ageMs = 180_000))
        runCurrent()
        assertEquals(listOf(nowMs - 180_000, nowMs - 60_000), history.samples.value.map { it.timeMs })

        val restarted = history()
        runCurrent()
        assertEquals(listOf(nowMs - 180_000, nowMs - 60_000), restarted.samples.value.map { it.timeMs })
    }

    @Test
    fun `expired samples go about once an hour, and at once on loading`() = runTest {
        val history = history()
        history.add(sample(ageMs = GraphHistory.RETENTION_MS + 30 * 60_000L))
        history.add(sample(ageMs = 60_000))
        runCurrent()
        assertEquals("Within the slack an expired sample stays: no rewrite each minute", 2, history.samples.value.size)

        nowMs += 31 * 60_000L
        history.add(sample(ageMs = 0))
        runCurrent()
        assertEquals("Past the slack the expired sample goes", 2, history.samples.value.size)
        assertEquals(2, file.readLines().size)

        nowMs += GraphHistory.RETENTION_MS - 10 * 60_000L
        val restarted = history()
        runCurrent()
        assertEquals("Loading drops every expired sample", 1, restarted.samples.value.size)
    }

    @Test
    fun `a line cut short by a crash is skipped, and clear deletes the file`() = runTest {
        val history = history()
        history.add(sample(ageMs = 60_000))
        runCurrent()
        file.appendText("[1800000000000,0.1")

        val restarted = history()
        runCurrent()
        assertEquals(1, restarted.samples.value.size)
        assertEquals(1, file.readLines().size)

        restarted.clear()
        runCurrent()
        assertTrue(restarted.samples.value.isEmpty())
        assertFalse(file.exists())
    }

    @Test
    fun `a peak is taken once`() {
        val peaks = PeakTracker()
        assertNull(peaks.take())
        listOf(0.2, 1.5, 0.7).forEach(peaks::offer)
        assertEquals(1.5, peaks.take()!!, 0.0)
        assertNull("Taking starts the next minute", peaks.take())
        peaks.offer(-70.0)
        peaks.offer(-75.0)
        assertEquals(-70.0, peaks.take()!!, 0.0)
    }

    @Test
    fun `the link's share of a minute up counts its drops`() {
        val t0 = 1_800_000_000_000L
        val up = LinkStats().afterLink(true, t0 - 120_000)
        assertEquals(0f, linkUpPercent(null, 0L, LinkStats(), t0), 0f)
        assertEquals(100f, linkUpPercent(null, 0L, up, t0), 0f)
        assertEquals(100f, linkUpPercent(up, t0 - 60_000, up, t0), 0f)
        // Down 15 s of the minute, back up before its end.
        val dropped = up.afterLink(false, t0 - 30_000).afterLink(true, t0 - 15_000)
        assertEquals(75f, linkUpPercent(up, t0 - 60_000, dropped, t0), 0.01f)
        // Down since 30 s before the end, still down.
        val down = up.afterLink(false, t0 - 30_000)
        assertEquals(50f, linkUpPercent(up, t0 - 60_000, down, t0), 0.01f)
        assertEquals(0f, linkUpPercent(down, t0, down, t0 + 60_000), 0.01f)
    }

    @Test
    fun `samples are taken on the whole minute`() {
        assertEquals(1_800_000_060_000L, nextGraphSampleTimeMs(1_800_000_000_000L))
        assertEquals(1_800_000_060_000L, nextGraphSampleTimeMs(1_800_000_059_999L))
    }
}
