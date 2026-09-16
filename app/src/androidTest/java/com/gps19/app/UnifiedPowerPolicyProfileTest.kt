package com.gps19.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gps19.core.engine.NET_REJOIN_THRESHOLD_MS
import com.gps19.core.engine.PowerStateProvider
import com.gps19.core.engine.TimeProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UnifiedPowerPolicyProfileTest: Automated profiling study for the staggered performance tier.
 * Sep.16.06:
 * - Issue #1050/1052 Test Suite Hardening: Updated constructor to include PowerStateProvider.
 * Sep.16.05:
 * - Issue #1060 Capability Consolidation: Updated shouldPokeHardware call to match 
 *   renamed parameter and unified schema (R-ID 348).
 */
@RunWith(AndroidJUnit4::class)
class UnifiedPowerPolicyProfileTest {

    private val mockTimeProvider = object : TimeProvider {
        private var time = 1000000L
        override fun elapsedRealtime(): Long {
            val current = time
            time += 30000L // Simulate a 30s tick step
            return current
        }
        override fun currentTimeMillis(): Long = 1700000000000L
    }

    private val fakePowerStateProvider = object : PowerStateProvider {
        var isIdle = false
        override fun isDeviceIdleMode(): Boolean = isIdle
    }

    @Test
    fun verifyBackoffConvergenceAndJitterBounds() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val policy = UnifiedPowerPolicy(context, mockTimeProvider, fakePowerStateProvider)

        // Profile progression across 20 successive reconnect attempts
        for (attempt in 0..20) {
            val delay = policy.calculateNextBackoff(attempt, isConnected = false)
            assertTrue("Delay must respect base threshold", delay >= NET_REJOIN_THRESHOLD_MS)
            assertTrue("Delay must be capped at 5 minutes", delay <= 300000L)
        }

        // Ensure maximum cap convergence is reached stable and reliably
        val capDelay = policy.calculateNextBackoff(10, isConnected = false)
        assertEquals("Should stay bounded at exactly 5 mins", 300000L, capDelay)
    }

    @Test
    fun verifyStaggeredTierHardwarePokeConstraints() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val policy = UnifiedPowerPolicy(context, mockTimeProvider, fakePowerStateProvider)
        val intervalMs = 30000L

        // Tick 1: Initial state
        val lastPoke = mockTimeProvider.elapsedRealtime()
        
        // Tick 2: Should require poke as mockTimeProvider advances 30s per invocation
        // Validating for the unified staggered tier (A15/S21FE)
        val shouldPoke = policy.shouldPokeHardware(isStaggered = true, lastPokeRt = lastPoke, intervalMs = intervalMs)
        assertTrue("Should poke when interval matches or exceeds threshold on staggered tier", shouldPoke)
    }
}
