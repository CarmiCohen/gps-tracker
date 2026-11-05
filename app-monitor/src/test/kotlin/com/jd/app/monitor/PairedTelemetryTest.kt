package com.jd.app.monitor

import com.jd.core.shared.LocationUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies Chapter 120.9: the HUD only shows data from the paired device. */
class PairedTelemetryTest {

    private val update = LocationUpdate(deviceId = "T")

    @Test
    fun updateFromThePairedDeviceIsKept() {
        assertEquals(update, update.ifFrom("T"))
    }

    @Test
    fun updateFromAnotherDeviceIsDropped() {
        assertNull(update.ifFrom("T2"))
    }

    @Test
    fun noPairedDeviceShowsNothing() {
        assertNull(update.ifFrom(null))
    }

    @Test
    fun noUpdateStaysEmpty() {
        assertNull((null as LocationUpdate?).ifFrom("T"))
    }
}
