package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.EventLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Chapter 190 (ISSUE-NOV10.8-O84 Nov15.4): the Monitor records each alarm raised, ended, and acknowledged. */
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
        assertEquals(listOf(LoggedEvent(EventLevel.INFO, "Ended: ${AlarmType.TILT_ALERT.title}")), alarmEvents(raised, cleared))

        val acknowledged = cleared.acknowledge()
        assertEquals("Acknowledging is recorded where the user gives it", emptyList<LoggedEvent>(), alarmEvents(cleared, acknowledged))

        val raisedAgain = acknowledged.update(setOf(AlarmType.TILT_ALERT), settings, nowMs = 3_000L)
        assertEquals(EventLevel.ALARM, alarmEvents(acknowledged, raisedAgain).single().level)
    }

    @Test
    fun `an acknowledged alarm whose condition then clears records Ended`() {
        val acknowledged = AlarmBook().update(setOf(AlarmType.TILT_ALERT), settings, nowMs = 1_000L).acknowledge()
        val cleared = acknowledged.update(emptySet(), settings, nowMs = 2_000L)
        assertEquals(listOf(LoggedEvent(EventLevel.INFO, "Ended: ${AlarmType.TILT_ALERT.title}")), alarmEvents(acknowledged, cleared))
    }

    @Test
    fun `an acknowledgement names the alarms that waited for it`() {
        val raised = AlarmBook().update(setOf(AlarmType.TILT_ALERT, AlarmType.LIFT_ALERT), settings, nowMs = 1_000L)
        assertEquals(
            LoggedEvent(EventLevel.INFO, "Acknowledged: ${AlarmType.TILT_ALERT.title}, ${AlarmType.LIFT_ALERT.title}"),
            acknowledgementEvent(raised)
        )
        assertNull("Nothing waited", acknowledgementEvent(raised.acknowledge()))
    }
}
