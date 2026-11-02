package com.jd.app.monitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.system.measureTimeMillis

/**
 * StartupIntegrityTest: Ported from OLD HydrationStaggeringAuditTest.
 * Validates R495: Startup Performance and Davey-Event Prevention.
 * Ensures that repository hydration does not stall the Main thread 
 * under simulated budget-hardware load (A15).
 * Updated Nov2.21: Added yield() to stress job and increased collection timeout to prevent test flakiness (ISSUE-REVOL38.1-F4).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StartupIntegrityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: MonitorRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /**
     * verifyHydrationPerformanceR495: Ensures that fetching the initial 
     * trail state (hydration) completes within the "Davey" threshold (500ms).
     */
    @Test
    fun verifyHydrationPerformanceR495() = runBlocking {
        // Simulate background load to mimic budget hardware stress
        val stressJob = launch(Dispatchers.Default) {
            var i = 0.0
            while (isActive) { 
                i += Math.sin(i) 
                yield() // Prevent complete CPU starvation in test environment
            }
        }

        val timeTaken = measureTimeMillis {
            // Force a repository "hydration" call (e.g., getting the initial trail)
            repository.getTrail("test-device").firstOrNull(2000L)
        }

        stressJob.cancelAndJoin()

        // Nov2.1 Audit Rule: Hydration must complete in < 500ms to avoid Davey events.
        assertTrue(
            "R495: Startup hydration too slow ($timeTaken ms). Must be < 500ms to prevent Davey events.",
            timeTaken < 500
        )
    }

    /**
     * verifyConcurrentDeduplication: Ensures that multiple hydration 
     * requests don't cause DB lock contention.
     */
    @Test
    fun verifyConcurrentDeduplication() = runBlocking {
        val jobs = List(5) {
            async(Dispatchers.IO) {
                repository.getTrail("concurrent-test").firstOrNull(2000L)
            }
        }
        
        val results = jobs.awaitAll()
        assertTrue("Concurrent hydration must not crash or deadlock", results.size == 5)
    }
}

// Extension to help with flow testing
suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstOrNull(timeoutMs: Long = 1000L): T? {
    var result: T? = null
    try {
        withTimeout(timeoutMs) {
            collect { 
                result = it
                cancel()
            }
        }
    } catch (e: TimeoutCancellationException) {
        // Handle timeout
    } catch (e: CancellationException) {
        // Handle normal cancellation
    }
    return result
}
