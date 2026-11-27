package com.jd.app.sensor

import com.jd.core.shared.GnssSummary
import com.jd.core.shared.GraphField
import com.jd.core.shared.GraphSample
import com.jd.core.shared.STANDARD_GRAVITY
import com.jd.core.shared.SensorBehaviour

/**
 * The Sensor's sample of one minute for the Graphs (ISSUE-NOV10.8-O84 part 3). The peaks are those of the minute, the
 * rest is read at its end. A limit is left out while its kind is switched off on the Monitor, since nothing alerts then;
 * the service's "unknown" readings (battery -1, temperature -1, no fix, no GNSS status) stay unknown.
 */
fun sensorGraphSample(
    timeMs: Long,
    vibrationPeakMps2: Double?,
    tiltPeakDeg: Double?,
    noisePeakDbfs: Double?,
    lightPeakLux: Double?,
    behaviour: SensorBehaviour,
    vibrationLimitMps2: Double,
    tiltLimitDeg: Double,
    /** The level that alerts at the minute's end; null while not listening (ISSUE-NOV25.22-O144). */
    noiseLimitDbfs: Double?,
    /** The light that alerts at the minute's end; null while not measuring (ISSUE-NOV25.22-O144). */
    lightLimitLux: Double?,
    fixAccuracyM: Float?,
    gnss: GnssSummary?,
    linkUpPercent: Float,
    latencyMs: Long?,
    queuedRows: Int,
    batteryPercent: Int?,
    isCharging: Boolean,
    temperatureC: Float?
): GraphSample = GraphSample.of(
    timeMs,
    mapOf(
        GraphField.VIBRATION_G to vibrationPeakMps2?.let { it / STANDARD_GRAVITY },
        GraphField.VIBRATION_LIMIT_G to (vibrationLimitMps2 / STANDARD_GRAVITY).takeIf { behaviour.vibrationAlert },
        GraphField.TILT_DEG to tiltPeakDeg,
        GraphField.TILT_LIMIT_DEG to tiltLimitDeg.takeIf { behaviour.tiltAlert },
        GraphField.NOISE_DBFS to noisePeakDbfs,
        GraphField.ACCURACY_M to fixAccuracyM,
        GraphField.SATS_USED to gnss?.used,
        GraphField.SATS_IN_VIEW to gnss?.inView,
        GraphField.SIGNAL_DBHZ to gnss?.meanCn0DbHz,
        GraphField.LINK_UP_PERCENT to linkUpPercent,
        GraphField.LATENCY_MS to latencyMs,
        GraphField.QUEUED_ROWS to queuedRows,
        GraphField.BATTERY_PERCENT to batteryPercent, // Null when not known, as in the rows (SIMP-NOV17.7-S1).
        GraphField.CHARGING to batteryPercent?.let { if (isCharging) 1 else 0 },
        GraphField.TEMPERATURE_C to temperatureC,
        GraphField.LIGHT_LUX to lightPeakLux,
        GraphField.NOISE_LIMIT_DBFS to noiseLimitDbfs?.takeIf { behaviour.noiseAlert },
        GraphField.LIGHT_LIMIT_LUX to lightLimitLux?.takeIf { behaviour.lightAlert }
    )
)
