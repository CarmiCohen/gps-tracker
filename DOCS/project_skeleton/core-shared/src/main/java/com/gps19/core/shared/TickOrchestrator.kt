package com.gps19.core.shared

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * TickOrchestrator: Managed lifecycle for periodic loops.
 * Corrected: Uses Monotonic Time (nanoseconds) for loop integrity (Point 20).
 */
class TickOrchestrator {
    private val managedJobs = ConcurrentHashMap<String, Job>()
    private val preemptionSignals = ConcurrentHashMap<String, Channel<Unit>>()

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

        val job = scope.launch {
            while (isActive) {
                val startTimeNanos = System.nanoTime()
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

    fun preemptLoop(name: String) {
        preemptionSignals[name]?.trySend(Unit)
    }

    fun cancelAll() {
        managedJobs.values.forEach { it.cancel() }
        managedJobs.clear()
        preemptionSignals.clear()
    }
}
