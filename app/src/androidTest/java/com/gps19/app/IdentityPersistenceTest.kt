package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gps19.core.engine.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * IdentityPersistenceTest: Instrumented verification for Issue #027.
 * Ensures Viewer ID persistence and uniqueness enforcement logic.
 */
@RunWith(AndroidJUnit4::class)
class IdentityPersistenceTest {

    private lateinit var repository: SettingsRepository
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val timeProvider = object : TimeProvider {
        override fun currentTimeMillis() = System.currentTimeMillis()
        override fun elapsedRealtime() = android.os.SystemClock.elapsedRealtime()
    }

    @Before
    fun setup() = runBlocking {
        repository = SettingsRepository(context, timeProvider)
        repository.resetStatsBulk()
        repository.clearDraftSettings()
        // Ensure baseline
        repository.saveString(TRACKER_ID_KEY, "T-ORIG")
        repository.saveString(VIEWER_ID_KEY, "V-ORIG")
    }

    @Test
    fun verifyViewerIdPersistenceDuringCommit() = runBlocking {
        val customViewerId = "V-CUSTOM-99"
        val alertSettings = repository.loadAlertSettings()

        // 1. Save draft with custom Viewer ID
        repository.saveDraftSettings(
            deviceId = "T-ORIG",
            viewerId = customViewerId,
            relayUrl = "https://relay.io",
            maxDistance = 100.0,
            alertSettings = alertSettings
        )

        // 2. Commit draft
        val result = repository.commitDraftSettings()

        // 3. Verify success and persistence
        assertTrue("Commit should succeed", result.anyChanged)
        assertFalse("There should be no uniqueness error", result.error != null)
        
        val persistedViewerId = repository.viewerIdFlow.first()
        assertEquals("Viewer ID must persist custom value", customViewerId, persistedViewerId)
    }

    @Test
    fun verifyUniquenessEnforcement() = runBlocking {
        val alertSettings = repository.loadAlertSettings()

        // Attempt to set Tracker and Viewer IDs to the same value
        repository.saveDraftSettings(
            deviceId = "SAME-ID",
            viewerId = "SAME-ID",
            relayUrl = "https://relay.io",
            maxDistance = 100.0,
            alertSettings = alertSettings
        )

        val result = repository.commitDraftSettings()

        assertTrue("Commit must fail due to non-unique IDs", result.error != null)
        assertEquals("Identity Conflict: Some IDs (e.g., 'viewer', 'Trk') are reserved for cross-version compatibility. Please choose unique IDs.", result.error)
        
        // Verify primary values did not change
        assertEquals("T-ORIG", repository.trackerIdFlow.first())
        assertEquals("V-ORIG", repository.viewerIdFlow.first())
    }
    
    @Test
    fun verifyDefaultIdentityPersistence() = runBlocking {
        // Clear everything to simulate fresh install
        repository.saveString(TRACKER_ID_KEY, "")
        repository.saveString(VIEWER_ID_KEY, "")
        
        assertEquals("Should default to T", SettingsRepository.DEFAULT_TRACKER_ID, repository.trackerIdFlow.first())
        assertEquals("Should default to V", SettingsRepository.DEFAULT_VIEWER_ID, repository.viewerIdFlow.first())
        
        val alertSettings = repository.loadAlertSettings()
        
        // Save draft without changing IDs (should use defaults)
        repository.saveDraftSettings(
            deviceId = SettingsRepository.DEFAULT_TRACKER_ID,
            viewerId = SettingsRepository.DEFAULT_VIEWER_ID,
            relayUrl = SettingsRepository.DEFAULT_RELAY_URL,
            maxDistance = SettingsRepository.DEFAULT_MAX_DISTANCE,
            alertSettings = alertSettings
        )
        
        val result = repository.commitDraftSettings()
        
        assertFalse("Commit should not fail on defaults: ${result.error}", result.error != null)
        assertEquals(SettingsRepository.DEFAULT_VIEWER_ID, repository.viewerIdFlow.first())
    }
}
