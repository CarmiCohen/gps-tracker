package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 203: live and backfilled rows share one encoder, and both carry satellites, signal and noise
 * (ISSUE-NOV15.5-O99 part 2). Only live rows carry `noiseOff` and `crit`.
 */
class LocationRowWireTest {

    private fun row() = LocationUpdate(
        sequenceId = 42L, timestamp = 1_000L, lat = 32.1, lng = 34.8, status = "VALID",
        satsUsed = 8, satsInView = 20, meanCn0DbHz = 33.5f, noiseDbfs = -60f,
        isNoiseDetectionOff = true, isCritical = true
    )

    @Test
    fun backfilledRowCarriesSatellitesSignalAndNoise() {
        val decoded = LocationRowWire.decode(LocationRowWire.encode(row(), "S1", live = false))

        assertEquals("S1", decoded.deviceId)
        assertEquals(42L, decoded.sequenceId)
        assertEquals(8, decoded.satsUsed)
        assertEquals(20, decoded.satsInView)
        assertEquals(33.5f, decoded.meanCn0DbHz!!, 0.001f)
        assertEquals(-60f, decoded.noiseDbfs!!, 0.001f)
    }

    @Test
    fun onlyLiveRowsCarryNoiseOffAndCritical() {
        val backfill = LocationRowWire.encode(row(), "S1", live = false)
        assertFalse(backfill.has("noiseOff"))
        assertFalse(backfill.has("crit"))

        val live = LocationRowWire.encode(row(), "S1", live = true)
        assertTrue(live.getBoolean("noiseOff"))
        assertTrue(live.getBoolean("crit"))
    }

    @Test
    fun unknownValuesAreLeftOutAndReadBackAsUnknown() {
        val encoded = LocationRowWire.encode(LocationUpdate(sequenceId = 1L), "S1", live = false)
        listOf("sats", "satsView", "snr", "noise").forEach { assertFalse(it, encoded.has(it)) }

        val decoded = LocationRowWire.decode(encoded)
        assertEquals(-1, decoded.satsUsed)
        assertEquals(-1, decoded.satsInView)
        assertNull(decoded.meanCn0DbHz)
        assertNull(decoded.noiseDbfs)
    }
}
