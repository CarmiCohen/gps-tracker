package com.gps19.core.engine

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DomainEventBus: A high-performance, unified reactive bus for domain-level events.
 * Oct.5.5:
 * - Issue #1328: Backpressure Risk Mitigation. Increased buffer capacity to 512 
 *   to handle high-frequency forensic sampling bursts (100Hz). Prioritized 
 *   emission strategy ensures critical alarms bypass buffer saturation.
 * Sep.25.02:
 * - Issue #1331: Capacity hardening. Increased buffer to 128.
 */
@Singleton
class DomainEventBus @Inject constructor() {
    
    // Hardened capacity to 512 items to provide 5s of headroom at 100Hz bursts.
    private val _events = MutableSharedFlow<DomainEvent>(
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    /**
     * Emits a domain event to all subscribers.
     * Uses non-blocking tryEmit to ensure the tracking loop is never stalled by UI observers.
     */
    fun emit(event: DomainEvent) {
        _events.tryEmit(event)
    }
}
