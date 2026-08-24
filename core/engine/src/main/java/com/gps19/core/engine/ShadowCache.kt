package com.gps19.core.engine

import java.util.LinkedHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * ShadowCache: A thread-safe LRU cache for system-level resource lookups.
 * Aug.22.04:
 * - Issue #280: Hardened LRU eviction strategy using ReentrantLock and 
 *   optimized initialCapacity to prevent race conditions and rehashing 
 *   stalls during 100Hz saturation bursts (R280).
 * Aug.20.00:
 * - Issue #217: Hardened thread-safety for atomic getOrPut operations.
 */
class ShadowCache<K, V>(private val maxCapacity: Int) {
    
    private val lock = ReentrantLock()
    
    // R280: Pre-calculate initial capacity to avoid rehashing during high-frequency bursts
    private val initialCapacity = (maxCapacity / 0.75f).toInt() + 1
    
    private val cache = object : LinkedHashMap<K, V>(initialCapacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            // R280: Structural changes and size checks are protected by the external lock
            return size > maxCapacity
        }
    }

    fun get(key: K): V? {
        lock.lock()
        try {
            return cache[key]
        } finally {
            lock.unlock()
        }
    }

    fun put(key: K, value: V) {
        lock.lock()
        try {
            cache[key] = value
        } finally {
            lock.unlock()
        }
    }

    fun getOrPut(key: K, defaultValue: () -> V): V {
        lock.lock()
        try {
            val cached = cache[key]
            if (cached != null) return cached
            
            val value = defaultValue()
            cache[key] = value
            return value
        } finally {
            lock.unlock()
        }
    }

    fun clear() {
        lock.lock()
        try {
            cache.clear()
        } finally {
            lock.unlock()
        }
    }

    fun size(): Int {
        lock.lock()
        try {
            return cache.size
        } finally {
            lock.unlock()
        }
    }
}
