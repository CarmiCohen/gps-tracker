package com.gps19.core.network

import com.gps19.core.shared.LocationUpdate
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * SignalingQueue: Handles 10Hz telemetry backpressure (Point 2).
 * Conflates data if the network cannot keep up with the sensing engine.
 */
class SignalingQueue(private val maxCapacity: Int = 100) {
    private val queue = ConcurrentLinkedDeque<LocationUpdate>()

    fun offer(update: LocationUpdate) {
        // If queue is full, remove the oldest non-critical update
        if (queue.size >= maxCapacity) {
            queue.pollFirst() 
        }
        queue.offerLast(update)
    }

    fun pollBatch(batchSize: Int = 10): List<LocationUpdate> {
        val batch = mutableListOf<LocationUpdate>()
        repeat(batchSize) {
            queue.pollFirst()?.let { batch.add(it) }
        }
        return batch
    }
}
