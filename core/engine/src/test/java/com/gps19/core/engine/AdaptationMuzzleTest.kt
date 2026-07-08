package com.gps19.core.engine

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AdaptationMuzzleTest {

    private lateinit var processor: LocationProcessor
    private val timeProvider = TestTimeProvider()
    private val listener = object : LocationProcessorListener {
        override fun onTrailPointSaved(lat: Double, lng: Double, isViewerTrail: Boolean, isJump: Boolean, timestamp: Long, isHindsightCorrected: Boolean, accuracy: Double, maxAccuracy: Double) {}
        override fun onLogAdded(message: String, type: String, isImportant: Boolean, isSpecial: Boolean, lat: Double, lng: Double, accuracy: Double, snr: Double?, vibe: Double?) {}
        override fun onMaxAccuracyChanged(accuracy: Double) {}
        override fun onChairBaselineChanged(baseline: Double) {}
        override fun onGpsStallDetected(ts: Long) {}
    }

    @Before
    fun setup() {
        processor = LocationProcessor(listener, timeProvider)
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
        // Lat 0.001 is ~111 meters.
        // We use a 2-second gap so speed is ~55.5 m/s.
        // This is > MAX_PHYSICAL_SPEED_MPS (33.33) and < OUTLIER_SPEED_CAP_MPS (83.33).
        // And dist > JUMP_POINT_DISTANCE_THRESHOLD (100.0).
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
