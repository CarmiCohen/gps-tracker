package com.jd.app.monitor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.ui.FixReading
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * ISSUE-NOV27.2-O170, chapter 148.1: the Monitor's own fixes reach its stored trail through the real path,
 * [OwnTrailRecorder] to [MonitorRepository.insertLocalTrail] to Room, one own row per fix, under this Monitor's ID.
 * Replaces `LocalTrailTest`, whose checks built rows and filtered lists of their own and never ran this path, so the
 * trail that barely grew (83 rows in 48 h) passed it. Android's background location limit is checked by chapter 264.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OwnTrailRecordingTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var repository: MonitorRepository
    @Inject lateinit var identityManager: ForensicIdentityManager
    @Inject lateinit var trailDao: TrailDao

    /** Fixes the test sends; replayed to a late collector, so no fix is lost before the recorder subscribes. */
    private val fixes = MutableSharedFlow<LocalFix>(replay = 16)
    private var permitted = true
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val source = object : MonitorLocationSource(ApplicationProvider.getApplicationContext<Context>()) {
        override fun hasPermission() = permitted
        override fun fixes(): Flow<LocalFix> = this@OwnTrailRecordingTest.fixes
    }

    private lateinit var recorder: OwnTrailRecorder

    @Before
    fun setUp() = runBlocking {
        hiltRule.inject()
        identityManager.setSelfId(FIRST_ID)
        recorder = OwnTrailRecorder(source, identityManager, repository)
    }

    @After
    fun tearDown() = scope.cancel()

    private fun fix(index: Int) = LocalFix(
        MapPosition(32.0 + index * 0.001, 35.0),
        FixReading(timeMs = BASE_MS + index * 10_000L, accuracyMeters = 4f + index, speedMps = 1.2f)
    )

    private fun ownRows(id: String) = runBlocking { trailDao.getTrail(id).first().filter { it.isLocalUser } }

    /** Waits until [id] holds [count] own rows, then a little longer, so a doubled row would show. */
    private fun awaitOwnRows(id: String, count: Int): List<TrailEntity> = runBlocking {
        withTimeout(5_000) { while (ownRows(id).size < count) delay(POLL_MS) }
        delay(300)
        ownRows(id)
    }

    @Test
    fun everyFixIsStoredAsAnOwnRowOfThisMonitor() = runBlocking {
        recorder.watch(scope)
        (1..3).forEach { fixes.emit(fix(it)) }

        val rows = awaitOwnRows(FIRST_ID, 3).sortedBy { it.timestamp }
        assertEquals(3, rows.size)
        assertEquals(listOf(BASE_MS + 10_000L, BASE_MS + 20_000L, BASE_MS + 30_000L), rows.map { it.timestamp })
        assertEquals(32.001, rows.first().lat, 1e-9)
        assertEquals(5f, rows.first().accuracy, 0f)
        assertTrue("Own rows are VALID", rows.all { it.status == "VALID" })
    }

    @Test
    fun aSecondStartRecordsEachFixOnce() = runBlocking {
        recorder.watch(scope)
        recorder.watch(scope) // A session restarted by Android calls watch again.
        (1..2).forEach { fixes.emit(fix(it)) }

        assertEquals(2, awaitOwnRows(FIRST_ID, 2).size)
    }

    @Test
    fun aNewMonitorIdTakesTheNextFixes() = runBlocking {
        recorder.watch(scope)
        fixes.emit(fix(1))
        awaitOwnRows(FIRST_ID, 1)

        identityManager.setSelfId(SECOND_ID)
        // The source restarts for the new ID and begins with the fix it had, as the real one does with its last known fix.
        fixes.emit(fix(2))

        val rows = awaitOwnRows(SECOND_ID, 2)
        assertEquals(listOf(BASE_MS + 10_000L, BASE_MS + 20_000L), rows.map { it.timestamp }.sorted())
        assertEquals("The old ID keeps its row", 1, ownRows(FIRST_ID).size)
    }

    @Test
    fun withoutLocationPermissionNothingIsRecorded() = runBlocking {
        permitted = false
        recorder.watch(scope)
        fixes.emit(fix(1))
        delay(500)

        assertEquals(0, ownRows(FIRST_ID).size)
    }

    private companion object {
        const val FIRST_ID = "MTEST1"
        const val SECOND_ID = "MTEST2"
        const val BASE_MS = 1_764_000_000_000L
        const val POLL_MS = 50L
    }
}
