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
        val interval1 = SensingEngine.getOptimalIntervalMs(46f, false, 100, true)
        assertEquals(10000L, interval1) // 0.1Hz

        // 2. Drop to 44C (Inside Hysteresis: 45C - 2C = 43C)
        // Should stay at 10000L
        val interval2 = SensingEngine.getOptimalIntervalMs(44f, false, 100, true)
        assertEquals("Hysteresis failed to hold Critical status at 44C", 10000L, interval2)

        // 3. Drop to 42.5C (Below Hysteresis for Critical): back to the moving rate of one row a second
        val interval3 = SensingEngine.getOptimalIntervalMs(42.5f, false, 100, true)
        assertEquals(1000L, interval3)
    }

    /** The 40C warning stage capped the row clock at 1 s; since Nov22.8 that is the fastest rate anyway (ISSUE-NOV22.5-O133). */
    @Test
    fun testThermalWarningHysteresis() {
        for (temp in listOf(41f, 39f, 37.5f)) {
            assertEquals("A warm moving Sensor at $temp C", 1000L, SensingEngine.getOptimalIntervalMs(temp, false, 100, true))
        }
    }
}
