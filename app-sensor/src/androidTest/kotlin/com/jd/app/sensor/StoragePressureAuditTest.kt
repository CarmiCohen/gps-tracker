package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.ForensicBuffer
import com.jd.core.shared.PoolRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * StoragePressureAuditTest: Ported from OLD codebase.
 * Validates R197: Storage Exhaustion Resilience and Forensic Priority.
 * Ensures the system handles low-disk scenarios without crashing in pure Kotlin.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StoragePressureAuditTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var buffer: ForensicBuffer
    private lateinit var testDir: File

    @Before
    fun setup() {
        hiltRule.inject()
        testDir = File(File(System.getProperty("java.io.tmpdir")), "storage_pressure_audit")
        testDir.deleteRecursively()
        testDir.mkdirs()
        buffer = ForensicBuffer(testDir)
        buffer.initialize()
    }

    /**
     * verifyForensicPriorityUnderPressure: Verifies that forensic writes 
     * are attempted even when storage is simulated as restricted.
     */
    @Test
    fun verifyForensicPriorityUnderPressure() {
        val update = PoolRegistry.locationUpdatePool.acquire()
        update.sequenceId = 777L
        
        // 1. Verify normal write
        buffer.writeTrace(update)
        assertTrue("Initial write should succeed", buffer.readRange(777, 777).isNotEmpty())

        // 2. Simulate storage restriction (Read-only filesystem)
        testDir.setReadOnly()
        
        try {
            // Write should handle the failure gracefully (no crash)
            update.sequenceId = 888L
            buffer.writeTrace(update)
            
            // Verify that the new sequence ID was NOT written due to restriction
            val results = buffer.readRange(888, 888)
            assertTrue("Write should have failed gracefully due to storage restriction", results.isEmpty())
        } finally {
            testDir.setWritable(true)
        }
        
        PoolRegistry.locationUpdatePool.release(update)
    }

    /**
     * verifyPruningStability: Ensures pruning doesn't corrupt active traces.
     */
    @Test
    fun verifyPruningStability() {
        val count = 50
        val update = PoolRegistry.locationUpdatePool.acquire()
        repeat(count) { i ->
            update.sequenceId = i.toLong()
            buffer.writeTrace(update)
        }
        
        // Simulate external pruning (clearing the storage directory)
        testDir.listFiles()?.forEach { it.delete() }
        
        // Verify system remains stable and reports empty buffer
        val result = buffer.readTraces()
        assertTrue("Buffer must report empty after external file deletion", result.isEmpty())
        assertFalse("hasPending must return false", buffer.hasPending())
        
        PoolRegistry.locationUpdatePool.release(update)
    }
}
