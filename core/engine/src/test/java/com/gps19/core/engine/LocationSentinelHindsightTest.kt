package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationSentinelHindsightTest {

    private lateinit var sentinel: LocationSentinel
    private val baseLat = 32.7940
    private val baseLng = 34.9896
    private val baseAlt = 100.0
    
    // 0.00045 deg lat is approx 50m -> Tier 3 (JITTER) at 1s interval (50m/s)
    private val jitterOffset = 0.00045 
    // 0.00009 deg lat is approx 10m
    private val normalOffset = 0.00009

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
            nowWall = 1000L,
            nowRealtime = 1000L
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
            nowWall = jumpTime,
            nowRealtime = jumpTime
        )

        assertEquals(SentinelStatus.JITTER, result1.status)
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
            nowWall = consistentTime,
            nowRealtime = consistentTime
        )

        assertEquals(SentinelStatus.TRAJECTORY_PROMOTED, result2.status)
        assertEquals(1, result2.promotedPoints?.size)
        assertEquals(jitterLat, result2.promotedPoints!![0].lat, 0.001)
        assertEquals(0, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `multi-point jump sequence triggers full promotion`() {
        // Use inconsistent bearings to prevent early one-by-one promotion
        
        // P1: Bearing 0
        val p1Lat = baseLat + jitterOffset 
        val p1Time = 2000L
        sentinel.processLocation(p1Lat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, p1Time, nowWall = p1Time, nowRealtime = p1Time)
        
        // P2: Bearing 90 (Inconsistent with P1)
        val p2Lat = p1Lat + jitterOffset
        val p2Lng = baseLng + jitterOffset
        val p2Time = 3000L
        sentinel.processLocation(p2Lat, p2Lng, baseAlt, 10.0, 10.0, 90.0, 40.0, 12, p2Time, nowWall = p2Time, nowRealtime = p2Time)

        // P3: Bearing 180 (Inconsistent with P2)
        val p3Lat = p2Lat + jitterOffset
        val p3Lng = p2Lng + jitterOffset
        val p3Time = 4000L
        sentinel.processLocation(p3Lat, p3Lng, baseAlt, 10.0, 10.0, 180.0, 40.0, 12, p3Time, nowWall = p3Time, nowRealtime = p3Time)

        assertEquals(3, sentinel.getHindsightBuffer().size)

        // Final point P4 consistent with P3
        val p4Lat = p3Lat + jitterOffset
        val p4Time = 5000L
        val result = sentinel.processLocation(p4Lat, p3Lng, baseAlt, 10.0, 10.0, 180.0, 40.0, 12, p4Time, nowWall = p4Time, nowRealtime = p4Time)

        assertEquals(SentinelStatus.TRAJECTORY_PROMOTED, result.status)
        assertEquals(3, result.promotedPoints?.size)
        assertEquals(0, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `promotion fails if angle exceeds tolerance`() {
        val jumpLat = baseLat + jitterOffset
        val jumpTime = 2000L
        sentinel.processLocation(jumpLat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, jumpTime, nowWall = jumpTime, nowRealtime = jumpTime)

        val nextLat = jumpLat
        val nextLng = baseLng + jitterOffset
        val nextTime = 3000L
        val result = sentinel.processLocation(nextLat, nextLng, baseAlt, 10.0, 10.0, 90.0, 40.0, 12, nextTime, nowWall = nextTime, nowRealtime = nextTime)

        assertTrue(result.status == SentinelStatus.JUMP || result.status == SentinelStatus.JITTER)
        assertEquals(2, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `promotion fails if speed delta exceeds tolerance`() {
        val jumpLat = baseLat + jitterOffset
        val jumpTime = 2000L 
        sentinel.processLocation(jumpLat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, jumpTime, nowWall = jumpTime, nowRealtime = jumpTime)

        val nextLat = jumpLat + normalOffset
        val nextTime = 3000L
        val result = sentinel.processLocation(nextLat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, nextTime, nowWall = nextTime, nowRealtime = nextTime)

        assertTrue(result.status == SentinelStatus.JUMP || result.status == SentinelStatus.JITTER)
        assertEquals(2, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `hindsight buffer prunes points older than max age`() {
        // 1. Add a jitter point
        sentinel.processLocation(baseLat + jitterOffset, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, 2000L, nowWall = 2000L, nowRealtime = 2000L)
        assertEquals(1, sentinel.getHindsightBuffer().size)

        // 2. Send ANOTHER jitter point much later with a bearing shift to prevent promotion.
        val lateTime = 33000L 
        val lateOffset = 0.0135 // Keeps speed at ~50m/s
        sentinel.processLocation(baseLat + lateOffset, baseLng, baseAlt, 10.0, 10.0, 180.0, 40.0, 12, lateTime, nowWall = lateTime, nowRealtime = lateTime)
        
        // The first point should have been pruned.
        assertEquals(1, sentinel.getHindsightBuffer().size)
        assertEquals(lateTime, sentinel.getHindsightBuffer()[0].ts)
    }

    @Test
    fun `hindsight buffer respects maximum size`() {
        sentinel.reset()
        sentinel.processLocation(baseLat, baseLng, baseAlt, 10.0, 10.0, 0.0, 40.0, 12, 1000L, nowWall = 1000L, nowRealtime = 1000L)
        
        for (i in 1..15) {
            val ts = 1000L + (i * 1000L)
            // Use massive bearing shifts to ensure points are jumps but NEVER promote
            val bearing = (i * 90.0) % 360.0
            sentinel.processLocation(baseLat + (jitterOffset * i), baseLng, baseAlt, 10.0, 10.0, bearing, 40.0, 12, ts, nowWall = ts, nowRealtime = ts)
        }

        assertEquals(10, sentinel.getHindsightBuffer().size)
    }
}
