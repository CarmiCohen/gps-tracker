package com.gps19.core.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ForensicIdentityTest: Verifying signature-based trace deduplication.
 * Oct.2.8:
 * - Issue #1330: Snap-to-Update Monolith. Migrated from SystemEvaluationSnapshot 
 *   to unified LocationUpdate DTO (R-ID 596).
 */
class ForensicIdentityTest {

    private lateinit var processor: LocationProcessor
    private val timeProvider = TestTimeProvider()

    @Before
    fun setup() {
        processor = LocationProcessor(timeProvider)
    }

    @Test
    fun `test Duplicate Coordinate Suppression`() = runBlocking {
        val lat = 32.1234
        val lng = 34.5678
        val ts = 1700000000000L
        
        timeProvider.wallTime = ts
        timeProvider.elapsedTime = 10000L

        val initialSnapshot = LocationUpdate(
            kinetic = KineticState(lat = lat, lng = lng, alt = 0.0, speed = 0.0, gpsTs = ts, accuracy = 5.0, bearing = 0.0),
            nowRt = timeProvider.elapsedTime,
            nowTs = ts
        )

        // 1. Process first point - should be saved
        processor.processGpsPoint(
            update = initialSnapshot,
            isViewerTrail = false,
            lastGpsTs = 0L,
            isLocal = true
        )

        val duplicateSnapshot = LocationUpdate(
            kinetic = KineticState(lat = lat, lng = lng, alt = 0.0, speed = 0.0, gpsTs = ts, accuracy = 5.0, bearing = 0.0),
            nowRt = timeProvider.elapsedTime,
            nowTs = ts
        )

        // 2. Process same point again after 5s - should be suppressed (within same TS)
        val result = processor.processGpsPoint(
            update = duplicateSnapshot,
            isViewerTrail = false,
            lastGpsTs = ts,
            isLocal = true
        )

        assertTrue("Duplicate point should be suppressed", result.isStalled)
    }

    private class TestTimeProvider : TimeProvider {
        var wallTime = 0L
        var elapsedTime = 0L
        override fun currentTimeMillis() = wallTime
        override fun elapsedRealtime() = elapsedTime
    }
}
