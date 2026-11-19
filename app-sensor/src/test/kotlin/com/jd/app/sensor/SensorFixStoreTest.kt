package com.jd.app.sensor

import com.jd.core.ui.FixReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Chapter 232.1: the store keeps the time of the last satellite fix for the GPS LED, and a stop clears it (Nov19.5). */
class SensorFixStoreTest {

    @Test
    fun theLastSatelliteFixIsKeptUntilTheServiceStops() {
        val store = SensorFixStore()
        assertNull(store.satelliteFixTimeMs.value)

        store.markSatelliteFix(1_000L)
        store.publish(FixReading(timeMs = 900L, accuracyMeters = 5f, speedMps = null))
        assertEquals(1_000L, store.satelliteFixTimeMs.value)

        store.clear()
        assertNull(store.satelliteFixTimeMs.value)
        assertNull(store.fix.value)
    }
}
