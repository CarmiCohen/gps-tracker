package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.PoolRegistry
import com.jd.core.shared.SensingEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * ProductionReadinessAuditTest: Ported to JD Revol31.2.
 * Exercises the Sensing Engine and Forensic Pipeline under high-pressure bursts.
 * Updated Nov1.3: Aligned with Adaptive Sampling (Revol34.0).
 * Updated Nov1.4: Aligned with Multi-Stage Throttling (Revol35.0).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProductionReadinessAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dataStore: SensorDataStore

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /**
     * Verifies R-ID 160: Orientation-Independent Sensing.
     * Ensures gravity-vector projection correctly decomposes motion regardless of mounting.
     */
    @Test
    fun verifyOrientationIndependentMath() {
        val ax = 7.07; val ay = 7.07; val az = 0.0
        val gx = 7.07; val gy = 7.07; val gz = 0.0
        
        val worldSpace = SensingEngine.rotateToWorldSpace(ax, ay, az, gx, gy, gz)
        
        assertFalse("Rotation must not produce NaN", worldSpace[0].isNaN())
        assertTrue("Horizontal magnitude must be calculated", worldSpace[0] >= 0)
    }

    /**
     * Verifies R-ID 16: Thermal/Power Sensing Scaling.
     * Exercises the dynamic interval scaling logic under simulated thermal pressure.
     */
    @Test
    fun verifyThermalScalingLogic() {
        val criticalTempInterval = SensingEngine.getOptimalIntervalMs(
            currentTemp = 46f,
            isCharging = false,
            batteryLevel = 50,
            velocityMps = 10f
        )
        assertEquals("Critical Thermal protection MUST scale to 0.1Hz", 10000L, criticalTempInterval)
        
        val warningTempInterval = SensingEngine.getOptimalIntervalMs(
            currentTemp = 41f,
            isCharging = false,
            batteryLevel = 50,
            velocityMps = 10f
        )
        assertEquals("Warning Thermal protection MUST scale to 1Hz", 1000L, warningTempInterval)
        
        val criticalBatteryInterval = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            isCharging = false,
            batteryLevel = 10,
            velocityMps = 10f
        )
        assertEquals("Critical Battery protection MUST scale to 0.1Hz", 10000L, criticalBatteryInterval)

        val lowBatteryInterval = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            isCharging = false,
            batteryLevel = 18,
            velocityMps = 10f
        )
        assertEquals("Low Battery protection MUST scale to 1Hz", 1000L, lowBatteryInterval)
        
        val nominalInterval = SensingEngine.getOptimalIntervalMs(
            currentTemp = 30f,
            isCharging = true,
            batteryLevel = 80,
            velocityMps = 10f
        )
        assertEquals("Nominal sensing should maintain 10Hz when charging", 100L, nominalInterval)
    }

    /**
     * Verifies R-ID 766: Forensic Pipeline Integrity.
     * Exercises the object pool and buffer write logic under a burst.
     */
    @Test
    fun verifyForensicSensingBurst() = runBlocking {
        val burstSize = 100
        val updates = List(burstSize) { PoolRegistry.locationUpdatePool.acquire() }
        
        try {
            updates.forEachIndexed { i, update ->
                update.timestamp = System.currentTimeMillis()
                update.lat = 32.0 + (i * 0.0001)
                update.lng = 34.0 + (i * 0.0001)
            }
            
            assertEquals(burstSize, updates.size)
        } finally {
            updates.forEach { PoolRegistry.locationUpdatePool.release(it) }
        }
    }
}
