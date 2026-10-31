package com.gps19.core.shared

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ObjectPool: Implements the Flyweight pattern to satisfy the 
 * "Zero-Allocation" requirement in the 100Hz sensing loop (Point 2).
 */
class ObjectPool<T>(private val factory: () -> T, private val cleaner: (T) -> Unit) {
    private val pool = ConcurrentLinkedQueue<T>()

    fun acquire(): T {
        return pool.poll() ?: factory()
    }

    fun release(obj: T) {
        cleaner(obj)
        pool.offer(obj)
    }
}

object PoolRegistry {
    val locationUpdatePool = ObjectPool(
        factory = { LocationUpdate() },
        cleaner = { it.reset() }
    )
}
