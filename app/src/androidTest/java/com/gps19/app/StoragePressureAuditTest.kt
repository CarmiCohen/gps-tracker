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
 * Oct.20.10:
 * - ISSUE-OCT18.22-F3: Removed triggerManualBatchProcessor() from init to prevent race 
 *   conditions where background processing steals logs from the buffer before flushLogs().
 *   Added delay(500) before assertions to ensure async trail persistence completes.
 * Oct.20.2:
 * - ISSUE-OCT18.22-F3: Replaced delay(3000) with deterministic logRepository.flushLogs() 
 *   and flushForensicTraces() to eliminate race conditions.
 * Oct.18.18:
 * - Maintenance: Explicitly triggered manual batch processor for test isolation.
 * Oct.15.9:
 * - Issue Fix: Updated TrailBuffer assertions to use primitive array access 
 *   (lats.any) to resolve compilation errors.
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
    @Inject lateinit var logRepository: LogRepository
    @Inject lateinit var timeProvider: TimeProvider

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking {
            logRepository.resetForTest()
            // Oct.20.10: triggerManualBatchProcessor() removed to avoid theft of logs 
            // from the buffer by the background loop during deterministic testing.
            repository.clearLogs()
            repository.clearTrails()
        }
    }

    @Test
    fun verifyLowStoragePrioritization() = runBlocking {
        // 1. Simulate Storage Low (95%)
        integrityMonitor.simulateStoragePressure(active = true, critical = false)
        delay(300) // Allow state to propagate to all observers

        // 2. Attempt to log different priorities
        logManager.submitToLogSink("NORMAL_LOG", "system", isImportant = false)
        logManager.submitToLogSink("IMPORTANT_LOG", "system", isImportant = true)
        logManager.submitToLogSink("SPECIAL_LOG", "system", isImportant = false, isSpecial = true)

        // 3. Attempt to save trail points
        repository.saveTrailPoint(1.0, 1.0, isViewer = false, status = SentinelStatus.VALID)
        repository.saveTrailPoint(2.0, 2.0, isViewer = false, status = SentinelStatus.TAMPER)

        // Oct.20.2: Deterministic flush instead of delay
        logRepository.flushLogs()
        logRepository.flushForensicTraces()

        // Oct.20.10: Brief delay for async trail persistence (which uses repository.scope)
        delay(500)

        val logs = repository.loadAllLogsStatic(100)
        val trail = repository.loadTrailStatic(false)

        // Verify: Normal log dropped, Important/Special kept
        assertFalse("Normal log should be dropped in Low Storage", logs.any { it.message == "NORMAL_LOG" })
        assertTrue("Important log should be kept in Low Storage", logs.any { it.message == "IMPORTANT_LOG" })
        assertTrue("Special log should be kept in Low Storage", logs.any { it.message == "SPECIAL_LOG" })

        // Verify: Normal trail dropped, Tamper (Special) kept
        assertFalse("Normal trail point should be dropped in Low Storage", trail.lats.any { it == 1.0 })
        assertTrue("High priority trail point should be kept in Low Storage", trail.lats.any { it == 2.0 })
    }

    @Test
    fun verifyCriticalStoragePrioritization() = runBlocking {
        // 1. Simulate Storage Critical (99%)
        integrityMonitor.simulateStoragePressure(active = true, critical = true)
        delay(300)

        // 2. Attempt to log
        logManager.submitToLogSink("IMPORTANT_LOG", "system", isImportant = true)
        logManager.submitToLogSink("SPECIAL_LOG", "system", isImportant = false, isSpecial = true)

        // 3. Attempt to save trail points
        repository.saveTrailPoint(2.0, 2.0, isViewer = false, status = SentinelStatus.TAMPER)

        // Oct.20.2: Deterministic flush
        logRepository.flushLogs()
        logRepository.flushForensicTraces()

        // Oct.20.10: Brief delay for async trail persistence
        delay(500)

        val logs = repository.loadAllLogsStatic(100)
        val trail = repository.loadTrailStatic(false)

        // Verify: Even Important logs dropped, only Special kept
        assertFalse("Important log should be dropped in Critical Storage", logs.any { it.message == "IMPORTANT_LOG" })
        assertTrue("Special log should be kept in Critical Storage", logs.any { it.message == "SPECIAL_LOG" })

        // Verify: All trails dropped in Critical Storage
        assertTrue("All trail points should be dropped in Critical Storage (Size was ${trail.size})", trail.size == 0)
    }
}
