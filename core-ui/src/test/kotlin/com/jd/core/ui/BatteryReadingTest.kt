package com.jd.core.ui

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies Chapter 122.2: battery values are real or absent, never invented. */
class BatteryReadingTest {

    @Test
    fun convertsSystemValues() {
        val reading = batteryReading(
            level = 37, scale = 50, status = BatteryManager.BATTERY_STATUS_DISCHARGING, tempTenths = 287
        )

        assertEquals(74, reading.level)
        assertFalse(reading.isCharging)
        assertEquals(28.7f, reading.tempCelsius!!, 0.001f)
    }

    @Test
    fun chargingAndFullBothCountAsCharging() {
        assertTrue(batteryReading(50, 100, BatteryManager.BATTERY_STATUS_CHARGING, 300).isCharging)
        assertTrue(batteryReading(100, 100, BatteryManager.BATTERY_STATUS_FULL, 300).isCharging)
    }

    @Test
    fun missingLevelIsNotInvented() {
        assertNull(batteryReading(level = -1, scale = 100, status = -1, tempTenths = 300).level)
        assertNull(batteryReading(level = 50, scale = -1, status = -1, tempTenths = 300).level)
        assertNull(batteryReading(level = 50, scale = 0, status = -1, tempTenths = 300).level)
    }

    @Test
    fun missingTemperatureIsNotInvented() {
        assertNull(batteryReading(50, 100, -1, tempTenths = Int.MIN_VALUE).tempCelsius)
    }

    @Test
    fun belowZeroTemperatureIsKept() {
        assertEquals(-5f, batteryReading(50, 100, -1, tempTenths = -50).tempCelsius!!, 0.001f)
    }
}
