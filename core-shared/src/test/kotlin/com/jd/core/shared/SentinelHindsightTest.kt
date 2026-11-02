package com.jd.core.shared

import org.junit.Assert.*
import org.junit.Test

/**
 * SentinelHindsightTest: Ported from OLD LocationSentinelHindsightTest.
 * Validates trajectory promotion and jump buffering logic in pure Kotlin.
 */
class SentinelHindsightTest {

    @Test
    fun `Verify trajectory promotion logic`() {
        val baseLat = 32.7940
        val baseLng = 34.9896
        val last = LocationUpdate(lat = baseLat, lng = baseLng, timestamp = 1000L, speed = 0f)
        
        // 1. Simulate a "Jump" (50m in 1s)
        val jitterLat = baseLat + 0.00045 
        val jumpUpdate = LocationUpdate(lat = jitterLat, lng = baseLng, timestamp = 2000L)
        
        val res1 = SentinelLogic.validateMovement(last, jumpUpdate, hasPhysicalMotion = false)
        // Note: SentinelLogic currently marks this as JUMP based on scores
        assertEquals(SentinelLogic.SentinelStatus.JUMP, res1.status)

        // 2. Simulate a "Consistent" point following the jump
        val consistentLat = jitterLat + 0.00045
        val consistentUpdate = LocationUpdate(lat = consistentLat, lng = baseLng, timestamp = 3000L)
        
        // The second point should be valid relative to the first "jump" point
        val res2 = SentinelLogic.validateMovement(jumpUpdate, consistentUpdate, hasPhysicalMotion = true)
        assertEquals(SentinelLogic.SentinelStatus.VALID, res2.status)
    }

    @Test
    fun `Verify speed outlier rejection`() {
        val last = LocationUpdate(lat = 32.0, lng = 34.0, timestamp = 1000L)
        val current = LocationUpdate(lat = 33.0, lng = 35.0, timestamp = 2000L) // Massive jump

        val res = SentinelLogic.validateMovement(last, current)
        assertEquals("Impossible speed must be marked OUTLIER", SentinelLogic.SentinelStatus.OUTLIER, res.status)
    }
}
