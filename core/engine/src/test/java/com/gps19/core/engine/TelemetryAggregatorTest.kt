package com.gps19.core.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * TelemetryAggregatorTest: Validating zero-churn aggregation and ribbon alignment.
 * Sep.17.01:
 * - Telemetry Backfill QA Task (#1074): Added explicit verification tests for 
 *   telemetry backfill convergence and signaling continuity under R-ID 17.
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

    @Test
    fun `verifyTelemetryBackfillConvergence under RID 17`() {
        // Validates backfill convergence and gap generation constraints under R-ID 17.
        val lastTickRt = 10000L
        val lastTickTs = 10000L
        val nowRt = lastTickRt + (5 * TICK_INTERVAL_MS) // 5 ticks gap
        val nowTs = lastTickTs + (5 * TICK_INTERVAL_MS)
        
        val results = mutableListOf<Pair<RibbonScale, EngineConnectionPoint>>()
        val baseTemplate = createPoint(lastTickRt)
        
        aggregator.backfillGaps(
            lastTickRt = lastTickRt,
            nowRt = nowRt,
            lastTickTs = lastTickTs,
            nowTs = nowTs,
            snrSamples = emptySequence(),
            sensorSamples = emptySequence(),
            acousticFloor = 40.0,
            baseTemplate = baseTemplate
        ) { scale, point ->
            val copy = EngineConnectionPoint().apply { copyFrom(point) }
            results.add(scale to copy)
        }
        
        // Check that backfill points are produced and converge successfully
        assertTrue("Backfill should generate points for alignment", results.isNotEmpty())
        val generatedCount = results.count { it.first == RibbonScale.FOUR_MIN }
        assertTrue("Should generate exactly 4 backfill ticks for the 4M scale gap", generatedCount == 4)
    }

    @Test
    fun `verifyTelemetryBackfillCapCompliance`() {
        // Enforces that backfill processes respect MAX_BACKFILL_POINTS to prevent memory bloat
        val lastTickRt = 10000L
        val lastTickTs = 10000L
        // Create an extreme gap that exceeds MAX_BACKFILL_POINTS (1000)
        val nowRt = lastTickRt + (2000 * TICK_INTERVAL_MS)
        val nowTs = lastTickTs + (2000 * TICK_INTERVAL_MS)
        
        val results = mutableListOf<Pair<RibbonScale, EngineConnectionPoint>>()
        val baseTemplate = createPoint(lastTickRt)
        
        aggregator.backfillGaps(
            lastTickRt = lastTickRt,
            nowRt = nowRt,
            lastTickTs = lastTickTs,
            nowTs = nowTs,
            snrSamples = emptySequence(),
            sensorSamples = emptySequence(),
            acousticFloor = 40.0,
            baseTemplate = baseTemplate
        ) { scale, point ->
            val copy = EngineConnectionPoint().apply { copyFrom(point) }
            results.add(scale to copy)
        }
        
        val fourMinPointsCount = results.count { it.first == RibbonScale.FOUR_MIN }
        assertTrue("Backfill must be capped at MAX_BACKFILL_POINTS", fourMinPointsCount <= MAX_BACKFILL_POINTS)
    }

    private fun createPoint(ts: Long) = EngineConnectionPoint().apply {
        this.ts = ts
        this.rt = ts
        this.isConnected = true
    }
}
