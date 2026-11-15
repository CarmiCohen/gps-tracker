package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.SensorFault
import com.jd.core.shared.SensorSetupItem
import com.jd.core.shared.SensorSetupReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 189 (ISSUE-NOV10.8-O83 part 2, ISSUE-NOV15.2-O97): the Monitor warns about what the Sensor phone lacks, as
 * the Sensor reports it, and shows whether the Sensor applies this Monitor's detection settings.
 */
class SensorSetupWarningsTest {

    private val settings = AlertSettings(sensorRevision = 10L)

    @Test
    fun `no report and a complete report give no warning`() {
        assertTrue(sensorSetupWarnings(null, settings).isEmpty())
        assertTrue(sensorSetupWarnings(SensorSetupReport(trackerId = "T"), settings).isEmpty())
    }

    @Test
    fun `each missing item and each fault is one line, items first`() {
        val report = SensorSetupReport(
            trackerId = "T",
            missing = listOf(SensorSetupItem.BACKGROUND_LOCATION, SensorSetupItem.BATTERY_UNRESTRICTED),
            faults = listOf(SensorFault.LOCATION_NEEDS_TAP)
        )
        assertEquals(
            listOf(
                "Location 'Allow all the time': missing",
                "Battery 'Unrestricted': missing",
                "Location is off since the Sensor phone restarted: tap its notification or open the Sensor app"
            ),
            sensorSetupWarnings(report, settings)
        )
    }

    @Test
    fun `a microphone problem is a warning only while noise detection is switched on`() {
        val report = SensorSetupReport(
            trackerId = "T",
            missing = listOf(SensorSetupItem.MICROPHONE, SensorSetupItem.NOTIFICATIONS),
            faults = listOf(SensorFault.MICROPHONE_SILENCED, SensorFault.LOCATION_NEEDS_TAP)
        )
        assertEquals(4, sensorSetupWarnings(report, settings).size)
        assertEquals(
            listOf("Notifications: missing", SensorFault.LOCATION_NEEDS_TAP.text),
            sensorSetupWarnings(report, settings.copy(acousticAlert = false))
        )
        SensorFault.entries.filter { it.isMicrophone }.forEach { fault ->
            assertTrue("$fault", sensorSetupWarnings(SensorSetupReport(faults = listOf(fault)), settings.copy(acousticAlert = false)).isEmpty())
        }
    }

    @Test
    fun `the warning line counts the items`() {
        assertEquals("SENSOR PHONE: 1 ITEM NEEDS ATTENTION", sensorSetupLineText(1))
        assertEquals("SENSOR PHONE: 3 ITEMS NEED ATTENTION", sensorSetupLineText(3))
    }

    @Test
    fun `the setup line of the info dialog`() {
        assertEquals("Sensor phone setup: not reported yet", sensorSetupStatusText(null, 0))
        assertEquals("Sensor phone setup: complete", sensorSetupStatusText(SensorSetupReport(trackerId = "T"), 0))
        assertEquals("Sensor phone setup: 2 to fix", sensorSetupStatusText(SensorSetupReport(trackerId = "T"), 2))
    }

    @Test
    fun `the detection settings line tells whether the Sensor applies this Monitor's revision`() {
        assertEquals("Sensor detection settings: not reported yet", sensorBehaviourStatusText(null, 10L))
        assertEquals("Sensor detection settings: applied", sensorBehaviourStatusText(SensorSetupReport(behaviourRevision = 10L), 10L))
        assertEquals("Sensor detection settings: not applied yet", sensorBehaviourStatusText(SensorSetupReport(behaviourRevision = 9L), 10L))
        assertEquals(
            "Sensor detection settings: newer ones from another Monitor are in use",
            sensorBehaviourStatusText(SensorSetupReport(behaviourRevision = 11L), 10L)
        )
    }
}
