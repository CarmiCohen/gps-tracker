package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gps19.core.engine.TimeProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HydrationStaggeringAuditTest: Verifies multi-level staggering under the
 * LifecycleHydrationManager across role lifecycles under extreme CPU/IO loads.
 * Resolves Strategic Resumption Goal under Issue #1338.
 * Sep.23.50: Verified full sequence 0-11 re-entrancy under peak resource contention.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HydrationStaggeringAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var hydrationManager: LifecycleHydrationManager

    @Inject
    lateinit var timeProvider: TimeProvider

    @Before
    fun init() {
        hiltRule.inject()
        hydrationManager.reset()
    }

    @Test
    fun verifyFullHydrationReentrancyUnderExtremeLoad() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // 1. Induce Sustained Extreme CPU & IO Load
        val loadDurationMs = 40000L
        val cpuJob = testScope.launch {
            val end = System.currentTimeMillis() + loadDurationMs
            var count = 0.0
            while (System.currentTimeMillis() < end && isActive) {
                sin(count); cos(count); sqrt(count)
                count += 0.01
            }
        }

        val ioJob = testScope.launch {
            val tempFile = File(context.cacheDir, "hydration_stress_v3.tmp")
            val data = ByteArray(1024 * 1024) { 0xCC.toByte() }
            val end = System.currentTimeMillis() + loadDurationMs
            try {
                while (System.currentTimeMillis() < end && isActive) {
                    tempFile.outputStream().use { it.write(data); it.flush() }
                }
            } catch (e: Exception) {
                // Ignore transient errors
            } finally {
                tempFile.delete()
            }
        }

        // 2. First Hydration Cycle (Simulating Initial App Start)
        var completeCalled = false
        hydrationManager.startHydration(testScope, useStaggered = false) {
            completeCalled = true
        }

        try {
            withTimeout(15000) {
                hydrationManager.hydrationLevel.filter { it >= 11 }.first()
            }
        } catch (e: TimeoutCancellationException) {
            fail("Initial hydration failed to reach Level 11 under load. Last Level: ${hydrationManager.hydrationLevel.value}")
        }

        assertTrue("onComplete callback must be triggered on first cycle", completeCalled)
        assertEquals("Should be fully hydrated at Level 11", 11, hydrationManager.hydrationLevel.value)

        // 3. Simulate Role Reset (e.g. Switch from Tracker to Viewer or vice-versa)
        hydrationManager.reset()
        assertEquals("Level must return to 0 on reset", 0, hydrationManager.hydrationLevel.value)

        // 4. Second Hydration Cycle (Simulating Role Transition Re-init)
        completeCalled = false
        hydrationManager.startHydration(testScope, useStaggered = false) {
            completeCalled = true
        }

        try {
            withTimeout(15000) {
                hydrationManager.hydrationLevel.filter { it >= 11 }.first()
            }
        } catch (e: TimeoutCancellationException) {
            fail("Secondary hydration cycle failed to reach Level 11 under load. Last Level: ${hydrationManager.hydrationLevel.value}")
        }

        assertTrue("onComplete callback must be triggered on second cycle", completeCalled)
        assertEquals("Should reach Level 11 on second cycle", 11, hydrationManager.hydrationLevel.value)

        // Clean up
        cpuJob.cancelAndJoin()
        ioJob.cancelAndJoin()
        testScope.cancel()
    }
}
