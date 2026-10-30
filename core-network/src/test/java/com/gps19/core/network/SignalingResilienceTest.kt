package com.gps19.core.network

import com.gps19.core.shared.LocationUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SignalingResilienceTest: Verifies Multi-Peer Signaling Resilience (ISSUE-REVOL30.0-F2).
 * Exercises the SignalingQueue conflation logic under high-pressure telemetry bursts.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignalingResilienceTest {

    @Test
    fun verifyQueueConflationUnderBurst() = runTest {
        val queue = SignalingQueue(maxCapacity = 10)
        
        // Fill queue with 20 updates (capacity is 10)
        repeat(20) { i ->
            queue.offer(LocationUpdate(sequenceId = i.toLong()))
        }
        
        val batch = queue.pollBatch(10)
        
        // Should contain the LAST 10 updates (10 to 19)
        assertEquals(10, batch.size)
        assertEquals(10L, batch[0].sequenceId)
        assertEquals(19L, batch.last().sequenceId)
    }

    @Test
    fun verifyBatchPollingEfficiency() = runTest {
        val queue = SignalingQueue(maxCapacity = 100)
        repeat(50) { i ->
            queue.offer(LocationUpdate(sequenceId = i.toLong()))
        }
        
        val batch1 = queue.pollBatch(30)
        assertEquals(30, batch1.size)
        assertEquals(0L, batch1[0].sequenceId)
        
        val batch2 = queue.pollBatch(30)
        assertEquals(20, batch2.size)
        assertEquals(30L, batch2[0].sequenceId)
    }
}
