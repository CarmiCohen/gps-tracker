package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.ForensicBuffer
import com.jd.core.shared.PoolRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * ForensicStressAuditTest: Ported from OLD codebase.
 * Verifies Forensic Integrity and Stress stability in pure Kotlin.
 * Validates R197: Zero data loss during sustained 100Hz bursts.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicStressAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var buffer: ForensicBuffer

    @Before
    fun init() {
        hiltRule.inject()
        val testDir = File(System.getProperty("java.io.tmpdir"), "forensic_stress_test")
        testDir.deleteRecursively()
        testDir.mkdirs()
        buffer = ForensicBuffer(testDir, capacity = 2000)
        buffer.initialize()
    }

    @Test
    fun verifySustainedBurstIntegrity() = runBlocking {
        // 1. High-frequency 100Hz burst (10ms intervals) for 5 seconds
        val burstCount = 500 
        val intervalMs = 10L
        val successWrites = AtomicInteger(0)
        
        val update = PoolRegistry.locationUpdatePool.acquire()
        
        val job = launch(Dispatchers.Default) {
            repeat(burstCount) { i ->
                update.apply {
                    sequenceId = i.toLong()
                    timestamp = System.currentTimeMillis()
                }
                buffer.writeTrace(update)
                successWrites.incrementAndGet()
                delay(intervalMs)
            }
        }
        
        job.join()
        PoolRegistry.locationUpdatePool.release(update)
        
        assertEquals("Failed to write all traces to forensic buffer", burstCount, successWrites.get())

        // 2. Verify zero data loss and chronological integrity
        val recovered = buffer.readTraces()
        assertEquals("Database count mismatch - data loss detected.", burstCount, recovered.size)
        
        for (i in 0 until burstCount) {
            assertEquals("Out of order sequence at index $i", i.toLong(), recovered[i].sequenceId)
        }
    }

    @Test
    fun verifyBufferWrapAroundStability() {
        val cap = buffer.capacity
        val overflowCount = cap + 100
        val update = PoolRegistry.locationUpdatePool.acquire()
        
        repeat(overflowCount) { i ->
            update.sequenceId = i.toLong()
            buffer.writeTrace(update)
        }
        
        val recovered = buffer.readTraces()
        assertEquals("Buffer must maintain exact capacity during overflow", cap, recovered.size)
        assertTrue("Buffer must contain the latest entries", recovered.any { it.sequenceId == (overflowCount - 1).toLong() })
        assertFalse("Oldest entries must be purged", recovered.any { it.sequenceId == 0L })
        
        PoolRegistry.locationUpdatePool.release(update)
    }
}
