package com.gps19.app

import android.content.Context
import com.gps19.core.engine.NET_REJOIN_THRESHOLD_MS
import com.gps19.core.engine.PowerStateProvider
import com.gps19.core.engine.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * UnifiedPowerPolicy: Central authority for power-awareness and signaling backoff.
 * Consolidates Doze deferral, exponential backoff, and hardware pokes to ensure 
 * consistent behavior across background service modules for the unified 
 * "staggered performance" tier (A15, S21FE).
 * Sep.16.06:
 * - Issue #1050/1052 Test Suite Hardening: Injected PowerStateProvider to 
 *   eliminate direct PowerManager dependency and facilitate deterministic testing.
 * Sep.16.05:
 * - Issue #1060 Capability Consolidation: Renamed isStaggeredTier to isStaggered 
 *   for consistency with unified schema.
 */
@Singleton
class UnifiedPowerPolicy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider,
    private val powerStateProvider: PowerStateProvider
) {
    private val random = java.util.Random()

    /**
     * Determines if non-critical signaling should be deferred based on Doze state.
     * Android 15 / Doze Awareness (R-ID 338).
     */
    fun shouldDeferSignaling(isInViolation: Boolean): Boolean {
        return powerStateProvider.isDeviceIdleMode() && !isInViolation
    }

    /**
     * Calculates exponential backoff with randomized jitter for reconnection attempts.
     */
    fun calculateNextBackoff(attempt: Int, isConnected: Boolean): Long {
        if (isConnected) return NET_REJOIN_THRESHOLD_MS
        
        val baseDelay = NET_REJOIN_THRESHOLD_MS
        val factor = 2.0.pow(min(attempt.toDouble(), 6.0)).toLong()
        val backoff = baseDelay * factor
        val jitter = random.nextInt(5000)
        
        return min(backoff + jitter, 300000L) // Cap at 5 minutes
    }

    /**
     * Determines if a hardware "poke" (WakeLock renewal) is required for background stability.
     */
    fun shouldPokeHardware(isStaggered: Boolean, lastPokeRt: Long, intervalMs: Long): Boolean {
        if (!isStaggered) return false
        return timeProvider.elapsedRealtime() - lastPokeRt > intervalMs
    }
}
