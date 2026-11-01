package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.SensingEngine
import com.jd.core.shared.TickOrchestrator
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * AdaptiveSamplingTest: Verifies velocity-based sampling transitions (Revol34.0).
 * Ensures that changes in movement speed correctly adjust the sensing loop interval.
 */
@RunWith(AndroidJUnit4::class)
class AdaptiveSamplingTest {

    @Test
    fun testSamplingIntervalTransitions() = runTest {
        val orchestrator = TickOrchestrator()
        val executionCount = AtomicInteger(0)
        val currentInterval = AtomicLong(1000L)
        
        val job = orchestrator.launchPeriodicLoop(
            name = "test_loop",
            scope = this,
            intervalProvider = { currentInterval.get() }
        ) {
            executionCount.incrementAndGet()
        }

        // 1. Stationary Baseline (0.1Hz / 10s)
        currentInterval.set(SensingEngine.getOptimalIntervalMs(30f, false, 80, 0f))
        assertEquals(10000L, currentInterval.get())
        orchestrator.preemptLoop("test_loop")
        
        // 2. Slow Movement (1Hz / 1s)
        currentInterval.set(SensingEngine.getOptimalIntervalMs(30f, false, 80, 1.0f))
        assertEquals(1000L, currentInterval.get())
        orchestrator.preemptLoop("test_loop")

        // 3. Fast Movement (10Hz / 0.1s)
        currentInterval.set(SensingEngine.getOptimalIntervalMs(30f, false, 80, 10.0f))
        assertEquals(100L, currentInterval.get())
        orchestrator.preemptLoop("test_loop")

        // 4. Survival Mode (Battery < 15%)
        currentInterval.set(SensingEngine.getOptimalIntervalMs(30f, false, 10, 10.0f))
        assertEquals(1000L, currentInterval.get()) // Floor at 1Hz

        job.cancel()
    }
}
