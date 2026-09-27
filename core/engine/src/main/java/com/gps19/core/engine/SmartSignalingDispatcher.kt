package com.gps19.core.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicReference

/**
 * SmartSignalingDispatcher: Unified reactive coordination layer for signaling.
 * Sep.27.7:
 * - Issue #1172: Smart Signaling Dispatcher. Merged conflation, queue processing, 
 *   and adaptive throttling into a transport-agnostic coordination layer.
 */
class SmartSignalingDispatcher(
    private val scope: CoroutineScope,
    private val isViolationProvider: () -> Boolean,
    private val jsonSink: (String, Map<String, Any?>) -> Unit,
    private val binarySink: (String, String, ByteArray) -> Unit,
    private val isConnectedProvider: () -> Boolean
) {
    sealed class Command {
        data class Json(val event: String, val data: Map<String, Any?>, val priority: SignalingPriority) : Command()
        data class Binary(val event: String, val routingId: String, val data: ByteArray, val priority: SignalingPriority) : Command()
    }

    private val queue = Channel<Command>(capacity = Channel.UNLIMITED)
    private val pendingLocationMap = AtomicReference<Map<String, Any?>?>(null)
    private var conflationJob: Job? = null
    private var processorJob: Job? = null

    init {
        startProcessor()
    }

    private fun startProcessor() {
        processorJob?.cancel()
        processorJob = scope.launch(Dispatchers.Default) {
            for (command in queue) {
                while (!isConnectedProvider()) {
                    delay(1000)
                }
                
                when (command) {
                    is Command.Json -> jsonSink(command.event, command.data)
                    is Command.Binary -> binarySink(command.event, command.routingId, command.data)
                }

                val delayMs = if (isViolationProvider()) SIGNALING_EMIT_DELAY_VIOLATION_MS else SIGNALING_EMIT_DELAY_MS
                delay(delayMs)
            }
        }
    }

    fun dispatch(command: Command) {
        when (command) {
            is Command.Json -> {
                if (command.priority == SignalingPriority.HIGH) {
                    // High priority JSON (Pings, Commands) bypasses the queue for minimum latency
                    // but still respects connection state via the sink implementation if needed.
                    // However, we want ordered delivery for some things.
                    // Requirement #1172 says "Merge conflation and throttling".
                    // CommunicationManager previously sent HIGH priority immediately.
                    jsonSink(command.event, command.data)
                } else if (command.event == "location_update") {
                    dispatchConflated(command.data)
                } else {
                    queue.trySend(command)
                }
            }
            is Command.Binary -> {
                if (command.priority == SignalingPriority.HIGH) {
                    binarySink(command.event, command.routingId, command.data)
                } else {
                    queue.trySend(command)
                }
            }
        }
    }

    private fun dispatchConflated(incoming: Map<String, Any?>) {
        pendingLocationMap.updateAndGet { current ->
            SignalingMessageConflator.conflate(current, incoming)
        }

        if (conflationJob == null || !conflationJob!!.isActive) {
            conflationJob = scope.launch(Dispatchers.Default) {
                val delayMs = if (isViolationProvider()) SIGNALING_CONFLATION_DELAY_VIOLATION_MS else SIGNALING_CONFLATION_DELAY_MS
                delay(delayMs)
                
                val toSend = pendingLocationMap.getAndSet(null)
                if (toSend != null) {
                    queue.trySend(Command.Json("location_update", toSend, SignalingPriority.NORMAL))
                }
            }
        }
    }

    fun shutdown() {
        processorJob?.cancel()
        conflationJob?.cancel()
        queue.close()
    }
}
