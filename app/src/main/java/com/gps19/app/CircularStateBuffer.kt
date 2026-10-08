package com.gps19.app

/**
 * CircularStateBuffer: A high-performance, zero-allocation circular buffer for forensic state snapshots.
 * Standardizes indexing around elapsedRealtime (RT) to prevent clock-drift issues.
 * Oct.8.8:
 * - Issue #SIMP-1012-1: Forensic Retrieval Optimization. Replaced Sequence-based 
 *   forensicSequence with inline forEachMatch and forEachDescending to achieve 
 *   truly zero-allocation parity (R-ID 392).
 * Sep.21.125:
 * - Issue #1153/1154: Hardened forensicSequence for thread safety and zero-allocation parity.
 *   Replaced toList() snapshot with a multi-pass custom Sequence implementation that holds 
 *   locks during flyweight transformation (R-ID 392).
 * - Refactored forEach to be truly zero-allocation by holding internal lock during iteration.
 * Sep.21.124:
 * - Added forensicSequence utility to support flyweight-based sampling across HardwareSuite.
 * Sep.06.17:
 * - Issue #922: Initial implementation for Forensic Buffering and Clock Parity.
 */
class CircularStateBuffer<T>(
    val capacity: Int,
    private val factory: () -> T,
    private val resetter: (T) -> Unit
) {
    @PublishedApi internal val buffer: Array<Any?> = Array(capacity) { factory() }
    @PublishedApi internal var writeIdx = 0
    @PublishedApi internal var count = 0

    @Suppress("UNCHECKED_CAST")
    fun next(): T {
        synchronized(this) {
            val item = buffer[writeIdx] as T
            resetter(item)
            writeIdx = (writeIdx + 1) % capacity
            if (count < capacity) count++
            return item
        }
    }

    /**
     * Executes an operation for each valid item in the buffer, from oldest to newest.
     * R-ID 392: Zero-allocation iteration under internal lock.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun forEach(action: (T) -> Unit) {
        synchronized(this) {
            val currentCount = count
            val startIdx = if (currentCount == capacity) writeIdx else 0
            for (i in 0 until currentCount) {
                val idx = (startIdx + i) % capacity
                action(buffer[idx] as T)
            }
        }
    }

    /**
     * Executes an operation for each valid item in the buffer, from newest to oldest.
     * R-ID 392: Zero-allocation reverse iteration under internal lock.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun forEachDescending(action: (T) -> Unit) {
        synchronized(this) {
            val currentCount = count
            val lastIdx = (writeIdx - 1 + capacity) % capacity
            for (i in 0 until currentCount) {
                val idx = (lastIdx - i + capacity) % capacity
                action(buffer[idx] as T)
            }
        }
    }

    /**
     * Forensic sampling utility: Executes an action on matching samples.
     * R-ID 392: Truly zero-allocation iteration without Sequence or Iterator overhead.
     * The action is executed under internal lock to ensure atomicity.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun forEachMatch(
        predicate: (T) -> Boolean,
        action: (T) -> Unit
    ) {
        synchronized(this) {
            val currentCount = count
            val startIdx = if (currentCount == capacity) writeIdx else 0
            for (i in 0 until currentCount) {
                val idx = (startIdx + i) % capacity
                val item = buffer[idx] as T
                if (predicate(item)) {
                    action(item)
                }
            }
        }
    }

    fun size() = synchronized(this) { count }
    fun clear() {
        synchronized(this) {
            writeIdx = 0
            count = 0
        }
    }
}
