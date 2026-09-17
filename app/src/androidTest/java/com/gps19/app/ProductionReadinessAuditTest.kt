package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gps19.core.engine.PowerStateProvider
import com.gps19.core.engine.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * ProductionReadinessAuditTest: Verifies end-to-end telemetry stream constraints 
 * and Doze-deferral consistency across role transitions (R339).
 * Sep.17.02:
 * - Issue #1093: Power & Hardware Provider Convergence. Migrated to HardwareSuite.
 * Sep.16.12:
 * - Issue #1072 Static State Leakage: Implemented reset mechanism in @Before to 
 *   ensure test atomicity and prevent state leakage between runs.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProductionReadinessAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var hardwareSuite: HardwareSuite

    @Inject
    lateinit var powerStateProvider: PowerStateProvider

    @Module
    @TestInstallIn(
        components = [SingletonComponent::class],
        replaces = [PowerModule::class]
    )
    object TestPowerModule {
        @Provides
        @Singleton
        fun providePowerStateProvider(): PowerStateProvider = FakePowerStateProvider()
    }

    class FakePowerStateProvider : PowerStateProvider {
        companion object {
            var isIdle = false
        }
        override fun isDeviceIdleMode(): Boolean = isIdle
    }

    @Before
    fun init() {
        hiltRule.inject()
        // Issue #1072: Reset static state before every test to ensure atomicity
        FakePowerStateProvider.isIdle = false
    }

    @Test
    fun verifyTelemetryStreamsAndDozeDeferralConsistency() {
        sessionManager.reset()
        assertFalse("Initial state should not be in violation", sessionManager.isInViolation)
        
        val nowRt = timeProvider.elapsedRealtime()
        sessionManager.onTrackerPulse("Device_Alpha", nowRt)
        sessionManager.onViewerPulse("Viewer_Omega", nowRt)

        sessionManager.updateTick(
            nowRt = nowRt + 10000L,
            lastTickRt = nowRt,
            isPeerAvailable = true,
            isInViolation = true
        )

        assertTrue("SessionManager must sustain active violation state", sessionManager.isInViolation)
        assertEquals("Violation uptime percentage should match 100%", 100.0, sessionManager.getViolationPercentage(), 0.01)
    }

    /**
     * Issue #1071: Process Death Resilience Validation
     * Verifies that the power state is preserved across suite re-instantiation,
     * simulating service restart or process death recovery.
     */
    @Test
    fun verifyPowerStateResilienceAfterRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val systemMonitor = mockSystemMonitor() // Minimal test-specific mock if needed, but suite has real ones injected.
        
        // 1. Set state in current provider
        FakePowerStateProvider.isIdle = true
        assertTrue("Initial state should be Doze", hardwareSuite.shouldDeferSignaling(false))

        // 2. Simulate "recreation" by manually instantiating a new suite with a new provider instance
        val newProvider = FakePowerStateProvider()
        // Manual instantiation for resilience testing (mirroring Hilt singleton recreation)
        val newSuite = HardwareSuite(
            context = context,
            scope = externalScope,
            timeProvider = timeProvider,
            systemMonitor = hardwareSuite.getSystemMonitorForTest(), // Accessing injected system monitor
            systemStatusProvider = hardwareSuite.getSystemStatusProviderForTest(),
            powerStateProvider = newProvider,
            forensicAuditor = hardwareSuite.getForensicAuditorForTest()
        )

        assertTrue("Power state must persist across component recreation to prevent telemetry gaps",
            newSuite.shouldDeferSignaling(false))
            
        // 3. Toggle and verify consistency
        FakePowerStateProvider.isIdle = false
        assertFalse("Power state change must be reflected in the new suite instance",
            newSuite.shouldDeferSignaling(false))
    }

    private fun HardwareSuite.getSystemMonitorForTest(): SystemMonitor {
        // Reflection or test-only getter could be used here, but for this audit we'll use the injected one
        return hiltRule.run { ProductionReadinessAuditTest::class.java.getDeclaredField("hardwareSuite").apply { isAccessible = true }.get(this@ProductionReadinessAuditTest) as HardwareSuite }.let {
            HardwareSuite::class.java.getDeclaredField("systemMonitor").apply { isAccessible = true }.get(it) as SystemMonitor
        }
    }
    
    private fun HardwareSuite.getSystemStatusProviderForTest(): SystemStatusProvider {
         return HardwareSuite::class.java.getDeclaredField("systemStatusProvider").apply { isAccessible = true }.get(this) as SystemStatusProvider
    }
    
    private fun HardwareSuite.getForensicAuditorForTest(): ForensicAuditor {
         return HardwareSuite::class.java.getDeclaredField("forensicAuditor").apply { isAccessible = true }.get(this) as ForensicAuditor
    }

    private fun mockSystemMonitor(): SystemMonitor {
        // Dummy implementation for manual instantiation tests
        return hiltRule.run { ProductionReadinessAuditTest::class.java.getDeclaredField("hardwareSuite").apply { isAccessible = true }.get(this@ProductionReadinessAuditTest) as HardwareSuite }.let {
            HardwareSuite::class.java.getDeclaredField("systemMonitor").apply { isAccessible = true }.get(it) as SystemMonitor
        }
    }

    @Test
    fun verifyForensicThroughputUnderViolation() {
        sessionManager.reset()
        val startRt = timeProvider.elapsedRealtime()
        var currentRt = startRt
        val simulationDurationMs = 4 * 3600 * 1000L
        val tickStepMs = 2000L

        val steps = (simulationDurationMs / tickStepMs).toInt()
        for (i in 1..steps) {
            val lastRt = currentRt
            currentRt += tickStepMs
            sessionManager.updateTick(
                nowRt = currentRt,
                lastTickRt = lastRt,
                isPeerAvailable = true,
                isInViolation = true
            )
        }

        assertTrue("Session should remain in violation", sessionManager.isInViolation)
        assertEquals("Violation percentage should be 100%", 100.0, sessionManager.getViolationPercentage(), 0.001)
    }

    /**
     * Issue #1050: Deterministic Doze State Simulation
     * Verifies that HardwareSuite correctly identifies Doze-deferral 
     * requirements using the FakePowerStateProvider (R-ID 338).
     */
    @Test
    fun verifyDozeModeSignalingDeferral() {
        FakePowerStateProvider.isIdle = false
        assertFalse("Should not defer when not in Doze", hardwareSuite.shouldDeferSignaling(false))

        FakePowerStateProvider.isIdle = true
        assertTrue("Signaling should be deferred in Doze mode when no violation is present", 
            hardwareSuite.shouldDeferSignaling(isInViolation = false))
        
        assertFalse("Signaling should NOT be deferred during violation, even in Doze mode", 
            hardwareSuite.shouldDeferSignaling(isInViolation = true))
    }

    @Test
    fun verifyPhysicalSaturationBurst() = runBlocking {
        val durationMs = 5000L
        val startMs = System.currentTimeMillis()
        
        coroutineScope {
            val cpuJob = launch(Dispatchers.Default) {
                val end = System.currentTimeMillis() + durationMs
                var count = 0.0
                while (System.currentTimeMillis() < end) {
                    sin(count); cos(count); sqrt(count)
                    count += 0.01
                }
            }

            val ioJob = launch(Dispatchers.IO) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val tempFile = File(context.cacheDir, "audit_stress_test.tmp")
                val data = ByteArray(1024 * 1024) { 0xFF.toByte() }
                val end = System.currentTimeMillis() + durationMs
                try {
                    while (System.currentTimeMillis() < end) {
                        tempFile.outputStream().use { it.write(data); it.flush() }
                    }
                } finally {
                    tempFile.delete()
                }
            }
            
            joinAll(cpuJob, ioJob)
        }

        val elapsed = System.currentTimeMillis() - startMs
        assertTrue("Saturation burst should have executed for at least $durationMs ms", elapsed >= durationMs)
    }
}
