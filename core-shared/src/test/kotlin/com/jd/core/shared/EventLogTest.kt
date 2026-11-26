package com.jd.core.shared

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Chapter 190 (ISSUE-NOV10.8-O84 Nov15.4): the event log keeps 7 days of events across restarts. */
@OptIn(ExperimentalCoroutinesApi::class)
class EventLogTest {

    private val directory: File = Files.createTempDirectory("event-log").toFile()
    private val file = File(directory, EventLog.FILE_NAME)
    private var nowMs = 1_800_000_000_000L

    @After
    fun cleanUp() {
        directory.deleteRecursively()
    }

    private fun TestScope.log(source: EventSource = EventSource.MONITOR) =
        EventLog(file, source, clock = { nowMs }, writer = StandardTestDispatcher(testScheduler))

    private fun event(id: String, ageMs: Long, level: EventLevel = EventLevel.INFO, source: EventSource = EventSource.SENSOR) =
        EventEntry(id, nowMs - ageMs, source, level, "event $id")

    @Test
    fun `an event is kept across a restart`() = runTest {
        val log = log()
        log.record(EventLevel.ALARM, "Alarm: Tracker tilted")
        runCurrent()
        assertEquals(1, log.events.value.size)

        val restarted = log()
        runCurrent()
        val kept = restarted.events.value.single()
        assertEquals("Alarm: Tracker tilted", kept.message)
        assertEquals(EventLevel.ALARM, kept.level)
        assertEquals(EventSource.MONITOR, kept.source)
        assertEquals(nowMs, kept.timestampMs)
    }

    @Test
    fun `a past event is decided from the loaded log, before later records, at its own time`() = runTest {
        log(EventSource.SENSOR).record(EventLevel.INFO, "Sensor service started")
        runCurrent()

        nowMs += 600_000
        val restarted = log(EventSource.SENSOR)
        var seen: List<String>? = null
        restarted.recordFrom { events ->
            seen = events.map { it.message }
            PastEvent(EventLevel.WARNING, "Sensor service stopped: app crashed", nowMs - 300_000)
        }
        restarted.record(EventLevel.INFO, "Sensor service started")
        runCurrent()
        assertEquals("The decision saw the stored log, not the later record", listOf("Sensor service started"), seen)
        assertEquals(
            listOf("Sensor service started", "Sensor service stopped: app crashed", "Sensor service started"),
            restarted.events.value.map { it.message }
        )
        assertEquals(nowMs - 300_000, restarted.events.value[1].timestampMs)
        assertEquals(EventSource.SENSOR, restarted.events.value[1].source)

        restarted.recordFrom { null }
        restarted.recordFrom { error("broken") }
        runCurrent()
        assertEquals("No event, and a failing decision changes nothing", 3, restarted.events.value.size)
    }

    @Test
    fun `the peer's events are merged once, in time order`() = runTest {
        val log = log()
        log.record(EventLevel.INFO, "Relay connected")
        runCurrent()
        val sensorEvents = listOf(event("a", ageMs = 60_000), event("b", ageMs = 30_000))
        log.merge(sensorEvents)
        log.merge(sensorEvents + event("c", ageMs = 10_000)) // Sent again with a new one.
        runCurrent()
        assertEquals(listOf("a", "b", "c"), log.events.value.filter { it.source == EventSource.SENSOR }.map { it.id })
        assertEquals("Oldest first", log.events.value.sortedBy { it.timestampMs }, log.events.value)

        val restarted = log()
        runCurrent()
        assertEquals(4, restarted.events.value.size)
    }

    @Test
    fun `events older than 7 days are dropped, also from the file`() = runTest {
        val log = log()
        log.merge(listOf(event("old", ageMs = EventLog.RETENTION_MS + 1), event("new", ageMs = EventLog.RETENTION_MS - 1)))
        runCurrent()
        assertEquals(listOf("new"), log.events.value.map { it.id })

        nowMs += 2 // "new" is now older than 7 days too.
        val restarted = log()
        runCurrent()
        assertTrue(restarted.events.value.isEmpty())
        assertEquals("The expired lines are removed from the file", "", file.readText())
    }

    @Test
    fun `a line cut short by a crash is skipped`() = runTest {
        val log = log()
        log.record(EventLevel.INFO, "Sensor service started")
        runCurrent()
        file.appendText("{\"id\":\"S1\",\"timestampMs\":")

        val restarted = log()
        runCurrent()
        assertEquals(listOf("Sensor service started"), restarted.events.value.map { it.message })
        assertEquals("The file is repaired", 1, file.readLines().size)
    }

    @Test
    fun `the log holds at most MAX_EVENTS, the newest`() = runTest {
        val log = log()
        log.merge((0..EventLog.MAX_EVENTS).map { event("e$it", ageMs = (EventLog.MAX_EVENTS - it).toLong()) })
        runCurrent()
        assertEquals(EventLog.MAX_EVENTS, log.events.value.size)
        assertEquals("e1", log.events.value.first().id)
    }

    @Test
    fun `clear deletes every event and the file`() = runTest {
        val log = log()
        log.record(EventLevel.WARNING, "Relay connection lost")
        runCurrent()
        log.clear()
        runCurrent()
        assertTrue(log.events.value.isEmpty())
        assertFalse(file.exists())
        log.record(EventLevel.INFO, "Relay connected")
        runCurrent()
        assertEquals(1, log().also { runCurrent() }.events.value.size)
    }

    @Test
    fun `ids differ and name the phone`() = runTest {
        val log = log(EventSource.SENSOR)
        repeat(50) { log.record(EventLevel.DETAIL, "tick") }
        runCurrent()
        val ids = log.events.value.map { it.id }
        assertEquals(50, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("S") })
    }

    @Test
    fun `the list shows the chosen period, newest first, details only on request`() {
        val events = listOf(
            event("week", ageMs = 3 * 24 * 3_600_000L),
            event("hours", ageMs = 2 * 3_600_000L),
            event("detail", ageMs = 60_000, level = EventLevel.DETAIL),
            event("now", ageMs = 1_000, level = EventLevel.ALARM)
        )
        assertEquals(listOf("now"), events.shown(ViewPeriod.HOUR, EventFilter.MAIN, nowMs = nowMs).map { it.id })
        assertEquals(listOf("now", "detail"), events.shown(ViewPeriod.HOUR, EventFilter.ALL, nowMs = nowMs).map { it.id })
        assertEquals(listOf("now", "hours"), events.shown(ViewPeriod.SIX_HOURS, EventFilter.MAIN, nowMs = nowMs).map { it.id })
        assertEquals(listOf("now", "hours", "week"), events.shown(ViewPeriod.WEEK, EventFilter.MAIN, nowMs = nowMs).map { it.id })
        assertEquals(EventLog.RETENTION_MS, ViewPeriod.WEEK.durationMs)
    }

    /**
     * ISSUE-NOV25.12-O142: the red Alarms filter lists only the alarms. ISSUE-NOV25.25-O156: one choice of three, Main
     * between Alarms and All; an alarm answered by the Monitor only under All.
     */
    @Test
    fun `the Alarms filter lists only the alarms, an alarm answered by the Monitor only under All`() {
        val events = listOf(
            event("warning", ageMs = 5_000, level = EventLevel.WARNING),
            event("echo", ageMs = 4_000, level = EventLevel.ALARM),
            event("info", ageMs = 3_000),
            event("detail", ageMs = 2_000, level = EventLevel.DETAIL),
            event("alarm", ageMs = 1_000, level = EventLevel.ALARM)
        )
        val echo = setOf("echo")
        assertEquals(listOf("alarm"), events.shown(ViewPeriod.HOUR, EventFilter.ALARMS, nowMs = nowMs, detailIds = echo).map { it.id })
        assertEquals(listOf("alarm", "info", "warning"), events.shown(ViewPeriod.HOUR, EventFilter.MAIN, nowMs = nowMs, detailIds = echo).map { it.id })
        assertEquals(listOf("alarm", "detail", "info", "echo", "warning"), events.shown(ViewPeriod.HOUR, EventFilter.ALL, nowMs = nowMs, detailIds = echo).map { it.id })
        assertEquals(listOf("Alarms", "Main", "All"), EventFilter.entries.map { it.label })
    }

    /** ISSUE-NOV25.12-O142: Settings exported and Trails saved are listed only under All (More before O156), and stay on the Sensor. */
    @Test
    fun `a saved file's event is listed only under All`() {
        val saved = listOf(event("saved", ageMs = 1_000, level = FILE_SAVED_LEVEL))
        assertEquals(emptyList<String>(), saved.shown(ViewPeriod.HOUR, EventFilter.MAIN, nowMs = nowMs).map { it.id })
        assertEquals(listOf("saved"), saved.shown(ViewPeriod.HOUR, EventFilter.ALL, nowMs = nowMs).map { it.id })
        assertEquals(emptyList<EventEntry>(), saved.forPeer())
    }

    @Test
    fun `the Sensor sends its newest events without details`() {
        val events = (1..EventLog.PEER_EVENTS_SENT + 5).map { event("e$it", ageMs = 1_000L * (100 - it)) } +
            event("detail", ageMs = 0, level = EventLevel.DETAIL)
        val newest = events.newestForPeer()
        val sent = newest.events
        assertEquals(EventLog.PEER_EVENTS_SENT, sent.size)
        assertEquals("e${EventLog.PEER_EVENTS_SENT + 5}", sent.last().id)
        assertTrue(sent.none { it.level == EventLevel.DETAIL })
        assertEquals("Complete after the newest one left out", events.single { it.id == "e5" }.timestampMs, newest.afterMs)
        assertEquals("Nothing left out: complete from the start", 0L, events.take(3).newestForPeer().afterMs)
    }

    /** Chapter 251 (ISSUE-NOV23.4-O139). */
    @Test
    fun `the given detail ids show only under All, and the Home Points received still go to the Monitor`() {
        val events = listOf(event("echo", ageMs = 2_000, level = EventLevel.ALARM), event("now", ageMs = 1_000, level = EventLevel.ALARM))
        assertEquals(listOf("now"), events.shown(ViewPeriod.HOUR, EventFilter.MAIN, nowMs = nowMs, detailIds = setOf("echo")).map { it.id })
        assertEquals(listOf("now", "echo"), events.shown(ViewPeriod.HOUR, EventFilter.ALL, nowMs = nowMs, detailIds = setOf("echo")).map { it.id })

        val homePoints = event("home", ageMs = 0, level = EventLevel.DETAIL).copy(message = "$HOME_POINTS_RECEIVED: 2, radius 50 m")
        assertEquals(listOf("home"), listOf(event("detail", ageMs = 0, level = EventLevel.DETAIL), homePoints).forPeer().map { it.id })
    }

    /** SIMP-NOV23.4-S1 (Nov23.9): the Home Points received are known by their kind, an older one by its wording. */
    @Test
    fun `the Home Points received go to the Monitor by their kind, whatever their wording`() {
        val detail = EventLevel.DETAIL
        val events = listOf(
            event("home", ageMs = 0, level = detail).copy(message = "New home: 3 points", kind = HOME_POINTS_RECEIVED_KIND),
            event("legacy", ageMs = 0, level = detail).copy(message = "$HOME_POINTS_RECEIVED: 2, radius 50 m"),
            event("other", ageMs = 0, level = detail).copy(message = "$HOME_POINTS_RECEIVED: worded so", kind = "OTHER")
        )
        assertEquals(listOf("home", "legacy"), events.forPeer().map { it.id })
    }

    @Test
    fun `an event recorded after the fact keeps its kind`() = runTest {
        val log = log(EventSource.SENSOR)
        log.recordFrom { PastEvent(EventLevel.WARNING, "$SERVICE_STOPPED: app crashed", nowMs - 5_000, kind = SERVICE_STOPPED_KIND) }
        runCurrent()
        assertEquals(listOf(SERVICE_STOPPED_KIND), log.events.value.map { it.kind })
    }

    @Test
    fun `a recorded kind is kept`() = runTest {
        val log = log(EventSource.SENSOR)
        log.record(EventLevel.ALARM, "Lift alert: 0.3 m", kind = LIFT_ALERT_STATUS)
        log.record(EventLevel.INFO, "Relay connected")
        runCurrent()
        assertEquals(listOf(LIFT_ALERT_STATUS, null), log.events.value.map { it.kind })
    }

    @Test
    fun `a request is answered with every event after, in chunks without holes (O98 part 4)`() {
        val events = (1..2 * EventLog.PEER_EVENTS_SENT + 10).map { event("e$it", ageMs = 1_000_000L - 1_000L * it) } +
            event("detail", ageMs = 0, level = EventLevel.DETAIL)
        val after = events[4].timestampMs
        val chunks = events.peerEventsAfter(after)
        assertEquals(listOf(EventLog.PEER_EVENTS_SENT, EventLog.PEER_EVENTS_SENT, 5), chunks.map { it.events.size })
        assertEquals("e6", chunks.first().events.first().id)
        assertEquals(after, chunks[0].afterMs)
        assertEquals(chunks[0].events.last().timestampMs, chunks[1].afterMs)
        assertEquals(chunks[1].events.last().timestampMs, chunks[2].afterMs)
        assertTrue(chunks.flatMap { it.events }.none { it.level == EventLevel.DETAIL })
        assertEquals(emptyList<PeerEvents>(), events.peerEventsAfter(nowMs))
    }

    @Test
    fun `a Monitor asks for the events it missed, once per retry time (O98 part 4)`() {
        var clockMs = 0L
        val catchUp = PeerEventCatchUp(syncedMs = 100L, clock = { clockMs })
        fun batch(after: Long?, vararg times: Long) =
            SensorEventBatch("T", times.map { EventEntry("s$it", it, EventSource.SENSOR, EventLevel.INFO, "m", "T") }, after)

        assertEquals("Contiguous: moves on", null, catchUp.received(batch(90L, 95L, 120L)))
        assertEquals("A hole after 120", 120L, catchUp.received(batch(200L, 210L)))
        clockMs += 1_000
        assertEquals("Asked already", null, catchUp.received(batch(210L, 220L)))
        assertEquals("Start and stop events say nothing", null, catchUp.received(batch(null, 500L)))
        assertEquals("The answer's first chunk", null, catchUp.received(batch(120L, 150L, 200L)))
        assertEquals("The answer's second chunk", null, catchUp.received(batch(200L, 210L, 220L)))
        assertEquals("Now the live batch fits", null, catchUp.received(batch(210L, 220L, 230L)))
        assertEquals(null, catchUp.received(batch(300L, 310L)))
        clockMs += PeerEventCatchUp.RETRY_MS
        assertEquals("An answer cut short: asked again after the retry time", 230L, catchUp.received(batch(300L, 320L)))
    }

    @Test
    fun `a Monitor starts from the newest event it keeps of its Sensor`() {
        val events = listOf(
            EventEntry("a", 10L, EventSource.SENSOR, EventLevel.INFO, "m", "T"),
            EventEntry("b", 20L, EventSource.SENSOR, EventLevel.INFO, "m", "OTHER"),
            EventEntry("c", 30L, EventSource.MONITOR, EventLevel.INFO, "m")
        )
        assertEquals(10L, PeerEventCatchUp.syncedFrom(events, "T"))
        assertEquals(0L, PeerEventCatchUp.syncedFrom(events, "NEW"))
    }

    @Test
    fun `the export has one line per event`() {
        val text = eventLogText(listOf(event("a", ageMs = 0, level = EventLevel.WARNING), event("b", ageMs = 0, source = EventSource.MONITOR)))
        val lines = text.lines().filter { it.isNotEmpty() }
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("Sensor") && lines[0].contains("WARNING") && lines[0].endsWith("event a"))
        assertTrue(lines[1].contains("Monitor") && lines[1].endsWith("event b"))
        assertEquals("", eventLogText(emptyList()))
    }

    @Test
    fun `a link logs its changes from its first connection on`() = runTest {
        val link = MutableStateFlow(false)
        val changes = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { link.linkChanges().toList(changes) }
        link.value = true
        link.value = false
        link.value = true
        job.cancel()
        assertEquals("No loss before the first connection", listOf(true, false, true), changes)
    }
}
