package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 203: live and backfilled rows share one encoder, and both carry satellites, signal and noise
 * (ISSUE-NOV15.5-O99 part 2). Only live rows carry `noiseOff` and `crit`. Chapter 207: battery, charging and
 * temperature travel on both, now that the forensic buffer keeps them (ISSUE-NOV17.3-O102). Chapter 244: the light
 * level travels on both as `lux` (SIMP-NOV22.4-S1). Chapter 247: the vibration travels on both as `vib`
 * (SIMP-NOV22.11-S1). Chapter 249: battery and temperature are left out while not known, and any temperature sent,
 * also -1 or below zero, is a reading (SIMP-NOV17.7-S1).
 */
class LocationRowWireTest {

    private fun row() = LocationUpdate(
        sequenceId = 42L, timestamp = 1_000L, lat = 32.1, lng = 34.8, status = "VALID",
        satsUsed = 8, satsInView = 20, meanCn0DbHz = 33.5f, noiseDbfs = -60f, lightLux = 412.5f,
        batteryLevel = 57, isCharging = true, tempCelsius = 31.4f, vibrationG = 0.25f,
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
        assertEquals(57, decoded.batteryLevel)
        assertTrue(decoded.isCharging)
        assertEquals(31.4f, decoded.tempCelsius!!, 0.001f)
        assertEquals(412.5f, decoded.lightLux!!, 0.001f)
        assertEquals(0.25f, decoded.vibrationG, 0.001f)
    }

    @Test
    fun liveRowCarriesTheVibrationAsVib() {
        val live = LocationRowWire.encode(row(), "S1", live = true)
        assertEquals(0.25, live.getDouble("vib"), 0.001)
        assertEquals(0.25f, LocationRowWire.decode(live).vibrationG, 0.001f)
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
        listOf("sats", "satsView", "snr", "noise", "lux", "batt", "temp").forEach { assertFalse(it, encoded.has(it)) }

        val decoded = LocationRowWire.decode(encoded)
        assertNull(decoded.batteryLevel)
        assertNull(decoded.tempCelsius)
        assertEquals(-1, decoded.satsUsed)
        assertEquals(-1, decoded.satsInView)
        assertNull(decoded.meanCn0DbHz)
        assertNull(decoded.noiseDbfs)
        assertNull(decoded.lightLux)
    }

    /** Chapter 249 (SIMP-NOV17.7-S1): a cold phone's -1 C and 0 C, and an empty battery, are readings, not unknown. */
    @Test
    fun freezingTemperatureAndEmptyBatteryAreReadings() {
        for (temp in listOf(-1f, 0f, -12.5f)) {
            val cold = row().apply { tempCelsius = temp; batteryLevel = 0 }
            val decoded = LocationRowWire.decode(LocationRowWire.encode(cold, "S1", live = true))
            assertEquals(temp, decoded.tempCelsius!!, 0.001f)
            assertEquals(0, decoded.batteryLevel)
        }
    }

    /** Chapter 249: a Sensor before Nov23.1 sent `batt` -1 when not known; it reads back as not known. */
    @Test
    fun olderSensorsUnknownBatteryReadsAsUnknown() {
        val older = LocationRowWire.encode(row(), "S1", live = true).put("batt", -1)
        assertNull(LocationRowWire.decode(older).batteryLevel)
    }

    /** Chapter 214 (ISSUE-NOV17.15-O114): plugged in travels as `plg`; a row without it reads back as not known. */
    @Test
    fun pluggedInTravelsAndIsUnknownWhenLeftOut() {
        val held = row().apply { isCharging = false; isPluggedIn = true }
        val decoded = LocationRowWire.decode(LocationRowWire.encode(held, "S1", live = true))
        assertFalse(decoded.isCharging)
        assertEquals(true, decoded.isPluggedIn)

        val older = LocationRowWire.encode(row(), "S1", live = true)
        assertFalse(older.has("plg"))
        assertNull(LocationRowWire.decode(older).isPluggedIn)
    }
}
