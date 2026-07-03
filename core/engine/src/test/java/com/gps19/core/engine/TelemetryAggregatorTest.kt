package com.gps19.core.engine

import org.junit.Assert.*
import org.junit.Test

class TelemetryAggregatorTest {

    private val aggregator = TelemetryAggregator()

    @Test
    fun `mergeWorstCase preserves negative peaks and positive minimums`() {
        val p1 = EngineConnectionPoint(
            ts = 1000L, rtt = 100, remoteSig = 10, isConnected = true,
            noiseIdx = 0.2, vibeIdx = 0.1, gpsIndex = 0.9
        )
        val p2 = EngineConnectionPoint(
            ts = 2000L, rtt = 500, remoteSig = 5, isConnected = true,
            noiseIdx = 0.8, vibeIdx = 0.5, gpsIndex = 0.4
        )

        val merged = aggregator.mergeWorstCase(p1, p2)

        assertEquals(500, merged.rtt)          // Max RTT
        assertEquals(5, merged.remoteSig)      // Min Signal
        assertEquals(0.8, merged.noiseIdx, 0.001)    // Max Noise
        assertEquals(0.5, merged.vibeIdx, 0.001)     // Max Vibe
        assertEquals(0.4, merged.gpsIndex, 0.001)    // Min GPS Index
    }

    @Test
    fun `processPoint aligns with 16M scale (4s interval)`() {
        val baseTs = 1000L 
        
        // Input points at 1s, 2s, 3s
        aggregator.processPoint(createPoint(baseTs))
        aggregator.processPoint(createPoint(baseTs + 1000L))
        aggregator.processPoint(createPoint(baseTs + 2000L))
        
        // Input 4th point at 4s (should trigger 16M result since 4 % 4 == 0)
        val alignedTs = 4000L 
        val results = aggregator.processPoint(createPoint(alignedTs))
        
        val sixteenMinMatch = results.find { it.first == RibbonScale.SIXTEEN_MIN }
        assertNotNull("16M scale should have produced a point at 4s mark", sixteenMinMatch)
    }

    private fun createPoint(ts: Long) = EngineConnectionPoint(
        ts = ts, rtt = 0, remoteSig = 0, isConnected = true
    )
}
