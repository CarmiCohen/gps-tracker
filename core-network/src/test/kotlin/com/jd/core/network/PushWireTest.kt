package com.jd.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 235 (SIMP-NOV18.12-S1): one decoder reads every push the relay sends the Monitor, and one rule tells
 * whether it comes from the paired tracker.
 */
class PushWireTest {

    private val rowText = """{"id":"T","lat":32.1,"lng":34.8,"ts":5000,"seq":7,"status":"ACOUSTIC_ALERT","crit":true}"""
    private val noticeText = """{"type":"sensor_silent","id":"T","lastTs":5000,"lastSeq":7,"silentMs":35004}"""
    private val setupText = """{"id":"T","type":"sensor_setup","missing":[],"faults":[],"behaviourRev":3,"sentAt":9000}"""

    @Test
    fun testEachKeyReadsAsItsMessage() {
        val row = PushWire.decode(mapOf(PushWire.ROW_KEY to rowText))
        assertTrue(row is PushMessage.Row)
        assertEquals(7L, (row as PushMessage.Row).row.sequenceId)
        assertEquals("T", row.trackerId)

        val notice = PushWire.decode(mapOf(PushWire.NOTICE_KEY to noticeText))
        assertEquals(PushMessage.Silence(SensorSilence("T", 5000L, 7L, 35004L)), notice)
        assertEquals("T", notice!!.trackerId)

        val setup = PushWire.decode(mapOf(PushWire.SETUP_KEY to setupText))
        assertTrue(setup is PushMessage.Setup)
        assertEquals(3L, (setup as PushMessage.Setup).report.behaviourRevision)
        assertEquals("T", setup.trackerId)
    }

    @Test
    fun testTheKeysAreTheRelays() {
        // relay index.js writes data: { row }, { notice } and { setup }; a rename there must show here.
        assertEquals("row", PushWire.ROW_KEY)
        assertEquals("notice", PushWire.NOTICE_KEY)
        assertEquals("setup", PushWire.SETUP_KEY)
    }

    @Test
    fun testAnUnreadablePushIsNoMessage() {
        assertNull("no data", PushWire.decode(emptyMap()))
        assertNull("an unknown key", PushWire.decode(mapOf("other" to rowText)))
        assertNull("a row that is not JSON", PushWire.decode(mapOf(PushWire.ROW_KEY to "not a row")))
        assertNull("another notice type", PushWire.decode(mapOf(PushWire.NOTICE_KEY to """{"type":"other","id":"T","lastTs":1}""")))
        assertNull("a malformed setup report", PushWire.decode(mapOf(PushWire.SETUP_KEY to """{"type":"sensor_setup","id":"T"}""")))
    }

    @Test
    fun testAnUnreadableSetupFallsBackToTheRow() {
        val message = PushWire.decode(mapOf(PushWire.SETUP_KEY to "not JSON", PushWire.ROW_KEY to rowText))
        assertTrue(message is PushMessage.Row)
    }

    @Test
    fun testOnlyThePairedTrackerIsTaken() {
        assertTrue(isPairedTracker("T", "T"))
        assertFalse("another tracker", isPairedTracker("X", "T"))
        assertFalse("an unpaired Monitor must stay asleep", isPairedTracker("T", null))
    }
}
