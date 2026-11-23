package com.jd.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SIMP-NOV8.103-S1: a sensing tick sends one alarm, the oldest waiting; no alarm found on the same tick is lost. */
class PendingAlarmsTest {

    private val breach = PendingAlarms.Alarm("GEOFENCE_BREACH", "Geofence breach")
    private val movement = PendingAlarms.Alarm("JUMP", "Violation detected: JUMP")
    private val noise = PendingAlarms.Alarm("ACOUSTIC_ALERT", "Noise alert")

    @Test
    fun aTickWithNothingFoundAndNothingWaitingSendsNoAlarm() {
        val alarms = PendingAlarms()
        assertNull(alarms.takeForTick(null, null))
        assertFalse(alarms.isNotEmpty())
    }

    @Test
    fun aBreachAndAMovementVerdictOnOneTickAreBothSent() {
        val alarms = PendingAlarms()
        assertEquals(breach, alarms.takeForTick(breach, movement))
        assertTrue("The movement verdict waits for the next tick", alarms.isNotEmpty())
        assertEquals(movement, alarms.takeForTick(null, null))
        assertNull(alarms.takeForTick(null, null))
    }

    @Test
    fun aSensorAlertQueuedBeforeTheTickGoesFirst() {
        val alarms = PendingAlarms()
        alarms.add(noise)
        assertEquals(noise, alarms.takeForTick(breach, null))
        assertEquals(breach, alarms.takeForTick(null, null))
        assertFalse(alarms.isNotEmpty())
    }

    @Test
    fun aMovementVerdictAloneIsSentOnItsOwnTick() {
        val alarms = PendingAlarms()
        assertEquals(movement, alarms.takeForTick(null, movement))
        assertFalse(alarms.isNotEmpty())
    }
}
