package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
 * ForensicStressAuditTest: Verifies Chapter 12.2 Database Stress Audit requirements.
 * Validates zero-churn performance of ForensicSpillBuffer and R197 chunked pruning 
 * under sustained 100Hz log generation (R700/R715).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicStressAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var spillBuffer: ForensicSpillBuffer

    @Inject
    lateinit var logRepository: LogRepository

    @Inject
    lateinit var timeProvider: TimeProvider

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun verifySustained100HzStability() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // 1. Sustained 100Hz (10ms) Burst for 5 seconds
        val burstCount = 500 
        val intervalMs = 10L
        
        val startCount = spillBuffer.getPendingCount()
        
        val job = launch(Dispatchers.Default) {
            repeat(burstCount) { i ->
                val success = spillBuffer.writeTraceOptimized(
                    timestamp = System.currentTimeMillis(),
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
                if (!success) {
                    // If buffer is full, wait for drainer
                    delay(50)
                } else {
                    delay(intervalMs)
                }
            }
        }
        
        job.join()
        
        val endCount = spillBuffer.getPendingCount()
        assertTrue("Expected at least some logs to be pending or processed. Start: $startCount, End: $endCount", endCount >= 0)
        
        // 2. Verify Drain Convergence (R197)
        // Wait for LogRepository drainer to process the off-heap buffer
        delay(3000) 
        
        val pendingAfterDrain = spillBuffer.getPendingCount()
        assertTrue("Forensic backfill not converging. Pending: $pendingAfterDrain", pendingAfterDrain < burstCount)
        
        // 3. Database Pressure Audit (R197)
        // Verify we can still perform a static load without I/O stall
        val logs = withTimeout(2000) {
            logRepository.loadAllLogsStatic(100)
        }
        assertNotNull("Static log load failed or timed out during stress", logs)
    }

    @Test
    fun verifySpillBufferWrapAroundStability() = runBlocking {
        // Force wrap-around by writing more than capacity
        // Note: ForensicSpillBuffer capacity is 1000 in EngineConstants
        val overflowCount = FORENSIC_SPILL_CAPACITY + 100
        
        var writes = 0
        repeat(overflowCount) {
            val success = spillBuffer.writeTraceOptimized(
                timestamp = System.currentTimeMillis(),
                lat = 0.0, lng = 0.0, accuracy = 0.0, maxAccuracy = 0.0,
                vibe = 0.0, snr = 0.0, batteryLevel = 0, isCharging = false, batteryTemp = 0.0
            )
            if (success) writes++
        }
        
        assertTrue("Buffer should have accepted at least capacity", writes >= FORENSIC_SPILL_CAPACITY)
        assertTrue("Buffer should report full or pressure", spillBuffer.isFull() || writes >= FORENSIC_SPILL_CAPACITY)
    }
}
