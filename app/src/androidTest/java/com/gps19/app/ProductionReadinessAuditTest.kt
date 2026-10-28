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
import kotlinx.coroutines.flow.first
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
        MockPowerStateProvider(delegate = real)
}

/**
 * ProductionReadinessAuditTest: Validates system behavior under production-like 
 * soak and pressure conditions.
 * Oct.28.23: Fixed compilation errors caused by removal of FORENSIC_SPILL_CAPACITY.
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
    
    @Inject
    lateinit var telemetryRepository: TelemetryRepository
    
    @Inject
    lateinit var connectivitySuite: ConnectivitySuite

    @Inject
    lateinit var signalingProvider: SignalingProvider
    
    @Inject
    lateinit var repository: MainRepository

    @Inject
    lateinit var testModeProvider: ManualTestModeProvider

    private val mockPowerStateProvider get() = powerStateProvider as MockPowerStateProvider

    @Before
    fun init() {
        hiltRule.inject()
        
        // Ensure clean state for each test run
        testModeProvider.setEnabled(true)
        sessionManager.reset()
        GpsApplication.resetTestStates()
        
        mockPowerStateProvider.useDelegate = false
        mockPowerStateProvider.isIdle = false
        forensicSpillBufferProvider.get().resetBufferForTest()
    }

    /**
     * Chapter 0.1: Variant Integrity (R-ID 610).
     */
    @Test
    fun verifyPlatinumVariantIntegrity() {
        val isPlatinum = BuildConfig.IS_PLATINUM
        val hasSuffix = BuildConfig.VERSION_NAME.endsWith("_PLATINUM")
        
        assertTrue(
            "R-ID 610 VIOLATION: Forensic Audit MUST be run on a PLATINUM build variant. " +
            "Current Variant: ${if (isPlatinum) "PLATINUM" else "DEBUG"} (${BuildConfig.VERSION_NAME})",
            isPlatinum && hasSuffix
        )
    }

    @Test
    fun verifyBidirectionalConnectivityAndSync() = runBlocking {
        repository.saveString(AppRole.VIEWER_SELF, TRACKER_ID_KEY, "T1")
        repository.saveString(AppRole.VIEWER_SELF, VIEWER_ID_KEY, "V1")
        connectivitySuite.start(MainRepository.DEFAULT_RELAY_URL, "T1", "V1", isTracker = false)
        telemetryRepository.updateRelayStatus(true)
        val nowRt = timeProvider.elapsedRealtime
        sessionManager.onViewerPulse("V1", nowRt)
        repository.updateRemoteActivity(nowRt)
        assertTrue("R-ID 605: Tracker MUST recognize Viewer pulse", sessionManager.getViewerCount() > 0)
        val lastActivity = repository.lastRemoteActivityTs.value
        assertTrue("R-ID 605: Peer activity timestamp must be fresh", nowRt - lastActivity < 5000)
    }

    /**
     * Chapter 15.2: High-Pressure Signaling Soak (R-ID 511).
     * Exercises SmartSignalingDispatcher conflation and priority logic.
     */
    @Test
    fun verifyHighPressureSignalingSoak() = runBlocking {
        repository.saveString(AppRole.TRACKER, TRACKER_ID_KEY, "TRK-SOAK")
        repository.saveString(AppRole.TRACKER, VIEWER_ID_KEY, "VWR-SOAK")
        connectivitySuite.start(MainRepository.DEFAULT_RELAY_URL, "TRK-SOAK", "VWR-SOAK", isTracker = true)
        telemetryRepository.updateRelayStatus(true)

        // Ensure connection state is propagated
        repeat(10) { if (!connectivitySuite.isConnected) delay(100) }
        assertTrue("Signaling MUST be connected for soak test", connectivitySuite.isConnected)

        val startReceived = repository.signalingMetrics.value.received
        val burstSize = 100
        
        repeat(burstSize) { i ->
            val update = LocationUpdate().apply {
                kinetic.lat = 32.0 + (i * 0.0001)
                kinetic.lng = 34.0 + (i * 0.0001)
                nowRt = timeProvider.elapsedRealtime
            }
            signalingProvider.transmit(update, SignalingPriority.NORMAL, fromViewer = false)
        }

        delay(3000)
        
        val metrics = repository.signalingMetrics.value
        val totalReceived = metrics.received - startReceived
        
        Timber.i("SOAK METRICS: Received: $totalReceived, Emitted: ${metrics.emitted}, Conflated: ${metrics.conflated}")
        
        assertTrue("Dispatcher MUST conflate high-frequency bursts", metrics.conflated > 0)
        assertTrue("Emitted frames MUST be less than received during burst", metrics.emitted < metrics.received)
    }

    @Test
    fun verifySignalingLifecycleProbes() = runBlocking {
        logRepository.setForensicStallSimulation(true)
        try {
            delay(2000)
            val buffer = forensicSpillBufferProvider.get()
            while (buffer.hasPending()) { buffer.commitDrain(1000) }
            forensicLogger.resetThrottling()
            val testInterface = "wlan${(10..99).random()}"
            val failureTag = "TEST_FAIL_${(100..999).random()}"
            val ts = timeProvider.currentTimeMillis
            val probe1 = LogEntry(localId = "P1-$ts", timestamp = ts, message = "Forensic Handover: TEST_UP ($testInterface)", type = "FORENSIC_TRACE", isImportant = false, role = AppRole.TRACKER.name)
            val probe2 = LogEntry(localId = "P2-$ts", timestamp = ts, message = "Forensic TX Failure: $failureTag (Mode: TRK, D:A, V:O)", type = "FORENSIC_TRACE", isImportant = false, role = AppRole.TRACKER.name)
            assertTrue(buffer.writeTrace(probe1)); assertTrue(buffer.writeTrace(probe2))
            var handoverFound = false
            var failureFound = false
            repeat(50) {
                val traces = buffer.peekToEntities(buffer.capacity).entities
                val dbLogs = logRepository.loadAllLogsStatic(1000)
                if (!handoverFound) handoverFound = traces.any { it.message.contains("Forensic Handover") } || dbLogs.any { it.message.contains("Forensic Handover") }
                if (!failureFound) failureFound = traces.any { it.message.contains("Forensic TX Failure") } || dbLogs.any { it.message.contains("Forensic TX Failure") }
                if (handoverFound && failureFound) return@repeat
                delay(100)
            }
            assertTrue(handoverFound && failureFound)
        } finally {
            logRepository.setForensicStallSimulation(false)
        }
    }

    @Test
    fun verifyExtendedSoakSimulation() = runBlocking {
        val startTime = timeProvider.elapsedRealtime
        val durationMs = 30000L
        var ticks = 0
        while (timeProvider.elapsedRealtime - startTime < durationMs) {
            val nowRt = timeProvider.elapsedRealtime
            sessionManager.onTrackerPulse("Soak_Device", nowRt)
            sessionManager.updateTick(nowRt + 1000, nowRt, true, isInViolation = false)
            if (ticks % 10 == 0) {
                repeat(5) { i -> forensicLogger.logHandover("SOAK_BURST_$ticks", "wlan$i", force = true) }
            }
            ticks++; delay(1000)
        }
    }

    @Test
    fun verifyTelemetryStreamsAndDozeDeferralConsistency() {
        sessionManager.reset()
        val nowRt = timeProvider.elapsedRealtime
        sessionManager.onTrackerPulse("Device_Alpha", nowRt)
        sessionManager.onViewerPulse("Viewer_Omega", nowRt)
        sessionManager.updateTick(nowRt + 10000L, nowRt, true, isInViolation = true)
        assertTrue(sessionManager.isInViolation)
        assertEquals(100.0, sessionManager.getViolationPercentage(), 0.01)
    }

    @Test
    fun verify24HourSoakSimulation() {
        sessionManager.reset()
        forensicAuditor.reset(AppRole.TRACKER)
        val startRt = timeProvider.elapsedRealtime
        var currentRt = startRt
        val durationMs = 24 * 3600 * 1000L
        val stepMs = 2000L
        val expectedInterval = 2000L
        var simulatedViolations = 0
        val steps = (durationMs / stepMs).toInt()
        for (i in 1..steps) {
            val lastRt = currentRt
            currentRt += stepMs
            val hasGap = i % 100 == 0
            val injectionDelay = if (hasGap) 2000L else 0L
            forensicAuditor.recordGpsFix(currentRt + injectionDelay, expectedInterval, AppRole.TRACKER)
            sessionManager.updateTick(currentRt, lastRt, true, hasGap)
            if (hasGap) simulatedViolations++
        }
        assertEquals((simulatedViolations.toDouble() / steps) * 100.0, sessionManager.getViolationPercentage(), 0.5)
    }

    @Test
    fun verifyPowerStateResilienceAfterRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        mockPowerStateProvider.useDelegate = false
        mockPowerStateProvider.isIdle = true
        assertTrue(hardwareSuite.shouldDeferSignaling(false))
        val newProvider = MockPowerStateProvider(delegate = AndroidPowerStateProvider(context), initialIdle = true)
        newProvider.useDelegate = false 
        val newSuite = HardwareSuite(context, CoroutineScope(SupervisorJob() + Dispatchers.Main), timeProvider, hardwareSuite.getSystemMonitorForTest(), hardwareSuite.getSystemStatusProviderForTest(), newProvider, hardwareSuite.getForensicAuditorForTest(), domainEventBus, ActivityContextProvider(context, timeProvider), telemetryRepository)
        assertTrue(newSuite.shouldDeferSignaling(false))
    }

    private fun HardwareSuite.getSystemMonitorForTest(): SystemMonitor = HardwareSuite::class.java.getDeclaredField("systemMonitor").apply { isAccessible = true }.get(this) as SystemMonitor
    private fun HardwareSuite.getSystemStatusProviderForTest(): SystemStatusProvider = HardwareSuite::class.java.getDeclaredField("systemStatusProvider").apply { isAccessible = true }.get(this) as SystemStatusProvider
    private fun HardwareSuite.getForensicAuditorForTest(): ForensicAuditor = HardwareSuite::class.java.getDeclaredField("forensicAuditor").apply { isAccessible = true }.get(this) as ForensicAuditor

    @Test
    fun verifyDozeModeSignalingDeferral() {
        mockPowerStateProvider.useDelegate = false
        mockPowerStateProvider.isIdle = true
        assertTrue(hardwareSuite.shouldDeferSignaling(false))
        assertFalse(hardwareSuite.shouldDeferSignaling(true))
    }
}
