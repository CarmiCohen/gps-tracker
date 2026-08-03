package com.gps19.core.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ForensicIdentityTest: Verifying signature-based trace deduplication.
 * Aug.04.50:
 * - Issue #715: Build Hardening. Updated to reactive flow collection to match 
 *   zero-churn ProcessorEvent migration and remediated LocationProcessorListener drift.
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

        // 1. Process first point - should be saved
        processor.processGpsPoint(
            lat = lat, lng = lng, alt = 0.0, androidSpeedMps = 0.0,
            gpsTs = ts, accuracy = 5.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = 0L, isLocal = true
        )

        // 2. Process same point again after 5s - should be suppressed (within same TS)
        val result = processor.processGpsPoint(
            lat = lat, lng = lng, alt = 0.0, androidSpeedMps = 0.0,
            gpsTs = ts, accuracy = 5.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = ts, isLocal = true
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
