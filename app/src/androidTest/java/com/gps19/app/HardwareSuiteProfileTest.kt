package com.gps19.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HardwareSuiteProfileTest: Automated profiling study for the staggered performance tier.
 * Sep.28.27:
 * - Issue #1374: Fixed SystemStatusProviderImpl mock instantiation to pass mockTimeProvider.
 */
@RunWith(AndroidJUnit4::class)
class HardwareSuiteProfileTest {

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

    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // DomainEventBus is a concrete class with internal buffering.
    private val testDomainEventBus = DomainEventBus()

    @Test
    fun verifyBackoffConvergenceAndJitterBounds() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val suite = HardwareSuite(
            context = context,
            scope = testScope,
            timeProvider = mockTimeProvider,
            systemMonitor = mockSystemMonitor(),
            systemStatusProvider = mockSystemStatusProvider(),
            powerStateProvider = fakePowerStateProvider,
            forensicAuditor = mockForensicAuditor(),
            domainEventBus = testDomainEventBus,
            activityContextProvider = mockActivityContextProvider(context)
        )

        // Profile progression across 20 successive reconnect attempts
        for (attempt in 0..20) {
            val delay = suite.calculateNextBackoff(attempt, isConnected = false)
            assertTrue("Delay must respect base threshold", delay >= NET_REJOIN_THRESHOLD_MS)
            assertTrue("Delay must be capped at 5 minutes", delay <= 300000L)
        }

        // Ensure maximum cap convergence is reached stable and reliably
        val capDelay = suite.calculateNextBackoff(10, isConnected = false)
        assertEquals("Should stay bounded at exactly 5 mins", 300000L, capDelay)
    }

    @Test
    fun verifyStaggeredTierHardwarePokeConstraints() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val suite = HardwareSuite(
            context = context,
            scope = testScope,
            timeProvider = mockTimeProvider,
            systemMonitor = mockSystemMonitor(),
            systemStatusProvider = mockSystemStatusProvider(),
            powerStateProvider = fakePowerStateProvider,
            forensicAuditor = mockForensicAuditor(),
            domainEventBus = testDomainEventBus,
            activityContextProvider = mockActivityContextProvider(context)
        )
        val intervalMs = 30000L

        // Tick 1: Initial state
        val lastPoke = mockTimeProvider.elapsedRealtime()
        
        // Tick 2: Should require poke as mockTimeProvider advances 30s per invocation
        val shouldPoke = suite.shouldPokeHardware(isStaggered = true, lastPokeRt = lastPoke, intervalMs = intervalMs)
        assertTrue("Should poke when interval matches or exceeds threshold on staggered tier", shouldPoke)
    }

    private fun mockSystemMonitor(): SystemMonitor {
        return SystemMonitor(ApplicationProvider.getApplicationContext(), mockTimeProvider)
    }

    private fun mockSystemStatusProvider(): SystemStatusProvider {
        return SystemStatusProviderImpl(ApplicationProvider.getApplicationContext(), testScope, mockTimeProvider)
    }

    private fun mockForensicAuditor(): ForensicAuditor {
        return ForensicAuditor(mockTimeProvider, mockSystemStatusProvider())
    }

    private fun mockActivityContextProvider(context: android.content.Context): ActivityContextProvider {
        return ActivityContextProvider(context, mockTimeProvider)
    }
}
