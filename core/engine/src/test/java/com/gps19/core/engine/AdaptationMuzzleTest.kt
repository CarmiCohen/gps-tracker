package com.gps19.core.engine

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AdaptationMuzzleTest: Validating A15-specific polling stabilization logic.
 * Sep.16.01:
 * - Issue #1059: Test Logic Alignment. Updated to correctly trigger internal muzzling 
 *   by establishing an initial interval before transition (R-ID 348, formerly R-ID 347).
 * Aug.04.50:
 * - Issue #715: Build Hardening. Updated to reactive flow collection to match 
 *   zero-churn ProcessorEvent migration.
 */
class AdaptationMuzzleTest {

    private lateinit var processor: LocationProcessor
    private val timeProvider = TestTimeProvider()

    @Before
    fun setup() {
        processor = LocationProcessor(timeProvider)
    }

    @Test
    fun `test Adaptation Muzzle suppresses jump during A15 frequency transition`() {
        val startLat = 52.5200
        val startLng = 13.4050
        val now = 1000000L
        timeProvider.wallTime = now
        timeProvider.elapsedTime = 10000L

        // 1. Establish initial fix and interval
        // Internal muzzle only activates if transitioning FROM a non-zero interval.
        processor.updateExpectedInterval(timeProvider.elapsedTime, 45000L) 

        processor.processGpsPoint(
            lat = startLat, lng = startLng, alt = 10.0, androidSpeedMps = 0.0,
            gpsTs = now, accuracy = 5.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = 0L, isLocal = true
        )

        // 2. Simulate a frequency transition (e.g., from 45s to 2s)
        // This triggers the internal muzzling logic in LocationProcessor.
        processor.updateExpectedInterval(timeProvider.elapsedTime, 2000L)

        // 3. Simulate a "Jump" artifact immediately after transition.
        val jumpLat = 52.5210 
        val jumpTs = now + 2000L
        timeProvider.wallTime = jumpTs
        timeProvider.elapsedTime += 2000L

        // With internal muzzle active - should be suppressed to VALID
        val resultMuzzled = processor.processGpsPoint(
            lat = jumpLat, lng = startLng, alt = 10.0, androidSpeedMps = 0.0,
            gpsTs = jumpTs, accuracy = 5.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = now, isLocal = true
        )

        assertEquals("Jump should be suppressed to VALID when muzzled", SentinelStatus.VALID, resultMuzzled.status)
        assertTrue("Should be marked spatially valid when muzzled", resultMuzzled.isSpatiallyValid)
        assertEquals("Should have adaptation note", "Settling A15 Polling...", resultMuzzled.suppressionNote)

        // 4. Advance time beyond ADAPTATION_SETTLING_MS (5s) to clear muzzle
        timeProvider.elapsedTime += 6000L
        val nextJumpLat = 52.5220
        val nextJumpTs = jumpTs + 6000L
        timeProvider.wallTime = nextJumpTs

        val resultExpired = processor.processGpsPoint(
            lat = nextJumpLat, lng = startLng, alt = 10.0, androidSpeedMps = 0.0,
            gpsTs = nextJumpTs, accuracy = 5.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = jumpTs, isLocal = true
        )

        assertEquals("Should be JUMP after muzzle expires", SentinelStatus.JUMP, resultExpired.status)
        assertFalse("Should not be spatially valid after muzzle expires", resultExpired.isSpatiallyValid)
    }

    private class TestTimeProvider : TimeProvider {
        var wallTime = 0L
        var elapsedTime = 0L
        override fun currentTimeMillis() = wallTime
        override fun elapsedRealtime() = elapsedTime
    }
}
