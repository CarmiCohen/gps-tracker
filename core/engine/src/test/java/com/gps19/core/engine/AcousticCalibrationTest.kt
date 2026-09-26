package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AcousticCalibrationTest: Auditing R810-M (Acoustic Floor Recovery).
 * Sep.26.3:
 * - Issue #1334: Adapted to LocationProcessingState and SystemEvaluationSnapshot API.
 */
class AcousticCalibrationTest {

    private val state = LocationProcessingState()
    private val INITIAL_TIME = 1000L
    private val MIN_FLOOR = ACOUSTIC_FLOOR_MIN_DB // 50.0

    @Before
    fun setup() {
        LocationSentinel.reset(state)
    }

    @Test
    fun `acoustic floor initializes to minimum threshold`() {
        val snap = SystemEvaluationSnapshot(
            atmospheric = AtmosphericState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 40.0),
            nowRt = INITIAL_TIME,
            nowTs = INITIAL_TIME
        )
        LocationSentinel.updateSensorState(state, snap)
        // Should be at least MIN_FLOOR
        assertEquals(MIN_FLOOR, state.acousticFloorDb, 0.001)
    }

    @Test
    fun `acoustic floor climbs during high decibel events`() {
        // Start at 50dB
        val snapStart = SystemEvaluationSnapshot(
            atmospheric = AtmosphericState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 50.0),
            nowRt = 1000,
            nowTs = 1000
        )
        LocationSentinel.updateSensorState(state, snapStart)
        
        // Sustained 90dB saturation
        var currentTime = 1000L
        for (i in 1..10) {
            currentTime += 1000
            val snap = SystemEvaluationSnapshot(
                atmospheric = AtmosphericState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 90.0),
                nowRt = currentTime,
                nowTs = currentTime
            )
            LocationSentinel.updateSensorState(state, snap)
        }
        
        // Floor should have increased from 50.0
        assertTrue("Floor should climb above $MIN_FLOOR during saturation", state.acousticFloorDb > MIN_FLOOR)
        assertTrue("Floor should stay below peak during climb", state.acousticFloorDb < 90.0)
    }

    @Test
    fun `acoustic floor recovers to baseline after saturation`() {
        // 1. Saturate the floor
        var currentTime = 1000L
        for (i in 1..60) { // 60 seconds of 90dB
            currentTime += 1000
            val snap = SystemEvaluationSnapshot(
                atmospheric = AtmosphericState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 90.0),
                nowRt = currentTime,
                nowTs = currentTime
            )
            LocationSentinel.updateSensorState(state, snap)
        }
        
        val saturatedFloor = state.acousticFloorDb
        assertTrue(saturatedFloor > 60.0)

        // 2. Return to silence (40dB) and verify recovery
        // We simulate a long period to see it return to MIN_FLOOR
        for (i in 1..600) { // 10 minutes of silence
            currentTime += 1000
            val snap = SystemEvaluationSnapshot(
                atmospheric = AtmosphericState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 40.0),
                nowRt = currentTime,
                nowTs = currentTime
            )
            LocationSentinel.updateSensorState(state, snap)
        }

        assertTrue("Floor should recover downwards", state.acousticFloorDb < saturatedFloor)
        assertEquals("Floor should eventually return to MIN_FLOOR", MIN_FLOOR, state.acousticFloorDb, 0.5)
    }

    @Test
    fun `acoustic floor contraction logic is independent of sampling updates`() {
        // Saturate
        val snap1 = SystemEvaluationSnapshot(
            atmospheric = AtmosphericState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 90.0),
            nowRt = 1000,
            nowTs = 1000
        )
        LocationSentinel.updateSensorState(state, snap1)
        
        val snap2 = SystemEvaluationSnapshot(
            atmospheric = AtmosphericState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 90.0),
            nowRt = 5000,
            nowTs = 5000
        )
        LocationSentinel.updateSensorState(state, snap2)
        
        val floorAtStart = state.acousticFloorDb
        
        // Pass time without updateSensorState calls (e.g. duty cycle off)
        // Then call again - contraction should have applied based on time delta
        val snap3 = SystemEvaluationSnapshot(
            atmospheric = AtmosphericState(vibration = 0.0, heading = 0.0, baroAlt = 100.0, acousticDb = 40.0),
            nowRt = 60000,
            nowTs = 60000
        )
        LocationSentinel.updateSensorState(state, snap3)
        
        assertTrue("Floor should have contracted significantly over 55s", state.acousticFloorDb < floorAtStart)
    }
}
