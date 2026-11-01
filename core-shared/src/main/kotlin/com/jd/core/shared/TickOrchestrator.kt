package com.jd.core.shared

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * TickOrchestrator: Managed lifecycle for periodic loops.
 * Corrected: Uses Monotonic Time (nanoseconds) for loop integrity (Point 20).
 * Updated Nov1.5: Added heartbeat monitoring for Forensic Watchdog (R-ID 85).
 */
class TickOrchestrator {
    private val managedJobs = ConcurrentHashMap<String, Job>()
    private val preemptionSignals = ConcurrentHashMap<String, Channel<Unit>>()
    private val lastTickNanos = ConcurrentHashMap<String, AtomicLong>()

    /**
     * Launches a periodic loop with preemption support.
     */
    fun launchPeriodicLoop(
        name: String,
        scope: CoroutineScope,
        intervalProvider: () -> Long,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        managedJobs[name]?.cancel()
        val signal = Channel<Unit>(capacity = Channel.CONFLATED)
        preemptionSignals[name] = signal
        lastTickNanos[name] = AtomicLong(System.nanoTime())

        val job = scope.launch {
            while (isActive) {
                val startTimeNanos = System.nanoTime()
                lastTickNanos[name]?.set(startTimeNanos)
                
                block()
                
                val elapsedMs = (System.nanoTime() - startTimeNanos) / 1_000_000
                val nextInterval = intervalProvider()
                val remaining = max(10L, nextInterval - elapsedMs)
                
                withTimeoutOrNull(remaining) {
                    signal.receive()
                }
            }
        }
        managedJobs[name] = job
        return job
    }

    /**
     * Returns the time since the last tick in nanoseconds for a specific loop.
     * Returns -1 if the loop is not running.
     */
    fun getNanosecondsSinceLastTick(name: String): Long {
        val lastTick = lastTickNanos[name]?.get() ?: return -1L
        return System.nanoTime() - lastTick
    }

    fun preemptLoop(name: String) {
        preemptionSignals[name]?.trySend(Unit)
    }

    fun cancelAll() {
        managedJobs.values.forEach { it.cancel() }
        managedJobs.clear()
        preemptionSignals.clear()
        lastTickNanos.clear()
    }
}
