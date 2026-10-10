package com.gps19.core.engine

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ForensicIdentityTest: Verifying signature-based trace deduplication.
 * Oct.10.4:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to property-based 
 *   TimeProvider API and fixed property invocation errors.
 */
class ForensicIdentityTest {

    private lateinit var processor: LocationProcessor
    private val timeProvider get() = (processor.getTimeProviderForTest() as TestTimeProvider)

    @Before
    fun setup() {
        processor = LocationProcessor(TestTimeProvider())
    }

    @Test
    fun `test Duplicate Coordinate Suppression`() = runBlocking {
        val lat = 32.1234
        val lng = 34.5678
        val ts = 1700000000000L
        
        val tp = timeProvider
        tp.wallTime = ts
        tp.elapsedTime = 10000L

        val initialSnapshot = LocationUpdate(
            kinetic = KineticState(lat = lat, lng = lng, alt = 0.0, speed = 0.0, gpsTs = ts, accuracy = 5.0, bearing = 0.0),
            nowRt = tp.elapsedRealtime,
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
            nowRt = tp.elapsedRealtime,
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
        override val currentTimeMillis: Long get() = wallTime
        override val elapsedRealtime: Long get() = elapsedTime
    }

    private fun LocationProcessor.getTimeProviderForTest(): TimeProvider {
        val field = LocationProcessor::class.java.getDeclaredField("timeProvider")
        field.isAccessible = true
        return field.get(this) as TimeProvider
    }
}
