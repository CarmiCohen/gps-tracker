package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.SentinelLogic
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HardeningAuditTest: Ported from OLD codebase.
 * Validates Anomaly Correlation (R133) and Silent Failure logic in pure Kotlin.
 */
@RunWith(AndroidJUnit4::class)
class HardeningAuditTest {

    @Test
    fun verifyAnomalyCorrelationR133() {
        // R133: GPS Stall + High CPU (>=0.85) should trigger Silent Failure
        val isSilent = SentinelLogic.isSilentFailure(
            gpsStalled = true,
            isTamperDetected = false,
            cpuLoad = 0.9,
            ioWait = 0.1,
            maxIoLatency = 100,
            isThermalThrottling = false
        )
        assertTrue("R133: GPS Stall + High CPU MUST trigger Silent Failure", isSilent)

        val isNotSilent = SentinelLogic.isSilentFailure(
            gpsStalled = true,
            isTamperDetected = false,
            cpuLoad = 0.1,
            ioWait = 0.1,
            maxIoLatency = 100,
            isThermalThrottling = false
        )
        assertFalse("R133: GPS Stall + Low Load should NOT trigger Silent Failure", isNotSilent)
    }

    @Test
    fun verifyTamperPrecedence() {
        // Tamper detection should inhibit Silent Failure flagging to ensure 
        // the theft alarm takes priority over "hardware stress" suppression.
        val isSilentWithTamper = SentinelLogic.isSilentFailure(
            gpsStalled = true,
            isTamperDetected = true,
            cpuLoad = 0.9,
            ioWait = 0.5,
            maxIoLatency = 1000,
            isThermalThrottling = true
        )
        assertFalse("Tamper detection MUST inhibit silent failure suppression", isSilentWithTamper)
    }
}
