package com.gps19.core.engine

import kotlinx.coroutines.*
import org.junit.Test

/**
 * EngineConcurrencyTest: Verifies thread safety of the core alarm logic.
 * Targets Chapter 7.3 of TEST_PROCEDURE.md.
 */
class EngineConcurrencyTest {
    private val mockTimeProvider = MockTimeProvider()
    
    @Test
    fun `Verify Engine Thread Safety - Zero ConcurrentModificationException under high load`() = runBlocking {
        val state = AlarmEvaluationState()
        val report = SystemHealthReport()
        val dispatcher = Dispatchers.Default
        
        // Simulate high-frequency sensor updates and alarm evaluation (250Hz simulation)
        val evaluationJob = launch(dispatcher) {
            repeat(1000) {
                MainAlarmLogic.detectViolations(
                    state = state,
                    timeProvider = mockTimeProvider,
                    report = report,
                    isLockedOut = false,
                    onSpike = { _, _ -> },
                    onTrigger = { },
                    onResolve = { _, _ -> }
                )
                // Small delay to simulate high-frequency but yielding execution
                yield()
            }
        }

        // Simultaneously simulate UI or Manager iterating over activeAlarms
        val iterationJob = launch(dispatcher) {
            repeat(1000) {
                synchronized(state.activeAlarms) {
                    state.activeAlarms.forEach { (key, value) ->
                        // Simulate read access to titles/subtitles used in UI
                        val title = value.title
                        val subtitle = value.subtitle
                    }
                }
                yield()
            }
        }

        joinAll(evaluationJob, iterationJob)
    }
}
