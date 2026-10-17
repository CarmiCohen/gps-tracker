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
 * NativeThrottlingStressTest: Verifies the propagation of native load-shedding signals (R-ID 603).
 * Oct16.11: Validates that isMemoryPressureThrottled propagates from SystemPressureBatch 
 * to sensor-specific batches (Vibration, Acoustic, Proximity) via JNI shared state.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NativeThrottlingStressTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var hardwareSuite: HardwareSuite

    @Inject
    lateinit var timeProvider: TimeProvider

    @Before
    fun init() {
        hiltRule.inject()
        GpsApplication.resetTestStates()
    }

    @Test
    fun verifyMemoryPressureThrottlingPropagation() = runBlocking {
        if (!JdHardwareManager.isAvailable()) {
            // Native layer must be initialized for this test to be valid
            val initialized = JdHardwareManager.initialize(timeProvider, "TEST_DEVICE")
            assertTrue("Native SDK must initialize for throttling verification", initialized)
        }

        // 1. Simulate Critical Memory Pressure
        val pressureBatch = SystemPressureBatch().apply {
            heapMb = 300.0
            memPressureThresholdMb = 200.0
            memCriticalThresholdMb = 250.0
            memHysteresisOffsetMb = 15.0
        }

        val pressureRes = JdHardwareManager.processSystemPressureNative(timeProvider, pressureBatch)
        assertTrue("JNI pressure processing should succeed", pressureRes)
        assertEquals("Native level should be CRITICAL (2)", 2, pressureBatch.currentMemLevel)

        // 2. Verify Vibration Throttling
        val vibeBatch = VibrationBatch().apply {
            nowRt = timeProvider.elapsedRealtime
            cpuLoad = 0.5
        }
        val vibeRes = JdHardwareManager.processVibrationBatchNative(timeProvider, vibeBatch, audit = false)
        assertTrue("Vibration JNI should succeed", vibeRes)
        assertTrue("Vibration should be throttled under memory pressure", vibeBatch.isMemoryPressureThrottled)

        // 3. Verify Acoustic Throttling
        val acousticBatch = AcousticBatch().apply {
            readCount = 10
            nowRt = timeProvider.elapsedRealtime
        }
        val acousticRes = JdHardwareManager.processAcousticBatchNative(timeProvider, acousticBatch, ShortArray(10))
        assertTrue("Acoustic JNI should succeed", acousticRes)
        assertTrue("Acoustic should be throttled under memory pressure", acousticBatch.isMemoryPressureThrottled)

        // 4. Verify Proximity Throttling
        val proximityBatch = ProximityBatch().apply {
            nowRt = timeProvider.elapsedRealtime
        }
        val proxRes = JdHardwareManager.processProximityBatchNative(timeProvider, proximityBatch)
        assertTrue("Proximity JNI should succeed", proxRes)
        assertTrue("Proximity should be throttled under memory pressure", proximityBatch.isMemoryPressureThrottled)

        // 5. Verify HardwareSuite Forensic Snapshot Synchronization
        // We trigger an actual sensor update via HardwareSuite's internal batching if possible,
        // but here we verify the resulting snapshot state.
        hardwareSuite.setMemoryPressureThrottled(true)
        val snapshot = hardwareSuite.consumeLogicSnapshot()
        assertTrue("Forensic snapshot must reflect throttled state", snapshot.isMemoryPressureThrottled)
    }

    @Test
    fun verifyThrottlingRecoveryAfterPressureRelief() = runBlocking {
        if (!JdHardwareManager.isAvailable()) {
            JdHardwareManager.initialize(timeProvider, "TEST_DEVICE")
        }

        // 1. Set Critical Pressure
        val pressureBatch = SystemPressureBatch().apply {
            heapMb = 300.0
            memPressureThresholdMb = 200.0
            memCriticalThresholdMb = 250.0
        }
        JdHardwareManager.processSystemPressureNative(timeProvider, pressureBatch)

        // 2. Relief Pressure
        pressureBatch.heapMb = 50.0
        JdHardwareManager.processSystemPressureNative(timeProvider, pressureBatch)
        assertEquals("Native level should be NORMAL (0)", 0, pressureBatch.currentMemLevel)

        // 3. Verify Throttling is cleared in next cycle
        val vibeBatch = VibrationBatch()
        JdHardwareManager.processVibrationBatchNative(timeProvider, vibeBatch, audit = false)
        assertFalse("Vibration throttling should clear after relief", vibeBatch.isMemoryPressureThrottled)
    }
}
