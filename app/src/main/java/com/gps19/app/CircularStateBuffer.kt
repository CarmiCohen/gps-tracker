package com.gps19.app

/**
 * CircularStateBuffer: A high-performance, zero-allocation circular buffer for forensic state snapshots.
 * Standardizes indexing around elapsedRealtime (RT) to prevent clock-drift issues.
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
     * Forensic sampling utility: Extracts a filtered sequence into a flyweight object.
     * R-ID 392: Thread-safe, multi-pass, zero-allocation sequence iteration.
     * The transform block is executed under lock to prevent data corruption if the 
     * writer overwrites the buffer object during extraction.
     */
    @Suppress("UNCHECKED_CAST")
    fun <R> forensicSequence(
        flyweight: R,
        predicate: (T) -> Boolean,
        transform: (T, R) -> Unit
    ): Sequence<R> = object : Sequence<R> {
        override fun iterator(): Iterator<R> = object : Iterator<R> {
            private var currentCount = 0
            private var startIdx = 0
            private var itemsInspected = 0
            private var nextItem: R? = null

            init {
                synchronized(this@CircularStateBuffer) {
                    currentCount = count
                    startIdx = if (currentCount == capacity) writeIdx else 0
                }
            }

            private fun findNext() {
                if (nextItem != null || itemsInspected >= currentCount) return
                synchronized(this@CircularStateBuffer) {
                    while (itemsInspected < currentCount) {
                        val idx = (startIdx + itemsInspected) % capacity
                        val item = buffer[idx] as T
                        itemsInspected++
                        if (predicate(item)) {
                            transform(item, flyweight)
                            nextItem = flyweight
                            return
                        }
                    }
                }
            }

            override fun hasNext(): Boolean {
                findNext()
                return nextItem != null
            }

            override fun next(): R {
                findNext()
                val item = nextItem ?: throw NoSuchElementException()
                nextItem = null
                return item
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
