package com.gps19.core.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AdaptationMuzzleTest: Validating A15-specific polling stabilization logic.
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

        // 1. Establish initial fix
        processor.processGpsPoint(
            lat = startLat, lng = startLng, alt = 10.0, androidSpeedMps = 0.0,
            gpsTs = now, accuracy = 5.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = 0L, isLocal = true
        )

        // 2. Simulate a "Jump" artifact.
        val jumpLat = 52.5210 
        val jumpTs = now + 2000L
        timeProvider.wallTime = jumpTs
        timeProvider.elapsedTime += 2000L

        // CASE A: Without muzzle - should be rejected as JUMP
        val resultWithoutMuzzle = processor.processGpsPoint(
            lat = jumpLat, lng = startLng, alt = 10.0, androidSpeedMps = 0.0,
            gpsTs = jumpTs, accuracy = 5.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = now, isLocal = true,
            isAdaptationMuzzled = false
        )
        assertEquals("Should be JUMP without muzzle", SentinelStatus.JUMP, resultWithoutMuzzle.status)
        assertFalse("Should not be spatially valid", resultWithoutMuzzle.isSpatiallyValid)

        // CASE B: With muzzle (Issue #057) - should be suppressed to VALID
        val resultWithMuzzle = processor.processGpsPoint(
            lat = jumpLat, lng = startLng, alt = 10.0, androidSpeedMps = 0.0,
            gpsTs = jumpTs, accuracy = 5.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = now, isLocal = true,
            isAdaptationMuzzled = true
        )

        assertEquals("Jump should be suppressed to VALID when muzzled", SentinelStatus.VALID, resultWithMuzzle.status)
        assertTrue("Should be marked spatially valid when muzzled", resultWithMuzzle.isSpatiallyValid)
        assertEquals("Should have adaptation note", "Settling A15 Polling...", resultWithMuzzle.suppressionNote)
    }

    private class TestTimeProvider : TimeProvider {
        var wallTime = 0L
        var elapsedTime = 0L
        override fun currentTimeMillis() = wallTime
        override fun elapsedRealtime() = elapsedTime
    }
}
