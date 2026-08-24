package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gps19.core.engine.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * StoragePressureAuditTest: Verifies Chapter 12.3 Persistence Prioritization.
 * Validates that LogManager and PersistencePolicy correctly gate data based on 
 * storage pressure thresholds (R197).
 * Aug.22.05:
 * - Issue Fix: Increased flush delay to 3000ms to align with LogRepository's 
 *   LOG_BATCH_DELAY_MS (2000ms) to resolve non-deterministic test failures.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StoragePressureAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var integrityMonitor: IntegrityMonitor
    @Inject lateinit var logManager: LogManager
    @Inject lateinit var repository: MainRepository
    @Inject lateinit var timeProvider: TimeProvider

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking {
            repository.clearLogs()
            repository.clearTrails()
        }
    }

    @Test
    fun verifyLowStoragePrioritization() = runBlocking {
        // 1. Simulate Storage Low (95%)
        integrityMonitor.simulateStoragePressure(active = true, critical = false)
        delay(200) // Allow flow to propagate

        // 2. Attempt to log different priorities
        logManager.submitToLogSink("NORMAL_LOG", "system", isImportant = false)
        logManager.submitToLogSink("IMPORTANT_LOG", "system", isImportant = true)
        logManager.submitToLogSink("SPECIAL_LOG", "system", isImportant = false, isSpecial = true)

        // 3. Attempt to save trail points
        repository.saveTrailPoint(1.0, 1.0, isViewer = false, status = SentinelStatus.VALID)
        repository.saveTrailPoint(2.0, 2.0, isViewer = false, status = SentinelStatus.TAMPER)

        // R197 Audit: Must exceed LOG_BATCH_DELAY_MS (2000ms) to guarantee flush
        delay(3000) 

        val logs = repository.loadAllLogsStatic(100)
        val trail = repository.loadTrailStatic(false)

        // Verify: Normal log dropped, Important/Special kept
        assertFalse("Normal log should be dropped in Low Storage", logs.any { it.message == "NORMAL_LOG" })
        assertTrue("Important log should be kept in Low Storage", logs.any { it.message == "IMPORTANT_LOG" })
        assertTrue("Special log should be kept in Low Storage", logs.any { it.message == "SPECIAL_LOG" })

        // Verify: Normal trail dropped, Tamper (Special) kept
        assertFalse("Normal trail point should be dropped in Low Storage", trail.any { it.lat == 1.0 })
        assertTrue("High priority trail point should be kept in Low Storage", trail.any { it.lat == 2.0 })
    }

    @Test
    fun verifyCriticalStoragePrioritization() = runBlocking {
        // 1. Simulate Storage Critical (99%)
        integrityMonitor.simulateStoragePressure(active = true, critical = true)
        delay(200)

        // 2. Attempt to log
        logManager.submitToLogSink("IMPORTANT_LOG", "system", isImportant = true)
        logManager.submitToLogSink("SPECIAL_LOG", "system", isImportant = false, isSpecial = true)

        // 3. Attempt to save trail points
        repository.saveTrailPoint(2.0, 2.0, isViewer = false, status = SentinelStatus.TAMPER)

        // R197 Audit: Wait for batch processor
        delay(3000)

        val logs = repository.loadAllLogsStatic(100)
        val trail = repository.loadTrailStatic(false)

        // Verify: Even Important logs dropped, only Special kept
        assertFalse("Important log should be dropped in Critical Storage", logs.any { it.message == "IMPORTANT_LOG" })
        assertTrue("Special log should be kept in Critical Storage", logs.any { it.message == "SPECIAL_LOG" })

        // Verify: All trails dropped in Critical Storage
        assertTrue("All trail points should be dropped in Critical Storage", trail.isEmpty())
    }
}
