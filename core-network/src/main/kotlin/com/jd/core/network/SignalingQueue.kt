package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * SignalingQueue: Handles 10Hz telemetry backpressure (Point 2).
 * Refactored Revol31.9: Atomic size tracking and critical event protection (ISSUE-REVOL31.9-F1).
 * Enhanced Nov4.3: Partition tolerance deduplication and isBuffered flag setting.
 * Hardened Nov7.11 (ISSUE-NOV6.0-O24): Thread-safe queue operations via ReentrantLock and order-preserving pushFront on failed flush.
 * Nov7.21 (SIMP-NOV7.19-S1): criticality comes from [LocationUpdate.isCritical], not from the status text.
 */
class SignalingQueue(private val maxCapacity: Int = 5000) {
    private val queue = ConcurrentLinkedDeque<LocationUpdate>()
    private val currentSize = AtomicInteger(0)
    private val seenSequenceIds = HashSet<Long>()
    private val lock = ReentrantLock()

    /**
     * Offers a new update to the queue. If full, drops the oldest non-critical update.
     */
    fun offer(update: LocationUpdate) = lock.withLock {
        // Partition tolerance: Mark as buffered
        update.isBuffered = true

        // Sequence deduplication for ordinary updates
        if (update.sequenceId > 0 && !update.isCritical) {
            if (seenSequenceIds.contains(update.sequenceId)) {
                return@withLock // Ignore duplicate during partition buffering
            }
            seenSequenceIds.add(update.sequenceId)
        }

        if (currentSize.get() >= maxCapacity) {
            val removed = dropOldestNonCriticalLocked()
            if (removed) currentSize.decrementAndGet()
        }
        
        if (currentSize.get() < maxCapacity) {
            queue.offerLast(update)
            currentSize.incrementAndGet()
        }
    }

    /**
     * Pushes a batch of failed updates back to the front of the queue in their original order.
     */
    fun pushFront(updates: List<LocationUpdate>) = lock.withLock {
        // Push in reverse order so updates[0] ends up at the very front of queue
        for (update in updates.asReversed()) {
            update.isBuffered = true
            if (update.sequenceId > 0 && !update.isCritical) {
                seenSequenceIds.add(update.sequenceId)
            }
            if (currentSize.get() < maxCapacity) {
                queue.offerFirst(update)
                currentSize.incrementAndGet()
            }
        }
    }

    private fun dropOldestNonCriticalLocked(): Boolean {
        val it = queue.iterator()
        while (it.hasNext()) {
            val next = it.next()
            if (!next.isCritical) {
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
    fun pollBatch(batchSize: Int = 50): List<LocationUpdate> = lock.withLock {
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
    
    fun clear() = lock.withLock {
        queue.clear()
        seenSequenceIds.clear()
        currentSize.set(0)
    }
}
