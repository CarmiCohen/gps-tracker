package com.jd.app.monitor

import com.jd.core.shared.LinkStats
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.PhoneState
import com.jd.core.shared.ReceivedSensorInfo
import com.jd.core.shared.SENSOR_INFO_STALE_MS
import com.jd.core.shared.SatelliteReading
import com.jd.core.shared.SensorInfo
import com.jd.core.ui.FixReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 191 (ISSUE-NOV10.8-O84 part 2, user decisions 2026-11-15): the Monitor's Info shows the Sensor's own Info,
 * gray once old, else its newest row, and always a short section about the Monitor phone.
 */
class MonitorInfoScreenTest {

    private val now = 1_794_000_000_000L
    private val satellites = listOf(SatelliteReading(7, 1, 36f, true))
    private val info = SensorInfo(capturedAtMs = now - 60_000L, sensorId = "T", fixTimeMs = now - 61_000L, lat = 32.0, lng = 34.0, satellites = satellites)
    private val localFix = LocalFix(MapPosition(32.0, 34.01), FixReading(timeMs = now - 4_000L, accuracyMeters = 9f, speedMps = null))

    private fun screen(received: ReceivedSensorInfo?, row: LocationUpdate? = null, fix: LocalFix? = localFix) =
        monitorInfoScreen(received, row, LinkStats(isConnected = true), fix, PhoneState(), "V", "Nov15.5", now)

    @Test
    fun `a fresh Info shows the Sensor's sections, its satellites, then this phone`() {
        // The Sensor's clock is a minute behind: the Info's age uses the Monitor's receive time, not the Sensor's clock.
        val state = screen(ReceivedSensorInfo(info, receivedAtMs = now - 3_000L))
        assertNull(state.note)
        assertEquals(
            listOf("SENSOR - SERVICE", "SENSOR - CONNECTION", "SENSOR - POSITION", "SENSOR - SENSORS", "SENSOR - PHONE", "THIS PHONE (MONITOR)"),
            state.sections.map { it.title }
        )
        assertFalse(state.sections.any { it.isStale })
        assertEquals(satellites, state.satellites)
        assertEquals("1 s", state.sections[2].lines.single { it.label == "Fix age" }.value)
        assertEquals("About 0.01 degrees of longitude apart", "943 m", state.sections.last().lines.single { it.label == "Distance to Sensor" }.value)
    }

    /** Chapter 197 (ISSUE-NOV15.5-O99 part 1): an Info sent without the satellite list still shows its summary. */
    @Test
    fun `an Info without the satellite list shows the summary and the list as pending`() {
        val lean = info.copy(satellites = emptyList(), satellitesUsed = 9, satellitesInView = 31, meanCn0DbHz = 30.5f)
        val state = screen(ReceivedSensorInfo(lean, receivedAtMs = now - 3_000L))
        val position = state.sections[2].lines
        assertEquals("9 used / 31 in view", position.single { it.label == "Satellites" }.value)
        assertTrue("The list comes once the Monitor's list asks for it", state.isSatelliteListPending)
        val olderSensor = screen(ReceivedSensorInfo(info, receivedAtMs = now - 3_000L))
        assertFalse("A received list is not pending", olderSensor.isSatelliteListPending)
        assertEquals("A Sensor before Nov16.4 sends only the list", "1 used / 1 in view", olderSensor.sections[2].lines.single { it.label == "Satellites" }.value)
        val none = info.copy(satellites = emptyList())
        assertFalse("No satellite in view is no pending list", screen(ReceivedSensorInfo(none, receivedAtMs = now - 3_000L)).isSatelliteListPending)
    }

    @Test
    fun `an old Info grays the Sensor's sections and says since when`() {
        val state = screen(ReceivedSensorInfo(info, receivedAtMs = now - SENSOR_INFO_STALE_MS - 1_000L))
        assertTrue(state.sections.dropLast(1).all { it.isStale })
        assertFalse("This phone's own values are current", state.sections.last().isStale)
        assertEquals("No Info from the Sensor for 21 s: the gray values are from then.", state.note)
    }

    @Test
    fun `without an Info the newest row stands in, and without either only this phone shows`() {
        val row = LocationUpdate(deviceId = "T", lat = 32.0, lng = 34.0, timestamp = now - 5_000L)
        val waiting = screen(null, row)
        assertEquals(WAITING_FOR_INFO_NOTE, waiting.note)
        assertEquals(listOf("SENSOR'S NEWEST ROW", "THIS PHONE (MONITOR)"), waiting.sections.map { it.title })
        assertEquals("943 m", waiting.sections.last().lines.single { it.label == "Distance to Sensor" }.value)
        assertTrue(waiting.satellites.isEmpty())

        val nothing = screen(null, null, fix = null)
        assertEquals(NO_SENSOR_DATA_NOTE, nothing.note)
        assertEquals(listOf("THIS PHONE (MONITOR)"), nothing.sections.map { it.title })
        assertEquals("--", nothing.sections.single().lines.single { it.label == "Distance to Sensor" }.value)
    }
}
