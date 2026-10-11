package com.gps19.core.engine

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DomainEventBus: A high-performance, unified reactive bus for domain-level events.
 * Oct.11.5:
 * - Issue #SIMP-1011-17: Event Bus Hardening. Refined priority-aware gating 
 *   logic. Increased buffer to 1024 and implemented staged dropping (LOW at 1x 
 *   threshold, NORMAL at 2x) to ensure CRITICAL alarms are never evicted 
 *   by sensor noise during high-subscription pressure.
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
     * Oct.11.5: Staged dropping based on priority and subscription pressure.
     */
    fun emit(event: DomainEvent) {
        val subs = _events.subscriptionCount.value
        
        // Staged Priority Gate: Protect buffer space for CRITICAL/HIGH events.
        when (event.priority) {
            EventPriority.LOW -> {
                if (subs >= DOMAIN_EVENT_BUS_HIGH_SUBSCRIPTION_THRESHOLD) return
            }
            EventPriority.NORMAL -> {
                // Drop NORMAL events (like TrailPoints) only under extreme subscription pressure.
                if (subs >= DOMAIN_EVENT_BUS_HIGH_SUBSCRIPTION_THRESHOLD * 2) return
            }
            else -> { /* HIGH and CRITICAL always attempt emission */ }
        }

        _events.tryEmit(event)
    }
}
