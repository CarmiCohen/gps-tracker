package com.jd.core.network

import com.jd.core.shared.SilenceCause
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Chapter 181 (ISSUE-NOV10.8-O86): the relay's Sensor silence notice reads back as the relay wrote it. */
class SensorSilenceWireTest {

    @Test
    fun testANoticeAsTheRelayWritesItIsRead() {
        // The text of relay index.js: JSON.stringify({ type: "sensor_silent", id, lastTs, lastSeq, silentMs }).
        val text = """{"type":"sensor_silent","id":"T","lastTs":1762900000123,"lastSeq":42,"silentMs":35004}"""

        assertEquals(SensorSilence("T", 1762900000123L, 42L, 35004L), SensorSilenceWire.decodeText(text))
    }

    @Test
    fun testAnythingElseIsNotANotice() {
        assertNull("another notice type", SensorSilenceWire.decodeText("""{"type":"other","id":"T","lastTs":1}"""))
        assertNull("no Sensor", SensorSilenceWire.decodeText("""{"type":"sensor_silent","lastTs":1}"""))
        assertNull("no last row time", SensorSilenceWire.decodeText("""{"type":"sensor_silent","id":"T"}"""))
        assertNull("not JSON", SensorSilenceWire.decodeText("not a notice"))
        assertNull("empty", SensorSilenceWire.decodeText(""))
    }

    /** Chapter 250 (ISSUE-NOV22.10-O137): the notice's cause, and the Sensor's last word. */
    @Test
    fun testTheCauseIsReadAndAnUnknownOrMissingOneIsAPlainSilence() {
        // Relay v6.049 adds the cause: JSON.stringify({ type: "sensor_silent", id, lastTs, lastSeq, silentMs, cause }).
        val text = """{"type":"sensor_silent","id":"T","lastTs":5600,"lastSeq":7,"silentMs":1000,"cause":"airplane_mode"}"""
        assertEquals(SensorSilence("T", 5600L, 7L, 1000L, SilenceCause.AIRPLANE_MODE), SensorSilenceWire.decodeText(text))
        SilenceCause.entries.forEach { cause ->
            val notice = """{"type":"sensor_silent","id":"T","lastTs":1,"cause":"${cause.wire}"}"""
            assertEquals(cause, SensorSilenceWire.decodeText(notice)?.cause)
        }
        val unknown = """{"type":"sensor_silent","id":"T","lastTs":1,"cause":"meteor"}"""
        assertEquals(SilenceCause.SILENT, SensorSilenceWire.decodeText(unknown)?.cause)
        assertEquals(SilenceCause.SILENT, SensorSilenceWire.decodeText("""{"type":"sensor_silent","id":"T","lastTs":1}""")?.cause)
    }

    @Test
    fun testTheLastWordCarriesTheSensorItsCauseAndItsTime() {
        val word = LastWordWire.encode("T", SilenceCause.OWNER_STOP, sentAtMs = 5600L)
        assertEquals("T", word.getString("id"))
        assertEquals("owner_stop", word.getString("cause"))
        assertEquals(5600L, word.getLong("ts"))
        assertEquals("sensor_last_word", LastWordWire.EVENT)
    }
}
