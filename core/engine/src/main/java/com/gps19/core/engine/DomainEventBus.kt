package com.gps19.core.engine

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DomainEventBus: A high-performance, unified reactive bus for domain-level events.
 * Sep.25.02:
 * - Issue #1331: Capacity hardening. Increased buffer to 128 and implemented 
 *   DROP_OLDEST strategy to guarantee non-blocking emission for the tick loop.
 * Sep.25.01:
 * - Issue #1322: Migrated to core engine module to support component-level 
 *   event emission from LocationProcessor and other core logic.
 * Sep.24.97:
 * - Issue #1291: Facilitates decoupling of the background evaluation loop from 
 *   side-effect components like forensics, ribbons, and remote status updates.
 */
@Singleton
class DomainEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<DomainEvent>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    /**
     * Emits a domain event to all subscribers.
     * Uses trySend-style buffering to ensure the emitter (tick loop) is never blocked.
     */
    fun emit(event: DomainEvent) {
        _events.tryEmit(event)
    }
}
