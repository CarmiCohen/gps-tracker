package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gps19.core.engine.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import javax.inject.Inject

/**
 * ForensicRecoveryAuditTest: Verifies zero-loss telemetry during process restarts.
 * Part of ISSUE-OCT27.6-F1 (R-ID 744 hardening).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicRecoveryAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var spillBuffer: ForensicSpillBuffer

    @Inject
    lateinit var logRepository: LogRepository

    @Inject
    lateinit var logDao: LogDao

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking {
            logRepository.resetForTest()
            logRepository.clearLogs()
            spillBuffer.resetBufferForTest()
        }
    }

    /**
     * verifyRecoveryAfterSimulatedCrash: Ensures traces written to the off-heap buffer
     * are correctly replayed by the repository after a restart.
     */
    @Test
    fun verifyRecoveryAfterSimulatedCrash() = runBlocking {
        val testTraceCount = 50
        val baseTs = 4000000L // Use distinct TS

        // 1. Write traces directly to the buffer, bypassing the drainer
        repeat(testTraceCount) { i ->
            spillBuffer.writeTraceOptimized(
                timestamp = baseTs + i,
                lat = 42.0,
                lng = 24.0,
                accuracy = 10.0,
                maxAccuracy = 15.0,
                vibe = 0.1,
                snr = 35.0,
                batteryLevel = 80,
                isCharging = false,
                batteryTemp = 25.0
            )
        }

        assertEquals("Buffer should have pending traces", testTraceCount, spillBuffer.getPendingCount())

        // 2. Trigger manual recovery (Simulating LogRepository.start() behavior)
        logRepository.triggerManualRecovery()

        // 3. Wait for buffer to drain
        withTimeout(10000) {
            while (spillBuffer.hasPending()) {
                delay(200)
            }
        }

        // 4. Manual flush to ensure recovery event is persisted
        logRepository.flushLogs()

        // 5. Polling verification for Database insertion
        var recoveredTraces = emptyList<LogEntity>()
        var recoveryLog: LogEntity? = null
        
        withTimeout(5000) {
            while (recoveredTraces.size < testTraceCount || recoveryLog == null) {
                val allLogs = logDao.getAllLogsStatic(200)
                recoveredTraces = allLogs.filter { it.type == "FORENSIC_TRACE" && it.timestamp >= baseTs }
                recoveryLog = allLogs.find { it.message.contains("Forensic Recovery Successful") }
                
                if (recoveredTraces.size < testTraceCount || recoveryLog == null) {
                    delay(200)
                }
            }
        }
        
        assertEquals("All abandoned traces should be recovered in DB", testTraceCount, recoveredTraces.size)
        assertNotNull("Recovery success event should be logged in DB", recoveryLog)
        
        // Verify trace integrity
        val firstTrace = recoveredTraces.find { it.timestamp == baseTs }
        assertEquals(42.0, firstTrace?.lat ?: 0.0, 0.0001)
        
        Timber.i("Forensic Recovery Audit: Verified $testTraceCount traces replayed. Recovery event found.")
    }
}
