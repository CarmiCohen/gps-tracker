package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
import com.jd.core.shared.SETTINGS_FILE_VERSION
import com.jd.core.shared.SettingsFileException
import com.jd.core.shared.importedOver
import com.jd.core.shared.settingsFileName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Chapter 194.1 (ISSUE-NOV10.8-O80): the Monitor's settings file keeps the pairing, the settings, the Home Points and
 * the trails across a reinstall, refuses any other file, and its settings reach the Sensor as new revisions.
 */
class MonitorSettingsFileTest {

    private val file = MonitorSettingsFile(
        exportedAtMs = 1_763_300_000_000L,
        appVersion = "Nov16.1",
        monitorId = "V",
        trackerId = "T",
        relayUrl = "wss://relay.example",
        dndBypass = true,
        alertSettings = AlertSettings(tiltAlert = false, alarmVolume = 0.5f, sensorRevision = 40L),
        sirenType = "Pulse",
        geofence = GeofenceConfig(listOf(HomePoint(32.1, 34.8), HomePoint(32.2, 34.9)), radiusMeters = 250.0, revision = 30L),
        trails = listOf(
            TrailEntity("T", 1_000L, 7L, 32.1, 34.8, 5f, "VALID"),
            TrailEntity("V", 2_000L, 0L, 32.2, 34.9, 9f, "VALID", isLocalUser = true)
        )
    )

    private fun roundTrip(original: MonitorSettingsFile): MonitorSettingsFile {
        val bytes = ByteArrayOutputStream().also { writeMonitorSettings(original, it) }.toByteArray()
        return readMonitorSettings(ByteArrayInputStream(bytes))
    }

    @Test
    fun everyFieldSurvivesTheFile() {
        assertEquals(file, roundTrip(file))
    }

    @Test
    fun anUnpairedMonitorWithTheDefaultRelaySurvivesToo() {
        val unpaired = MonitorSettingsFile(exportedAtMs = 1L, trackerId = null, relayUrl = null)
        assertEquals(unpaired, roundTrip(unpaired))
    }

    @Test
    fun theFileNamesItsFormatAndVersion() {
        val text = ByteArrayOutputStream().also { writeMonitorSettings(file, it) }.toString(Charsets.UTF_8.name())
        assertTrue(text.contains("\"format\":\"$MONITOR_SETTINGS_FORMAT\""))
        assertTrue(text.contains("\"formatVersion\":$SETTINGS_FILE_VERSION"))
    }

    @Test
    fun aLaterVersionThatOnlyAddsFieldsIsRead() {
        val json = """{"format":"$MONITOR_SETTINGS_FORMAT","formatVersion":1,"trackerId":"T","someNewField":3}"""
        assertEquals("T", readMonitorSettings(json.byteInputStream()).trackerId)
    }

    @Test
    fun anyOtherFileIsRefusedWithAMessage() {
        assertRefused("""{"format":"GPS Sensor settings","formatVersion":1,"sensorId":"T"}""", "Not a $MONITOR_SETTINGS_FORMAT file")
        assertRefused("not json at all", "Not a $MONITOR_SETTINGS_FORMAT file")
        assertRefused("""{"trackerId":"T"}""", "Not a $MONITOR_SETTINGS_FORMAT file")
        assertRefused(
            """{"format":"$MONITOR_SETTINGS_FORMAT","formatVersion":${SETTINGS_FILE_VERSION + 1}}""",
            "The file is from a newer app version; update the app first"
        )
    }

    @Test
    fun importedSettingsAreNewerThanEveryCopy() {
        val now = 20L // A clock behind both revisions.
        assertEquals("Newer than this phone's", 91L, file.alertSettings.importedOver(AlertSettings(sensorRevision = 90L), now).sensorRevision)
        assertEquals("Newer than the file's", 41L, file.alertSettings.importedOver(AlertSettings(sensorRevision = 5L), now).sensorRevision)
        val fence = file.geofence.importedOver(GeofenceConfig(revision = 95L), now)
        assertEquals(96L, fence.revision)
        assertEquals(31L, file.geofence.importedOver(GeofenceConfig(revision = 2L), now).revision)
        assertEquals("The file's values are kept", file.geofence.points, fence.points)
        // A fresh install holds revision 0: the import gets the time, as an edit does.
        assertEquals(1_000L, file.geofence.importedOver(GeofenceConfig(), nowMs = 1_000L).revision)
    }

    @Test
    fun theSuggestedNameCarriesAppAndTime() {
        val name = settingsFileName("Monitor", 0L)
        assertTrue(name, Regex("""GPS-Monitor-settings-\d{8}-\d{4}\.json""").matches(name))
    }

    private fun assertRefused(json: String, message: String) {
        try {
            readMonitorSettings(json.byteInputStream())
            fail("Read: $json")
        } catch (e: SettingsFileException) {
            assertEquals(message, e.message)
        }
    }
}
