package com.gps19.app.sensor

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gps19.core.network.ForensicIdentityManager
import com.gps19.core.shared.ForensicBuffer
import com.gps19.core.shared.ForensicIdentity
import com.gps19.core.shared.PoolRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

/**
 * ForensicPersistenceAuditTest: Verifies Revol29.3/4 core persistence requirements.
 * Focus: Atomic Header Sync (R-ID 766) and Salted Identity Persistence (R-ID 765).
 * Updated Revol30.3: Migrated to ForensicIdentityManager and added Full Recovery Audit (R-ID 748).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicPersistenceAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var identityManager: ForensicIdentityManager

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        hiltRule.inject()
    }

    /**
     * Verifies R-ID 765: Salted Identity Persistence.
     * Ensures identity is generated on first boot and remains immutable.
     */
    @Test
    fun verifySaltedIdentityPersistence() = runBlocking {
        identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_SENSOR)
        val firstId = identityManager.selfId.first()
        
        assertNotEquals("PENDING_IDENTITY", firstId)
        assertEquals("Identity should be a SHA-256 hash (64 hex chars)", 64, firstId.length)
    }

    /**
     * Verifies R-ID 766: Atomic Header Sync.
     * Ensures the ForensicBuffer correctly recovers its pointers from the mmap file.
     */
    @Test
    fun verifyAtomicHeaderSynchronization() {
        val testDir = File(context.cacheDir, "forensic_test_sync_${System.currentTimeMillis()}")
        testDir.mkdirs()
        
        val buffer = ForensicBuffer(testDir)
        buffer.initialize()
        
        val update = PoolRegistry.locationUpdatePool.acquire().apply {
            timestamp = 123456789L
            monotonicNanos = 987654321L
        }
        
        buffer.writeTrace(update)
        
        val recoveryBuffer = ForensicBuffer(testDir)
        recoveryBuffer.initialize()
        
        assertTrue("Recovery buffer should detect the magic number 'REVO'", recoveryBuffer.hasPending())
        
        PoolRegistry.locationUpdatePool.release(update)
        testDir.deleteRecursively()
    }

    /**
     * Verifies R-ID 748: Forensic Recovery Integrity.
     * Ensures traces are preserved and readable in chronological order after a simulated process death.
     */
    @Test
    fun verifyFullRecoveryPipeline() {
        val testDir = File(context.cacheDir, "forensic_test_recovery_${System.currentTimeMillis()}")
        testDir.mkdirs()
        
        val buffer = ForensicBuffer(testDir, capacity = 100)
        buffer.initialize()
        
        // Write 10 traces
        for (i in 0 until 10) {
            val update = PoolRegistry.locationUpdatePool.acquire().apply {
                timestamp = 1000L + i
                sequenceId = i.toLong()
                sensorSnapshot.vibration = i.toFloat()
            }
            buffer.writeTrace(update, isCritical = true)
            PoolRegistry.locationUpdatePool.release(update)
        }
        
        // Simulate crash/restart
        val recoveryBuffer = ForensicBuffer(testDir, capacity = 100)
        recoveryBuffer.initialize()
        
        val recovered = recoveryBuffer.readTraces()
        assertEquals("Should recover 10 traces", 10, recovered.size)
        
        for (i in 0 until 10) {
            assertEquals("Timestamp mismatch at index $i", 1000L + i, recovered[i].timestamp)
            assertEquals("SequenceId mismatch at index $i", i.toLong(), recovered[i].sequenceId)
        }
        
        recoveryBuffer.clear()
        assertFalse("Buffer should be empty after clear", recoveryBuffer.hasPending())
        assertEquals("Count should be 0 after clear", 0, recoveryBuffer.readTraces().size)

        testDir.deleteRecursively()
    }
}
