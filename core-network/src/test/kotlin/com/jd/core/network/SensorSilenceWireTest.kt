package com.jd.core.network

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
}
