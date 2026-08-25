package com.gps19.core.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ShadowCacheTest {

    @Test
    fun testConcurrentAccess() = runBlocking {
        val cache = ShadowCache<Int, String>(10)
        val threads = 20
        val iterations = 1000

        val jobs = List(threads) { t ->
            launch(Dispatchers.Default) {
                repeat(iterations) { i ->
                    val key = (t * iterations + i) % 20 // High collision and eviction
                    cache.getOrPut(key) { "Value-$key" }
                }
            }
        }
        jobs.forEach { it.join() }

        // Size should never exceed capacity
        assert(cache.size() <= 10)
    }

    @Test
    fun testLockContentionDuringEviction() = runBlocking {
        val cache = ShadowCache<Int, Int>(5)
        val hitCount = AtomicInteger(0)

        // Fill the cache
        repeat(5) { cache.put(it, it) }

        val jobs = List(10) { t ->
            launch(Dispatchers.Default) {
                repeat(100) { i ->
                    val key = i % 10 // Constant eviction
                    cache.getOrPut(key) {
                        hitCount.incrementAndGet()
                        Thread.sleep(1) // Simulate slow producer
                        key
                    }
                }
            }
        }
        jobs.forEach { it.join() }
        
        assert(cache.size() <= 5)
    }

    @Test
    fun testLruEvictionOrder() {
        val cache = ShadowCache<Int, String>(3)
        
        cache.put(1, "A")
        cache.put(2, "B")
        cache.put(3, "C")
        
        // Access 1 to move it to the end (most recent)
        cache.get(1)
        
        // Add 4, should evict 2 (least recently used)
        cache.put(4, "D")
        
        assertEquals("A", cache.get(1))
        assertNull(cache.get(2))
        assertEquals("C", cache.get(3))
        assertEquals("D", cache.get(4))
    }
}
