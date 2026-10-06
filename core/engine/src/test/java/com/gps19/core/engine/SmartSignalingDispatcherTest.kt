package com.gps19.core.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
class SmartSignalingDispatcherTest {

    @Test
    fun `High priority message should not be delayed by Normal priority backlog`() = runTest {
        val highPriorityReceivedTs = AtomicLong(0)
        
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testTimeProvider = object : TimeProvider {
            override fun currentTimeMillis() = testScheduler.currentTime
            override fun elapsedRealtime() = testScheduler.currentTime
        }
        
        val dispatcher = SmartSignalingDispatcher(
            scope = this,
            isViolationProvider = { false },
            jsonSink = { event, _ ->
                if (event == "critical_alert") {
                    highPriorityReceivedTs.set(testScheduler.currentTime)
                }
            },
            binarySink = { _, _, _ -> },
            isConnectedProvider = { true },
            timeProvider = testTimeProvider,
            dispatcher = testDispatcher
        )

        // 1. Inject 50 Normal logs. 
        // With 100ms delay each, 50 logs would take 5000ms if HIGH was stuck in FIFO.
        repeat(50) {
            dispatcher.dispatch(SmartSignalingDispatcher.Command.Json(
                "log_update", 
                mapOf("message" to "Normal $it"), 
                SignalingPriority.NORMAL
            ))
        }

        // 2. Inject a High priority message
        val highSendTs = testScheduler.currentTime
        dispatcher.dispatch(SmartSignalingDispatcher.Command.Json(
            "critical_alert", 
            mapOf("message" to "CRITICAL"), 
            SignalingPriority.HIGH
        ))

        // Process all pending tasks
        advanceUntilIdle()

        val delay = highPriorityReceivedTs.get() - highSendTs
        
        // Rule 1.119: HIGH priority must bypass the backlog immediately.
        // Expected delay is 0ms because of preemption.
        assertTrue("High priority message delayed by $delay ms, expected < 50ms", delay < 50)
        
        dispatcher.shutdown()
    }
}
