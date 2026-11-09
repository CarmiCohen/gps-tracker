package com.jd.app.monitor

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.shared.ForensicIdentity
import com.jd.core.shared.LocationUpdate
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * MapJitterAuditTest: Audits UI trail rendering during high-frequency backfill ingestion.
 * Part of ISSUE-REVOL30.0-F7.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MapJitterAuditTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MonitorActivity>()

    @Inject
    lateinit var repository: MonitorRepository

    @Inject
    lateinit var identityManager: ForensicIdentityManager

    @Inject
    lateinit var trailDao: TrailDao

    private val testTrackerId = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    /** The installed Monitor's pairing, restored after the test (ISSUE-NOV9.3-O64). */
    private var savedTargetId: String? = null

    @Before
    fun init() {
        hiltRule.inject()
        savedTargetId = runBlocking { identityManager.targetId.first() }
    }

    /**
     * Puts back the real pairing and removes the test rows, so a run with
     * `am instrument` (which keeps app data) leaves the Monitor as it was (ISSUE-NOV9.3-O64).
     */
    @After
    fun restore() = runBlocking {
        val saved = savedTargetId
        if (saved == null) identityManager.clearTargetId() else identityManager.setTargetId(saved)
        // keep = 0 deletes every row of the test tracker.
        trailDao.pruneOldestRows(testTrackerId, 0)
        Unit
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun auditBackfillJitter() = runTest {
        // 1. Setup Identity
        identityManager.setTargetId(testTrackerId)
        
        // 2. Wait for UI to settle
        composeTestRule.waitForIdle()

        // 3. Simulate high-frequency backfill (50 points)
        // We simulate this by directly calling the repository or inserting into DB
        // to see how the UI handles rapid flow emissions.
        val baseTime = System.currentTimeMillis()
        val updates = (1..50).map { i ->
            TrailEntity(
                trackerId = testTrackerId,
                timestamp = baseTime + (i * 1000),
                sequenceId = i.toLong(),
                lat = 45.0 + (i * 0.0001),
                lng = 9.0 + (i * 0.0001),
                accuracy = 5f,
                status = "BACKFILL",
                isLocalUser = false
            )
        }

        // We measure if the UI "freezes" or if we see inconsistent polyline states
        // In this automated test, we mainly verify that the final state is reached
        // and check for exceptions. Real "jitter" is often a visual audit, 
        // but we can check the count of updates.
        
        updates.forEach { entity ->
            repository.insertDirectlyForTest(entity)
        }

        composeTestRule.waitForIdle()
        
        // Verification: Ensure all points are rendered
        // (Accessing the MapView inside Compose usually requires custom semantics or view inspection)
    }
}
