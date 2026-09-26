package com.gps19.core.engine

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AdaptationMuzzleTest: Validating A15-specific polling stabilization logic.
 * Sep.26.3:
 * - Issue #1334: Updated to SystemEvaluationSnapshot API.
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
        processor.updateExpectedInterval(timeProvider.elapsedTime, 45000L) 

        val initialSnapshot = SystemEvaluationSnapshot(
            kinetic = KineticState(lat = startLat, lng = startLng, alt = 10.0, gpsTs = now, accuracy = 5.0),
            nowRt = timeProvider.elapsedTime,
            nowTs = now
        )
        processor.processGpsPoint(
            snapshot = initialSnapshot,
            isViewerTrail = false,
            lastGpsTs = 0L,
            isLocal = true
        )

        // 2. Simulate a frequency transition
        processor.updateExpectedInterval(timeProvider.elapsedTime, 2000L)

        // 3. Simulate a "Jump" artifact immediately after transition.
        val jumpLat = 52.5210 
        val jumpTs = now + 2000L
        timeProvider.wallTime = jumpTs
        timeProvider.elapsedTime += 2000L

        val jumpSnapshot = SystemEvaluationSnapshot(
            kinetic = KineticState(lat = jumpLat, lng = startLng, alt = 10.0, gpsTs = jumpTs, accuracy = 5.0),
            nowRt = timeProvider.elapsedTime,
            nowTs = jumpTs
        )

        // With internal muzzle active - should be suppressed to VALID
        val resultMuzzled = processor.processGpsPoint(
            snapshot = jumpSnapshot,
            isViewerTrail = false,
            lastGpsTs = now,
            isLocal = true
        )

        assertEquals("Jump should be suppressed to VALID when muzzled", SentinelStatus.VALID, resultMuzzled.status)
        assertTrue("Should be marked spatially valid when muzzled", resultMuzzled.isSpatiallyValid)
        assertEquals("Should have adaptation note", "Settling A15 Polling...", resultMuzzled.suppressionNote)

        // 4. Advance time beyond ADAPTATION_SETTLING_MS (5s) to clear muzzle
        timeProvider.elapsedTime += 6000L
        val nextJumpLat = 52.5220
        val nextJumpTs = jumpTs + 6000L
        timeProvider.wallTime = nextJumpTs

        val expiredSnapshot = SystemEvaluationSnapshot(
            kinetic = KineticState(lat = nextJumpLat, lng = startLng, alt = 10.0, gpsTs = nextJumpTs, accuracy = 5.0),
            nowRt = timeProvider.elapsedTime,
            nowTs = nextJumpTs
        )

        val resultExpired = processor.processGpsPoint(
            snapshot = expiredSnapshot,
            isViewerTrail = false,
            lastGpsTs = jumpTs,
            isLocal = true
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
