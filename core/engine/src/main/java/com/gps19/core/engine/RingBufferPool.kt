package com.gps19.core.engine

import java.util.concurrent.atomic.AtomicInteger

/**
 * RingBufferPool: A thread-safe, zero-allocation object pool for high-frequency telemetry entities.
 * Issue #1160: Flyweight & Pooling Expansion.
 */
class RingBufferPool<T>(
    val capacity: Int,
    private val factory: () -> T,
    private val resetter: (T) -> Unit
) {
    private val pool = Array<Any?>(capacity) { factory() }
    private val index = AtomicInteger(0)

    /**
     * acquire: Retrieves the next available object from the ring.
     * Note: This is a circular pool; it does not block or grow. It overwrites oldest entries
     * if the consumer is slower than the producer, which is acceptable for flyweight telemetry.
     */
    @Suppress("UNCHECKED_CAST")
    fun acquire(): T {
        val i = index.getAndIncrement() % capacity
        val obj = pool[i.let { if (it < 0) it + capacity else it }] as T
        resetter(obj)
        return obj
    }
}
