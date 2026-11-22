package com.jd.app.sensor

import android.app.ApplicationExitInfo
import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventSource
import com.jd.core.shared.PastEvent
import com.jd.core.shared.SERVICE_STOPPED
import com.jd.core.shared.SensorFault
import com.jd.core.shared.SensorSetupItem
import com.jd.core.shared.SensorSetupReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Chapter 190 (ISSUE-NOV10.8-O84 Nov15.4): the Sensor records each change of its phone setup once. */
class SensorEventsTest {

    private val lacking = SensorSetupReport(
        missing = listOf(SensorSetupItem.MICROPHONE),
        faults = listOf(SensorFault.LOCATION_NEEDS_TAP),
        behaviourRevision = 1L
    )

    @Test
    fun `the first report is recorded, a lacking one as a warning naming each problem`() {
        val event = setupChangeEvent(null, lacking)!!
        assertEquals(EventLevel.WARNING, event.level)
        assertEquals("Phone setup needs attention: Microphone missing; ${SensorFault.LOCATION_NEEDS_TAP.text}", event.message)
        assertEquals(LoggedEvent(EventLevel.INFO, "Phone setup complete"), setupChangeEvent(null, SensorSetupReport()))
    }

    @Test
    fun `an unchanged setup records nothing, also with new detection settings`() {
        assertNull(setupChangeEvent(lacking, lacking))
        assertNull(setupChangeEvent(lacking, lacking.copy(behaviourRevision = 2L)))
    }

    @Test
    fun `a fixed setup is recorded as complete`() {
        assertEquals(LoggedEvent(EventLevel.INFO, "Phone setup complete"), setupChangeEvent(lacking, SensorSetupReport(behaviourRevision = 1L)))
    }

    private val bootMs = 1_800_000_000_000L
    private fun entry(atMs: Long, message: String) = EventEntry("S$atMs", atMs, EventSource.SENSOR, EventLevel.INFO, message)
    private val started = entry(bootMs + 1_000, serviceStartedMessage("Nov16.3"))
    private val lastSign = entry(bootMs + 5_000, "GPS fix current again")

    @Test
    fun `chapter 196 - a recorded stop, or no start at all, needs no event and no exit lookup`() {
        val noLookup = { error("asked") }
        assertNull(unrecordedStopEvent(emptyList(), bootMs, noLookup))
        assertNull(unrecordedStopEvent(listOf(started, lastSign, entry(bootMs + 6_000, SERVICE_STOPPED)), bootMs, noLookup))
    }

    @Test
    fun `chapter 196 - an unrecorded stop takes the first Android exit after the start`() {
        val exits = listOf(
            ProcessExit(bootMs + 9_000, ApplicationExitInfo.REASON_CRASH), // A later run without the service.
            ProcessExit(bootMs + 7_000, ApplicationExitInfo.REASON_LOW_MEMORY),
            ProcessExit(bootMs + 500, ApplicationExitInfo.REASON_USER_REQUESTED) // Before the start: an older run.
        )
        assertEquals(
            PastEvent(EventLevel.WARNING, "$SERVICE_STOPPED: killed by Android for memory", bootMs + 7_000),
            unrecordedStopEvent(listOf(started, lastSign), bootMs, { exits })
        )
    }

    @Test
    fun `chapter 196 - without an exit record the stop follows the last event, as a restart when before boot`() {
        assertEquals(
            PastEvent(EventLevel.WARNING, "$SERVICE_STOPPED: reason unknown (after this, exact time unknown)", bootMs + 5_001),
            unrecordedStopEvent(listOf(started, lastSign), bootMs, { emptyList() })
        )
        val laterBoot = bootMs + 3_600_000
        assertEquals(
            "$SERVICE_STOPPED: phone shut down or restarted (after this, exact time unknown)",
            unrecordedStopEvent(listOf(started, lastSign), laterBoot, { emptyList() })!!.message
        )
    }

    @Test
    fun `chapter 196 - a stop recorded after the fact counts as recorded`() {
        val pastStop = entry(bootMs + 7_000, "$SERVICE_STOPPED: app crashed")
        assertNull(unrecordedStopEvent(listOf(started, pastStop), bootMs) { error("asked") })
        assertEquals("app updated", exitReasonText(ApplicationExitInfo.REASON_PACKAGE_UPDATED))
        assertEquals("killed by Android (reason 99)", exitReasonText(99))
    }

    @Test
    fun `a lost relay is a warning, the Monitor's coming and going is information`() {
        assertEquals(EventLevel.WARNING, relayLinkEvent(false).level)
        assertEquals(EventLevel.INFO, relayLinkEvent(true).level)
        assertEquals(EventLevel.INFO, monitorLinkEvent(false).level)
    }

    @Test
    fun `chapter 240 - the first message of a Monitor names it`() {
        assertEquals(LoggedEvent(EventLevel.INFO, "Heard from Monitor M1"), monitorHeardEvent("M1"))
    }
}
