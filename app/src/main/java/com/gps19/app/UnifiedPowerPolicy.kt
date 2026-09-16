package com.gps19.app

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.gps19.core.engine.NET_REJOIN_THRESHOLD_MS
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
 * Sep.16.03:
 * - Legacy Cleanup (#1057): Removed historical references to deprecated A15 
 *   power policy components (R-ID 348).
 * Sep.16.00:
 * - Issue #1055 Unified Performance Tier: Broadened to support all staggered 
 *   performance hardware (R-ID 347).
 * Sep.15.04:
 * - Context Shadowing Automation (#1047): Switched to @ApplicationContext 
 *   as IPC optimization is now handled globally in GpsApplication (R-ID 240).
 */
@Singleton
class UnifiedPowerPolicy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val random = java.util.Random()

    /**
     * Determines if non-critical signaling should be deferred based on Doze state.
     * Android 15 / Doze Awareness (R-ID 338).
     */
    fun shouldDeferSignaling(isInViolation: Boolean): Boolean {
        // Since minSdk is 24, isDeviceIdleMode is always available.
        return powerManager.isDeviceIdleMode && !isInViolation
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
    fun shouldPokeHardware(isStaggeredTier: Boolean, lastPokeRt: Long, intervalMs: Long): Boolean {
        if (!isStaggeredTier) return false
        return timeProvider.elapsedRealtime() - lastPokeRt > intervalMs
    }
}
