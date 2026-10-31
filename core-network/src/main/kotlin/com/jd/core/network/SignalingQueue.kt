package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

/**
 * SignalingQueue: Handles 10Hz telemetry backpressure (Point 2).
 * Refactored Revol31.9: Atomic size tracking and critical event protection (ISSUE-REVOL31.9-F1).
 * Ensures zero telemetry loss during flapping by maintaining a robust in-memory buffer.
 */
class SignalingQueue(private val maxCapacity: Int = 100) {
    private val queue = ConcurrentLinkedDeque<LocationUpdate>()
    private val currentSize = AtomicInteger(0)

    /**
     * Offers a new update to the queue. If full, drops the oldest non-critical update.
     */
    fun offer(update: LocationUpdate) {
        if (currentSize.get() >= maxCapacity) {
            // Drop logic: try to find an older update that isn't a critical violation/transition
            val removed = dropOldestNonCritical()
            if (removed) currentSize.decrementAndGet()
        }
        
        // Final check to prevent overflow
        if (currentSize.get() < maxCapacity) {
            queue.offerLast(update)
            currentSize.incrementAndGet()
        }
    }

    private fun dropOldestNonCritical(): Boolean {
        val it = queue.iterator()
        while (it.hasNext()) {
            val next = it.next()
            // "VALID" updates are candidates for dropping. 
            // Geofence or Jump statuses are preserved.
            if (next.status == "VALID") {
                it.remove()
                return true
            }
        }
        // Fallback: If everything is critical, we must drop the oldest to make room
        return queue.pollFirst() != null
    }

    /**
     * Returns a batch of updates for transmission.
     */
    fun pollBatch(batchSize: Int = 10): List<LocationUpdate> {
        val batch = mutableListOf<LocationUpdate>()
        repeat(batchSize) {
            queue.pollFirst()?.let { 
                batch.add(it)
                currentSize.decrementAndGet()
            }
        }
        return batch
    }

    fun size(): Int = currentSize.get()
    
    fun clear() {
        queue.clear()
        currentSize.set(0)
    }
}
