package com.jd.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ISSUE-NOV7.19-O43: the Sensor reports NO_FIX once its newest fix is no longer current. The rule itself and its
 * tests moved to core-ui (`com.jd.core.ui.FixFreshnessTest`, SIMP-NOV7.21-S1); the service reads it through
 * `SensorFixStore.currentLocation`.
 */
class FixFreshnessTest {

    @Test
    fun theNoFixStatusIsTheOneTheMonitorReads() {
        assertEquals("NO_FIX", NO_FIX_STATUS)
    }
}
