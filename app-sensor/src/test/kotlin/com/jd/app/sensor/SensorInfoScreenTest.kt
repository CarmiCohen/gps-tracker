package com.jd.app.sensor

import com.jd.core.shared.SatelliteReading
import com.jd.core.shared.SensorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 191 (ISSUE-NOV10.8-O84 part 2): the Sensor's own Info screen, from the running service only. */
class SensorInfoScreenTest {

    @Test
    fun `a stopped service shows a note and no values`() {
        val state = sensorInfoScreen(null)
        assertEquals(SERVICE_STOPPED_NOTE, state.note)
        assertTrue(state.sections.isEmpty())
    }

    @Test
    fun `a running service shows its sections and satellites`() {
        val satellites = listOf(SatelliteReading(3, 1, 30f, true))
        val state = sensorInfoScreen(SensorInfo(capturedAtMs = 1L, satellites = satellites))
        assertNull(state.note)
        assertEquals(listOf("SERVICE", "CONNECTION", "POSITION", "SENSORS", "PHONE"), state.sections.map { it.title })
        assertSame(satellites, state.satellites)
    }

    @Test
    fun `the source gives the running service's Info and none once it stops`() {
        val source = SensorInfoSource()
        assertNull(source.current())
        source.register { SensorInfo(sensorId = "T") }
        assertEquals("T", source.current()!!.sensorId)
        source.unregister()
        assertNull(source.current())
    }
}
