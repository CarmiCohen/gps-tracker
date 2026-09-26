package com.gps19.core.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ForensicIdentityTest: Verifying signature-based trace deduplication.
 * Sep.26.3:
 * - Issue #1334: Adapted to SystemEvaluationSnapshot API.
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

        val initialSnapshot = SystemEvaluationSnapshot(
            kinetic = KineticState(lat = lat, lng = lng, alt = 0.0, speed = 0.0, gpsTs = ts, accuracy = 5.0, bearing = 0.0),
            nowRt = timeProvider.elapsedTime,
            nowTs = ts
        )

        // 1. Process first point - should be saved
        processor.processGpsPoint(
            snapshot = initialSnapshot,
            isViewerTrail = false,
            lastGpsTs = 0L,
            isLocal = true
        )

        val duplicateSnapshot = SystemEvaluationSnapshot(
            kinetic = KineticState(lat = lat, lng = lng, alt = 0.0, speed = 0.0, gpsTs = ts, accuracy = 5.0, bearing = 0.0),
            nowRt = timeProvider.elapsedTime,
            nowTs = ts
        )

        // 2. Process same point again after 5s - should be suppressed (within same TS)
        val result = processor.processGpsPoint(
            snapshot = duplicateSnapshot,
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
