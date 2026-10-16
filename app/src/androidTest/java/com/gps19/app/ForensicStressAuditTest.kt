package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gps19.core.engine.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicInteger

/**
 * ForensicStressAuditTest: Verifies Forensic Integrity and Stress stability.
 * Oct.16.3: Formalized state reset via SIMP-OCT16.3-S1.
 * Oct.13.7: 
 * - Updated tearDown to runBlocking for suspendable LogRepository cleanups.
 * - Improved burst loop to track and assert successful writes (Diagnosing 755/1000 under-count).
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
        // Issue #SIMP-OCT16.3-S1: Formalized global state reset.
        GpsApplication.resetTestStates()

        // Set test mode globally to inhibit background interference
        LogRepository.setTestMode(true)
        hiltRule.inject()
        
        runBlocking {
            // 1. Reset Repository and stop any existing background jobs
            logRepository.resetForTest()
            
            // 2. Synchronously clear logs to ensure isolation
            withContext(Dispatchers.IO) {
                logDao.clearAll()
            }
            // 3. Clear the memory-mapped buffer
            spillBuffer.resetBufferForTest()
            
            // 4. Now that we are isolated, manually start the drainer for this test session
            logRepository.triggerManualDrainer()
        }
    }
    
    @After
    fun tearDown() {
        runBlocking {
            // Stop background tasks to prevent leakage into the next test
            logRepository.stopBackgroundTasks()
            LogRepository.setTestMode(false)
        }
    }

    @Test
    fun verifySustainedBurstIntegrity() = runBlocking {
        // 1. High-frequency 100Hz burst (10ms)
        val burstCount = 1000 
        val intervalMs = 10L
        val successWrites = AtomicInteger(0)
        val failWrites = AtomicInteger(0)
        
        val startTime = timeProvider.currentTimeMillis
        
        // Use a separate scope to avoid cancelling the test scope if one write fails
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
                if (success) {
                    successWrites.incrementAndGet()
                    delay(intervalMs)
                } else {
                    failWrites.incrementAndGet()
                    delay(50) 
                }
            }
        }
        
        job.join()
        
        Timber.i("Forensic Stress: Burst complete. Success: ${successWrites.get()}, Fails: ${failWrites.get()}")
        assertEquals("Failed to write all traces to spill buffer", burstCount, successWrites.get())

        // 2. Wait for drainer convergence
        // The drainer runs periodically; we wait up to 40s for full backfill
        var attempts = 0
        var dbCount = 0
        while (attempts < 40 && dbCount < burstCount) {
            delay(1000)
            val logs = logRepository.loadAllLogsStatic(burstCount + 500)
            dbCount = logs.filter { it.type == "FORENSIC_TRACE" }.size
            
            val pending = spillBuffer.getPendingCount()
            Timber.d("Forensic Convergence: DB Count: $dbCount, Pending: $pending (Attempt $attempts/40)")
            
            attempts++
        }
        
        val finalPending = spillBuffer.getPendingCount()
        
        // 3. Verify zero data loss (R197) and zero duplication
        assertEquals("Database count mismatch - data loss or duplication leak.", burstCount, dbCount)
        assertEquals("Buffer should be empty after convergence.", 0, finalPending)
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
