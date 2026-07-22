package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * LocationSentinelHindsightTest: Validating trajectory promotion and jump buffering.
 * July.21.00:
 * - Forensic Alignment: Restored tests for GtoEngine trajectory promotion.
 * - Monotonic Rt: Standardized on mandatory monotonic timing parameters.
 */
class LocationSentinelHindsightTest {

    private lateinit var sentinel: LocationSentinel
    private val baseLat = 32.7940
    private val baseLng = 34.9896
    private val baseAlt = 100.0
    
    // 0.00045 deg lat is approx 50m -> Tier 3 (JITTER) at 1s interval (50m/s)
    private val jitterOffset = 0.00045 

    @Before
    fun setup() {
        sentinel = LocationSentinel()
        // Initialize with a valid starting point
        sentinel.processLocation(
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
        val result1 = sentinel.processLocation(
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
        assertEquals(1, sentinel.getHindsightBuffer().size)

        // 2. Send a consistent point (another 50m in 1s -> 50m/s, consistent with prev)
        val consistentLat = jitterLat + jitterOffset
        val consistentTime = 3000L
        val result2 = sentinel.processLocation(
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

        assertEquals(SentinelStatus.VALID, result2.status)
        assertTrue(result2.reason.contains("Trajectory Promoted"))
        assertEquals(0, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `multi-point jump sequence triggers full promotion`() {
        // P1: Bearing 0
        val p1Lat = baseLat + jitterOffset 
        val p1Time = 2000L
        sentinel.processLocation(p1Lat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, p1Time, nowTs = p1Time, nowRt = p1Time)
        
        // P2: Bearing 0 (Consistent with P1)
        val p2Lat = p1Lat + jitterOffset
        val p2Time = 3000L
        sentinel.processLocation(p2Lat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, p2Time, nowTs = p2Time, nowRt = p2Time)

        // P3: Bearing 0 (Consistent with P2)
        val p3Lat = p2Lat + jitterOffset
        val p3Time = 4000L
        sentinel.processLocation(p3Lat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, p3Time, nowTs = p3Time, nowRt = p3Time)

        // Final point P4 consistent with P3 triggers promotion of the window
        val p4Lat = p3Lat + jitterOffset
        val p4Time = 5000L
        val result = sentinel.processLocation(p4Lat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, p4Time, nowTs = p4Time, nowRt = p4Time)

        assertEquals(SentinelStatus.VALID, result.status)
        assertTrue(result.reason.contains("Trajectory Promoted"))
        assertEquals(0, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `promotion fails if angle exceeds tolerance`() {
        val jumpLat = baseLat + jitterOffset
        val jumpTime = 2000L
        sentinel.processLocation(jumpLat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, jumpTime, nowTs = jumpTime, nowRt = jumpTime)

        // P2 with massive bearing shift relative to P1-vector
        val nextLat = jumpLat
        val nextLng = baseLng + jitterOffset
        val nextTime = 3000L
        val result = sentinel.processLocation(nextLat, nextLng, baseAlt, 10.0, 10.0, 90.0, 40.0, 12, nextTime, nowTs = nextTime, nowRt = nextTime)

        assertEquals(SentinelStatus.JUMP, result.status)
        assertEquals(2, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `hindsight buffer respects maximum size`() {
        sentinel.reset()
        sentinel.processLocation(baseLat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, 1000L, nowTs = 1000L, nowRt = 1000L)
        
        for (i in 1..25) {
            val ts = 1000L + (i * 1000L)
            // Use massive bearing shifts to ensure points are jumps but NEVER promote
            val bearing = (i * 90.0) % 360.0
            sentinel.processLocation(baseLat + (jitterOffset * i), baseLng, baseAlt, 10.0, 10.0, bearing, 40.0, 12, ts, nowTs = ts, nowRt = ts)
        }

        // GtoEngine window limit is 20 in July.21.00
        assertEquals(20, sentinel.getHindsightBuffer().size)
    }
}
