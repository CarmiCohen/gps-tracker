package com.jd.app.sensor

import com.jd.core.shared.LocationUpdate
import org.junit.Assert.assertEquals
import org.junit.Test

class UnknownTelemetryTest {

    @Test
    fun locationUpdateDefaultsToUnknownValues() {
        val update = LocationUpdate()
        assertEquals(-1f, update.speed, 0.001f)
        assertEquals(-1, update.batteryLevel)
        assertEquals(-1f, update.tempCelsius, 0.001f)
    }

    @Test
    fun locationUpdateResetRestoresUnknownValues() {
        val update = LocationUpdate(speed = 10f, batteryLevel = 80, tempCelsius = 35f)
        update.reset()
        assertEquals(-1f, update.speed, 0.001f)
        assertEquals(-1, update.batteryLevel)
        assertEquals(-1f, update.tempCelsius, 0.001f)
    }
}
