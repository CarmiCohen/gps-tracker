package com.gps19.app

/**
 * CircularStateBuffer: A high-performance, zero-allocation circular buffer for forensic state snapshots.
 * Standardizes indexing around elapsedRealtime (RT) to prevent clock-drift issues.
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
        val item = buffer[writeIdx] as T
        resetter(item)
        writeIdx = (writeIdx + 1) % capacity
        if (count < capacity) count++
        return item
    }

    /**
     * Executes an operation for each valid item in the buffer, from oldest to newest.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun forEach(action: (T) -> Unit) {
        val currentCount = count
        val startIdx = if (currentCount == capacity) writeIdx else 0
        for (i in 0 until currentCount) {
            val idx = (startIdx + i) % capacity
            action(buffer[idx] as T)
        }
    }

    /**
     * Returns a sequence of items, intended for forensic sampling.
     */
    @Suppress("UNCHECKED_CAST")
    fun asSequence(): Sequence<T> = sequence {
        val currentCount = count
        val startIdx = if (currentCount == capacity) writeIdx else 0
        for (i in 0 until currentCount) {
            val idx = (startIdx + i) % capacity
            yield(buffer[idx] as T)
        }
    }

    fun size() = count
    fun clear() {
        writeIdx = 0
        count = 0
    }
}
