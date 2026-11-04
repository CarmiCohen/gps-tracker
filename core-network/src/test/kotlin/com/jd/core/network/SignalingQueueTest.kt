package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalingQueueTest {

    @Test
    fun testQueueConflation() {
        val queue = SignalingQueue(maxCapacity = 10)
        
        // Fill and exceed capacity
        for (i in 1..15) {
            queue.offer(LocationUpdate(sequenceId = i.toLong()))
        }
        
        // Should contain the last 10 updates: 6 to 15
        val batch = queue.pollBatch(15)
        assertEquals(10, batch.size)
        assertEquals(6L, batch.first().sequenceId)
        assertEquals(15L, batch.last().sequenceId)
    }

    @Test
    fun testBatchPolling() {
        val queue = SignalingQueue(maxCapacity = 100)
        for (i in 1..25) {
            queue.offer(LocationUpdate(sequenceId = i.toLong()))
        }
        
        val batch1 = queue.pollBatch(10)
        assertEquals(10, batch1.size)
        assertEquals(1L, batch1.first().sequenceId)
        
        val batch2 = queue.pollBatch(20)
        assertEquals(15, batch2.size)
        assertEquals(11L, batch2.first().sequenceId)
        assertEquals(25L, batch2.last().sequenceId)
    }

    @Test
    fun testPartitionDeduplicationAndBufferFlag() {
        val queue = SignalingQueue(maxCapacity = 100)
        
        // Offer duplicate sequence IDs during partition
        queue.offer(LocationUpdate(sequenceId = 10L))
        queue.offer(LocationUpdate(sequenceId = 10L)) // duplicate should be ignored
        queue.offer(LocationUpdate(sequenceId = 11L))
        
        assertEquals(2, queue.size())
        val batch = queue.pollBatch(10)
        assertEquals(2, batch.size)
        assertTrue(batch[0].isBuffered)
        assertEquals(10L, batch[0].sequenceId)
        assertTrue(batch[1].isBuffered)
        assertEquals(11L, batch[1].sequenceId)
    }
}
