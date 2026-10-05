package com.gps19.core.engine

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DomainEventBus: A high-performance, unified reactive bus for domain-level events.
 * Oct.5.6:
 * - Issue #1328: Phase 2 - Prioritized Drop Strategy. Implemented selective 
 *   dropping of LOW priority events when subscription count exceeds threshold 
 *   to mitigate UI-induced backpressure.
 * Oct.5.5:
 * - Issue #1328: Backpressure Risk Mitigation. Increased buffer capacity to 512 
 *   to handle high-frequency forensic sampling bursts (100Hz). 
 */
@Singleton
class DomainEventBus @Inject constructor() {
    
    private val _events = MutableSharedFlow<DomainEvent>(
        extraBufferCapacity = DOMAIN_EVENT_BUS_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    /**
     * Emits a domain event to all subscribers.
     * Uses non-blocking tryEmit to ensure the tracking loop is never stalled by UI observers.
     * Oct.5.6: Drops LOW priority events if subscription count is high.
     */
    fun emit(event: DomainEvent) {
        if (event.priority == EventPriority.LOW && 
            _events.subscriptionCount.value >= DOMAIN_EVENT_BUS_HIGH_SUBSCRIPTION_THRESHOLD) {
            return
        }
        _events.tryEmit(event)
    }
}
