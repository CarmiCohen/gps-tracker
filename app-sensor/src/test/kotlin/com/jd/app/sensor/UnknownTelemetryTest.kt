package com.jd.app.sensor

import com.jd.core.shared.LocationUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Battery and temperature not known are null since Nov23.1, as -1 is also a real temperature (SIMP-NOV17.7-S1, chapter 249). */
class UnknownTelemetryTest {

    @Test
    fun locationUpdateDefaultsToUnknownValues() {
        val update = LocationUpdate()
        assertEquals(-1f, update.speed, 0.001f)
        assertNull(update.batteryLevel)
        assertNull(update.tempCelsius)
    }

    @Test
    fun locationUpdateResetRestoresUnknownValues() {
        val update = LocationUpdate(speed = 10f, batteryLevel = 80, tempCelsius = 35f)
        update.reset()
        assertEquals(-1f, update.speed, 0.001f)
        assertNull(update.batteryLevel)
        assertNull(update.tempCelsius)
    }
}
