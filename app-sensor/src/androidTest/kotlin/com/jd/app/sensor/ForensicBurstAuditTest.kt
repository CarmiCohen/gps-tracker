package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.ForensicBuffer
import com.jd.core.shared.PoolRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject
import kotlin.system.measureTimeMillis

/**
 * ForensicBurstAuditTest: Ported from OLD ForensicConvergenceAuditTest.
 * Validates R-ID GAP-OCT27.0-1: Sustained 100Hz burst handling and 
 * drain efficiency of the ForensicBuffer.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicBurstAuditTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var buffer: ForensicBuffer

    @Before
    fun setup() {
        hiltRule.inject()
        // Use a temporary test directory
        val testDir = File(System.getProperty("java.io.tmpdir"), "forensic_test_burst")
        testDir.deleteRecursively()
        testDir.mkdirs()
        buffer = ForensicBuffer(testDir)
        buffer.initialize()
    }

    /**
     * verifySustained100HzBurst: Simulates a 100Hz telemetry burst 
     * and verifies buffer integrity.
     */
    @Test
    fun verifySustained100HzBurst() = runBlocking {
        val burstSize = 1000 // 10 seconds at 100Hz
        val update = PoolRegistry.locationUpdatePool.acquire()
        
        try {
            val timeTaken = measureTimeMillis {
                repeat(burstSize) { i ->
                    update.apply {
                        sequenceId = i.toLong()
                        timestamp = System.currentTimeMillis()
                        lat = 32.0; lng = 34.0
                    }
                    buffer.writeTrace(update)
                }
            }
            
            assertTrue(
                "Burst injection too slow: ${timeTaken}ms for $burstSize updates", 
                timeTaken < 5000 // Must handle 100Hz (10ms/update) with margin
            )

            // Verify readability of the burst
            val readBack = buffer.readRange(0, 999)
            assertTrue("Buffer must return all $burstSize updates", readBack.size == burstSize)
            
        } finally {
            PoolRegistry.locationUpdatePool.release(update)
        }
    }

    /**
     * verifyDrainEfficiency: Measures read latency for forensic backfill ranges.
     */
    @Test
    fun verifyDrainEfficiency() = runBlocking {
        val count = 500
        val update = PoolRegistry.locationUpdatePool.acquire()
        repeat(count) { i ->
            update.sequenceId = i.toLong()
            buffer.writeTrace(update)
        }
        PoolRegistry.locationUpdatePool.release(update)

        val timeTaken = measureTimeMillis {
            val result = buffer.readRange(0, 499)
            assertTrue(result.size == count)
        }

        val avgLatency = timeTaken.toDouble() / count
        assertTrue("Backfill drain efficiency too low: ${avgLatency}ms/trace", avgLatency < 1.0)
    }
}
