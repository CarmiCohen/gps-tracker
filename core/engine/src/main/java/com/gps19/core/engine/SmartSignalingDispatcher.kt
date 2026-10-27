package com.gps19.core.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.selects.select
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * SmartSignalingDispatcher: Unified reactive coordination layer for signaling.
 * Oct.27.1:
 * - ISSUE-OCT27.0-1: Constant Alignment. Removed internal companion constants 
 *   to use centralized EngineConstants.kt, resolving conflation audit failures.
 * Oct.14.6:
 * - Issue #SIMP-OCT13.20-S1: Unified Signaling Payload Conflation logic.
 */
class SmartSignalingDispatcher(
    private var scope: CoroutineScope,
    private val isViolationProvider: () -> Boolean,
    private val wireSink: SignalingWireSink,
    private val encoder: SignalingEncoder,
    private val isConnectedProvider: () -> Boolean,
    private val rttProvider: () -> Int = { 0 },
    private val performanceTier: PerformanceTier = PerformanceTier.STANDARD,
    private val timeProvider: TimeProvider = object : TimeProvider {
        override val currentTimeMillis: Long get() = System.currentTimeMillis()
        override val elapsedRealtime: Long get() = System.currentTimeMillis()
    },
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SignalingPipeline {

    sealed class Command {
        abstract val priority: SignalingPriority
        data class Json(val event: String, val data: Map<String, Any?>, override val priority: SignalingPriority) : Command()
        data class Binary(val event: String, val routingId: String, val data: ByteArray, override val priority: SignalingPriority) : Command()
        data class Object(val event: String, val update: LocationUpdate, val fromViewer: Boolean, override val priority: SignalingPriority) : Command()
    }

    private class ConflationBucket<T> {
        val pending = AtomicReference<T?>(null)
        val scheduledTs = AtomicLong(0)
        val firstEntryTs = AtomicLong(0)
        val burstCount = AtomicInteger(0)

        fun reset() {
            pending.set(null)
            scheduledTs.set(0)
            firstEntryTs.set(0)
            burstCount.set(0)
        }
    }

    private var criticalQueue = Channel<Command>(capacity = SIGNALING_QUEUE_LIMIT_CRITICAL, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var highQueue = Channel<Command>(capacity = SIGNALING_QUEUE_LIMIT_HIGH, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var normalQueue = Channel<Command>(capacity = SIGNALING_QUEUE_LIMIT_NORMAL, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    
    private val locMapBucket = ConflationBucket<Map<String, Any?>>()
    private val locObjBucket = ConflationBucket<LocationUpdate>()
    private val logBucket = ConflationBucket<Map<String, Any?>>()

    private var conflationSignal = Channel<Unit>(capacity = Channel.CONFLATED)
    private var conflationJob: Job? = null

    private val framesReceived = AtomicLong(0)
    private val framesEmitted = AtomicLong(0)
    private val framesConflated = AtomicLong(0)

    private val deltaState = SignalingDeltaState()

    private val _metricsFlow = MutableStateFlow(SignalingPipeline.Metrics(0, 0, 0))
    override val metricsFlow: StateFlow<SignalingPipeline.Metrics> = _metricsFlow.asStateFlow()

    private var processorJob: Job? = null
    
    @Volatile
    private var lastNormalEmitTs = 0L

    init {
        startProcessor()
        startConflationLoop()
    }

    override fun reinitialize(scope: CoroutineScope) {
        if (processorJob?.isActive == true) return
        
        shutdown()
        this.scope = scope
        criticalQueue = Channel(capacity = SIGNALING_QUEUE_LIMIT_CRITICAL, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        highQueue = Channel(capacity = SIGNALING_QUEUE_LIMIT_HIGH, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        normalQueue = Channel(capacity = SIGNALING_QUEUE_LIMIT_NORMAL, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        conflationSignal = Channel(capacity = Channel.CONFLATED)
        
        locMapBucket.reset()
        locObjBucket.reset()
        logBucket.reset()
        
        startProcessor()
        startConflationLoop()
    }

    override fun reset() {
        deltaState.reset()
    }

    private fun startProcessor() {
        processorJob?.cancel()
        processorJob = scope.launch(dispatcher) {
            while (isActive) {
                while (!isConnectedProvider() && isActive) {
                    delay(1000)
                }
                if (!isActive) break

                try {
                    val command = criticalQueue.tryReceive().getOrNull() 
                        ?: highQueue.tryReceive().getOrNull() 
                        ?: select<Command> {
                            criticalQueue.onReceive { it }
                            highQueue.onReceive { it }
                            normalQueue.onReceive { it }
                        }

                    if (command.priority == SignalingPriority.NORMAL) {
                        val now = timeProvider.currentTimeMillis
                        val rtt = rttProvider()
                        val isStaggered = performanceTier == PerformanceTier.STAGGERED
                        
                        var delayMs = if (isViolationProvider()) SIGNALING_EMIT_DELAY_VIOLATION_MS else SIGNALING_EMIT_DELAY_MS
                        
                        if (isStaggered) delayMs = (delayMs * 1.5).toLong()
                        if (rtt > 500) delayMs = (delayMs * 2.0).toLong()
                        
                        val elapsed = now - lastNormalEmitTs
                        if (elapsed < delayMs) {
                            val waitTime = delayMs - elapsed
                            val fastReady = withTimeoutOrNull(waitTime) {
                                select<Command?> {
                                    criticalQueue.onReceive { it }
                                    highQueue.onReceive { it }
                                }
                            }
                            
                            if (fastReady != null) {
                                emit(fastReady)
                                normalQueue.trySend(command) 
                                continue
                            }
                        }
                        lastNormalEmitTs = timeProvider.currentTimeMillis
                    }

                    emit(command)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (e is ClosedReceiveChannelException) break
                    delay(100)
                }
            }
        }
    }

    private fun startConflationLoop() {
        conflationJob?.cancel()
        conflationJob = scope.launch(dispatcher) {
            try {
                while (isActive) {
                    conflationSignal.receive()
                    
                    while (isActive) {
                        val now = timeProvider.currentTimeMillis
                        var nextCheck = Long.MAX_VALUE
                        
                        nextCheck = minOf(nextCheck, checkBucket(locMapBucket, now) { 
                            Command.Json("location_update", it, SignalingPriority.NORMAL) 
                        })

                        nextCheck = minOf(nextCheck, checkBucket(locObjBucket, now) { 
                            Command.Object("location_update_bin", it, false, SignalingPriority.NORMAL) 
                        })

                        nextCheck = minOf(nextCheck, checkBucket(logBucket, now) { 
                            Command.Json("log_update", it, SignalingPriority.NORMAL) 
                        })

                        if (nextCheck == Long.MAX_VALUE) break
                        
                        val waitTime = nextCheck.coerceAtLeast(10L)
                        try {
                            withTimeoutOrNull(waitTime) {
                                conflationSignal.receive()
                            }
                        } catch (e: ClosedReceiveChannelException) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException || e is ClosedReceiveChannelException) return@launch
                delay(1000) 
            }
        }
    }

    private fun <T> checkBucket(bucket: ConflationBucket<T>, now: Long, commandFactory: (T) -> Command): Long {
        val ts = bucket.scheduledTs.get()
        if (ts > 0) {
            if (now >= ts) {
                bucket.scheduledTs.set(0)
                bucket.firstEntryTs.set(0)
                bucket.burstCount.set(0)
                bucket.pending.getAndSet(null)?.let {
                    enqueue(commandFactory(it))
                }
            } else {
                return ts - now
            }
        }
        return Long.MAX_VALUE
    }

    private fun emit(command: Command) {
        val emitted = framesEmitted.incrementAndGet()
        updateMetrics(emitted = emitted)
        when (command) {
            is Command.Json -> wireSink.emitJson(command.event, command.data)
            is Command.Binary -> wireSink.emitBinary(command.event, command.routingId, command.data)
            is Command.Object -> {
                val payload = encoder.encodeObject(command.update, deltaState, command.fromViewer)
                wireSink.emitBinary(command.event, SignalingConstants.getTransmissionId(command.update.deviceId), payload)
            }
        }
    }

    override fun dispatchJson(event: String, data: Map<String, Any?>, priority: SignalingPriority) {
        dispatch(Command.Json(event, data, priority))
    }

    override fun dispatchBinary(event: String, routingId: String, data: ByteArray, priority: SignalingPriority) {
        dispatch(Command.Binary(event, routingId, data, priority))
    }

    override fun dispatchObject(event: String, update: LocationUpdate, priority: SignalingPriority, fromViewer: Boolean) {
        dispatch(Command.Object(event, update, fromViewer, priority))
    }

    private fun dispatch(command: Command) {
        val received = framesReceived.incrementAndGet()
        updateMetrics(received = received)
        when (command) {
            is Command.Json -> {
                if (command.event == "location_update" && command.priority == SignalingPriority.NORMAL) {
                    dispatchConflated(
                        bucket = locMapBucket,
                        incoming = command.data,
                        conflater = ::conflateMaps,
                        flushCommandFactory = { Command.Json("location_update", it, SignalingPriority.NORMAL) }
                    )
                } else if (command.event == "log_update" && command.priority == SignalingPriority.NORMAL) {
                    dispatchConflated(
                        bucket = logBucket,
                        incoming = command.data,
                        conflater = ::conflateLogMaps,
                        shouldFlush = { pending, new ->
                            (pending?.get("message") as? String) != (new["message"] as? String)
                        },
                        flushCommandFactory = { Command.Json("log_update", it, SignalingPriority.NORMAL) },
                        delayMultiplier = 2
                    )
                } else {
                    enqueue(command)
                }
            }
            is Command.Object -> {
                if (command.event == "location_update_bin" && command.priority == SignalingPriority.NORMAL) {
                    dispatchConflated(
                        bucket = locObjBucket,
                        incoming = command.update,
                        conflater = ::conflateLocationUpdates,
                        flushCommandFactory = { Command.Object("location_update_bin", it, false, SignalingPriority.NORMAL) }
                    )
                } else {
                    enqueue(command)
                }
            }
            is Command.Binary -> enqueue(command)
        }
    }

    private fun enqueue(command: Command) {
        when (command.priority) {
            SignalingPriority.CRITICAL -> criticalQueue.trySend(command)
            SignalingPriority.HIGH -> highQueue.trySend(command)
            SignalingPriority.NORMAL -> normalQueue.trySend(command)
        }
    }

    private fun updateMetrics(received: Long? = null, emitted: Long? = null, conflated: Long? = null) {
        _metricsFlow.update { current ->
            SignalingPipeline.Metrics(
                received = received ?: current.received,
                emitted = emitted ?: current.emitted,
                conflated = conflated ?: current.conflated
            )
        }
    }

    private fun <T> dispatchConflated(
        bucket: ConflationBucket<T>,
        incoming: T,
        conflater: (T?, T) -> T,
        shouldFlush: (T?, T) -> Boolean = { _, _ -> false },
        flushCommandFactory: (T) -> Command,
        delayMultiplier: Int = 1
    ) {
        val existing = bucket.pending.get()
        if (existing != null && shouldFlush(existing, incoming)) {
            bucket.pending.getAndSet(null)?.let {
                enqueue(flushCommandFactory(it))
            }
            bucket.reset()
        }

        var wasConflated = false
        bucket.pending.updateAndGet { current ->
            if (current != null) wasConflated = true
            conflater(current, incoming)
        }

        if (wasConflated) {
            val conf = framesConflated.incrementAndGet()
            updateMetrics(conflated = conf)
            bucket.burstCount.incrementAndGet()
        }

        updateBucketSchedule(bucket, delayMultiplier)
    }

    private fun <T> updateBucketSchedule(bucket: ConflationBucket<T>, delayMultiplier: Int = 1) {
        val now = timeProvider.currentTimeMillis
        if (bucket.scheduledTs.get() == 0L) {
            bucket.firstEntryTs.set(now)
            val delayMs = calculateDelay(bucket.burstCount.get()) * delayMultiplier
            bucket.scheduledTs.set(now + delayMs)
            conflationSignal.trySend(Unit)
        } else {
            val currentScheduled = bucket.scheduledTs.get()
            val firstArrival = bucket.firstEntryTs.get()
            
            val starvationCap = calculateStarvationCap()
            if (now - firstArrival >= starvationCap) return

            val newDelay = calculateDelay(bucket.burstCount.get()) * delayMultiplier
            val targetScheduled = now + newDelay
            
            val finalTarget = minOf(targetScheduled, firstArrival + starvationCap)
            
            if (currentScheduled < finalTarget && finalTarget - now >= newDelay / 2) {
                 bucket.scheduledTs.compareAndSet(currentScheduled, finalTarget)
            }
        }
    }

    private fun calculateDelay(burstCount: Int): Long {
        val rtt = rttProvider()
        val isStaggered = performanceTier == PerformanceTier.STAGGERED
        
        var baseDelay = if (isViolationProvider()) SIGNALING_CONFLATION_DELAY_VIOLATION_MS else SIGNALING_CONFLATION_DELAY_MS
        
        if (isStaggered) baseDelay = (baseDelay * 1.5).toLong()
        if (rtt > 500) baseDelay = (baseDelay * 2.0).toLong()
        
        val burstPressureThreshold = 5
        if (burstCount < burstPressureThreshold) return baseDelay
        
        val pressureBonus = (burstCount - burstPressureThreshold) * 100L
        val maxDelay = calculateStarvationCap()
        
        return minOf(baseDelay + pressureBonus, maxDelay)
    }
    
    private fun calculateStarvationCap(): Long {
        val rtt = rttProvider()
        val isStaggered = performanceTier == PerformanceTier.STAGGERED
        
        var cap = MAX_CONFLATION_DELAY_MS
        if (isStaggered) cap = (cap * 1.5).toLong()
        if (rtt > 800) cap = (cap * 2.0).toLong()
        
        return cap
    }

    private fun conflateMaps(pending: Map<String, Any?>?, incoming: Map<String, Any?>): Map<String, Any?> {
        if (pending == null) return incoming
        val merged = pending.toMutableMap()
        merged.putAll(incoming)
        merged["timestamp"] = incoming["timestamp"] 
        return merged
    }

    private fun conflateLocationUpdates(pending: LocationUpdate?, incoming: LocationUpdate): LocationUpdate {
        if (pending == null) return incoming
        
        val pF = pending.integrity.forensic
        val iF = incoming.integrity.forensic
        
        if (iF.thermal == null && pF.thermal != null) iF.thermal = pF.thermal
        if (iF.heap == null && pF.heap != null) iF.heap = pF.heap
        if (iF.snr == null && pF.snr != null) iF.snr = pF.snr
        if (iF.vibe == null && pF.vibe != null) iF.vibe = pF.vibe

        if (incoming.integrity.uptimeMs == 0L && pending.integrity.uptimeMs > 0) {
            incoming.integrity.uptimeMs = pending.integrity.uptimeMs
        }
        return incoming
    }

    private fun conflateLogMaps(pending: Map<String, Any?>?, incoming: Map<String, Any?>): Map<String, Any?> {
        if (pending == null) return incoming
        
        val pendingMsg = pending["message"] as? String
        val incomingMsg = incoming["message"] as? String
        
        return if (pendingMsg != null && pendingMsg == incomingMsg) {
            val merged = pending.toMutableMap()
            val currentCount = (merged["burst_count"] as? Int) ?: 1
            merged["burst_count"] = currentCount + 1
            merged["timestamp"] = incoming["timestamp"]
            merged
        } else {
            incoming
        }
    }

    override fun shutdown() {
        processorJob?.cancel()
        conflationJob?.cancel()
        criticalQueue.close()
        highQueue.close()
        normalQueue.close()
        conflationSignal.close()
    }
}
