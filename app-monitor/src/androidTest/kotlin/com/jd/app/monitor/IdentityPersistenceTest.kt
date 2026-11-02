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
 * IdentityPersistenceTest: Ported from OLD codebase.
 * Ensures Tracker/Viewer identity persistence and uniqueness (Point 25).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class IdentityPersistenceTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var identityManager: ForensicIdentityManager

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun verifyIdentityPersistence() = runBlocking {
        // 1. Generate an identity
        identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_MONITOR)
        val firstId = identityManager.selfId.first()
        
        // 2. Verify it is a valid hex hash
        assertTrue("Identity must be a 64-char hex hash", ForensicIdentity.isValid(firstId))
        
        // 3. Verify it persists (simulated by re-fetching from the manager)
        val reFetchedId = identityManager.selfId.first()
        assertEquals("Identity must be stable across fetches", firstId, reFetchedId)
    }

    @Test
    fun verifyTargetIdPersistence() = runBlocking {
        val customTargetId = "a".repeat(64)
        
        identityManager.setTargetId(customTargetId)
        val savedId = identityManager.targetId.first()
        
        assertEquals("Target ID must persist in storage", customTargetId, savedId)
    }

    @Test
    fun verifyResetToFactoryDefaults() = runBlocking {
        identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_MONITOR)
        val originalId = identityManager.selfId.first()
        
        // Set a target to verify it gets cleared
        identityManager.setTargetId("b".repeat(64))
        
        // R-ID 615: Trigger Reset
        identityManager.resetIdentity(ForensicIdentity.PREFIX_MONITOR)
        
        val newId = identityManager.selfId.first()
        val targetIdAfterReset = identityManager.targetId.first()
        
        assertNotEquals("Self ID must be fresh after reset", originalId, newId)
        assertNull("Target ID must be cleared after factory reset", targetIdAfterReset)
    }
}
