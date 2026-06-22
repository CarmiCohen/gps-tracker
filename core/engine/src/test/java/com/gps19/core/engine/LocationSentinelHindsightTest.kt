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
    
    // 0.00045 deg lat is approx 50m
    private val jumpOffset = 0.00045 
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
            accuracy = 10f,
            bearing = 0f,
            snr = 40f,
            satsUsed = 12,
            timestamp = 1000L,
            nowWall = 1000L,
            nowRealtime = 1000L
        )
    }

    @Test
    fun `single point jump followed by consistent point triggers promotion`() {
        // 1. Send a "jump" point (50m away in 1 second -> 50m/s, > 33.33 but < 83.33)
        val jumpLat = baseLat + jumpOffset
        val jumpTime = 2000L
        val result1 = sentinel.processLocation(
            lat = jumpLat,
            lng = baseLng,
            alt = baseAlt,
            accuracy = 10f,
            bearing = 0f,
            snr = 40f,
            satsUsed = 12,
            timestamp = jumpTime,
            nowWall = jumpTime,
            nowRealtime = jumpTime
        )

        assertEquals(SentinelStatus.JUMP, result1.status)
        assertEquals(1, sentinel.getHindsightBuffer().size)

        // 2. Send a consistent point (another 10m further in 1 second -> 10m/s, consistent with bearing 0)
        val consistentLat = jumpLat + normalOffset
        val consistentTime = 3000L
        val result2 = sentinel.processLocation(
            lat = consistentLat,
            lng = baseLng,
            alt = baseAlt,
            accuracy = 10f,
            bearing = 0f,
            snr = 40f,
            satsUsed = 12,
            timestamp = consistentTime,
            nowWall = consistentTime,
            nowRealtime = consistentTime
        )

        assertEquals(SentinelStatus.TRAJECTORY_PROMOTED, result2.status)
        assertEquals(1, result2.promotedPoints?.size)
        assertEquals(jumpLat, result2.promotedPoints!![0].lat, 0.00001)
        assertEquals(0, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `multi-point jump sequence triggers full promotion`() {
        // Send 3 points that are jumps relative to the origin, but consistent with each other
        // Start: baseLat
        // P1: +50m (Jump from Start)
        val p1Lat = baseLat + jumpOffset 
        val p1Time = 2000L
        sentinel.processLocation(p1Lat, baseLng, baseAlt, 10f, 0f, 40f, 12, p1Time, nowWall = p1Time, nowRealtime = p1Time)
        
        // P2: +10m from P1 (Consistent with P1)
        val p2Lat = p1Lat + normalOffset
        val p2Time = 3000L
        sentinel.processLocation(p2Lat, baseLng, baseAlt, 10f, 0f, 40f, 12, p2Time, nowWall = p2Time, nowRealtime = p2Time)

        // P3: +10m from P2 (Consistent with P2)
        val p3Lat = p2Lat + normalOffset
        val p3Time = 4000L
        sentinel.processLocation(p3Lat, baseLng, baseAlt, 10f, 0f, 40f, 12, p3Time, nowWall = p3Time, nowRealtime = p3Time)

        assertEquals(3, sentinel.getHindsightBuffer().size)

        // Final point confirming the trajectory
        val p4Lat = p3Lat + normalOffset
        val p4Time = 5000L
        val result = sentinel.processLocation(p4Lat, baseLng, baseAlt, 10f, 0f, 40f, 12, p4Time, nowWall = p4Time, nowRealtime = p4Time)

        assertEquals(SentinelStatus.TRAJECTORY_PROMOTED, result.status)
        assertEquals(3, result.promotedPoints?.size)
        assertEquals(0, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `promotion fails if angle exceeds tolerance`() {
        // Jump point towards North (0 deg)
        val jumpLat = baseLat + jumpOffset
        val jumpTime = 2000L
        sentinel.processLocation(jumpLat, baseLng, baseAlt, 10f, 0f, 40f, 12, jumpTime, nowWall = jumpTime, nowRealtime = jumpTime)

        // New point towards East (90 deg) -> angle diff 90 > PROMOTION_ANGLE_TOLERANCE (30)
        val nextLat = jumpLat
        val nextLng = baseLng + normalOffset
        val nextTime = 3000L
        val result = sentinel.processLocation(nextLat, nextLng, baseAlt, 10f, 90f, 40f, 12, nextTime, nowWall = nextTime, nowRealtime = nextTime)

        // It should still be a JUMP/JITTER and NOT promoted
        assertTrue(result.status == SentinelStatus.JUMP || result.status == SentinelStatus.JITTER)
        assertEquals(2, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `promotion fails if speed delta exceeds tolerance`() {
        // Jump point at 50m/s
        val jumpLat = baseLat + jumpOffset
        val jumpTime = 2000L 
        sentinel.processLocation(jumpLat, baseLng, baseAlt, 10f, 0f, 40f, 12, jumpTime, nowWall = jumpTime, nowRealtime = jumpTime)

        // Next point implies a very different speed (e.g. 1m/s)
        // abs(1 - 50) = 49 > 10.0 tolerance.
        val nextLat = jumpLat + (normalOffset / 10.0)
        val nextTime = 3000L
        val result = sentinel.processLocation(nextLat, baseLng, baseAlt, 10f, 0f, 40f, 12, nextTime, nowWall = nextTime, nowRealtime = nextTime)

        assertTrue(result.status == SentinelStatus.JUMP || result.status == SentinelStatus.JITTER)
        assertEquals(2, sentinel.getHindsightBuffer().size)
    }

    @Test
    fun `hindsight buffer prunes points older than max age`() {
        // Add a jump point
        sentinel.processLocation(baseLat + jumpOffset, baseLng, baseAlt, 10f, 0f, 40f, 12, 2000L, nowWall = 2000L, nowRealtime = 2000L)
        assertEquals(1, sentinel.getHindsightBuffer().size)

        // Wait longer than HINDSIGHT_MAX_AGE_MS (30s)
        val lateTime = 2000L + 35000L
        sentinel.processLocation(baseLat + jumpOffset * 2, baseLng, baseAlt, 10f, 0f, 40f, 12, lateTime, nowWall = lateTime, nowRealtime = lateTime)
        
        // The first point should have been pruned before the new rejected point was added
        assertEquals(1, sentinel.getHindsightBuffer().size)
        assertEquals(lateTime, sentinel.getHindsightBuffer()[0].ts)
    }

    @Test
    fun `hindsight buffer respects maximum size`() {
        // HINDSIGHT_BUFFER_SIZE = 5
        for (i in 1..7) {
            val ts = 1000L + (i * 1000L)
            sentinel.processLocation(baseLat + normalOffset * i, baseLng, baseAlt, 10f, 0f, 40f, 12, ts, nowWall = ts, nowRealtime = ts)
            // Note: These need to be jumps relative to the "last valid" point to be stored.
            // Since they are small increments, they might be VALID.
            // Let's force them to be JUMP by making them all jumps from the same start.
            // Actually, processLocation updates lastValid if it's VALID.
            // If they are JUMP, they don't update lastValid.
        }
        // Let's rethink. If I send 7 jumps from the SAME baseLat, they all stay in buffer.
        sentinel.reset()
        sentinel.processLocation(baseLat, baseLng, baseAlt, 10f, 0f, 40f, 12, 1000L, nowWall = 1000L, nowRealtime = 1000L)
        
        for (i in 1..7) {
            val ts = 2000L + (i * 10L) // Very fast speed -> JUMP
            sentinel.processLocation(baseLat + jumpOffset, baseLng + (i * 0.00001), baseAlt, 10f, 0f, 40f, 12, ts, nowWall = ts, nowRealtime = ts)
        }

        assertEquals(5, sentinel.getHindsightBuffer().size)
    }
}
