package com.jd.app.monitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.shared.ForensicIdentity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * IdentityAuditTest: Ported from OLD IdentityPersistenceTest.
 * Validates R499: Identity Uniqueness and Persistence.
 * Ensures the Monitor module handles identity conflicts and 
 * factory resets correctly in pure Kotlin.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class IdentityAuditTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var identityManager: ForensicIdentityManager

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /**
     * verifyTargetIdPersistence: Ensures that the tracker ID being 
     * followed by this monitor persists across sessions.
     */
    @Test
    fun verifyTargetIdPersistence() = runBlocking {
        val testId = "a".repeat(64) // Valid 64-char hex
        
        identityManager.setTargetId(testId)
        val savedId = identityManager.targetId.first()
        
        assertEquals("Target ID must persist in DataStore", testId, savedId)
    }

    /**
     * verifyIdentityFormatValidation: Ensures only valid 64-char 
     * hex hashes are accepted as target IDs.
     */
    @Test
    fun verifyIdentityFormatValidation() = runBlocking {
        val invalidId = "short-id"
        identityManager.setTargetId(invalidId)
        
        val currentId = identityManager.targetId.first()
        assertNotEquals("Invalid ID format must be rejected", invalidId, currentId)
    }

    /**
     * verifyResetToDefaultsR615: Ensures that resetIdentity clears 
     * all tracking state and generates a fresh ID.
     */
    @Test
    fun verifyResetToDefaultsR615() = runBlocking {
        identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_MONITOR)
        val originalId = identityManager.selfId.first()
        
        identityManager.setTargetId("b".repeat(64))
        
        // Trigger Reset
        identityManager.resetIdentity(ForensicIdentity.PREFIX_MONITOR)
        
        val newId = identityManager.selfId.first()
        val targetIdAfterReset = identityManager.targetId.first()
        
        assertNotEquals("Self ID must be regenerated after reset", originalId, newId)
        assertNull("Target ID must be cleared after factory reset", targetIdAfterReset)
    }
}
