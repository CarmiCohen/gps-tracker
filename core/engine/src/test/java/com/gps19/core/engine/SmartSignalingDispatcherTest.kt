package com.gps19.core.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
class SmartSignalingDispatcherTest {

    @Test
    fun `High priority message should not be delayed by Normal priority backlog`() = runTest {
        val highPriorityReceivedTs = AtomicLong(0)
        
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testTimeProvider = object : TimeProvider {
            override val currentTimeMillis: Long get() = testScheduler.currentTime
            override val elapsedRealtime: Long get() = testScheduler.currentTime
        }
        
        val dispatcher = SmartSignalingDispatcher(
            scope = this,
            isViolationProvider = { false },
            wireSink = object : SignalingWireSink {
                override fun emitJson(event: String, data: Map<String, Any?>) {
                    if (event == "critical_alert") {
                        highPriorityReceivedTs.set(testScheduler.currentTime)
                    }
                }
                override fun emitBinary(event: String, routingId: String, data: ByteArray) {}
            },
            encoder = object : SignalingEncoder {
                override fun encodeObject(update: LocationUpdate, deltaState: SignalingDeltaState, fromViewer: Boolean): ByteArray = ByteArray(0)
            },
            isConnectedProvider = { true },
            timeProvider = testTimeProvider,
            dispatcher = testDispatcher
        )

        // 1. Inject 50 Normal logs. 
        repeat(50) {
            dispatcher.dispatchJson(
                "log_update", 
                mapOf("message" to "Normal $it"), 
                SignalingPriority.NORMAL
            )
        }

        // 2. Inject a High priority message
        val highSendTs = testScheduler.currentTime
        dispatcher.dispatchJson(
            "critical_alert", 
            mapOf("message" to "CRITICAL"), 
            SignalingPriority.HIGH
        )

        // Process all pending tasks
        advanceUntilIdle()

        val delay = highPriorityReceivedTs.get() - highSendTs
        
        // Rule 1.119: HIGH priority must bypass the backlog immediately.
        assertTrue("High priority message delayed by $delay ms, expected < 50ms", delay < 50)
        
        dispatcher.shutdown()
    }

    @Test
    fun `Metrics should track frames received emitted and conflated`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val dispatcher = SmartSignalingDispatcher(
            scope = this,
            isViolationProvider = { false },
            wireSink = object : SignalingWireSink {
                override fun emitJson(event: String, data: Map<String, Any?>) {}
                override fun emitBinary(event: String, routingId: String, data: ByteArray) {}
            },
            encoder = object : SignalingEncoder {
                override fun encodeObject(update: LocationUpdate, deltaState: SignalingDeltaState, fromViewer: Boolean): ByteArray = ByteArray(0)
            },
            isConnectedProvider = { true },
            dispatcher = testDispatcher
        )

        // Dispatch 3 identical logs (should result in 2 conflations)
        repeat(3) {
            dispatcher.dispatchJson(
                "log_update",
                mapOf("message" to "test"),
                SignalingPriority.NORMAL
            )
        }

        advanceUntilIdle()

        val metrics = dispatcher.metricsFlow.value
        assertTrue("Expected 3 frames received, got ${metrics.received}", metrics.received == 3L)
        assertTrue("Expected 2 frames conflated, got ${metrics.conflated}", metrics.conflated == 2L)
        
        dispatcher.shutdown()
    }

    @Test
    fun `Stress test with interleaved telemetry bursts should conflate correctly`() = runTest {
        val emitCount = AtomicInteger(0)
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testTimeProvider = object : TimeProvider {
            override val currentTimeMillis: Long get() = testScheduler.currentTime
            override val elapsedRealtime: Long get() = testScheduler.currentTime
        }

        val dispatcher = SmartSignalingDispatcher(
            scope = this,
            isViolationProvider = { false },
            wireSink = object : SignalingWireSink {
                override fun emitJson(event: String, data: Map<String, Any?>) { emitCount.incrementAndGet() }
                override fun emitBinary(event: String, routingId: String, data: ByteArray) { emitCount.incrementAndGet() }
            },
            encoder = object : SignalingEncoder {
                override fun encodeObject(update: LocationUpdate, deltaState: SignalingDeltaState, fromViewer: Boolean): ByteArray = ByteArray(0)
            },
            isConnectedProvider = { true },
            timeProvider = testTimeProvider,
            dispatcher = testDispatcher
        )

        // Simultaneous burst of 10 Location Maps, 10 Location Objects, and 10 Logs
        repeat(10) { i ->
            dispatcher.dispatchJson(
                "location_update",
                mapOf("lat" to 1.0, "lon" to i.toDouble()),
                SignalingPriority.NORMAL
            )
            dispatcher.dispatchObject(
                "location_update_bin",
                LocationUpdate().apply {
                    kinetic.lat = 1.0
                    kinetic.lng = i.toDouble()
                },
                SignalingPriority.NORMAL
            )
            dispatcher.dispatchJson(
                "log_update",
                mapOf("message" to "stress_log"),
                SignalingPriority.NORMAL
            )
        }

        // Expected results:
        // Received: 30 (10+10+10)
        // Conflated: 27 (9 per bucket, since each bucket flushes 1)
        // Emitted: 3 (1 per telemetry type)
        
        advanceUntilIdle()

        val metrics = dispatcher.metricsFlow.value
        assertEquals("Received frames mismatch", 30L, metrics.received)
        assertEquals("Conflated frames mismatch", 27L, metrics.conflated)
        assertEquals("Emitted frames mismatch", 3L, metrics.emitted)
        assertEquals("Sink calls mismatch", 3, emitCount.get())

        dispatcher.shutdown()
    }
}
