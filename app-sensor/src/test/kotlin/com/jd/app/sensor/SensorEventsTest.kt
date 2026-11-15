package com.jd.app.sensor

import com.jd.core.shared.EventLevel
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

    @Test
    fun `a lost relay is a warning, the Monitor's coming and going is information`() {
        assertEquals(EventLevel.WARNING, relayLinkEvent(false).level)
        assertEquals(EventLevel.INFO, relayLinkEvent(true).level)
        assertEquals(EventLevel.INFO, monitorLinkEvent(false).level)
    }
}
