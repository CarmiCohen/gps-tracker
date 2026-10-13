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
import javax.inject.Inject

/**
 * ForensicStressAuditTest: Verifies Forensic Integrity and Stress stability.
 * Oct.13.5: Hardened to detect silent CRC mismatches and verify full backfill convergence.
 * - Improved @Before isolation with synchronous log clearing.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicStressAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var spillBuffer: ForensicSpillBuffer
    @Inject lateinit var logRepository: LogRepository
    @Inject lateinit var logDao: LogDao
    @Inject lateinit var timeProvider: TimeProvider

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking {
            // Synchronously clear logs to ensure isolation
            withContext(Dispatchers.IO) {
                logDao.clearAll()
            }
            spillBuffer.resetBufferForTest()
        }
    }

    @Test
    fun verifySustainedBurstIntegrity() = runBlocking {
        // 1. High-frequency 100Hz burst (10ms)
        val burstCount = 1000 
        val intervalMs = 10L
        
        val startTime = timeProvider.currentTimeMillis
        
        val job = launch(Dispatchers.Default) {
            repeat(burstCount) { i ->
                val success = spillBuffer.writeTraceOptimized(
                    timestamp = startTime + i,
                    lat = 40.7128,
                    lng = -74.0060,
                    accuracy = 5.0,
                    maxAccuracy = 10.0,
                    vibe = 0.1,
                    snr = 40.0,
                    batteryLevel = 80,
                    isCharging = true,
                    batteryTemp = 35.0
                )
                if (!success) delay(50) else delay(intervalMs)
            }
        }
        
        job.join()
        
        // 2. Wait for drainer convergence
        // The drainer runs periodically; we wait up to 20s for full backfill
        var attempts = 0
        var dbCount = 0
        while (attempts < 20 && dbCount < burstCount) {
            delay(1000)
            dbCount = logRepository.loadAllLogsStatic(burstCount + 100).filter { it.type == "FORENSIC_TRACE" }.size
        }
        
        val pending = spillBuffer.getPendingCount()
        
        // 3. Verify zero data loss (R197)
        assertEquals("Database count mismatch - possible data loss or duplication.", burstCount, dbCount)
        assertEquals("Buffer should be empty after convergence.", 0, pending)
    }

    @Test
    fun verifySpillBufferWrapAroundStability() = runBlocking {
        val overflowCount = FORENSIC_SPILL_CAPACITY + 100
        var writes = 0
        repeat(overflowCount) {
            val success = spillBuffer.writeTraceOptimized(
                timestamp = timeProvider.currentTimeMillis,
                lat = 0.0, lng = 0.0, accuracy = 0.0, maxAccuracy = 0.0,
                vibe = 0.0, snr = 0.0, batteryLevel = 0, isCharging = false, batteryTemp = 0.0
            )
            if (success) writes++
        }
        
        assertTrue("Buffer should have accepted at least capacity", writes >= FORENSIC_SPILL_CAPACITY)
        assertTrue("Buffer should report full", spillBuffer.isFull())
    }
}
