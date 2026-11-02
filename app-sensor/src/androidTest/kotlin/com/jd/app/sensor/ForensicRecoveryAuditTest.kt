package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.ForensicBuffer
import com.jd.core.shared.PoolRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * ForensicRecoveryAuditTest: Ported from OLD version.
 * Verifies zero-loss telemetry during process restarts (R-ID 744 hardening).
 * Ensures that the MappedByteBuffer state persists across object instances.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicRecoveryAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() { hiltRule.inject() }

    @Test
    fun verifyRecoveryAfterSimulatedCrash() {
        val testDir = File(System.getProperty("java.io.tmpdir"), "forensic_recovery_test")
        testDir.deleteRecursively()
        testDir.mkdirs()

        val testTraceCount = 50
        val baseTs = 4000000L

        // 1. First instance writes traces
        val buffer1 = ForensicBuffer(testDir)
        buffer1.initialize()
        
        val update = PoolRegistry.locationUpdatePool.acquire()
        repeat(testTraceCount) { i ->
            update.apply {
                timestamp = baseTs + i
                sequenceId = i.toLong()
                lat = 42.0; lng = 24.0
            }
            buffer1.writeTrace(update)
        }
        PoolRegistry.locationUpdatePool.release(update)

        assertTrue("Buffer should have pending traces", buffer1.hasPending())

        // 2. Second instance (simulated restart) recovers state from file
        val buffer2 = ForensicBuffer(testDir)
        buffer2.initialize()

        val recovered = buffer2.readTraces()
        assertEquals("All traces must be recovered from disk", testTraceCount, recovered.size)
        assertEquals("Data integrity check failed", 42.0, recovered.first().lat, 0.0001)
    }
}
