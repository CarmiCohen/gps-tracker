package com.jd.core.shared

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

/*
 * Learning sits per seat placement (ISSUE-NOV26.0-O163 part 2). The fixed sit rule comes from one chair (row 83);
 * chairs, people and cars differ, so the Monitor guides a learning round (sit, stand up, a bump) and the Sensor keeps
 * what each sit check measured as labelled examples per placement, in its settings file. Judging an event by the
 * examples is part 3: until then the examples change no detection.
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
    val examples: List<SitExample> = emptyList()
) {
    fun isLearnedOn(model: String): Boolean = phoneModel == model

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
        val updated = PlacementExamples(placementId, phoneModel, hasBarometer, trimmed.sortedBy { it.atMs })
        return copy(placements = placements.filterNot { it.placementId == placementId } + updated)
    }

    fun withoutPlacement(placementId: Long): SitExampleBook = copy(placements = placements.filterNot { it.placementId == placementId })

    /** Only the placements the Monitor still lists; an empty [ids] keeps all (a request that names none). */
    fun keeping(ids: Collection<Long>): SitExampleBook =
        if (ids.isEmpty()) this else copy(placements = placements.filter { it.placementId in ids })
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
 * first. [keepIds] are all the placements the Monitor lists: the Sensor drops the examples of any other.
 */
@Serializable
data class SitLearnRequest(
    val placementId: Long,
    val label: SitLabel? = null,
    val clear: Boolean = false,
    val keepIds: List<Long> = emptyList()
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

/**
 * The Sensor's side of a learning round (ISSUE-NOV26.0-O163 part 2): holds the step the Monitor asked for, and turns
 * the next sit check into its example. While a step waits, and [SIT_LEARN_TAIL_MS] after it, the round's own sits and
 * jolts must not alarm ([isHoldingAlerts]): the user sits on purpose with the Monitor in hand. Times are monotonic.
 * Thread-safe: requests come from the network, checks from the motion sensor thread.
 */
class SitLearner {
    private data class Step(val placementId: Long, val label: SitLabel, val untilMs: Long)

    private var step: Step? = null
    private var holdUntilMs = Long.MIN_VALUE

    /** Starts the step of [request], or ends a waiting one when the request only asks or clears. */
    @Synchronized
    fun onRequest(request: SitLearnRequest, nowMs: Long) {
        val label = request.label
        step = label?.let { Step(request.placementId, it, nowMs + SIT_LEARN_STEP_MS) }
        holdUntilMs = if (label != null) nowMs + SIT_LEARN_STEP_MS else minOf(holdUntilMs, nowMs + SIT_LEARN_TAIL_MS)
    }

    /** The label waited for now, or null. */
    @Synchronized
    fun waitingFor(nowMs: Long): SitLabel? = step?.takeIf { nowMs < it.untilMs }?.label

    @Synchronized
    fun isHoldingAlerts(nowMs: Long): Boolean = nowMs < holdUntilMs

    /** The placement and example [check] gives when a step waits; it ends the step. [wallMs] dates the example. */
    @Synchronized
    fun onSitCheck(check: SitCheck, nowMs: Long, wallMs: Long): Pair<Long, SitExample>? {
        val current = step?.takeIf { nowMs < it.untilMs } ?: return null
        step = null
        holdUntilMs = nowMs + SIT_LEARN_TAIL_MS
        return current.placementId to SitExample(current.label, wallMs, check.features, check.verdict)
    }
}

/** The steps of the Monitor's learning round (ISSUE-NOV26.0-O163 part 2): three sits and stand-ups, then a bump. */
val SIT_LEARN_STEPS: List<SitLabel> =
    listOf(SitLabel.SIT, SitLabel.STAND_UP, SitLabel.SIT, SitLabel.STAND_UP, SitLabel.SIT, SitLabel.STAND_UP, SitLabel.BUMP)

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
    if (examples == null || examples.examples.isEmpty()) return "No examples learned"
    val counts = SitLabel.entries.mapNotNull { label ->
        examples.count(label).takeIf { it > 0 }?.let { "$it ${if (it == 1) label.singular else label.plural}" }
    }.joinToString(", ")
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
