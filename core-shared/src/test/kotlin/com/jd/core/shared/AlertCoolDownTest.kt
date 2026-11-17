package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 211 (ISSUE-NOV17.10-O111): the Sensor sends one event alert per kind a minute. */
class AlertCoolDownTest {

    @Test
    fun theSameKindWaitsOneMinute() {
        val coolDown = AlertCoolDown()
        assertTrue(coolDown.tryPass(TILT_ALERT_STATUS, 0))
        assertFalse(coolDown.tryPass(TILT_ALERT_STATUS, ALERT_COOL_DOWN_MS - 1))
        assertTrue(coolDown.tryPass(TILT_ALERT_STATUS, ALERT_COOL_DOWN_MS))
    }

    @Test
    fun aHeldBackAlertDoesNotExtendTheCoolDown() {
        val coolDown = AlertCoolDown()
        assertTrue(coolDown.tryPass(VIBRATION_ALERT_STATUS, 0))
        assertFalse(coolDown.tryPass(VIBRATION_ALERT_STATUS, 50_000))
        assertTrue(coolDown.tryPass(VIBRATION_ALERT_STATUS, 60_000))
    }

    @Test
    fun otherKindsAreNotHeldBack() {
        val coolDown = AlertCoolDown()
        val kinds = listOf(ACOUSTIC_ALERT_STATUS, TILT_ALERT_STATUS, LIFT_ALERT_STATUS, VIBRATION_ALERT_STATUS)
        assertEquals("Each kind alerts once", kinds, kinds.filter { coolDown.tryPass(it, 1_000) })
        assertTrue("No kind twice", kinds.none { coolDown.tryPass(it, 2_000) })
    }
}
