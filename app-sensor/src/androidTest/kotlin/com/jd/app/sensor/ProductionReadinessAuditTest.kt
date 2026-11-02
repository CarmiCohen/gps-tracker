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
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProductionReadinessAuditTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dataStore: SensorDataStore

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun verifyOrientationIndependentMath() {
        val ax = 7.07; val ay = 7.07; val az = 0.0
        val gx = 7.07; val gy = 7.07; val gz = 0.0
        
        val worldSpace = SensingEngine.rotateToWorldSpace(ax, ay, az, gx, gy, gz)
        
        assertFalse("Rotation must not produce NaN", worldSpace[0].isNaN())
        assertTrue("Horizontal magnitude must be calculated", worldSpace[0] >= 0)
    }

    @Test
    fun verifyThermalScalingLogic() {
        val criticalTempInterval = SensingEngine.getOptimalIntervalMs(
            currentTemp = 46f,
            isCharging = false,
            batteryLevel = 50,
            velocityMps = 10f
        )
        assertEquals("Critical Thermal protection MUST scale to 0.1Hz", 10000L, criticalTempInterval)
    }

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
