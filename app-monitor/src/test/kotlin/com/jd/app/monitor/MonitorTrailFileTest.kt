package com.jd.app.monitor

import com.jd.core.shared.SETTINGS_FILE_VERSION
import com.jd.core.shared.SettingsFileException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Chapter 221 (ISSUE-NOV17.7-O107): the map's SAVE writes every trail row to a file that LOAD reads back and that no
 * other file passes for; a CLEAR keeps the Sensor's older rows away, live or by backfill, without losing newer ones.
 */
class MonitorTrailFileTest {

    private val file = MonitorTrailFile(
        exportedAtMs = 1_763_400_000_000L,
        appVersion = "Nov18.5",
        monitorId = "V",
        trackerId = "T",
        trails = listOf(
            TrailEntity("T", 1_000L, 7L, 32.1, 34.8, 5f, "VALID"),
            TrailEntity("V", 2_000L, 2_000L, 32.2, 34.9, 9f, "VALID", isLocalUser = true)
        )
    )

    private fun roundTrip(original: MonitorTrailFile): MonitorTrailFile {
        val bytes = ByteArrayOutputStream().also { writeMonitorTrails(original, it) }.toByteArray()
        return readMonitorTrails(ByteArrayInputStream(bytes))
    }

    @Test
    fun everyRowSurvivesTheFile() {
        assertEquals(file, roundTrip(file))
    }

    @Test
    fun anyOtherFileIsRefusedWithAMessage() {
        assertRefused("""{"format":"$MONITOR_SETTINGS_FORMAT","formatVersion":1,"trackerId":"T"}""", "Not a $MONITOR_TRAIL_FORMAT file")
        assertRefused("not json at all", "Not a $MONITOR_TRAIL_FORMAT file")
        assertRefused("""{"trails":[]}""", "Not a $MONITOR_TRAIL_FORMAT file")
        assertRefused(
            """{"format":"$MONITOR_TRAIL_FORMAT","formatVersion":${SETTINGS_FILE_VERSION + 1}}""",
            "The file is from a newer app version; update the app first"
        )
    }

    @Test
    fun theSettingsFileIsNoTrailFileNorTheOtherWayRound() {
        val bytes = ByteArrayOutputStream().also { writeMonitorTrails(file, it) }.toByteArray()
        try {
            readMonitorSettings(ByteArrayInputStream(bytes))
            fail("A trail file was read as settings")
        } catch (e: SettingsFileException) {
            assertEquals("Not a $MONITOR_SETTINGS_FORMAT file", e.message)
        }
    }

    @Test
    fun theSuggestedNameCarriesTheTime() {
        val name = trailFileName(0L)
        assertTrue(name, Regex("""GPS-Monitor-trails-\d{8}-\d{4}\.json""").matches(name))
    }

    @Test
    fun aClearKeepsOnlyTheClearedTrackersOlderRowsAway() {
        val clear = TrailClear("T", atMs = 5_000L)
        assertFalse("An older row of the cleared tracker", clear.keeps(TrailEntity("T", 4_999L, 40L, 1.0, 1.0, 5f, "VALID")))
        assertTrue("A row from the clear on", clear.keeps(TrailEntity("T", 5_000L, 41L, 1.0, 1.0, 5f, "VALID")))
        assertTrue("Another tracker's row", clear.keeps(TrailEntity("U", 1_000L, 3L, 1.0, 1.0, 5f, "VALID")))
        assertTrue("No clear keeps every row", (null as TrailClear?).keeps(TrailEntity("T", 1L, 1L, 1.0, 1.0, 5f, "VALID")))
    }

    @Test
    fun aClearedStartIsNoGap() {
        val clear = TrailClear("T", atMs = 5_000L)
        assertTrue(clear.clearedStartOf("T"))
        assertFalse("Another tracker still gets its whole history", clear.clearedStartOf("U"))
        assertFalse("No clear, no cleared start", (null as TrailClear?).clearedStartOf("T"))
    }

    private fun assertRefused(json: String, message: String) {
        try {
            readMonitorTrails(json.byteInputStream())
            fail("Read: $json")
        } catch (e: SettingsFileException) {
            assertEquals(message, e.message)
        }
    }
}
