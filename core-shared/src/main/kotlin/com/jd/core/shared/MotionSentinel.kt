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
 *   held for [LIFT_HOLD_MS] alerts (ISSUE-NOV17.10-O111: a rise that falls back is no lift). Following the gravity
 *   direction keeps a turning phone right, a sideways push adds nothing to first order, and subtracting the resting
 *   magnitude cancels the sensor's own offset. The integration takes the median of the last [LIFT_MEDIAN_SAMPLES]
 *   samples (ISSUE-NOV25.23-O154): a knock is a spike of a few ms that the 50 Hz sampling catches in one sample at a
 *   random size, and integrated it leaves a speed that never returns to zero (on the A15 under a cushion, knocks of
 *   1.2 and 5.7 g gave one sample each, 0.2 and 1.2 m/s, and lifts of 40 and 144 cm); a hand's push lasts far longer.
 *   A harder knock pushes for a whole 0.1 s and passes the median: a jolt over [LIFT_KNOCK_MS2] voids the integration
 *   it falls in, and a new one waits for rest; and the hold counts only while the integrated speed is near zero
 *   ([LIFT_HELD_MAX_SPEED_MPS]): a held phone has none, a weaker knock may leave one that never returns.
 * - **Lift (barometer)**: the smoothed pressure moves at least [LIFT_BARO_MIN_M] of height away from a reference that
 *   follows slowly (so weather does not count), for [BARO_CONFIRM_MS].
 * - **Vibration**: [VIBRATION_HITS] hits within [VIBRATION_HIT_WINDOW_MS] (ISSUE-NOV17.10-O111: a single knock on the
 *   table is no intruder); a hit is a rise of the sample's distance from the estimated gravity to [vibrationThreshold]
 *   or more (a knock, a shove, a shake), [VIBRATION_HIT_GAP_MS] or more after the previous hit. It arms after the same rest but
 *   on its own ([isVibrationArmed]): a vibration alert does not disarm tilt and lift, so a rough pickup gives both, and
 *   switching one alarm off on the Monitor never hides the other.
 * - **Own vibration**: while the phone itself rings or vibrates ([isOwnVibrationActive]) and [OWN_VIBRATION_TAIL_MS]
 *   after, no vibration alerts and vibration stays armed (ISSUE-NOV9.2-O62). A vibration alert is held for
 *   [VIBRATION_HOLD_MS] and dropped if the phone's own vibration is reported meanwhile, since the motor may start a
 *   little before Android reports the ringing.
 * - **Sit** (ISSUE-NOV25.22-O150, user decision 2026-11-25: the phone lies on or under a seat): the first motion of the
 *   armed phone opens a sit check of at most [SIT_WINDOW_MS]. A sit is a jolt of [SIT_JOLT_MS2] whose first clear
 *   vertical move ([SIT_FIRST_MOVE_MS2] over its first 0.1 s) is downward, then the phone at rest for [SIT_REST_MS],
 *   tilted less than the tilt limit (more is the tilt alarm's). Standing up moves it upward first, as does a pickup. On a
 *   phone with a barometer, a height drop of [SIT_BARO_MIN_M] to [SIT_BARO_MAX_M] counts when the first move is unclear.
 *   Measured on the A15 under a seat cushion (Nov25.23): the phone lies on the hard seat and barely moves; sits and
 *   stand-ups both jolt it 0.4 to 0.6 g and tilt it 0.1 to 2.8 degrees, so neither the tilt nor an integrated height
 *   (its speed never returned to zero: 7 and 18 cm of false drop) tells them apart; the first 0.1 s did (down 0.15 and
 *   0.67 m/s² sitting, up 0.07 and 0.15 standing). Knocks on the chair and moving while seated: 0.11 to 0.18 g; hard
 *   knocks on its frame 1.2 g and more, over [SIT_JOLT_MAX_MS2].
 * - **Sitting period** (ISSUE-NOV25.23-O155, user decisions 2026-11-26): the first sit alerts (`Sit alert (checking)`)
 *   and opens a period, without disarming; inside it sits raise no alert (shifting, sitting down again). On a seat the
 *   accelerometer sees a still sitter as an empty seat (A15, Nov26.3: the 10 s [StillnessMeter] windows of a still
 *   sitter matched the empty chair's), so only movement tells: a window over [SIT_MOVEMENT_TILT_DEG] or
 *   [SIT_MOVEMENT_OVER_20MG], or a sit check with a jolt. A movement in the first [SIT_CONFIRM_MS] confirms the sit,
 *   none leaves it unconfirmed ([onSitPeriod]); the period stays open either way. A stand-up (up first) followed by
 *   [SIT_STOOD_UP_EMPTY_MS] without movement ends it, dated to the stand-up; a movement in between means still seated.
 *   Without a stand-up, [SIT_QUIET_END_MS] without movement ends it, dated to the last movement. Inside the period the
 *   occupied movement stays within a fixed envelope: vibration counts only hits over [SIT_JOLT_MAX_MS2], the
 *   accelerometer lift (weak evidence under a cushion: 146 and 48 cm of false lift, Nov26.1) is only noted, and a sit's
 *   or stand-up's own vibration alert is dropped (held while the sit check is open). What no seat explains still alarms:
 *   a tilt over the limit, the phone resting [SIT_PERIOD_NEW_REST_DEG] or more away from where it lay before the sit,
 *   and the barometer's lift.
 *
 * Times are monotonic milliseconds (sensor event time). Not thread-safe: one sensor thread owns it; [gravity],
 * [isArmed], and [isVibrationArmed] may be read from any thread. [onNote] gets two lines for each sit check with a
 * jolt, its 0.1 s trace and, unless it alerted, why not, and one line for a lift integration that rose
 * [LIFT_NOTE_HEIGHT_M] without a lift: for tuning on the phone.
 */
class MotionSentinel(
    private val armAfterStillMs: Long = MOTION_ARM_AFTER_STILL_MS,
    private val onNote: (String) -> Unit = {},
    private val onSitPeriod: (SitPeriodEvent) -> Unit = {}
) {

    enum class Kind { TILT, LIFT, VIBRATION, SIT }

    data class Alert(val kind: Kind, val detail: String)

    /**
     * False while the Monitor has `Sit / Seat Occupied Sensor` switched off: no sitting period, and a sit's vibration
     * is not dropped, as no sit alert covers it (ISSUE-NOV25.23-O155).
     */
    @Volatile var isSitEnabled = true

    private var sitPeriod: SitPeriod? = null

    /** The open sitting period (ISSUE-NOV25.23-O155); times are this sentinel's. */
    private class SitPeriod(val startMs: Long) {
        var lastMovementMs = startMs
        var isResolved = false // Confirmed or unconfirmed already reported.
        var standUpMs: Long? = null // The stand-up that may end the period, while no movement follows.
        var standUpEndMs = 0L
    }

    /** The tilt that alerts, from the Monitor's `Tilt Sensitivity Threshold` slider (ISSUE-NOV10.8-O83). */
    @Volatile var tiltThresholdDegrees: Double = tiltThresholdForSensitivity(DEFAULT_TILT_SENSITIVITY)

    /** The deviation from gravity in m/s² that alerts, from the Monitor's `Vibration Sensitivity Threshold` slider (ISSUE-NOV10.8-O83). */
    @Volatile var vibrationThreshold: Double = vibrationThresholdForSensitivity(DEFAULT_VIBRATION_SENSITIVITY)

    /** True while the phone itself rings, plays a notification, or is in a call: its own motor shakes it (ISSUE-NOV9.2-O62). */
    @Volatile var isOwnVibrationActive = false

    @Volatile private var gravitySnapshot: DoubleArray? = null
    @Volatile var isArmed = false
        private set
    @Volatile var isVibrationArmed = false
        private set

    /** The newest sample's distance from the estimated gravity in m/s², for the Info screens (ISSUE-NOV10.8-O84 part 2). */
    @Volatile var vibration = 0.0
        private set

    /** The largest [vibration] of about the last [VIBRATION_PEAK_WINDOW_MS] (ISSUE-NOV10.8-O84 part 2). */
    @Volatile var vibrationPeak = 0.0
        private set
    private var vibrationPeakMs = 0L

    /** The tilt from the armed resting position in degrees, null while tilt is not armed (ISSUE-NOV10.8-O84 part 2). */
    @Volatile var tiltDegrees: Double? = null
        private set

    /** The smoothed air pressure in hPa, null without a barometer (ISSUE-NOV10.8-O84 part 2). */
    val pressureHpa: Double? get() = pressureSmoothed

    private var gx = 0.0
    private var gy = 0.0
    private var gz = 0.0
    private var lastSampleMs = -1L
    private var lastMotionMs = 0L
    private var ownVibrationSeenMs: Long? = null
    private var heldVibration: Alert? = null
    private var heldVibrationSinceMs = 0L
    private val vibrationHits = ArrayDeque<Pair<Long, Double>>() // Time and size of each hit within the window.
    private var wasOverVibrationLimit = false // The previous sample's; a hit starts with a rise over the limit.

    private var refX = 0.0
    private var refY = 0.0
    private var refZ = 0.0
    private var refMagnitude = 0.0
    private var tiltSinceMs: Long? = null

    private var liftStartMs: Long? = null
    private var liftVelocity = 0.0
    private var liftHeight = 0.0
    private var liftQuietSinceMs: Long? = null
    private var liftHeldSinceMs: Long? = null
    private var liftPeakHeight = 0.0 // For the note of an integration that ends without a lift.
    private var isLiftAfterKnock = false // A knock voided the integration; none until the phone rests (ISSUE-NOV25.23-O154).
    private var liftAfterKnockQuietSinceMs: Long? = null
    private val recentUpward = DoubleArray(LIFT_MEDIAN_SAMPLES) // The last samples' upward acceleration, a ring.
    private var recentUpwardNext = 0

    @Volatile private var pressureSmoothed: Double? = null
    private var pressureReference: Double? = null
    private var lastPressureMs = -1L
    private var baroSinceMs: Long? = null

    // The open sit check (ISSUE-NOV25.22-O150), null when none.
    private var sitStartMs: Long? = null
    private var sitRefX = 0.0
    private var sitRefY = 0.0
    private var sitRefZ = 0.0
    private var sitRefPressure: Double? = null
    private var sitVelocity = 0.0 // For the trace only.
    private var sitPeakJolt = 0.0
    private var sitFirstMove: Double? = null // The first 0.1 s mean of the upward acceleration of [SIT_FIRST_MOVE_MS2] or more.
    private var sitQuietSinceMs: Long? = null

    private val stillness = StillnessMeter() // ISSUE-NOV25.23-O155: a seated person against an empty seat.

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
            recentUpward.fill(0.0)
            sitStartMs = null
            stillness.reset()
            lastMotionMs = nowMs
            return emptyList()
        }
        val dt = gapMs / 1000.0
        // The resting direction before this sample, for a sit check that opens on it.
        val previousGx = gx
        val previousGy = gy
        val previousGz = gz
        // Measured against the gravity before this sample, so a single sharp knock counts at its full size.
        val vibration = norm(x - gx, y - gy, z - gz)
        this.vibration = vibration
        if (vibration >= vibrationPeak || nowMs - vibrationPeakMs > VIBRATION_PEAK_WINDOW_MS) {
            vibrationPeak = vibration
            vibrationPeakMs = nowMs
        }
        if (!isSitEnabled) sitPeriod = null
        stillness.onSample(x, y, z, vibration, nowMs)?.let { window ->
            if (sitPeriod != null) onNote("Stillness 10 s: ${window.describe()}") // For tuning, only while seated.
            onStillnessWindow(window, nowMs)
        }
        val alpha = dt / (GRAVITY_TIME_CONSTANT_S + dt)
        gx += alpha * (x - gx)
        gy += alpha * (y - gy)
        gz += alpha * (z - gz)
        publishGravity()

        val deviation = norm(x - gx, y - gy, z - gz)
        val isMoving = deviation > STILL_MAX_DEVIATION
        val vibrationAlert = checkVibration(vibration, nowMs)
        if (isMoving || vibrationAlert != null) lastMotionMs = nowMs
        val hasRested = nowMs - lastMotionMs >= armAfterStillMs
        if (hasRested) isVibrationArmed = true
        if (sitStartMs == null && isArmed && isMoving && !isOwnVibrationRecent(nowMs)) openSit(nowMs, previousGx, previousGy, previousGz)
        val sitAlert = checkSit(x, y, z, nowMs, dt, vibration, isMoving)
        followSitPeriod(nowMs)
        return listOfNotNull(vibrationAlert, sitAlert, tiltOrLift(x, y, z, nowMs, dt, vibration, isMoving, hasRested))
    }

    /** A movement window of the open period, after the sit and after a stand-up's own window (ISSUE-NOV25.23-O155). */
    private fun onStillnessWindow(window: StillnessMeter.Window, nowMs: Long) {
        val period = sitPeriod ?: return
        if (window.slowTiltDeg < SIT_MOVEMENT_TILT_DEG && window.over20Mg < SIT_MOVEMENT_OVER_20MG) return
        val windowStartMs = nowMs - STILLNESS_WINDOW_MS
        if (windowStartMs < period.startMs) return // The sit's own settling.
        if (period.standUpMs != null && windowStartMs < period.standUpEndMs) return // The stand-up's own window.
        onSitterMovement(period, nowMs, "a movement window (${window.describe()})")
    }

    private fun onSitterMovement(period: SitPeriod, nowMs: Long, what: String) {
        period.lastMovementMs = nowMs
        if (period.standUpMs != null) {
            period.standUpMs = null
            onNote("Sitting period: $what after the stand-up, still seated")
        }
        if (!period.isResolved) {
            period.isResolved = true
            onSitPeriod(SitPeriodEvent(SitPeriodEvent.Kind.CONFIRMED, period.startMs, nowMs))
        }
    }

    /** Reports an unconfirmed sit and ends the period when its time has come (ISSUE-NOV25.23-O155). */
    private fun followSitPeriod(nowMs: Long) {
        val period = sitPeriod ?: return
        if (!period.isResolved && nowMs - period.startMs >= SIT_CONFIRM_MS) {
            period.isResolved = true
            onSitPeriod(SitPeriodEvent(SitPeriodEvent.Kind.UNCONFIRMED, period.startMs, nowMs))
        }
        val standUpMs = period.standUpMs
        val end = when {
            standUpMs != null -> if (nowMs - period.standUpEndMs >= SIT_STOOD_UP_EMPTY_MS) SitPeriodEvent(SitPeriodEvent.Kind.ENDED_STOOD_UP, period.startMs, standUpMs) else null
            nowMs - period.lastMovementMs >= SIT_QUIET_END_MS -> SitPeriodEvent(SitPeriodEvent.Kind.ENDED_QUIET, period.startMs, period.lastMovementMs)
            else -> null
        } ?: return
        sitPeriod = null
        onSitPeriod(end)
    }

    /** Drops a vibration alert held during a sit check that was a sit or a stand-up: the seat's own jolts (ISSUE-NOV25.23-O155). */
    private fun dropSitVibration(why: String) {
        if (heldVibration != null) onNote("Vibration alert dropped: $why")
        heldVibration = null
        vibrationHits.clear()
    }

    private fun isOwnVibrationRecent(nowMs: Long): Boolean = ownVibrationSeenMs?.let { nowMs - it <= OWN_VIBRATION_TAIL_MS } == true

    private fun openSit(nowMs: Long, refX: Double, refY: Double, refZ: Double) {
        sitStartMs = nowMs
        sitRefX = refX; sitRefY = refY; sitRefZ = refZ
        sitRefPressure = pressureSmoothed
        sitVelocity = 0.0
        sitPeakJolt = 0.0
        sitFirstMove = null
        sitQuietSinceMs = null
        sitTraceUp.setLength(0)
        sitTraceTilt.setLength(0)
        sitBinStartMs = nowMs
        sitBinSum = 0.0
        sitBinCount = 0
    }

    // A 0.1 s trace of the open sit check, for tuning on the phone (Nov25.23): upward acceleration and tilt.
    private val sitTraceUp = StringBuilder()
    private val sitTraceTilt = StringBuilder()
    private var sitBinStartMs = 0L
    private var sitBinSum = 0.0
    private var sitBinCount = 0

    private fun traceSit(upward: Double, nowMs: Long) {
        sitBinSum += upward
        sitBinCount++
        if (nowMs - sitBinStartMs < 100) return
        val mean = sitBinSum / sitBinCount
        if (sitFirstMove == null && abs(mean) >= SIT_FIRST_MOVE_MS2) sitFirstMove = mean
        if (sitTraceUp.isNotEmpty()) { sitTraceUp.append(' '); sitTraceTilt.append(' ') }
        sitTraceUp.append(kotlin.math.round(mean * 100).toInt())
        sitTraceTilt.append(kotlin.math.round(angleDegrees(gx, gy, gz, sitRefX, sitRefY, sitRefZ) * 10).toInt())
        sitBinStartMs = nowMs
        sitBinSum = 0.0
        sitBinCount = 0
    }

    private fun noteSitTrace(nowMs: Long) = onNote(
        "Sit trace over ${nowMs - (sitStartMs ?: nowMs)} ms, end speed ${"%.2f".format(java.util.Locale.US, sitVelocity)} m/s; up cm/s2: $sitTraceUp; tilt 0.1 deg: $sitTraceTilt"
    )

    /** Follows the open sit check (ISSUE-NOV25.22-O150); returns a sit alert when it is confirmed. */
    private fun checkSit(x: Double, y: Double, z: Double, nowMs: Long, dt: Double, vibration: Double, isMoving: Boolean): Alert? {
        val start = sitStartMs ?: return null
        // Along the resting direction before the check: a sideways jolt adds nothing, as the current estimate leans
        // towards it; a small tilt adds only -g(1 - cos), 0.01 m/s² at 3 degrees.
        val refLength = sitRefMagnitude().coerceAtLeast(1.0)
        val upward = (x * sitRefX + y * sitRefY + z * sitRefZ) / refLength - refLength
        sitVelocity += upward * dt
        traceSit(upward, nowMs)
        sitPeakJolt = maxOf(sitPeakJolt, vibration)
        sitQuietSinceMs = if (isMoving) null else sitQuietSinceMs ?: nowMs
        val isRested = sitQuietSinceMs?.let { nowMs - it >= SIT_REST_MS } == true
        val isOver = nowMs - start >= SIT_WINDOW_MS
        if (!isRested && !isOver) return null
        val hasJolt = sitPeakJolt >= SIT_JOLT_MS2
        val isKnock = sitPeakJolt > SIT_JOLT_MAX_MS2
        val firstMove = sitFirstMove
        val isDownFirst = firstMove != null && firstMove < 0
        val isUpFirst = firstMove != null && firstMove > 0
        val tilt = angleDegrees(gx, gy, gz, sitRefX, sitRefY, sitRefZ)
        val isOverTiltLimit = tilt >= tiltThresholdDegrees
        val baroDrop = sitBaroDrop()
        val isBaroDrop = baroDrop != null && baroDrop >= SIT_BARO_MIN_M && baroDrop <= SIT_BARO_MAX_M
        val detail = listOfNotNull(
            firstMove?.let { "first move ${if (it < 0) "down" else "up"} ${"%.2f".format(java.util.Locale.US, abs(it))} m/s²" },
            "jolt ${"%.2f".format(java.util.Locale.US, sitPeakJolt / STANDARD_GRAVITY)} g",
            "tilt ${"%.1f".format(java.util.Locale.US, tilt)} deg",
            baroDrop?.let { "barometer ${if (it >= 0) "drop" else "rise"} ${(abs(it) * 100).toInt()} cm" }
        ).joinToString(", ")
        val isCandidate = isRested && hasJolt && !isKnock && !isOverTiltLimit && !isUpFirst
        val period = sitPeriod
        if (isCandidate && (isDownFirst || isBaroDrop)) {
            noteSitTrace(nowMs)
            sitStartMs = null
            if (!isSitEnabled) return Alert(Kind.SIT, detail) // Not sent; no period (ISSUE-NOV25.23-O155).
            dropSitVibration("the jolts of a sit")
            if (period == null) {
                // No disarm (ISSUE-NOV25.23-O155): tilt keeps the resting position from before the sit.
                sitPeriod = SitPeriod(nowMs)
                return Alert(Kind.SIT, detail)
            }
            onNote("Sit inside the sitting period, no new alert: $detail")
            if (start >= period.startMs + SIT_SETTLE_MS) onSitterMovement(period, nowMs, "a sit")
            return null
        }
        if (period != null && isRested && hasJolt && !isKnock && !isOverTiltLimit && isUpFirst) {
            noteSitTrace(nowMs)
            sitStartMs = null
            dropSitVibration("the jolts of a stand-up")
            onNote("Stand-up in the sitting period: it ends after ${SIT_STOOD_UP_EMPTY_MS / 60_000} min without movement: $detail")
            period.standUpMs = start
            period.standUpEndMs = nowMs
            return null
        }
        // No clear first move: a barometer, lagging its 2 s smoothing, may still show the drop later in the window.
        if (isCandidate && !isOver && sitRefPressure != null && pressureSmoothed != null) return null
        if (sitPeakJolt >= SIT_NOTE_JOLT_MS2) {
            noteSitTrace(nowMs)
            val why = when {
                !hasJolt -> "jolt under ${"%.2f".format(java.util.Locale.US, SIT_JOLT_MS2 / STANDARD_GRAVITY)} g"
                !isRested -> "no rest within ${SIT_WINDOW_MS / 1000} s"
                isKnock -> "too sharp: a knock or a blow"
                isUpFirst -> "up first: standing up or a pickup"
                isOverTiltLimit -> "tilted over the limit"
                else -> "no clear first move"
            }
            onNote("Sit not confirmed ($why): $detail")
            if (period != null && start >= period.startMs + SIT_SETTLE_MS) onSitterMovement(period, nowMs, "a jolt")
        }
        sitStartMs = null
        return null
    }

    private fun sitRefMagnitude(): Double = norm(sitRefX, sitRefY, sitRefZ)

    /** The barometric height drop since the sit check opened in m (a rise is negative), null without a barometer. */
    private fun sitBaroDrop(): Double? {
        val reference = sitRefPressure ?: return null
        val now = pressureSmoothed ?: return null
        return pressureAltitudeMeters(reference) - pressureAltitudeMeters(now)
    }

    /**
     * A vibration alert after [VIBRATION_HITS] hits (ISSUE-NOV17.10-O111), held [VIBRATION_HOLD_MS] first; none while the
     * phone's own motor runs (ISSUE-NOV9.2-O62).
     */
    private fun checkVibration(vibration: Double, nowMs: Long): Alert? {
        // Inside a sitting period a sitter's movement is no hit; a blow is (ISSUE-NOV25.23-O155).
        val limit = if (sitPeriod != null) maxOf(vibrationThreshold, SIT_JOLT_MAX_MS2) else vibrationThreshold
        val isOverLimit = vibration >= limit
        val wasOverLimit = wasOverVibrationLimit
        wasOverVibrationLimit = isOverLimit
        if (isOwnVibrationActive) ownVibrationSeenMs = nowMs
        if (ownVibrationSeenMs?.let { nowMs - it <= OWN_VIBRATION_TAIL_MS } == true) {
            heldVibration = null // Its own ringing, not a knock: vibration stays armed.
            vibrationHits.clear()
            return null
        }
        while (vibrationHits.isNotEmpty() && nowMs - vibrationHits.first().first > VIBRATION_HIT_WINDOW_MS) vibrationHits.removeFirst()
        // A rise over the limit, not a lasting deviation (a turn, a pickup), and not the ringing of the last hit.
        val isNewHit = isOverLimit && !wasOverLimit && vibrationHits.lastOrNull()?.let { nowMs - it.first >= VIBRATION_HIT_GAP_MS } != false
        if (heldVibration == null && isVibrationArmed && isNewHit) {
            vibrationHits.addLast(nowMs to vibration)
            if (vibrationHits.size >= VIBRATION_HITS) {
                val strongest = vibrationHits.maxOf { it.second }
                val spanS = (nowMs - vibrationHits.first().first) / 1000.0
                heldVibration = Alert(
                    Kind.VIBRATION,
                    "${vibrationHits.size} hits in ${"%.1f".format(java.util.Locale.US, spanS)} s, strongest ${"%.2f".format(java.util.Locale.US, strongest / STANDARD_GRAVITY)} g (limit ${"%.2f".format(java.util.Locale.US, vibrationThreshold / STANDARD_GRAVITY)} g)"
                )
                heldVibrationSinceMs = nowMs
                vibrationHits.clear()
            }
        }
        val held = heldVibration ?: return null
        if (nowMs - heldVibrationSinceMs < VIBRATION_HOLD_MS) return null
        // A sit or a stand-up jolts as much: held until the open sit check decides (ISSUE-NOV25.23-O155).
        if (sitStartMs != null && isSitEnabled) return null
        heldVibration = null
        isVibrationArmed = false
        return held
    }

    private fun tiltOrLift(x: Double, y: Double, z: Double, nowMs: Long, dt: Double, vibration: Double, isMoving: Boolean, hasRested: Boolean): Alert? {
        if (!isArmed) {
            if (hasRested) arm()
            return null
        }

        val angle = angleDegrees(gx, gy, gz, refX, refY, refZ)
        tiltDegrees = angle
        if (sitPeriod != null && hasRested && angle >= SIT_PERIOD_NEW_REST_DEG) {
            // Sitting moves the phone a few degrees at most; resting further away it was found and moved (ISSUE-NOV25.23-O155).
            return disarm(nowMs, Alert(Kind.TILT, "new resting position, ${"%.1f".format(java.util.Locale.US, angle)} deg from before the sit (limit ${SIT_PERIOD_NEW_REST_DEG.toInt()} deg while seated)"))
        }
        if (angle >= tiltThresholdDegrees) {
            val since = tiltSinceMs ?: nowMs.also { tiltSinceMs = it }
            if (nowMs - since >= TILT_CONFIRM_MS) {
                return disarm(nowMs, Alert(Kind.TILT, "tilted ${angle.toInt()} deg (limit ${tiltThresholdDegrees.toInt()} deg)"))
            }
        } else {
            tiltSinceMs = null
        }

        val gravityLength = norm(gx, gy, gz).coerceAtLeast(1.0) // Only near free fall could it be smaller.
        val upward = medianUpward((x * gx + y * gy + z * gz) / gravityLength - refMagnitude)
        if (vibration > LIFT_KNOCK_MS2) {
            // A knock: its burst of a few samples cannot be integrated, so the integration it falls in is void, and a
            // new one waits for the phone to rest (ISSUE-NOV25.23-O154: knocks of 2.3 to 4.8 g left 0.28 to 0.77 m/s).
            if (liftStartMs != null && !isLiftAfterKnock) {
                onNote("Lift not confirmed: a knock of ${"%.2f".format(java.util.Locale.US, vibration / STANDARD_GRAVITY)} g voided the integration")
            }
            liftStartMs = null
            liftHeldSinceMs = null
            isLiftAfterKnock = true
            liftAfterKnockQuietSinceMs = null
            return null
        }
        if (isLiftAfterKnock) {
            liftAfterKnockQuietSinceMs = if (isMoving) null else liftAfterKnockQuietSinceMs ?: nowMs
            if (liftAfterKnockQuietSinceMs?.let { nowMs - it >= LIFT_SETTLE_MS } != true) return null
            isLiftAfterKnock = false
        }
        if (liftStartMs == null && isMoving) {
            liftStartMs = nowMs
            liftVelocity = 0.0
            liftHeight = 0.0
            liftPeakHeight = 0.0
            liftQuietSinceMs = null
            liftHeldSinceMs = null
        }
        val start = liftStartMs ?: return null
        liftVelocity += upward * dt
        liftHeight += liftVelocity * dt
        liftPeakHeight = maxOf(liftPeakHeight, liftHeight)
        if (liftHeight >= LIFT_MIN_HEIGHT_M) {
            // Held up, moving or not: the integration goes on until the hold ends (ISSUE-NOV17.10-O111). A held phone has
            // no speed, so the hold counts only while the integrated speed is near zero; a knock's push leaves a speed
            // that never returns (ISSUE-NOV25.23-O154: 0.77 m/s and 79 cm on the A15, Nov25.25).
            if (abs(liftVelocity) > LIFT_HELD_MAX_SPEED_MPS) {
                liftHeldSinceMs = null
                if (nowMs - start >= LIFT_GIVE_UP_MS) {
                    liftStartMs = null
                    onNote("Lift not confirmed: ${(liftHeight * 100).toInt()} cm in ${nowMs - start} ms, but the speed never returned to zero (${"%.2f".format(java.util.Locale.US, liftVelocity)} m/s): a knock")
                }
                return null
            }
            val heldSince = liftHeldSinceMs ?: nowMs.also { liftHeldSinceMs = it }
            if (nowMs - heldSince >= LIFT_HOLD_MS && sitPeriod != null) {
                onNote("Lift not alarmed inside the sitting period (weak evidence under a seat): about ${(liftHeight * 100).toInt()} cm")
                liftStartMs = null
                liftHeldSinceMs = null
                return null
            }
            if (nowMs - heldSince >= LIFT_HOLD_MS) {
                return disarm(nowMs, Alert(Kind.LIFT, "accelerometer, about ${(liftHeight * 100).toInt()} cm up for ${LIFT_HOLD_MS / 1000.0} s"))
            }
            return null
        }
        liftHeldSinceMs = null // Fell back below the height: put down again, or a rebound.
        liftQuietSinceMs = if (isMoving) null else liftQuietSinceMs ?: nowMs
        val settled = liftQuietSinceMs?.let { nowMs - it >= LIFT_SETTLE_MS } == true
        if (settled || nowMs - start >= LIFT_WINDOW_MS) {
            liftStartMs = null
            if (liftPeakHeight >= LIFT_NOTE_HEIGHT_M) {
                onNote("Lift not confirmed: up to ${(liftPeakHeight * 100).toInt()} cm in ${nowMs - start} ms, end speed ${"%.2f".format(java.util.Locale.US, liftVelocity)} m/s")
            }
        }
        return null
    }

    /**
     * The median of the last [LIFT_MEDIAN_SAMPLES] upward accelerations, [upward] included (ISSUE-NOV25.23-O154): a knock's
     * one or two sample spike drops out, a push of three samples (60 ms) or more passes, two samples late.
     */
    private fun medianUpward(upward: Double): Double {
        recentUpward[recentUpwardNext] = upward
        recentUpwardNext = (recentUpwardNext + 1) % recentUpward.size
        return recentUpward.sortedArray()[recentUpward.size / 2]
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
        liftHeldSinceMs = null
        recentUpward.fill(0.0)
        pressureReference = pressureSmoothed
        baroSinceMs = null
    }

    private fun disarm(nowMs: Long, alert: Alert): Alert {
        isArmed = false
        tiltDegrees = null
        lastMotionMs = nowMs
        tiltSinceMs = null
        liftStartMs = null
        liftHeldSinceMs = null
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

/** The Info screens' vibration peak covers about this long (ISSUE-NOV10.8-O84 part 2). */
const val VIBRATION_PEAK_WINDOW_MS = 60_000L

/** Rest needed before tilt and lift are armed; a car waiting at a red light can arm too (see SOT 1.400). */
const val MOTION_ARM_AFTER_STILL_MS = 30_000L

/** Above this distance from the estimated gravity (m/s²), a sample is motion. */
const val STILL_MAX_DEVIATION = 0.35

/**
 * A tilt must last this long, so a knock that shakes the gravity estimate, or a phone tipped and set back, does not
 * count (0.5 s until ISSUE-NOV17.10-O111).
 */
const val TILT_CONFIRM_MS = 2_000L

/** The height the accelerometer must see the phone rise. */
const val LIFT_MIN_HEIGHT_M = 0.20

/** The longest accelerometer integration; longer, its error grows too much. */
const val LIFT_WINDOW_MS = 3_000L

/** An integration ends when the phone has been still this long, unless it is held up ([LIFT_HOLD_MS]). */
const val LIFT_SETTLE_MS = 300L

/** The phone must stay [LIFT_MIN_HEIGHT_M] up this long to be a lift (ISSUE-NOV17.10-O111). */
const val LIFT_HOLD_MS = 1_000L

/** The lift integration takes the median of this many samples, 0.1 s at 50 Hz, so a knock's spike drops out (ISSUE-NOV25.23-O154). */
const val LIFT_MEDIAN_SAMPLES = 5

/**
 * The hold of a lift counts only while the integrated speed is at most this (m/s), as a held phone has none
 * (ISSUE-NOV25.23-O154): a knock on the A15's chair left 0.77 m/s at rest (Nov25.25), real pickups end near 0.
 */
const val LIFT_HELD_MAX_SPEED_MPS = 0.3

/** An integration over the height whose speed has not returned to [LIFT_HELD_MAX_SPEED_MPS] ends this long after its start. */
const val LIFT_GIVE_UP_MS = 5_000L

/** An integration that rose this high (m) without a lift logs a note, for tuning on the phone (ISSUE-NOV25.23-O154). */
const val LIFT_NOTE_HEIGHT_M = 0.10

const val STANDARD_GRAVITY = 9.80665

/**
 * A jolt stronger than this (m/s², 1.0 g, as [SIT_JOLT_MAX_MS2]) is a knock: it voids the lift integration it falls in
 * (ISSUE-NOV25.23-O154). Knocks on the A15's chair measured 1.2 to 5.7 g (Nov25.23, Nov25.25); a hand's pickup stays far
 * below (stand-ups under a cushion 0.4 to 0.6 g). A snatch over 1 g is no accelerometer lift; its vibration or tilt
 * usually still alerts.
 */
const val LIFT_KNOCK_MS2 = 1.0 * STANDARD_GRAVITY

const val DEFAULT_VIBRATION_SENSITIVITY = 0.5f

/** A vibration alert waits this long, so the phone's own ringing reported a little late still cancels it. */
const val VIBRATION_HOLD_MS = 200L

/**
 * The hits within [VIBRATION_HIT_WINDOW_MS] that make a vibration alert (ISSUE-NOV17.10-O111): on the A15 resting on a
 * desk, single hits over the 0.13 g default came about once in 20 to 60 minutes, each alone. 4 since ISSUE-NOV25.25-O162
 * (user decision, 2026-11-26: a few hits over about 10 s; 3 before).
 */
const val VIBRATION_HITS = 4

const val VIBRATION_HIT_WINDOW_MS = 10_000L

/** Samples over the limit closer than this to the last hit belong to it: one knock rings for a few samples. */
const val VIBRATION_HIT_GAP_MS = 300L

/** Vibration stays ignored this long after the phone's own ringing or notification ends (the motor runs down). */
const val OWN_VIBRATION_TAIL_MS = 1_000L

/**
 * The `Vibration Sensitivity Threshold` slider (0.1 to 1.0) as a deviation from gravity in m/s², linear between three
 * points measured on the A15 in Nov9.4 (ISSUE-NOV9.2-O62): 0.1 gives 0.82 g (a hard blow, about the old spec's 0.8 G
 * shock), 0.5 gives 0.13 g (a hard knock on the table; knocks on the phone are 0.18 g and more), 1.0 gives 0.1 g (the
 * most a resting phone is known not to reach). The phone's own ringing (0.11 g) is ignored, not limited.
 */
fun vibrationThresholdForSensitivity(sensitivity: Float): Double {
    val s = sensitivity.coerceIn(0.1f, 1.0f).toDouble()
    val g = if (s <= 0.5) 0.82 + (0.13 - 0.82) * (s - 0.1) / 0.4 else 0.13 + (0.10 - 0.13) * (s - 0.5) / 0.5
    return g * STANDARD_GRAVITY
}

/** A sit check lasts at most this long from the first motion (the old `CHAIR_PLUNGE_PHASE_TIMEOUT_MS`, ISSUE-NOV25.22-O150). */
const val SIT_WINDOW_MS = 5_000L

/** A sit check with a jolt this strong (m/s², 0.1 g) logs its trace, for tuning (ISSUE-NOV25.23-O155: gentle sits under [SIT_JOLT_MS2]). */
const val SIT_NOTE_JOLT_MS2 = 0.1 * STANDARD_GRAVITY

/** The seat has taken the weight once the phone has rested this long. */
const val SIT_REST_MS = 500L

/** A jolt this strong (m/s², 0.25 g) may be a sit: sits measured 0.37 to 0.53 g, knocks on the chair 0.11 to 0.18 (A15, Nov25.23). */
const val SIT_JOLT_MS2 = 0.25 * STANDARD_GRAVITY

/**
 * A jolt stronger than this (m/s², 1.0 g) is a knock or a blow, not a sit, which the cushion damps: sits measured 0.28 to
 * 0.53 g, stand-ups 0.39 to 0.63, knocks on the chair's frame 1.21 and 5.71 (A15, Nov25.23). A knock between the two
 * limits that moves the phone down first is taken for a sit.
 */
const val SIT_JOLT_MAX_MS2 = 1.0 * STANDARD_GRAVITY

/** The first 0.1 s whose mean upward acceleration (m/s²) is this large, up or down, is a check's first move. */
const val SIT_FIRST_MOVE_MS2 = 0.05

/** A barometric height drop in this range (m) is a sign of a sit (the old spec's 0.08 m); more is no seat. */
const val SIT_BARO_MIN_M = 0.08
const val SIT_BARO_MAX_M = 0.5

/**
 * A 10 s stillness window with this slow tilt (degrees) or [SIT_MOVEMENT_OVER_20MG] samples over 20 mg is a sitter's
 * movement (ISSUE-NOV25.23-O155). A15 under the cushion, Nov26.3: the empty chair and a still sitter at most 0.21 deg
 * and 2 samples; sitting naturally, half the windows 0.39 to 0.91 deg or 5 to 117 samples, quiet gaps up to 50 s.
 */
const val SIT_MOVEMENT_TILT_DEG = 0.3
const val SIT_MOVEMENT_OVER_20MG = 5

/** A sit is confirmed by a movement within this long, or reported unconfirmed (ISSUE-NOV25.23-O155, user decision 2026-11-26). */
const val SIT_CONFIRM_MS = 60_000L

/** A sit check of a sitter counts as movement only this long after the period's sit, past its settling (ISSUE-NOV25.23-O155). */
const val SIT_SETTLE_MS = 2_000L

/** A stand-up followed by this long without movement ends the sitting period (ISSUE-NOV25.23-O155, user decision 2026-11-26). */
const val SIT_STOOD_UP_EMPTY_MS = 2 * 60_000L

/** With no stand-up seen, this long without movement ends the sitting period (ISSUE-NOV25.23-O155, user decision 2026-11-26). */
const val SIT_QUIET_END_MS = 15 * 60_000L

/**
 * Inside a sitting period the phone resting this far (degrees) from where it lay before the sit alarms as a tilt
 * (ISSUE-NOV25.23-O155): sits and stand-ups tipped the A15 under a cushion 0.1 to 2.8 degrees (Nov25.23, Nov26.3).
 */
const val SIT_PERIOD_NEW_REST_DEG = 5.0

/**
 * An event of a sitting period (ISSUE-NOV25.23-O155), with the sentinel's times: [startMs] is the sit; [atMs] is the
 * confirming movement, the unconfirmed report, the stand-up, or the last movement.
 */
data class SitPeriodEvent(val kind: Kind, val startMs: Long, val atMs: Long) {
    enum class Kind { CONFIRMED, UNCONFIRMED, ENDED_STOOD_UP, ENDED_QUIET }

    /** The event's text; [wallTime] turns a sentinel time into `HH:mm` (ISSUE-NOV25.23-O155). */
    fun message(wallTime: (Long) -> String): String = when (kind) {
        Kind.CONFIRMED -> "Sit confirmed: the sitter moved ${(atMs - startMs) / 1000} s after sitting down"
        Kind.UNCONFIRMED -> "Sit unconfirmed: no movement within ${SIT_CONFIRM_MS / 60_000} min (an empty seat or a still sitter)"
        Kind.ENDED_STOOD_UP -> "Sit ended: seated ${seatedMinutes()} (${wallTime(startMs)} to ${wallTime(atMs)})"
        Kind.ENDED_QUIET ->
            "Sit ended: no movement for ${SIT_QUIET_END_MS / 60_000} min; seated ${seatedMinutes()} (${wallTime(startMs)} to ${wallTime(atMs)}, the last movement)"
    }

    private fun seatedMinutes(): String {
        val minutes = (atMs - startMs + 30_000L) / 60_000L
        return if (minutes < 1) "under 1 min" else "$minutes min"
    }
}

/** The barometric height change that counts as a lift (the old spec's 0.8 m). */
const val LIFT_BARO_MIN_M = 0.8

/** A barometric height change must last this long, so a door slam's pressure wave does not count. */
const val BARO_CONFIRM_MS = 2_000L

const val DEFAULT_TILT_SENSITIVITY = 0.5f

/** The `Tilt Sensitivity Threshold` slider (0.1 to 1.0) as an angle: 0.5 gives the old spec's 15°, 1.0 gives 5°. */
fun tiltThresholdForSensitivity(sensitivity: Float): Double = 25.0 - 20.0 * sensitivity.coerceIn(0.1f, 1.0f)

/** The tilt slider's limit as the user sees it, for example `15°` (ISSUE-NOV9.4-O67: the limit itself, not a percentage). */
fun tiltLimitLabel(sensitivity: Float): String = "${kotlin.math.round(tiltThresholdForSensitivity(sensitivity)).toInt()}°"

/** The vibration slider's limit as the user sees it, for example `0.13 g` (ISSUE-NOV9.4-O67). */
fun vibrationLimitLabel(sensitivity: Float): String =
    "%.2f g".format(java.util.Locale.US, vibrationThresholdForSensitivity(sensitivity) / STANDARD_GRAVITY)

/** The standard-atmosphere altitude of [hPa], as `SensorManager.getAltitude` computes it; only differences are used. */
fun pressureAltitudeMeters(hPa: Double): Double = 44_330.0 * (1.0 - (hPa / 1013.25).pow(1.0 / 5.255))
