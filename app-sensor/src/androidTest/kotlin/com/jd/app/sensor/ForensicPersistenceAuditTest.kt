package com.jd.app.sensor

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.shared.ForensicBuffer
import com.jd.core.shared.ForensicIdentity
import com.jd.core.shared.PoolRegistry
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
 * ForensicPersistenceAuditTest: Ported to JD Revol31.2.
 * Focus: Atomic Header Sync (R-ID 766) and Salted Identity Persistence (R-ID 765).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicPersistenceAuditTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

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
     */
    @Test
    fun verifyFullRecoveryPipeline() {
        val testDir = File(context.cacheDir, "forensic_test_recovery_${System.currentTimeMillis()}")
        testDir.mkdirs()
        
        val buffer = ForensicBuffer(testDir, capacity = 100)
        buffer.initialize()
        
        for (i in 0 until 10) {
            val update = PoolRegistry.locationUpdatePool.acquire().apply {
                timestamp = 1000L + i
                sequenceId = i.toLong()
                sensorSnapshot.vibration = i.toFloat()
            }
            buffer.writeTrace(update, isCritical = true)
            PoolRegistry.locationUpdatePool.release(update)
        }
        
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
        testDir.deleteRecursively()
    }
}
