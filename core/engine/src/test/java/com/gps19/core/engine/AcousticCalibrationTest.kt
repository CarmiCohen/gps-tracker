package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AcousticCalibrationTest: Auditing R810-M (Acoustic Floor Recovery).
 * Verifies that the adaptive floor correctly handles saturation and recovery
 * to prevent long-term deafness after loud events.
 */
class AcousticCalibrationTest {

    private lateinit var sentinel: LocationSentinel
    private val INITIAL_TIME = 1000L
    private val MIN_FLOOR = ACOUSTIC_FLOOR_MIN_DB // 50.0

    @Before
    fun setup() {
        sentinel = LocationSentinel()
        sentinel.reset()
    }

    @Test
    fun `acoustic floor initializes to minimum threshold`() {
        sentinel.updateSensorState(
            vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 40.0, 
            nowRt = INITIAL_TIME, nowTs = INITIAL_TIME
        )
        // Should be at least MIN_FLOOR
        assertEquals(MIN_FLOOR, sentinel.acousticFloorDb, 0.001)
    }

    @Test
    fun `acoustic floor climbs during high decibel events`() {
        // Start at 50dB
        sentinel.updateSensorState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 50.0, nowRt = 1000, nowTs = 1000)
        
        // Sustained 90dB saturation
        var currentTime = 1000L
        for (i in 1..10) {
            currentTime += 1000
            sentinel.updateSensorState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 90.0, nowRt = currentTime, nowTs = currentTime)
        }
        
        // Floor should have increased from 50.0
        assertTrue("Floor should climb above $MIN_FLOOR during saturation", sentinel.acousticFloorDb > MIN_FLOOR)
        assertTrue("Floor should stay below peak during climb", sentinel.acousticFloorDb < 90.0)
    }

    @Test
    fun `acoustic floor recovers to baseline after saturation`() {
        // 1. Saturate the floor
        var currentTime = 1000L
        for (i in 1..60) { // 60 seconds of 90dB
            currentTime += 1000
            sentinel.updateSensorState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 90.0, nowRt = currentTime, nowTs = currentTime)
        }
        
        val saturatedFloor = sentinel.acousticFloorDb
        assertTrue(saturatedFloor > 60.0)

        // 2. Return to silence (40dB) and verify recovery
        // We simulate a long period to see it return to MIN_FLOOR
        for (i in 1..600) { // 10 minutes of silence
            currentTime += 1000
            sentinel.updateSensorState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 40.0, nowRt = currentTime, nowTs = currentTime)
        }

        assertTrue("Floor should recover downwards", sentinel.acousticFloorDb < saturatedFloor)
        assertEquals("Floor should eventually return to MIN_FLOOR", MIN_FLOOR, sentinel.acousticFloorDb, 0.5)
    }

    @Test
    fun `acoustic floor contraction logic is independent of sampling updates`() {
        // Saturate
        sentinel.updateSensorState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 90.0, nowRt = 1000, nowTs = 1000)
        sentinel.updateSensorState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 90.0, nowRt = 5000, nowTs = 5000)
        
        val floorAtStart = sentinel.acousticFloorDb
        
        // Pass time without updateSensorState calls (e.g. duty cycle off)
        // Then call again - contraction should have applied based on time delta
        sentinel.updateSensorState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 40.0, nowRt = 60000, nowTs = 60000)
        
        assertTrue("Floor should have contracted significantly over 55s", sentinel.acousticFloorDb < floorAtStart)
    }
}
