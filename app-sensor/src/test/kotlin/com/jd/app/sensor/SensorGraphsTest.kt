package com.jd.app.sensor

import com.jd.core.shared.GnssSummary
import com.jd.core.shared.GraphField
import com.jd.core.shared.STANDARD_GRAVITY
import com.jd.core.shared.SensorBehaviour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Chapter 192 (ISSUE-NOV10.8-O84 part 3): what the Sensor's minute sample holds. */
class SensorGraphsTest {

    private fun sample(
        behaviour: SensorBehaviour = SensorBehaviour(),
        vibrationPeak: Double? = 0.5 * STANDARD_GRAVITY,
        gnss: GnssSummary? = GnssSummary(26, 55, 23.94f),
        battery: Int = 87,
        temperature: Float = 31.5f
    ) = sensorGraphSample(
        timeMs = 1_800_000_000_000L,
        vibrationPeakMps2 = vibrationPeak,
        tiltPeakDeg = 4.0,
        noisePeakDbfs = -61.0,
        behaviour = behaviour,
        vibrationLimitMps2 = 0.8 * STANDARD_GRAVITY,
        tiltLimitDeg = 15.0,
        fixAccuracyM = 14f,
        gnss = gnss,
        linkUpPercent = 75f,
        latencyMs = 361L,
        queuedRows = 0,
        batteryPercent = battery,
        isCharging = true,
        temperatureC = temperature
    )

    @Test
    fun `the minute's readings land in their fields, vibration in g`() {
        val sample = sample()
        assertEquals(0.5f, sample[GraphField.VIBRATION_G])
        assertEquals(0.8f, sample[GraphField.VIBRATION_LIMIT_G])
        assertEquals(4f, sample[GraphField.TILT_DEG])
        assertEquals(15f, sample[GraphField.TILT_LIMIT_DEG])
        assertEquals(-61f, sample[GraphField.NOISE_DBFS])
        assertEquals(14f, sample[GraphField.ACCURACY_M])
        assertEquals(26f, sample[GraphField.SATS_USED])
        assertEquals(55f, sample[GraphField.SATS_IN_VIEW])
        assertEquals(23.9f, sample[GraphField.SIGNAL_DBHZ])
        assertEquals(75f, sample[GraphField.LINK_UP_PERCENT])
        assertEquals(361f, sample[GraphField.LATENCY_MS])
        assertEquals(0f, sample[GraphField.QUEUED_ROWS])
        assertEquals(87f, sample[GraphField.BATTERY_PERCENT])
        assertEquals(1f, sample[GraphField.CHARGING])
        assertEquals(31.5f, sample[GraphField.TEMPERATURE_C])
        assertNull("The Monitor's own fields", sample[GraphField.ROWS_LIVE])
    }

    @Test
    fun `a switched-off kind has no limit, and an unknown reading stays unknown`() {
        val sample = sample(
            behaviour = SensorBehaviour(vibrationAlert = false, tiltAlert = false),
            vibrationPeak = null,
            gnss = null,
            battery = -1,
            temperature = -1f
        )
        assertNull(sample[GraphField.VIBRATION_LIMIT_G])
        assertNull(sample[GraphField.TILT_LIMIT_DEG])
        assertNull(sample[GraphField.VIBRATION_G])
        assertNull(sample[GraphField.SATS_USED])
        assertNull(sample[GraphField.SIGNAL_DBHZ])
        assertNull(sample[GraphField.BATTERY_PERCENT])
        assertNull(sample[GraphField.CHARGING])
        assertNull(sample[GraphField.TEMPERATURE_C])
    }
}
