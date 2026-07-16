package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * ForensicIdentityTest: Verifies point classification logic.
 * July.1.15:
 * - Issue #512: Consolidate Sentinel Statuses. Replaced OUTLIER with JUMP.
 */
class ForensicIdentityTest {

    private val timeProvider = object : TimeProvider {
        override fun currentTimeMillis() = 1000L
        override fun elapsedRealtime() = 1000L
    }

    private val listener = object : LocationProcessorListener {
        override fun onTrailPointSaved(lat: Double, lng: Double, isViewerTrail: Boolean, isJump: Boolean, timestamp: Long, accuracy: Double, maxAccuracy: Double) {}
        override fun onLogAdded(message: String, type: String, isImportant: Boolean, isSpecial: Boolean, lat: Double, lng: Double, accuracy: Double, snr: Double?, vibe: Double?) {}
        override fun onMaxAccuracyChanged(accuracy: Double) {}
        override fun onChairBaselineChanged(baseline: Double) {}
        override fun onGpsStallDetected(ts: Long) { }
    }

    private lateinit var processor: LocationProcessor

    @Before
    fun setup() {
        processor = LocationProcessor(timeProvider)
        processor.setListener(listener)
    }

    @Test
    fun `processGpsPoint handles valid point`() {
        val result = processor.processGpsPoint(
            lat = 32.0, lng = 34.0, alt = 10.0, androidSpeedMps = 0.0,
            gpsTs = 1000L, accuracy = 10.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = 0L, isLocal = true
        )

        assertEquals("Status should be VALID", SentinelStatus.VALID, result.status)
    }

    @Test
    fun `processGpsPoint handles jump scenario`() {
        // First point to establish baseline
        processor.processGpsPoint(
            lat = 32.0, lng = 34.0, alt = 10.0, androidSpeedMps = 0.0,
            gpsTs = 1000L, accuracy = 10.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = 0L, isLocal = true
        )

        // Second point is a massive jump (far away)
        // Issue #512: Consolidated to JUMP
        val jumpResult = processor.processGpsPoint(
            lat = 40.0, lng = 50.0, alt = 10.0, androidSpeedMps = 0.0,
            gpsTs = 2000L, accuracy = 10.0, bearing = 0.0, snr = 40.0, satsUsed = 10,
            isViewerTrail = false, lastGpsTs = 1000L, isLocal = true
        )

        assertEquals("Status should be JUMP due to distance", SentinelStatus.JUMP, jumpResult.status)
    }
}
