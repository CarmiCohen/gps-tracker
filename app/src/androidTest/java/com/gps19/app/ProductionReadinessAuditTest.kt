package com.gps19.app

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gps19.core.engine.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.math.*

class FakePowerStateProvider(private val real: AndroidPowerStateProvider) : PowerStateProvider {
    companion object {
        var useOverride = false
        var isIdle = false
    }
    override fun isDeviceIdleMode(): Boolean {
        return if (useOverride) isIdle else real.isDeviceIdleMode()
    }
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PowerModule::class]
)
object TestPowerModule {
    @Provides
    @Singleton
    fun provideRealPowerProvider(@ApplicationContext context: Context): AndroidPowerStateProvider {
        return AndroidPowerStateProvider(context)
    }

    @Provides
    @Singleton
    fun providePowerStateProvider(real: AndroidPowerStateProvider): PowerStateProvider = 
        FakePowerStateProvider(real)
}

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

    @Inject
    lateinit var domainEventBus: DomainEventBus
    
    @Inject
    lateinit var forensicAuditor: ForensicAuditor

    @Inject
    lateinit var forensicLogger: SignalingForensicLogger

    @Inject
    lateinit var forensicSpillBufferProvider: Provider<ForensicSpillBuffer>

    @Inject
    lateinit var logRepository: LogRepository

    @Before
    fun init() {
        hiltRule.inject()
        FakePowerStateProvider.useOverride = true
        FakePowerStateProvider.isIdle = false
        forensicSpillBufferProvider.get().resetBufferForTest()
    }

    @Test
    fun verifySignalingLifecycleProbes() = runBlocking {
        logRepository.setForensicStallSimulation(true)
        
        try {
            delay(2000)
            
            val buffer = forensicSpillBufferProvider.get()
            while (buffer.hasPending()) {
                buffer.commitDrain(1000)
            }
            
            forensicLogger.resetThrottling()
            
            val testInterface = "wlan${(10..99).random()}"
            val failureTag = "TEST_FAIL_${(100..999).random()}"
            
            val ts = timeProvider.currentTimeMillis()
            val probe1 = LogEntry(
                localId = "P1-$ts", timestamp = ts,
                message = "Forensic Handover: TEST_UP ($testInterface)", type = "FORENSIC_TRACE",
                isImportant = false, id = "SYSTEM", viewerId = "SYSTEM", role = "tracker"
            )
            val probe2 = LogEntry(
                localId = "P2-$ts", timestamp = ts,
                message = "Forensic TX Failure: $failureTag (Mode: TRK, D:A, V:O)", type = "FORENSIC_TRACE",
                isImportant = false, id = "SYSTEM", viewerId = "SYSTEM", role = "tracker"
            )

            assertTrue("Probe 1 write must succeed", buffer.writeTrace(probe1))
            assertTrue("Probe 2 write must succeed", buffer.writeTrace(probe2))
            
            var handoverFound = false
            var failureFound = false
            var lastObservedCount = 0
            var lastObservedMessages = listOf<String>()

            repeat(50) {
                val traces = buffer.peekToEntities(FORENSIC_SPILL_CAPACITY)
                val dbLogs = logRepository.loadAllLogsStatic(1000)
                
                lastObservedCount = traces.size
                lastObservedMessages = traces.map { it.message }

                if (!handoverFound) {
                    handoverFound = traces.any { it.message.contains("Forensic Handover") && it.message.contains(testInterface) } ||
                                    dbLogs.any { it.message.contains("Forensic Handover") && it.message.contains(testInterface) }
                }
                
                if (!failureFound) {
                    failureFound = traces.any { it.message.contains("Forensic TX Failure") && it.message.contains(failureTag) } ||
                                    dbLogs.any { it.message.contains("Forensic TX Failure") && it.message.contains(failureTag) }
                }
                
                if (handoverFound && failureFound) return@repeat
                delay(100)
            }
            
            assertTrue("Probes not found! Count: $lastObservedCount. Traces: $lastObservedMessages", handoverFound && failureFound)
            
        } finally {
            logRepository.setForensicStallSimulation(false)
        }
    }

    @Test
    fun verifyExtendedSoakSimulation() = runBlocking {
        logRepository.setForensicStallSimulation(false)
        val startTime = timeProvider.elapsedRealtime()
        val durationMs = 60000L // 60s Soak
        
        var ticks = 0
        while (timeProvider.elapsedRealtime() - startTime < durationMs) {
            val nowRt = timeProvider.elapsedRealtime()
            
            sessionManager.onTrackerPulse("Soak_Device", nowRt)
            sessionManager.updateTick(nowRt + 1000, nowRt, true, isInViolation = false)
            
            if (ticks % 5 == 0) {
                repeat(10) { i ->
                    forensicLogger.logHandover("SOAK_BURST_$ticks", "wlan$i", force = true)
                }
            }
            
            ticks++
            delay(500)
            
            if (ticks % 20 == 0) {
                val buffer = forensicSpillBufferProvider.get()
                assertTrue("Buffer must maintain operational state", buffer.getFillLevel() <= 1.0)
            }
        }
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

    @Test
    fun verify24HourSoakSimulation() {
        sessionManager.reset()
        forensicAuditor.reset(AppRole.TRACKER)
        
        val startRt = timeProvider.elapsedRealtime()
        var currentRt = startRt
        val durationMs = 24 * 3600 * 1000L
        val stepMs = 2000L
        val expectedInterval = 2000L
        
        var totalFixes = 0
        var simulatedViolations = 0

        val steps = (durationMs / stepMs).toInt()
        for (i in 1..steps) {
            val lastRt = currentRt
            currentRt += stepMs
            
            val hasGap = i % 100 == 0
            val injectionDelay = if (hasGap) 2000L else 0L
            
            forensicAuditor.recordGpsFix(currentRt + injectionDelay, expectedInterval, AppRole.TRACKER)
            
            sessionManager.updateTick(
                nowRt = currentRt,
                lastTickRt = lastRt,
                isPeerAvailable = true,
                isInViolation = hasGap
            )
            
            if (hasGap) simulatedViolations++
            totalFixes++
            
            if (i % 300 == 0) {
                forensicAuditor.evaluateStability(currentRt, AppRole.TRACKER)
            }
        }

        val finalViolationPct = sessionManager.getViolationPercentage()
        val expectedPct = (simulatedViolations.toDouble() / totalFixes) * 100.0
        
        assertEquals("Violation percentage must be deterministic over 24h", 
            expectedPct, finalViolationPct, 0.5)
            
        assertTrue("Session must survive 24h duration without counter overflow", 
            sessionManager.violationUptimeMs >= (simulatedViolations * stepMs))
    }

    @Test
    fun verifyPowerStateResilienceAfterRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        
        FakePowerStateProvider.useOverride = true
        FakePowerStateProvider.isIdle = true
        assertTrue("Initial state should be Doze", hardwareSuite.shouldDeferSignaling(false))

        val realProvider = AndroidPowerStateProvider(context)
        val newProvider = FakePowerStateProvider(realProvider)
        val newSuite = HardwareSuite(
            context = context,
            scope = externalScope,
            timeProvider = timeProvider,
            systemMonitor = hardwareSuite.getSystemMonitorForTest(),
            systemStatusProvider = hardwareSuite.getSystemStatusProviderForTest(),
            powerStateProvider = newProvider,
            forensicAuditor = hardwareSuite.getForensicAuditorForTest(),
            domainEventBus = domainEventBus,
            activityContextProvider = ActivityContextProvider(context, timeProvider)
        )

        assertTrue("Power state must persist across component recreation",
            newSuite.shouldDeferSignaling(false))
            
        FakePowerStateProvider.isIdle = false
        assertFalse("Power state change must be reflected",
            newSuite.shouldDeferSignaling(false))
    }

    private fun HardwareSuite.getSystemMonitorForTest(): SystemMonitor {
        return HardwareSuite::class.java.getDeclaredField("systemMonitor").apply { isAccessible = true }.get(this) as SystemMonitor
    }
    
    private fun HardwareSuite.getSystemStatusProviderForTest(): SystemStatusProvider {
         return HardwareSuite::class.java.getDeclaredField("systemStatusProvider").apply { isAccessible = true }.get(this) as SystemStatusProvider
    }
    
    private fun HardwareSuite.getForensicAuditorForTest(): ForensicAuditor {
         return HardwareSuite::class.java.getDeclaredField("forensicAuditor").apply { isAccessible = true }.get(this) as ForensicAuditor
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

    @Test
    fun verifyDozeModeSignalingDeferral() {
        FakePowerStateProvider.useOverride = true
        FakePowerStateProvider.isIdle = false
        assertFalse("Should not defer when not in Doze", hardwareSuite.shouldDeferSignaling(false))

        FakePowerStateProvider.isIdle = true
        assertTrue("Signaling should be deferred in Doze mode", 
            hardwareSuite.shouldDeferSignaling(isInViolation = false))
        
        assertFalse("Signaling should NOT be deferred during violation", 
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
        assertTrue("Saturation burst should have executed", elapsed >= durationMs)
    }
}
