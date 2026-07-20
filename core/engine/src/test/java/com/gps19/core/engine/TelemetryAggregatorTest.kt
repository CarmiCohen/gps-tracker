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
    fun `processPoint aligns with 16M scale (4 ticks = 8s interval)`() {
        // R405: TICK_INTERVAL_MS is 2000ms. 
        // RibbonScale.SIXTEEN_MIN has intervalSeconds = 4 (which means 4 ticks).
        val baseTs = 2000L 
        
        // Input points at ticks 1, 2, 3 (2s, 4s, 6s)
        aggregator.processPoint(createPoint(baseTs))
        aggregator.processPoint(createPoint(baseTs + 2000L))
        aggregator.processPoint(createPoint(baseTs + 4000L))
        
        // Input 4th point at tick 4 (8s) (should trigger 16M result since 4 % 4 == 0)
        val alignedTs = 8000L 
        val results = aggregator.processPoint(createPoint(alignedTs))
        
        val sixteenMinMatch = results.find { it.first == RibbonScale.SIXTEEN_MIN }
        assertNotNull("16M scale should have produced a point at 8s mark (tick 4)", sixteenMinMatch)
    }

    private fun createPoint(ts: Long) = EngineConnectionPoint(
        ts = ts, rt = ts, rtt = 0, remoteSig = 0, isConnected = true
    )
}
