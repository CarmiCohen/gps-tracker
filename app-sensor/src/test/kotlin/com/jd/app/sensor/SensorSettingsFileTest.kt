package com.jd.app.sensor

import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.SettingsFileException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Chapter 194.2 (ISSUE-NOV10.8-O80, ISSUE-NOV5.2-O16): the Sensor's settings file keeps its ID, relay and the Monitor's
 * settings across a reinstall, and the imported Sensor continues its sequence numbers above any the Monitor holds.
 */
class SensorSettingsFileTest {

    private val file = SensorSettingsFile(
        exportedAtMs = 1_000_000L,
        appVersion = "Nov16.1",
        sensorId = "T",
        relayUrl = "wss://relay.example",
        behaviour = SensorBehaviour(noiseAlert = false, tiltSensitivity = 0.7f, revision = 12L),
        geofence = GeofenceConfig(listOf(HomePoint(32.1, 34.8)), radiusMeters = 150.0, revision = 11L),
        nextSequenceId = 500_000L
    )

    @Test
    fun everyFieldSurvivesTheFile() {
        val bytes = ByteArrayOutputStream().also { writeSensorSettings(file, it) }.toByteArray()
        assertEquals(file, readSensorSettings(ByteArrayInputStream(bytes)))
    }

    @Test
    fun importedAtOnceTheNumberingContinuesAboveTheExportedNumber() {
        assertEquals(500_000L + SEQUENCE_FLOOR_MARGIN, file.sequenceFloor(nowMs = file.exportedAtMs))
    }

    @Test
    fun theFloorCoversEveryRowTheOldSensorCouldHaveSentSinceTheExport() {
        val oneHourLater = file.exportedAtMs + 3_600_000L
        // At most one row per 100 ms: 36,000 numbers in an hour.
        assertEquals(500_000L + 36_000L + SEQUENCE_FLOOR_MARGIN, file.sequenceFloor(oneHourLater))
    }

    @Test
    fun aClockBehindTheExportAddsNothing() {
        assertEquals(500_000L + SEQUENCE_FLOOR_MARGIN, file.sequenceFloor(nowMs = file.exportedAtMs - 60_000L))
    }

    @Test
    fun withoutANumberNoFloorIsSet() {
        assertNull(file.copy(nextSequenceId = null).sequenceFloor(file.exportedAtMs))
        assertNull(file.copy(nextSequenceId = 0L).sequenceFloor(file.exportedAtMs))
    }

    @Test
    fun aMonitorFileOrAFileWithoutFormatIsRefused() {
        for (json in listOf("""{"format":"GPS Monitor settings","formatVersion":1,"trackerId":"T"}""", """{"sensorId":"T"}""")) {
            try {
                readSensorSettings(json.byteInputStream())
                fail("Read: $json")
            } catch (e: SettingsFileException) {
                assertEquals("Not a $SENSOR_SETTINGS_FORMAT file", e.message)
            }
        }
    }
}
