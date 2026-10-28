package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gps19.core.engine.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import kotlin.system.measureTimeMillis

/**
 * ForensicConvergenceAuditTest: Verifies GAP-OCT27.0-1.
 * Audits the LogRepository's ability to sustain 100Hz bursts and drain performance.
 * Oct.27.1: Disabled test mode during audit to allow background drain tasks to run.
 * Oct.28.23: Fixed compilation errors caused by removal of FORENSIC_SPILL_CAPACITY.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicConvergenceAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var logRepository: LogRepository
    @Inject lateinit var logManager: LogManager
    @Inject lateinit var forensicSpillBufferProvider: Provider<ForensicSpillBuffer>
    @Inject lateinit var timeProvider: TimeProvider
    @Inject lateinit var integrityMonitor: IntegrityMonitor
    @Inject lateinit var testModeProvider: ManualTestModeProvider

    @Before
    fun init() {
        hiltRule.inject()
        GpsApplication.resetTestStates()
        runBlocking {
            logRepository.resetForTest()
            integrityMonitor.resetStats()
            forensicSpillBufferProvider.get().resetBufferForTest()
        }
    }

    /**
     * verifyForensicConvergenceUnder100HzBurst:
     * Simulates a sustained 100Hz burst for 30 seconds (representing a high-pressure window)
     * and verifies that the ForensicSpillBuffer converges (drains) and IOW remains low.
     */
    @Test
    fun verifyForensicConvergenceUnder100HzBurst() = runBlocking {
        // Oct.27.1: Explicitly disable test mode to allow LogRepository background tasks.
        testModeProvider.setEnabled(false)
        
        logRepository.start()
        val buffer = forensicSpillBufferProvider.get()
        
        val burstFrequencyHz = 100
        val burstDurationMs = 30_000L
        val intervalMs = 1000L / burstFrequencyHz
        
        var totalInjected = 0
        val startTime = timeProvider.elapsedRealtime
        
        Timber.i("AUDIT: Starting 100Hz burst for ${burstDurationMs}ms...")
        
        // Background monitoring of IOW
        var maxIowSeen = 0.0
        val monitorJob = launch {
            while (isActive) {
                val currentIow = integrityMonitor.healthFlow.value.ioWait
                if (currentIow > maxIowSeen) maxIowSeen = currentIow
                delay(500)
            }
        }

        // burst injection
        while (timeProvider.elapsedRealtime - startTime < burstDurationMs) {
            val now = timeProvider.currentTimeMillis
            logManager.logForensicTraceOptimized(
                timestamp = now,
                lat = 32.7940,
                lng = 34.9896,
                accuracy = 10.0,
                maxAccuracy = 15.0,
                vibe = 0.1,
                snr = 35.0,
                batteryLevel = 80,
                isCharging = true,
                batteryTemp = 35.0
            )
            totalInjected++
            delay(intervalMs)
        }
        
        Timber.i("AUDIT: Injected $totalInjected traces. Waiting for convergence...")
        
        // Wait for drain to catch up
        val timeout = 25_000L // Increased timeout for slower storage
        val convergenceStart = timeProvider.elapsedRealtime
        while (buffer.hasPending() && timeProvider.elapsedRealtime - convergenceStart < timeout) {
            delay(1000)
            Timber.d("AUDIT: Pending in buffer: ${buffer.getPendingCount()}")
        }
        
        monitorJob.cancelAndJoin()
        
        val remaining = buffer.getPendingCount()
        Timber.i("AUDIT: Convergence Result - Remaining: $remaining, Max IOW: $maxIowSeen")
        
        // Assertions
        assertEquals("R-ID GAP-OCT27.0-1: Buffer must converge after burst", 0, remaining)
        assertTrue(
            "R-ID GAP-OCT27.0-1: IOW ($maxIowSeen) must not exceed threshold (${SILENT_FAILURE_IOW_THRESHOLD})",
            maxIowSeen <= SILENT_FAILURE_IOW_THRESHOLD
        )
    }

    /**
     * verifyDrainEfficiency: Measures raw DB insertion latency during high-pressure drain cycles.
     */
    @Test
    fun verifyDrainEfficiency() = runBlocking {
        val buffer = forensicSpillBufferProvider.get()
        buffer.resetBufferForTest()
        
        // Fill buffer to 50%
        val fillCount = FORENSIC_SPILL_CAPACITY_DEFAULT / 2
        repeat(fillCount) { i ->
            buffer.writeTraceOptimized(
                timestamp = 1000L + i, lat = 1.0, lng = 2.0, accuracy = 3.0, maxAccuracy = 4.0,
                vibe = 0.5, snr = 30.0, batteryLevel = 50, isCharging = true, batteryTemp = 30.0
            )
        }
        
        val timeTaken = measureTimeMillis {
            logRepository.flushForensicTraces()
        }
        
        Timber.i("AUDIT: Drained $fillCount traces in ${timeTaken}ms.")
        assertFalse("Buffer should be empty after flush", buffer.hasPending())
        
        // R-ID GAP-OCT27.0-1: Average latency per trace should be < 1.0ms
        val avgLatency = timeTaken.toDouble() / fillCount.toDouble()
        Timber.i("AUDIT: Avg latency per trace: ${"%.3f".format(avgLatency)}ms")
        
        assertTrue("Drain efficiency too low: ${avgLatency}ms/trace", avgLatency < 1.0)
    }
}
