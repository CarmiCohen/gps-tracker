package com.jd.app.monitor

import com.jd.core.shared.SensorSetupItem
import com.jd.core.shared.SensorSetupReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 227 (ISSUE-NOV10.8-O86, the O83 warnings by push): the Sensor's setup report reaches the Monitor by the relay
 * connection and by push; the later sent copy wins, and each new warning is notified once (user decision 2026-11-18).
 */
class SensorSetupNoticesTest {

    private fun report(sentAtMs: Long, tracker: String = "T", vararg missing: SensorSetupItem) =
        SensorSetupReport(trackerId = tracker, missing = missing.toList(), sentAtMs = sentAtMs)

    @Test
    fun `the later sent report wins, whichever road brought it`() {
        assertTrue("The first report", isNewerSetupReport(null, report(5)))
        assertTrue(isNewerSetupReport(report(5), report(6)))
        assertTrue("The same report by the other road", isNewerSetupReport(report(5), report(5)))
        assertFalse("A push held while the phone dozed", isNewerSetupReport(report(6, missing = arrayOf(SensorSetupItem.MICROPHONE)), report(5)))
    }

    @Test
    fun `another tracker's report and a report of a Sensor before Nov18_12 are always taken`() {
        assertTrue("A new pairing starts over", isNewerSetupReport(report(9_000, tracker = "T"), report(5, tracker = "U")))
        assertTrue("No time on either: the later arrival", isNewerSetupReport(report(0), report(0)))
    }

    @Test
    fun `a warning not notified before shows the notification`() {
        assertEquals(SetupNotice.SHOW, setupNotice(listOf("Microphone: missing"), emptySet()))
        assertEquals(
            "One new beside an old one",
            SetupNotice.SHOW,
            setupNotice(listOf("Microphone: missing", "Notifications: missing"), setOf("Microphone: missing"))
        )
        assertEquals(
            "A warning that went and came back is new again",
            SetupNotice.SHOW,
            setupNotice(listOf("Microphone: missing"), setOf("Notifications: missing"))
        )
    }

    @Test
    fun `the same or fewer warnings leave the notification alone, and none removes it`() {
        assertEquals("Sent again when a Monitor comes online", SetupNotice.KEEP, setupNotice(listOf("Microphone: missing"), setOf("Microphone: missing")))
        assertEquals(
            "One of two fixed",
            SetupNotice.KEEP,
            setupNotice(listOf("Microphone: missing"), setOf("Microphone: missing", "Notifications: missing"))
        )
        assertEquals(SetupNotice.CANCEL, setupNotice(emptyList(), setOf("Microphone: missing")))
        assertEquals("Nothing shown and nothing missing", SetupNotice.CANCEL, setupNotice(emptyList(), emptySet()))
    }
}
