package com.jd.app.monitor

import com.jd.core.shared.ACOUSTIC_ALERT_STATUS
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.LIGHT_ALERT_STATUS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 237 (ISSUE-NOV22.4-O130): a light rise the Sensor reports raises the Monitor's `Tracker light detected`
 * alarm, switched by `Ambient Light Sensor` (`AlertSettings.lightAlert`), like the noise alarm of chapter 161.
 */
class LightAlarmTest {

    private fun reading(status: String) = TrackerReading(1_000L, 1_000L, 80, true, 30f, 32.0, 34.0, 5f, status)

    private fun conditions(status: String? = null, violationType: String? = null) = alarmConditions(
        reading = reading(status ?: "VALID"),
        watchingSinceMs = 0L,
        nowMs = 1_000L,
        active = emptySet(),
        hasViolation = violationType != null,
        violationType = violationType
    )

    @Test
    fun `a light row or violation raises the light alarm, not a GPS jump`() {
        assertEquals(setOf(AlarmType.LIGHT_ALERT), conditions(status = LIGHT_ALERT_STATUS))
        assertEquals(setOf(AlarmType.LIGHT_ALERT), conditions(violationType = LIGHT_ALERT_STATUS))
        assertEquals("Tracker light detected", AlarmType.LIGHT_ALERT.title)
        assertTrue(AlarmType.LIGHT_ALERT.isEvent)
    }

    @Test
    fun `the light alarm stays until acknowledged after its row has passed`() {
        val settings = AlertSettings()
        val raised = AlarmBook().update(setOf(AlarmType.LIGHT_ALERT), settings, 1_000L)
        val passed = raised.update(emptySet(), settings, 2_000L)
        assertTrue(passed.needsSiren)
        assertFalse(passed.acknowledge().update(emptySet(), settings, 3_000L).needsSiren)
    }

    @Test
    fun `the light switch allows only the light alarm`() {
        val off = AlertSettings(lightAlert = false)
        assertFalse(off.allows(AlarmType.LIGHT_ALERT))
        assertTrue(off.allows(AlarmType.ACOUSTIC_ALERT))
        assertFalse(AlertSettings(acousticAlert = false).allows(AlarmType.ACOUSTIC_ALERT))
        assertTrue(AlertSettings(acousticAlert = false).allows(AlarmType.LIGHT_ALERT))
        assertFalse(AlertSettings(globalMute = true).allows(AlarmType.LIGHT_ALERT))
        assertTrue(AlarmBook().update(setOf(AlarmType.LIGHT_ALERT), off, 1_000L).alarms.isEmpty())
    }

    @Test
    fun `light and noise together raise both alarms`() {
        assertEquals(
            setOf(AlarmType.LIGHT_ALERT, AlarmType.ACOUSTIC_ALERT),
            conditions(status = LIGHT_ALERT_STATUS, violationType = ACOUSTIC_ALERT_STATUS)
        )
    }
}
