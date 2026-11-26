package com.jd.core.shared

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

/*
 * Learning sits per seat placement (ISSUE-NOV26.0-O163 part 2). The fixed sit rule comes from one chair (row 83);
 * chairs, people and cars differ, so the Monitor guides a learning round (sit, stand up, a bump) and the Sensor keeps
 * what each sit check measured as labelled examples per placement, in its settings file. The round's last step
 * (Nov26.26) keeps 2 min of a seated person moving naturally as the placement's occupied envelope ([OccupiedEnvelope],
 * ISSUE-NOV25.23-O155 decision (b)). Judging an event by the examples is part 3: until then they change no detection.
 */

/** What one sit check measured (ISSUE-NOV26.0-O163 part 2), the numbers a learned placement is judged by. */
@Serializable
data class SitFeatures(
    /** The strongest sample's distance from gravity, in g. */
    val joltG: Double,
    /** The first clear 0.1 s mean of the upward acceleration in m/s², negative downward; null when none was clear. */
    val firstMoveMs2: Double? = null,
    /** The resting direction's change over the check, in degrees. */
    val tiltDeg: Double,
    /** From the first motion until the phone came to rest, or until the check's window ended. */
    val settleMs: Long,
    /** The barometric height drop in cm (a rise is negative); null without a barometer. */
    val baroDropCm: Double? = null
) {
    /** `first move down 0.15 m/s², jolt 0.45 g, tilt 0.8 deg, settled in 0.6 s`, as the Monitor lists an example. */
    fun describe(): String = listOfNotNull(
        firstMoveMs2?.let { "first move ${if (it < 0) "down" else "up"} ${"%.2f".format(java.util.Locale.US, abs(it))} m/s²" } ?: "no clear first move",
        "jolt ${"%.2f".format(java.util.Locale.US, joltG)} g",
        "tilt ${"%.1f".format(java.util.Locale.US, tiltDeg)} deg",
        "settled in ${"%.1f".format(java.util.Locale.US, settleMs / 1000.0)} s",
        baroDropCm?.let { "barometer ${if (it >= 0) "drop" else "rise"} ${round(abs(it)).toInt()} cm" }
    ).joinToString(", ")
}

/** What the fixed rule made of a sit check. */
@Serializable
enum class SitVerdict { SIT, STAND_UP, NONE }

/** A finished sit check with a jolt of at least [SIT_NOTE_JOLT_MS2], as [MotionSentinel] reports it to its owner. */
data class SitCheck(val features: SitFeatures, val verdict: SitVerdict)

/** What the user did for an example; only [SIT] should alert. */
@Serializable
enum class SitLabel(val singular: String, val plural: String) {
    SIT("sit", "sits"), STAND_UP("stand-up", "stand-ups"), BUMP("bump", "bumps");

    val isSit: Boolean get() = this == SIT
}

/** One labelled sit check of a learning round; [atMs] is the Sensor's wall clock, [ruleVerdict] what the fixed rule said. */
@Serializable
data class SitExample(val label: SitLabel, val atMs: Long, val features: SitFeatures, val ruleVerdict: SitVerdict)

/**
 * The examples of one placement (ISSUE-NOV26.0-O163), with the phone they were learned on: another phone's
 * accelerometer noise, rate, range, barometer and weight differ (user agreed 2026-11-26), so its examples do not count
 * there ([isLearnedOn]).
 */
@Serializable
data class PlacementExamples(
    val placementId: Long,
    val phoneModel: String,
    val hasBarometer: Boolean,
    val examples: List<SitExample> = emptyList(),
    /** The round's seated step (Nov26.26); null before it ran, and in a file or report before Nov26.26. */
    val occupied: OccupiedEnvelope? = null
) {
    fun isLearnedOn(model: String): Boolean = phoneModel == model

    val isEmpty: Boolean get() = examples.isEmpty() && occupied == null

    fun count(label: SitLabel): Int = examples.count { it.label == label }

    /** The newest example of [label], by which the Monitor's guide sees its step's example arrive (the count stops at the cap). */
    fun newest(label: SitLabel): SitExample? = examples.filter { it.label == label }.maxByOrNull { it.atMs }

    /**
     * How many examples lie nearer an example of the other side (a sit against a stand-up or a bump) than one of their
     * own: each example left out in turn and matched to its nearest one (ISSUE-NOV26.0-O163 part 2: warn when sits and
     * non-sits overlap). 0 while either side has no example.
     */
    fun overlapCount(): Int {
        if (examples.none { it.label.isSit } || examples.all { it.label.isSit }) return 0
        return examples.indices.count { i ->
            val nearest = examples.indices.filter { it != i }.minByOrNull { sitDistance(examples[i].features, examples[it].features) }
            nearest != null && examples[nearest].label.isSit != examples[i].label.isSit
        }
    }
}

/** Every placement's examples, as the Sensor keeps them in its settings (ISSUE-NOV26.0-O163 part 2). */
@Serializable
data class SitExampleBook(val placements: List<PlacementExamples> = emptyList()) {
    fun of(placementId: Long): PlacementExamples? = placements.firstOrNull { it.placementId == placementId }

    /**
     * This book with [example] added to [placementId], learned on [phoneModel]. Examples learned on another phone are
     * dropped first (the round starts again on this one); a placement keeps the newest [SIT_EXAMPLES_PER_LABEL_MAX] per label.
     */
    fun withExample(placementId: Long, example: SitExample, phoneModel: String, hasBarometer: Boolean): SitExampleBook {
        val current = of(placementId)?.takeIf { it.isLearnedOn(phoneModel) }
        val kept = current?.examples.orEmpty() + example
        val trimmed = kept.filter { it.label != example.label } + kept.filter { it.label == example.label }.takeLast(SIT_EXAMPLES_PER_LABEL_MAX)
        val updated = PlacementExamples(placementId, phoneModel, hasBarometer, trimmed.sortedBy { it.atMs }, current?.occupied)
        return copy(placements = placements.filterNot { it.placementId == placementId } + updated)
    }

    /** This book with [envelope] as the occupied envelope of [placementId], replacing an older one; another phone's examples go first. */
    fun withOccupied(placementId: Long, envelope: OccupiedEnvelope, phoneModel: String, hasBarometer: Boolean): SitExampleBook {
        val current = of(placementId)?.takeIf { it.isLearnedOn(phoneModel) } ?: PlacementExamples(placementId, phoneModel, hasBarometer)
        val updated = current.copy(hasBarometer = hasBarometer, occupied = envelope)
        return copy(placements = placements.filterNot { it.placementId == placementId } + updated)
    }

    fun withoutPlacement(placementId: Long): SitExampleBook = copy(placements = placements.filterNot { it.placementId == placementId })

    /** Only the placements the Monitor still lists; an empty [ids] keeps all (a request that names none). */
    fun keeping(ids: Collection<Long>): SitExampleBook =
        if (ids.isEmpty()) this else copy(placements = placements.filter { it.placementId in ids })
}

/**
 * A seated person, or a car seat with its engine running, over the round's seated step (ISSUE-NOV26.0-O163 part 2,
 * Nov26.26): the [StillnessMeter] windows of [SIT_LEARN_OCCUPIED_MS], dated [atMs] on the Sensor's wall clock. It is
 * the placement's occupied movement (ISSUE-NOV25.23-O155 decision (b)): how much a sitter moves the phone there, and in
 * a car the engine's shake and the body's roll; part 3 judges a sitting period's movement against it in place of the
 * fixed [SIT_MOVEMENT_TILT_DEG], [SIT_MOVEMENT_OVER_20MG] and [SIT_JOLT_MAX_MS2].
 */
@Serializable
data class OccupiedEnvelope(val atMs: Long, val windows: List<StillnessMeter.Window>) {
    /** The windows the fixed rule takes for a sitter's movement; 0 means a sitter is not measurable at this placement. */
    val movingWindows: Int get() = windows.count { it.isMovement }

    /**
     * `movement in 9 of 11 windows of 10 s; peak 0.18 g, slow tilt up to 0.42 deg, rms up to 6.1 mg`, the slow tilt
     * named roll for a car seat; with no movement window it says that a sitter is not measurable there.
     */
    fun describe(type: SeatType): String {
        if (windows.isEmpty()) return "nothing measured"
        val tiltName = if (type == SeatType.CAR_SEAT) "roll" else "slow tilt"
        val numbers = "peak ${"%.2f".format(java.util.Locale.US, windows.maxOf { it.peakMg } / 1000.0)} g, " +
            "$tiltName up to ${"%.2f".format(java.util.Locale.US, windows.maxOf { it.slowTiltDeg })} deg, " +
            "rms up to ${"%.1f".format(java.util.Locale.US, windows.maxOf { it.rmsMg })} mg"
        val note = if (movingWindows == 0) "; no movement measurable: here a still sitter looks like an empty seat" else ""
        return "movement in $movingWindows of ${windows.size} windows of ${STILLNESS_WINDOW_MS / 1000} s; $numbers$note"
    }
}

/** A placement keeps at most this many examples of each label; a new one replaces the oldest. */
const val SIT_EXAMPLES_PER_LABEL_MAX = 10

/**
 * The distance of two checks, each number divided by about the spread that separated sits from stand-ups under the
 * A15's cushion (Nov25.23, Nov26.3): first move 0.1 m/s², jolt 0.1 g, tilt 1 deg, settling 0.5 s, barometer 5 cm.
 * An unclear first move counts as 0; the barometer only when both have it.
 */
fun sitDistance(a: SitFeatures, b: SitFeatures): Double {
    fun sq(x: Double) = x * x
    var sum = sq(((a.firstMoveMs2 ?: 0.0) - (b.firstMoveMs2 ?: 0.0)) / 0.1) +
        sq((a.joltG - b.joltG) / 0.1) +
        sq((a.tiltDeg - b.tiltDeg) / 1.0) +
        sq((a.settleMs - b.settleMs) / 500.0)
    val baroA = a.baroDropCm
    val baroB = b.baroDropCm
    if (baroA != null && baroB != null) sum += sq((baroA - baroB) / 5.0)
    return sqrt(sum)
}

/**
 * The Monitor asking its Sensor about the examples of [placementId] (ISSUE-NOV26.0-O163 part 2): [label] records the
 * next sit check as that label's example, null only asks for the examples; [clear] deletes the placement's examples
 * first. [keepIds] are all the placements the Monitor lists: the Sensor drops the examples of any other. [occupied]
 * (Nov26.26, without a [label]) records the seated step's [OccupiedEnvelope]; a Sensor before Nov26.26 ignores it.
 */
@Serializable
data class SitLearnRequest(
    val placementId: Long,
    val label: SitLabel? = null,
    val clear: Boolean = false,
    val keepIds: List<Long> = emptyList(),
    val occupied: Boolean = false
)

/** The Sensor's examples for its Monitors, with its own phone, so a Monitor can tell examples of another phone. */
@Serializable
data class SitExampleReport(
    val phoneModel: String,
    val hasBarometer: Boolean,
    val book: SitExampleBook = SitExampleBook(),
    /** The label the Sensor waits for, null when none (the step's window ended or the example came). */
    val waitingFor: SitLabel? = null
)

/** One learning step waits this long for a sit check on the Sensor; the Monitor's guide waits as long. */
const val SIT_LEARN_STEP_MS = 60_000L

/** After an example, sit and vibration alerts stay held this long, for the next step's request to arrive. */
const val SIT_LEARN_TAIL_MS = 15_000L

/** The seated step lasts this long (ISSUE-NOV26.0-O163 part 2, Nov26.26): about 11 whole stillness windows. */
const val SIT_LEARN_OCCUPIED_MS = 2 * 60_000L

/**
 * The seated step ends on the first window closing [SIT_LEARN_OCCUPIED_MS] after its start, up to one window later, and
 * gives up after this (samples paused); the Monitor waits as long.
 */
const val SIT_LEARN_OCCUPIED_WAIT_MS = SIT_LEARN_OCCUPIED_MS + 2 * STILLNESS_WINDOW_MS

/**
 * The Sensor's side of a learning round (ISSUE-NOV26.0-O163 part 2): holds the step the Monitor asked for, and turns
 * the next sit check into its example, or the stillness windows of the seated step into the placement's
 * [OccupiedEnvelope]. While a step waits, and [SIT_LEARN_TAIL_MS] after it, the round's own sits and jolts must not
 * alarm ([isHoldingAlerts]): the user sits on purpose with the Monitor in hand. Times are monotonic.
 * Thread-safe: requests come from the network, checks and windows from the motion sensor thread.
 */
class SitLearner {
    /** A labelled step, or the seated step when [label] is null. */
    private class Step(val placementId: Long, val label: SitLabel?, val startMs: Long, val untilMs: Long) {
        val windows = ArrayList<StillnessMeter.Window>()
    }

    private var step: Step? = null
    private var holdUntilMs = Long.MIN_VALUE

    /** Starts the step of [request], or ends a waiting one when the request only asks or clears. */
    @Synchronized
    fun onRequest(request: SitLearnRequest, nowMs: Long) {
        val label = request.label
        step = when {
            label != null -> Step(request.placementId, label, nowMs, nowMs + SIT_LEARN_STEP_MS)
            request.occupied -> Step(request.placementId, null, nowMs, nowMs + SIT_LEARN_OCCUPIED_WAIT_MS)
            else -> null
        }
        holdUntilMs = step?.untilMs ?: minOf(holdUntilMs, nowMs + SIT_LEARN_TAIL_MS)
    }

    /** The label waited for now, or null (also during the seated step). */
    @Synchronized
    fun waitingFor(nowMs: Long): SitLabel? = step?.takeIf { nowMs < it.untilMs }?.label

    @Synchronized
    fun isHoldingAlerts(nowMs: Long): Boolean = nowMs < holdUntilMs

    /** The placement and example [check] gives when a step waits; it ends the step. [wallMs] dates the example. */
    @Synchronized
    fun onSitCheck(check: SitCheck, nowMs: Long, wallMs: Long): Pair<Long, SitExample>? {
        val current = step?.takeIf { nowMs < it.untilMs } ?: return null
        val label = current.label ?: return null // The seated step's sits and shifts are part of its windows.
        step = null
        holdUntilMs = nowMs + SIT_LEARN_TAIL_MS
        return current.placementId to SitExample(label, wallMs, check.features, check.verdict)
    }

    /**
     * Adds a stillness [window] closing at [nowMs] to the seated step; a window begun before the step is left out.
     * Returns the placement and its envelope on the first window closing [SIT_LEARN_OCCUPIED_MS] after the start; it
     * ends the step. [wallMs] dates the envelope.
     */
    @Synchronized
    fun onWindow(window: StillnessMeter.Window, nowMs: Long, wallMs: Long): Pair<Long, OccupiedEnvelope>? {
        val current = step?.takeIf { nowMs < it.untilMs && it.label == null } ?: return null
        if (nowMs - STILLNESS_WINDOW_MS < current.startMs) return null
        current.windows.add(window)
        if (nowMs - current.startMs < SIT_LEARN_OCCUPIED_MS) return null
        step = null
        holdUntilMs = nowMs + SIT_LEARN_TAIL_MS
        return current.placementId to OccupiedEnvelope(wallMs, current.windows.toList())
    }
}

/**
 * The labelled steps of the Monitor's learning round (ISSUE-NOV26.0-O163 part 2): three sits and stand-ups, then a
 * bump; the seated step follows them as the last one ([SIT_LEARN_STEP_COUNT]).
 */
val SIT_LEARN_STEPS: List<SitLabel> =
    listOf(SitLabel.SIT, SitLabel.STAND_UP, SitLabel.SIT, SitLabel.STAND_UP, SitLabel.SIT, SitLabel.STAND_UP, SitLabel.BUMP)

/** All steps of the round: [SIT_LEARN_STEPS] and the seated step (Nov26.26). */
val SIT_LEARN_STEP_COUNT: Int = SIT_LEARN_STEPS.size + 1

/**
 * What the Monitor asks the user to do in the seated step (Nov26.26). A car seat is learned parked with the engine
 * running, not driving: the round holds only sit and vibration alerts, so a drive's turns and hills would raise tilt.
 */
fun sitOccupiedInstruction(type: SeatType): String {
    val minutes = SIT_LEARN_OCCUPIED_MS / 60_000
    return if (type == SeatType.CAR_SEAT) "Sit in the seat for $minutes min with the engine running and move as on a ride: shift, turn the wheel, reach for something. Do not get out."
    else "Sit on the seat for $minutes min and move as you naturally would: shift, lean, reach for something. Do not get up."
}

/**
 * The Monitor's line under a step with [secondsLeft] of its wait: a labelled step waits for the Sensor; the seated
 * step ([isSeated]) counts down its [SIT_LEARN_OCCUPIED_MS] first, then the Sensor's last window.
 */
fun sitLearnWaitText(isSeated: Boolean, isTimedOut: Boolean, secondsLeft: Int): String {
    val finishS = ((SIT_LEARN_OCCUPIED_WAIT_MS - SIT_LEARN_OCCUPIED_MS) / 1000).toInt()
    return when {
        isTimedOut && isSeated -> "Nothing received in ${SIT_LEARN_OCCUPIED_WAIT_MS / 1000} s: TRY AGAIN, or SKIP the step."
        isTimedOut -> "Nothing felt in ${SIT_LEARN_STEP_MS / 1000} s: TRY AGAIN, or SKIP the step."
        isSeated && secondsLeft > finishS -> "Keep going: ${secondsLeft - finishS} s"
        isSeated -> "Finishing on the Sensor: $secondsLeft s"
        else -> "Waiting for the Sensor: $secondsLeft s"
    }
}

/** What the Monitor asks the user to do for [label]. */
fun sitLearnInstruction(label: SitLabel, type: SeatType): String = when (label) {
    SitLabel.SIT -> if (type == SeatType.CAR_SEAT) "Get in and sit down on the seat as usual, then stay still for a moment." else "Sit down on the seat as usual, then stay still for a moment."
    SitLabel.STAND_UP -> if (type == SeatType.CAR_SEAT) "Get out of the seat, then leave it alone for a moment." else "Stand up from the seat, then leave it alone for a moment."
    SitLabel.BUMP -> if (type == SeatType.CAR_SEAT) "Without sitting, push down on the seat or close a door once." else "Without sitting, knock or bump the seat once, as a passer-by might."
}

/**
 * The placement's examples in one line for the Monitor's list (ISSUE-NOV26.0-O163): `No examples learned`, or
 * `3 sits, 3 stand-ups, 1 bump, learned on Galaxy A15 (barometer)`; examples of another phone than [sensorModel] say so.
 */
fun sitExamplesSummary(examples: PlacementExamples?, sensorModel: String?): String {
    if (examples == null || examples.isEmpty) return "No examples learned"
    val counts = (SitLabel.entries.mapNotNull { label ->
        examples.count(label).takeIf { it > 0 }?.let { "$it ${if (it == 1) label.singular else label.plural}" }
    } + listOfNotNull(examples.occupied?.let { "${SIT_LEARN_OCCUPIED_MS / 60_000} min seated" })).joinToString(", ")
    val phone = examples.phoneModel + if (examples.hasBarometer) " (barometer)" else ""
    return if (sensorModel != null && !examples.isLearnedOn(sensorModel)) "$counts, learned on another phone ($phone): learn again"
    else "$counts, learned on $phone"
}

/** The overlap warning of [examples], or null when sits and non-sits stand apart (ISSUE-NOV26.0-O163 part 2). */
fun sitOverlapWarning(examples: PlacementExamples?): String? {
    val overlap = examples?.overlapCount() ?: 0
    if (overlap == 0) return null
    return "Sits and non-sits overlap: $overlap of ${examples!!.examples.size} examples are nearer the other kind. Learn again, more distinctly."
}
