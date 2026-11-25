package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventSource
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
import com.jd.core.shared.LIFT_ALERT_STATUS
import com.jd.core.shared.TILT_ALERT_STATUS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Chapter 190 (ISSUE-NOV10.8-O84 Nov15.4): the Monitor records each alarm raised, ended, and acknowledged; chapter 251
 * (ISSUE-NOV23.4-O139): the end and the acknowledgement as details, and the Sensor alarms it answered listed only with
 * Details.
 */
class MonitorEventsTest {

    private val settings = AlertSettings()

    @Test
    fun `a raised alarm is recorded once`() {
        val empty = AlarmBook()
        val raised = empty.update(setOf(AlarmType.TILT_ALERT), settings, nowMs = 1_000L)
        assertEquals(listOf(LoggedEvent(EventLevel.ALARM, "Alarm: ${AlarmType.TILT_ALERT.title}")), alarmEvents(empty, raised))

        val again = raised.update(setOf(AlarmType.TILT_ALERT), settings, nowMs = 2_000L)
        assertEquals("A condition that still holds records nothing", emptyList<LoggedEvent>(), alarmEvents(raised, again))
    }

    @Test
    fun `an ended condition is recorded, and a new raise after acknowledgement too`() {
        val raised = AlarmBook().update(setOf(AlarmType.TILT_ALERT), settings, nowMs = 1_000L)
        val cleared = raised.update(emptySet(), settings, nowMs = 2_000L)
        assertEquals(listOf(LoggedEvent(EventLevel.DETAIL, "Ended: ${AlarmType.TILT_ALERT.title}")), alarmEvents(raised, cleared))

        val acknowledged = cleared.acknowledge()
        assertEquals("Acknowledging is recorded where the user gives it", emptyList<LoggedEvent>(), alarmEvents(cleared, acknowledged))

        val raisedAgain = acknowledged.update(setOf(AlarmType.TILT_ALERT), settings, nowMs = 3_000L)
        assertEquals(EventLevel.ALARM, alarmEvents(acknowledged, raisedAgain).single().level)
    }

    @Test
    fun `an acknowledged alarm whose condition then clears records Ended`() {
        val acknowledged = AlarmBook().update(setOf(AlarmType.TILT_ALERT), settings, nowMs = 1_000L).acknowledge()
        val cleared = acknowledged.update(emptySet(), settings, nowMs = 2_000L)
        assertEquals(listOf(LoggedEvent(EventLevel.DETAIL, "Ended: ${AlarmType.TILT_ALERT.title}")), alarmEvents(acknowledged, cleared))
    }

    /** ISSUE-NOV17.10-O110: a state alarm that ends unacknowledged still records its start and its end. */
    @Test
    fun `an unacknowledged state alarm that clears records Alarm then Ended`() {
        val empty = AlarmBook()
        val raised = empty.update(setOf(AlarmType.POWER_DISCONNECTED), settings, nowMs = 1_000L)
        val cleared = raised.update(emptySet(), settings, nowMs = 2_000L)

        assertEquals(listOf(LoggedEvent(EventLevel.ALARM, "Alarm: ${AlarmType.POWER_DISCONNECTED.title}")), alarmEvents(empty, raised))
        assertEquals(listOf(LoggedEvent(EventLevel.DETAIL, "Ended: ${AlarmType.POWER_DISCONNECTED.title}")), alarmEvents(raised, cleared))
        assertNull("Nothing is left to acknowledge", acknowledgementEvent(cleared))
    }

    /** ISSUE-NOV15.4-O98 part 2. */
    @Test
    fun `the tracker going offline and returning is recorded, not before it is first heard`() {
        val unheard = AlarmBook()
        val online = AlarmBook(isTrackerOnline = true)
        val offline = AlarmBook(isTrackerOnline = false)
        assertEquals(listOf(LoggedEvent(EventLevel.INFO, "Sensor online")), alarmEvents(unheard, online))
        assertEquals(EventLevel.WARNING, alarmEvents(online, offline).single().level)
        assertEquals(listOf(LoggedEvent(EventLevel.INFO, "Sensor online")), alarmEvents(offline, online))
        assertEquals("No change", emptyList<LoggedEvent>(), alarmEvents(online, online))
        assertEquals("A new pairing starts over", emptyList<LoggedEvent>(), alarmEvents(online, unheard))
        assertEquals("Kept by update and acknowledge", true, online.update(setOf(AlarmType.TILT_ALERT), settings, 1_000L).acknowledge().isTrackerOnline)
    }

    @Test
    fun `an acknowledgement names the alarms that waited for it`() {
        val raised = AlarmBook().update(setOf(AlarmType.TILT_ALERT, AlarmType.LIFT_ALERT), settings, nowMs = 1_000L)
        assertEquals(
            LoggedEvent(EventLevel.DETAIL, "Acknowledged: ${AlarmType.TILT_ALERT.title}, ${AlarmType.LIFT_ALERT.title}"),
            acknowledgementEvent(raised)
        )
        assertNull("Nothing waited", acknowledgementEvent(raised.acknowledge()))
    }

    /** Chapter 251 (ISSUE-NOV23.4-O139). */
    @Test
    fun `a Sensor alarm the Monitor answered with the same alarm is a detail, any other stays listed`() {
        val t = 1_800_000_000_000L
        fun sensor(id: String, atMs: Long, kind: String?) =
            EventEntry(id, atMs, EventSource.SENSOR, EventLevel.ALARM, "Sensor alarm $id", trackerId = "T", kind = kind)
        fun monitor(id: String, atMs: Long, type: AlarmType) = EventEntry(id, atMs, EventSource.MONITOR, EventLevel.ALARM, alarmMessage(type))
        val events = listOf(
            sensor("lift", t, LIFT_ALERT_STATUS), monitor("m-lift", t + 2_000, AlarmType.LIFT_ALERT),
            sensor("tilt", t + 1_000, TILT_ALERT_STATUS), // No tilt alarm answered it; the lift alarm does not count.
            sensor("old", t + 3_000, null), // A Sensor before Nov23.4 names no kind.
            sensor("long-before", t + 2_000 - ECHO_AFTER_MS - 1, LIFT_ALERT_STATUS),
            sensor("clock-ahead", t + 2_000 + ECHO_BEFORE_MS, LIFT_ALERT_STATUS),
            sensor("too-far-ahead", t + 2_001 + ECHO_BEFORE_MS, LIFT_ALERT_STATUS),
            sensor("jump", t + 10_000, "JUMP"), monitor("m-jump", t + 9_000, AlarmType.GPS_JUMP)
        )
        assertEquals(setOf("lift", "clock-ahead", "jump"), echoedSensorAlarms(events))
        assertEquals("No Monitor alarm, nothing hidden", emptySet<String>(), echoedSensorAlarms(events.filter { it.source == EventSource.SENSOR }))
    }

    /** SIMP-NOV25.0-S1: one row per alarm that has both ended and been acknowledged. */
    @Test
    fun `an alarm both ended and acknowledged gets one summary line, any other keeps its own`() {
        fun event(id: String, atMs: Long, level: EventLevel, message: String, source: EventSource = EventSource.MONITOR) =
            EventEntry(id, atMs, source, level, message)
        val lift = AlarmType.LIFT_ALERT.title
        val tilt = AlarmType.TILT_ALERT.title
        val power = AlarmType.POWER_DISCONNECTED.title
        val events = listOf(
            event("lift-1", 1, EventLevel.ALARM, alarmMessage(AlarmType.LIFT_ALERT)),
            event("tilt-1", 2, EventLevel.ALARM, alarmMessage(AlarmType.TILT_ALERT)),
            event("sensor-ended", 3, EventLevel.DETAIL, "Ended: $lift", EventSource.SENSOR), // Not the Monitor's.
            event("ack-1", 4, EventLevel.DETAIL, "Acknowledged: $lift, $tilt"),
            event("lift-ended", 5, EventLevel.DETAIL, "Ended: $lift"),
            event("power", 6, EventLevel.ALARM, alarmMessage(AlarmType.POWER_DISCONNECTED)),
            event("power-ended", 7, EventLevel.DETAIL, "Ended: $power"), // Ended, never acknowledged.
            event("lift-2", 8, EventLevel.ALARM, alarmMessage(AlarmType.LIFT_ALERT)),
            event("tilt-ended", 9, EventLevel.DETAIL, "Ended: $tilt"), // After a later alarm of another type: still tilt-1's.
            event("ack-2", 10, EventLevel.DETAIL, "Acknowledged: $lift") // lift-2 acknowledged, not ended.
        )
        assertEquals(
            mapOf(
                "lift-1" to "Alarm: $lift, ended t5, acknowledged t4",
                "tilt-1" to "Alarm: $tilt, ended t9, acknowledged t4"
            ),
            finishedAlarmTexts(events) { "t$it" }
        )
    }

    /** Chapter 253 (ISSUE-NOV22.7-O134): a Monitor that takes the shared set says so in its list, not only with More. */
    @Test
    fun `taking the shared Home Points is listed`() {
        val shared = GeofenceConfig(listOf(HomePoint(32.1, 34.8), HomePoint(32.2, 34.9)), radiusMeters = 150.0, revision = 9L)
        assertEquals(
            LoggedEvent(EventLevel.INFO, "Home points updated to the shared set: 2, radius 150 m"),
            sharedHomePointsEvent(shared)
        )
    }
}
