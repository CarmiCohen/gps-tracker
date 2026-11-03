package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.SensingEngine
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ThermalHysteresisTest: Verifies boundary stability for thermal throttling (R133).
 * Ensures that once a throttling state is entered, it persists until the temperature 
 * drops below the hysteresis threshold (2.0C).
 */
@RunWith(AndroidJUnit4::class)
class ThermalHysteresisTest {

    @Before
    fun setup() {
        SensingEngine.resetThermalState()
    }

    @Test
    fun testThermalCriticalHysteresis() {
        // 1. Nominal -> Critical (46C)
        val interval1 = SensingEngine.getOptimalIntervalMs(46f, false, 100, 10f)
        assertEquals(10000L, interval1) // 0.1Hz

        // 2. Drop to 44C (Inside Hysteresis: 45C - 2C = 43C)
        // Should stay at 10000L
        val interval2 = SensingEngine.getOptimalIntervalMs(44f, false, 100, 10f)
        assertEquals("Hysteresis failed to hold Critical status at 44C", 10000L, interval2)

        // 3. Drop to 42.5C (Below Hysteresis for Critical, but Warning is 40C)
        // Should drop to 1000L (Warning level)
        val interval3 = SensingEngine.getOptimalIntervalMs(42.5f, false, 100, 10f)
        assertEquals(1000L, interval3)
    }

    @Test
    fun testThermalWarningHysteresis() {
        // 1. Nominal -> Warning (41C)
        val interval1 = SensingEngine.getOptimalIntervalMs(41f, false, 100, 10f)
        assertEquals(1000L, interval1) // 1Hz

        // 2. Drop to 39C (Inside Hysteresis: 40C - 2C = 38C)
        // Should stay at 1000L
        val interval2 = SensingEngine.getOptimalIntervalMs(39f, false, 100, 10f)
        assertEquals("Hysteresis failed to hold Warning status at 39C", 1000L, interval2)

        // 3. Drop to 37.5C (Below Hysteresis for Warning)
        // Should drop to 100L (Nominal)
        val interval3 = SensingEngine.getOptimalIntervalMs(37.5f, false, 100, 10f)
        assertEquals(100L, interval3)
    }
}
