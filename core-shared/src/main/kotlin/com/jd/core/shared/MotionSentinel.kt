package com.jd.core.shared

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Detects that the resting tracker is tilted or lifted (ISSUE-NOV5.2-O11 Group 5) or shaken (Group 7), from the accelerometer alone, plus
 * the barometer on phones that have one (user decision, 2026-11-09). The A15 has neither a barometer, a gyroscope, nor
 * a gravity sensor, so everything here works from raw accelerometer samples.
 *
 * - **Gravity**: a low-pass of the accelerometer ([gravity]); also used by the Sensor for its vibration value on phones
 *   without a gravity sensor.
 * - **Armed**: after the phone has rested for [armAfterStillMs]. Arming takes the resting gravity as the reference.
 *   An alert disarms; the next rest re-arms at the new place, so one disturbance gives one alert.
 * - **Tilt**: the gravity direction differs from the reference by at least [tiltThresholdDegrees] for [TILT_CONFIRM_MS].
 * - **Lift (accelerometer)**: from the first motion after arming, the acceleration along the current gravity direction,
 *   minus the resting gravity, is integrated twice over at most [LIFT_WINDOW_MS]; a height of [LIFT_MIN_HEIGHT_M]
 *   alerts. Following the gravity direction keeps a turning phone right, a sideways push adds nothing to first order,
 *   and subtracting the resting magnitude cancels the sensor's own offset.
 * - **Lift (barometer)**: the smoothed pressure moves at least [LIFT_BARO_MIN_M] of height away from a reference that
 *   follows slowly (so weather does not count), for [BARO_CONFIRM_MS].
 * - **Vibration**: one sample differs from the estimated gravity by at least [vibrationThreshold] (a knock, a shove, a
 *   shake). It arms after the same rest but on its own ([isVibrationArmed]): a vibration alert does not disarm tilt and
 *   lift, so a rough pickup gives both, and switching one alarm off on the Monitor never hides the other.
 *
 * Times are monotonic milliseconds (sensor event time). Not thread-safe: one sensor thread owns it; [gravity],
 * [isArmed], and [isVibrationArmed] may be read from any thread.
 */
class MotionSentinel(private val armAfterStillMs: Long = MOTION_ARM_AFTER_STILL_MS) {

    enum class Kind { TILT, LIFT, VIBRATION }

    data class Alert(val kind: Kind, val detail: String)

    /** The tilt that alerts, from the Sensor's own `Tilt Sensitivity Threshold` slider. */
    @Volatile var tiltThresholdDegrees: Double = tiltThresholdForSensitivity(DEFAULT_TILT_SENSITIVITY)

    /** The deviation from gravity in m/s² that alerts, from the Sensor's own `Vibration Sensitivity Threshold` slider. */
    @Volatile var vibrationThreshold: Double = vibrationThresholdForSensitivity(DEFAULT_VIBRATION_SENSITIVITY)

    @Volatile private var gravitySnapshot: DoubleArray? = null
    @Volatile var isArmed = false
        private set
    @Volatile var isVibrationArmed = false
        private set

    private var gx = 0.0
    private var gy = 0.0
    private var gz = 0.0
    private var lastSampleMs = -1L
    private var lastMotionMs = 0L

    private var refX = 0.0
    private var refY = 0.0
    private var refZ = 0.0
    private var refMagnitude = 0.0
    private var tiltSinceMs: Long? = null

    private var liftStartMs: Long? = null
    private var liftVelocity = 0.0
    private var liftHeight = 0.0
    private var liftQuietSinceMs: Long? = null

    private var pressureSmoothed: Double? = null
    private var pressureReference: Double? = null
    private var lastPressureMs = -1L
    private var baroSinceMs: Long? = null

    /** The estimated gravity vector in m/s², or null before the first sample. */
    fun gravity(): DoubleArray? = gravitySnapshot

    /**
     * Feeds one accelerometer sample in m/s²; returns the alerts it caused, almost always none. A vibration and a tilt
     * or lift can end on the same sample, so both are returned.
     */
    fun onAccelerometer(x: Double, y: Double, z: Double, nowMs: Long): List<Alert> {
        if (lastSampleMs < 0) {
            gx = x; gy = y; gz = z
            lastSampleMs = nowMs
            lastMotionMs = nowMs
            publishGravity()
            return emptyList()
        }
        val gapMs = nowMs - lastSampleMs
        lastSampleMs = nowMs
        if (gapMs <= 0) return emptyList()
        if (gapMs > MAX_SAMPLE_GAP_MS) {
            // Samples stopped (sensor paused): neither the integration nor the stillness can bridge the gap.
            gx = x; gy = y; gz = z
            publishGravity()
            liftStartMs = null
            lastMotionMs = nowMs
            return emptyList()
        }
        val dt = gapMs / 1000.0
        // Measured against the gravity before this sample, so a single sharp knock counts at its full size.
        val vibration = norm(x - gx, y - gy, z - gz)
        val alpha = dt / (GRAVITY_TIME_CONSTANT_S + dt)
        gx += alpha * (x - gx)
        gy += alpha * (y - gy)
        gz += alpha * (z - gz)
        publishGravity()

        val deviation = norm(x - gx, y - gy, z - gz)
        val isMoving = deviation > STILL_MAX_DEVIATION
        val vibrationAlert = if (isVibrationArmed && vibration >= vibrationThreshold) {
            isVibrationArmed = false
            Alert(Kind.VIBRATION, "${"%.2f".format(java.util.Locale.US, vibration / STANDARD_GRAVITY)} g (limit ${"%.2f".format(java.util.Locale.US, vibrationThreshold / STANDARD_GRAVITY)} g)")
        } else null
        if (isMoving || vibrationAlert != null) lastMotionMs = nowMs
        val hasRested = nowMs - lastMotionMs >= armAfterStillMs
        if (hasRested) isVibrationArmed = true
        return listOfNotNull(vibrationAlert, tiltOrLift(x, y, z, nowMs, dt, isMoving, hasRested))
    }

    private fun tiltOrLift(x: Double, y: Double, z: Double, nowMs: Long, dt: Double, isMoving: Boolean, hasRested: Boolean): Alert? {
        if (!isArmed) {
            if (hasRested) arm()
            return null
        }

        val angle = angleDegrees(gx, gy, gz, refX, refY, refZ)
        if (angle >= tiltThresholdDegrees) {
            val since = tiltSinceMs ?: nowMs.also { tiltSinceMs = it }
            if (nowMs - since >= TILT_CONFIRM_MS) {
                return disarm(nowMs, Alert(Kind.TILT, "tilted ${angle.toInt()} deg (limit ${tiltThresholdDegrees.toInt()} deg)"))
            }
        } else {
            tiltSinceMs = null
        }

        if (liftStartMs == null && isMoving) {
            liftStartMs = nowMs
            liftVelocity = 0.0
            liftHeight = 0.0
            liftQuietSinceMs = null
        }
        val start = liftStartMs ?: return null
        val gravityLength = norm(gx, gy, gz).coerceAtLeast(1.0) // Only near free fall could it be smaller.
        val upward = (x * gx + y * gy + z * gz) / gravityLength - refMagnitude
        liftVelocity += upward * dt
        liftHeight += liftVelocity * dt
        if (liftHeight >= LIFT_MIN_HEIGHT_M) {
            return disarm(nowMs, Alert(Kind.LIFT, "accelerometer, about ${(liftHeight * 100).toInt()} cm up"))
        }
        liftQuietSinceMs = if (isMoving) null else liftQuietSinceMs ?: nowMs
        val settled = liftQuietSinceMs?.let { nowMs - it >= LIFT_SETTLE_MS } == true
        if (settled || nowMs - start >= LIFT_WINDOW_MS) liftStartMs = null
        return null
    }

    /** Feeds one barometer sample in hPa; returns an alert or null. */
    fun onPressure(hPa: Double, nowMs: Long): Alert? {
        val gapMs = if (lastPressureMs < 0) 0L else nowMs - lastPressureMs
        lastPressureMs = nowMs
        val previous = pressureSmoothed
        val smoothed = if (previous == null || gapMs > MAX_PRESSURE_GAP_MS) {
            hPa
        } else {
            val dt = gapMs / 1000.0
            previous + dt / (PRESSURE_TIME_CONSTANT_S + dt) * (hPa - previous)
        }
        pressureSmoothed = smoothed
        if (!isArmed) return null
        val reference = pressureReference
        if (reference == null || gapMs > MAX_PRESSURE_GAP_MS) {
            pressureReference = smoothed
            baroSinceMs = null
            return null
        }
        val dt = gapMs / 1000.0
        pressureReference = reference + dt / (PRESSURE_REFERENCE_TIME_CONSTANT_S + dt) * (smoothed - reference)
        val heightChange = pressureAltitudeMeters(smoothed) - pressureAltitudeMeters(reference)
        if (abs(heightChange) >= LIFT_BARO_MIN_M) {
            val since = baroSinceMs ?: nowMs.also { baroSinceMs = it }
            if (nowMs - since >= BARO_CONFIRM_MS) {
                return disarm(nowMs, Alert(Kind.LIFT, "barometer, height change ${"%.1f".format(java.util.Locale.US, heightChange)} m"))
            }
        } else {
            baroSinceMs = null
        }
        return null
    }

    private fun arm() {
        isArmed = true
        refX = gx; refY = gy; refZ = gz
        refMagnitude = norm(gx, gy, gz)
        tiltSinceMs = null
        liftStartMs = null
        pressureReference = pressureSmoothed
        baroSinceMs = null
    }

    private fun disarm(nowMs: Long, alert: Alert): Alert {
        isArmed = false
        lastMotionMs = nowMs
        tiltSinceMs = null
        liftStartMs = null
        pressureReference = null
        baroSinceMs = null
        return alert
    }

    private fun publishGravity() {
        gravitySnapshot = doubleArrayOf(gx, gy, gz)
    }

    private companion object {
        const val GRAVITY_TIME_CONSTANT_S = 0.3
        const val PRESSURE_TIME_CONSTANT_S = 2.0
        const val PRESSURE_REFERENCE_TIME_CONSTANT_S = 120.0
        const val MAX_SAMPLE_GAP_MS = 200L
        const val MAX_PRESSURE_GAP_MS = 5_000L

        fun norm(x: Double, y: Double, z: Double) = sqrt(x * x + y * y + z * z)

        fun angleDegrees(ax: Double, ay: Double, az: Double, bx: Double, by: Double, bz: Double): Double {
            val lengths = norm(ax, ay, az) * norm(bx, by, bz)
            if (lengths < 1e-6) return 0.0
            return Math.toDegrees(acos(((ax * bx + ay * by + az * bz) / lengths).coerceIn(-1.0, 1.0)))
        }
    }
}

/** Rest needed before tilt and lift are armed; a car waiting at a red light can arm too (see SOT 1.400). */
const val MOTION_ARM_AFTER_STILL_MS = 30_000L

/** Above this distance from the estimated gravity (m/s²), a sample is motion. */
const val STILL_MAX_DEVIATION = 0.35

/** A tilt must last this long, so a knock that shakes the gravity estimate does not count. */
const val TILT_CONFIRM_MS = 500L

/** The height the accelerometer must see the phone rise. */
const val LIFT_MIN_HEIGHT_M = 0.20

/** The longest accelerometer integration; longer, its error grows too much. */
const val LIFT_WINDOW_MS = 3_000L

/** An integration ends when the phone has been still this long. */
const val LIFT_SETTLE_MS = 300L

const val STANDARD_GRAVITY = 9.80665

const val DEFAULT_VIBRATION_SENSITIVITY = 0.5f

/**
 * The `Vibration Sensitivity Threshold` slider (0.1 to 1.0) as a deviation from gravity in m/s²: 0.1 gives 0.82 g (about
 * the old spec's 0.8 G shock), 0.5 gives 0.5 g (a knock on the phone), 1.0 gives 0.1 g (a light tap on the table).
 */
fun vibrationThresholdForSensitivity(sensitivity: Float): Double =
    (0.9 - 0.8 * sensitivity.coerceIn(0.1f, 1.0f)) * STANDARD_GRAVITY

/** The barometric height change that counts as a lift (the old spec's 0.8 m). */
const val LIFT_BARO_MIN_M = 0.8

/** A barometric height change must last this long, so a door slam's pressure wave does not count. */
const val BARO_CONFIRM_MS = 2_000L

const val DEFAULT_TILT_SENSITIVITY = 0.5f

/** The `Tilt Sensitivity Threshold` slider (0.1 to 1.0) as an angle: 0.5 gives the old spec's 15°, 1.0 gives 5°. */
fun tiltThresholdForSensitivity(sensitivity: Float): Double = 25.0 - 20.0 * sensitivity.coerceIn(0.1f, 1.0f)

/** The standard-atmosphere altitude of [hPa], as `SensorManager.getAltitude` computes it; only differences are used. */
fun pressureAltitudeMeters(hPa: Double): Double = 44_330.0 * (1.0 - (hPa / 1013.25).pow(1.0 / 5.255))
