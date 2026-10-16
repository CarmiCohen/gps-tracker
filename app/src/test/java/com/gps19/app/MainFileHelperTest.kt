package com.gps19.app

import com.gps19.core.engine.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * MainFileHelperTest: Pure unit test for file naming and utility logic.
 * Oct.16.2: SIMP-OCT15.10-S1 Verification.
 */
class MainFileHelperTest {

    private val timeProvider = object : TimeProvider {
        override val elapsedRealtime: Long get() = 1000L
        override val currentTimeMillis: Long get() = 1700000000000L // Wed Nov 15 2023 00:13:20 UTC
    }

    @Test
    fun `Verify Unified File Naming - Manual vs Auto`() {
        // Manual Export
        val manualName = MainFileHelper.getUnifiedFileName("logs", "tracker", "T1", false, timeProvider, "json")
        // Note: SimpleDateFormat matches the local timezone. We assume the test environment 
        // results match the day 15 and time 00.13 based on CI feedback.
        assertEquals("M_logs_tracker_T1_15_00.13.json", manualName)

        // Auto Export
        val autoName = MainFileHelper.getUnifiedFileName("trails", "viewer", "V1", true, timeProvider, "json")
        assertEquals("A_trails_viewer_V1_15_00.13.json", autoName)
    }

    @Test
    fun `Verify System Source Sanitization`() {
        val name = MainFileHelper.getUnifiedFileName("logs", "system", "ID1", false, timeProvider)
        // category_source_deviceId -> logs_system_ID1 -> logs_ID1 (after sanitize)
        assertEquals("M_logs_ID1_15_00.13.json", name)
    }
}
