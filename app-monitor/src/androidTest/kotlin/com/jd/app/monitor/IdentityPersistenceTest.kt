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
        
        // 2. Verify it is a valid default ID ("V")
        assertTrue("Identity must be valid", ForensicIdentity.isValid(firstId))
        assertEquals("Default Monitor ID must be 'V'", ForensicIdentity.DEFAULT_MONITOR_ID, firstId)
        
        // 3. Verify it persists (simulated by re-fetching from the manager)
        val reFetchedId = identityManager.selfId.first()
        assertEquals("Identity must be stable across fetches", firstId, reFetchedId)
    }

    @Test
    fun verifyIdentityPersistenceAcrossReopen() = runBlocking {
        identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_MONITOR)
        identityManager.setSelfId("custom_persistent_id")
        
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val reopenedManager = ForensicIdentityManager(context)
        
        val reloadedId = reopenedManager.selfId.first()
        assertEquals("Identity must persist across manager re-instantiation (app restart)", "custom_persistent_id", reloadedId)
        
        reopenedManager.resetIdentity(ForensicIdentity.PREFIX_MONITOR)
    }

    @Test
    fun verifyTargetIdPersistence() = runBlocking {
        val customTargetId = ForensicIdentity.DEFAULT_TRACKER_ID
        
        identityManager.setTargetId(customTargetId)
        val savedId = identityManager.targetId.first()
        
        assertEquals("Target ID must persist in storage", customTargetId, savedId)
    }

    @Test
    fun verifyResetToFactoryDefaults() = runBlocking {
        identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_MONITOR)
        identityManager.setSelfId("custom_id_before_reset")
        
        // Set a target to verify it gets cleared
        identityManager.setTargetId("b".repeat(64))
        
        // R-ID 615: Trigger Reset
        identityManager.resetIdentity(ForensicIdentity.PREFIX_MONITOR)
        
        val newId = identityManager.selfId.first()
        val targetIdAfterReset = identityManager.targetId.first()
        
        assertEquals("Self ID must be default Monitor ID 'V' after reset", ForensicIdentity.DEFAULT_MONITOR_ID, newId)
        assertNull("Target ID must be cleared after factory reset", targetIdAfterReset)
    }
}
