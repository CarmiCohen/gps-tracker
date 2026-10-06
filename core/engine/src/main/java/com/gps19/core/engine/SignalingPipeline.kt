package com.gps19.core.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * SignalingDeltaState: Encapsulates reference coordinates for delta-encoding.
 */
class SignalingDeltaState {
    private val latE7 = AtomicInteger(0)
    private val lngE7 = AtomicInteger(0)

    fun getAndSetLat(value: Int): Int = latE7.getAndSet(value)
    fun getAndSetLng(value: Int): Int = lngE7.getAndSet(value)

    fun reset() {
        latE7.set(0)
        lngE7.set(0)
    }
}

/**
 * SignalingEncoder: Interface for transforming high-level objects to wire format.
 * Decouples engine from Protobuf (Rule 1.125).
 */
interface SignalingEncoder {
    fun encodeObject(update: LocationUpdate, deltaState: SignalingDeltaState, fromViewer: Boolean): ByteArray
}

/**
 * SignalingWireSink: Interface for the final wire-level emission (e.g. Socket.io).
 */
interface SignalingWireSink {
    fun emitJson(event: String, data: Map<String, Any?>)
    fun emitBinary(event: String, routingId: String, data: ByteArray)
}

/**
 * SignalingPipeline: Unified interface for signaling emission, conflation, and optimization.
 * Oct.6.20:
 * - Issue #SIGN-1006-12: SignalingPipeline Abstraction. Encapsulates conflation, 
 *   delta-encoding, and compression.
 */
interface SignalingPipeline {
    /**
     * Metrics: Performance counters for the signaling pipeline.
     */
    data class Metrics(val received: Long, val emitted: Long, val conflated: Long)

    /**
     * dispatchJson: Enqueues a JSON message for transmission.
     */
    fun dispatchJson(event: String, data: Map<String, Any?>, priority: SignalingPriority = SignalingPriority.NORMAL)

    /**
     * dispatchBinary: Enqueues a pre-serialized binary message for transmission.
     */
    fun dispatchBinary(event: String, routingId: String, data: ByteArray, priority: SignalingPriority = SignalingPriority.NORMAL)

    /**
     * dispatchObject: Enqueues a high-level telemetry object for transmission.
     */
    fun dispatchObject(event: String, update: LocationUpdate, priority: SignalingPriority = SignalingPriority.NORMAL, fromViewer: Boolean = false)
    
    /**
     * reset: Clears internal optimization state (e.g., delta encoding).
     */
    fun reset()

    /**
     * shutdown: Stops the pipeline and cancels all pending tasks.
     */
    fun shutdown()

    /**
     * reinitialize: Resets the pipeline with a new CoroutineScope.
     */
    fun reinitialize(scope: CoroutineScope)
    
    /**
     * metricsFlow: Observable stream of performance metrics.
     */
    val metricsFlow: StateFlow<Metrics>
}
