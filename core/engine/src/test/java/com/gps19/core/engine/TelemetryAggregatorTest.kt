package com.gps19.core.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * TelemetryAggregatorTest: Validating zero-churn aggregation and ribbon alignment.
 * Aug.04.50:
 * - Issue #715: Build Hardening. Updated processPoint calls to include required 
 *   onResult callbacks and removed deprecated mergeWorstCase validation.
 */
class TelemetryAggregatorTest {

    private val aggregator = TelemetryAggregator()

    @Test
    fun `processPoint aligns with 16M scale (4 ticks = 8s interval)`() {
        // R405: TICK_INTERVAL_MS is 2000ms. 
        // RibbonScale.SIXTEEN_MIN has intervalSeconds = 4 (which means 4 ticks).
        val baseTs = 2000L 
        
        val results = mutableListOf<Pair<RibbonScale, EngineConnectionPoint>>()
        val collector: (RibbonScale, EngineConnectionPoint) -> Unit = { scale, point ->
            // Copy the point because it's a flyweight
            val copy = EngineConnectionPoint().apply { copyFrom(point) }
            results.add(scale to copy)
        }

        // Input points at ticks 1, 2, 3 (2s, 4s, 6s)
        aggregator.processPoint(createPoint(baseTs), collector)
        aggregator.processPoint(createPoint(baseTs + 2000L), collector)
        aggregator.processPoint(createPoint(baseTs + 4000L), collector)
        
        // Input 4th point at tick 4 (8s) (should trigger 16M result since 4 % 4 == 0)
        val alignedTs = 8000L 
        aggregator.processPoint(createPoint(alignedTs), collector)
        
        val sixteenMinMatch = results.find { it.first == RibbonScale.SIXTEEN_MIN }
        assertNotNull("16M scale should have produced a point at 8s mark (tick 4)", sixteenMinMatch)
    }

    private fun createPoint(ts: Long) = EngineConnectionPoint().apply {
        this.ts = ts
        this.rt = ts
        this.isConnected = true
    }
}
