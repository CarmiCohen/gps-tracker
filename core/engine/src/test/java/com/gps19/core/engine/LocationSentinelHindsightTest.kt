package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * LocationSentinelHindsightTest: Validating trajectory promotion and jump buffering.
 * Sep.26.3:
 * - Issue #1334: Adapted to SystemEvaluationSnapshot API and LocationProcessingState state architecture.
 */
class LocationSentinelHindsightTest {

    private val state = LocationProcessingState()
    private val baseLat = 32.7940
    private val baseLng = 34.9896
    private val baseAlt = 100.0
    
    // 0.00045 deg lat is approx 50m -> Tier 3 (JITTER) at 1s interval (50m/s)
    private val jitterOffset = 0.00045 

    @Before
    fun setup() {
        LocationSentinel.reset(state)
        // Initialize with a valid starting point
        LocationSentinel.processLocation(
            state = state,
            lat = baseLat,
            lng = baseLng,
            alt = baseAlt,
            accuracy = 10.0,
            maxAccuracy = 10.0,
            bearing = 0.0,
            snr = 40.0,
            satsUsed = 12,
            timestamp = 1000L,
            nowTs = 1000L,
            nowRt = 1000L
        )
    }

    @Test
    fun `single point jump followed by consistent point triggers promotion`() {
        // 1. Send a "jitter" point (50m away in 1 second -> 50m/s, Tier 3)
        val jitterLat = baseLat + jitterOffset
        val jumpTime = 2000L
        val result1 = LocationSentinel.processLocation(
            state = state,
            lat = jitterLat,
            lng = baseLng,
            alt = baseAlt,
            accuracy = 10.0,
            maxAccuracy = 10.0,
            bearing = 0.0,
            snr = 40.0,
            satsUsed = 12,
            timestamp = jumpTime,
            nowTs = jumpTime,
            nowRt = jumpTime
        )

        assertEquals(SentinelStatus.JUMP, result1.status)
        assertEquals(1, LocationSentinel.getHindsightBuffer(state).size)

        // 2. Send a consistent point (consistent with prev)
        val consistentLat = jitterLat + jitterOffset
        val consistentTime = 3000L
        val result2 = LocationSentinel.processLocation(
            state = state,
            lat = consistentLat,
            lng = baseLng,
            alt = baseAlt,
            accuracy = 10.0,
            maxAccuracy = 10.0,
            bearing = 0.0,
            snr = 40.0,
            satsUsed = 12,
            timestamp = consistentTime,
            nowTs = consistentTime,
            nowRt = consistentTime
        )

        assertEquals(SentinelStatus.TRAJECTORY_PROMOTED, result2.status)
        assertTrue(result2.reason.contains("Trajectory Promoted"))
        assertEquals(0, LocationSentinel.getHindsightBuffer(state).size)
    }

    @Test
    fun `multi-point jump sequence triggers full promotion`() {
        // P1
        val p1Lat = baseLat + jitterOffset 
        val p1Time = 2000L
        LocationSentinel.processLocation(state, p1Lat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, p1Time, nowTs = p1Time, nowRt = p1Time)
        
        // P2
        val p2Lat = p1Lat + jitterOffset
        val p2Time = 3000L
        LocationSentinel.processLocation(state, p2Lat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, p2Time, nowTs = p2Time, nowRt = p2Time)

        // P3
        val p3Lat = p2Lat + jitterOffset
        val p3Time = 4000L
        LocationSentinel.processLocation(state, p3Lat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, p3Time, nowTs = p3Time, nowRt = p3Time)

        // Final point P4 consistent with P3 triggers promotion
        val p4Lat = p3Lat + jitterOffset
        val p4Time = 5000L
        val result = LocationSentinel.processLocation(state, p4Lat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, p4Time, nowTs = p4Time, nowRt = p4Time)

        assertEquals(SentinelStatus.TRAJECTORY_PROMOTED, result.status)
        assertTrue(result.reason.contains("Trajectory Promoted"))
        assertEquals(0, LocationSentinel.getHindsightBuffer(state).size)
    }

    @Test
    fun `hindsight buffer respects maximum size`() {
        LocationSentinel.reset(state)
        LocationSentinel.processLocation(state, baseLat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, 1000L, nowTs = 1000L, nowRt = 1000L)
        
        for (i in 1..25) {
            val ts = 1000L + (i * 1000L)
            val bearing = (i * 90.0) % 360.0
            LocationSentinel.processLocation(state, baseLat + (jitterOffset * i), baseLng, baseAlt, 10.0, 10.0, bearing, 40.0, 12, ts, nowTs = ts, nowRt = ts)
        }

        assertEquals(5, LocationSentinel.getHindsightBuffer(state).size)
    }
}
