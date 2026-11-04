package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

/**
 * SignalingQueue: Handles 10Hz telemetry backpressure (Point 2).
 * Refactored Revol31.9: Atomic size tracking and critical event protection (ISSUE-REVOL31.9-F1).
 * Enhanced Nov4.3: Partition tolerance deduplication and isBuffered flag setting.
 */
class SignalingQueue(private val maxCapacity: Int = 5000) {
    private val queue = ConcurrentLinkedDeque<LocationUpdate>()
    private val currentSize = AtomicInteger(0)
    private val seenSequenceIds = HashSet<Long>()

    /**
     * Offers a new update to the queue. If full, drops the oldest non-critical update.
     */
    fun offer(update: LocationUpdate) {
        // Partition tolerance: Mark as buffered
        update.isBuffered = true

        // Sequence deduplication for normal valid updates
        if (update.sequenceId > 0 && update.status == "VALID") {
            if (seenSequenceIds.contains(update.sequenceId)) {
                return // Ignore duplicate during partition buffering
            }
            seenSequenceIds.add(update.sequenceId)
        }

        if (currentSize.get() >= maxCapacity) {
            val removed = dropOldestNonCritical()
            if (removed) currentSize.decrementAndGet()
        }
        
        if (currentSize.get() < maxCapacity) {
            queue.offerLast(update)
            currentSize.incrementAndGet()
        }
    }

    private fun dropOldestNonCritical(): Boolean {
        val it = queue.iterator()
        while (it.hasNext()) {
            val next = it.next()
            if (next.status == "VALID") {
                seenSequenceIds.remove(next.sequenceId)
                it.remove()
                return true
            }
        }
        val polled = queue.pollFirst()
        if (polled != null) {
            seenSequenceIds.remove(polled.sequenceId)
        }
        return polled != null
    }

    /**
     * Returns a batch of updates for transmission.
     */
    fun pollBatch(batchSize: Int = 50): List<LocationUpdate> {
        val batch = mutableListOf<LocationUpdate>()
        repeat(batchSize) {
            queue.pollFirst()?.let { update ->
                seenSequenceIds.remove(update.sequenceId)
                batch.add(update)
                currentSize.decrementAndGet()
            }
        }
        return batch
    }

    fun size(): Int = currentSize.get()
    
    fun clear() {
        queue.clear()
        seenSequenceIds.clear()
        currentSize.set(0)
    }
}
