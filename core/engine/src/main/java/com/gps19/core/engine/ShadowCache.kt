package com.gps19.core.engine

import java.util.LinkedHashMap

/**
 * ShadowCache: A thread-safe LRU cache for system-level resource lookups.
 * Aug.20.00:
 * - Issue #217: Hardened thread-safety for atomic getOrPut operations.
 * Aug.19.13:
 * - Issue #217: Shadow-Cache Eviction Strategy. Implemented LRU-based eviction 
 *   to prevent unbounded memory growth during multi-day sessions (R217).
 */
class ShadowCache<K, V>(private val maxCapacity: Int) {
    
    private val lock = Any()
    private val cache = object : LinkedHashMap<K, V>(maxCapacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxCapacity
        }
    }

    fun get(key: K): V? = synchronized(lock) {
        cache[key]
    }

    fun put(key: K, value: V) = synchronized(lock) {
        cache[key] = value
    }

    fun getOrPut(key: K, defaultValue: () -> V): V = synchronized(lock) {
        val cached = cache[key]
        if (cached != null) return cached
        
        val value = defaultValue()
        cache[key] = value
        return value
    }

    fun clear() = synchronized(lock) {
        cache.clear()
    }

    fun size(): Int = synchronized(lock) {
        cache.size
    }
}
