package com.gps19.core.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicReference

/**
 * SmartSignalingDispatcher: Unified reactive coordination layer for signaling.
 * Oct.3.8:
 * - Issue #1172: Smart Signaling Dispatcher. Completed migration of ALL signaling 
 *   triggers. Removed HIGH priority bypass to ensure connection-aware ordered 
 *   delivery for all commands (R-ID 510).
 * - Throttling Hardening: Adjusted processor to allow burst delivery for HIGH 
 *   priority commands while maintaining inter-frame delays for telemetry.
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
                // Connection Guard: Ensure we only attempt emission when the transport is ready.
                while (!isConnectedProvider() && isActive) {
                    delay(1000)
                }
                if (!isActive) break
                
                val priority = when(command) {
                    is Command.Json -> command.priority
                    is Command.Binary -> command.priority
                }

                when (command) {
                    is Command.Json -> jsonSink(command.event, command.data)
                    is Command.Binary -> binarySink(command.event, command.routingId, command.data)
                }

                // Adaptive Throttling: High priority commands (joins, pings, immediate alerts)
                // bypass the inter-frame delay to ensure system responsiveness.
                if (priority != SignalingPriority.HIGH) {
                    val delayMs = if (isViolationProvider()) SIGNALING_EMIT_DELAY_VIOLATION_MS else SIGNALING_EMIT_DELAY_MS
                    delay(delayMs)
                }
            }
        }
    }

    fun dispatch(command: Command) {
        when (command) {
            is Command.Json -> {
                // location_update is subject to conflation if priority is NORMAL/LOW.
                if (command.event == "location_update" && command.priority != SignalingPriority.HIGH) {
                    dispatchConflated(command.data)
                } else {
                    // All other JSON commands (including HIGH priority) are queued to maintain order
                    // and ensure connectivity checks.
                    queue.trySend(command)
                }
            }
            is Command.Binary -> {
                // Binary telemetry currently bypasses conflation but respects the queue/throttling.
                queue.trySend(command)
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
