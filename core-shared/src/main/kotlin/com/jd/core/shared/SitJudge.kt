package com.jd.core.shared

/*
 * Judging sits by the placement's learned examples (ISSUE-NOV26.0-O163 part 3). A sit check is compared with the
 * active placement's examples learned on this phone: the nearest sit and the nearest non-sit (a stand-up or a bump)
 * decide. A clear match decides alone; a tie, or a sit matched only weakly, still alerts, marked uncertain (a security
 * app); with no example close, or none learned, the fixed rule of [MotionSentinel] decides as before. The seated
 * step's [OccupiedEnvelope] sets how hard a hit must be to count as vibration inside a sitting period, in place of the
 * fixed [SIT_JOLT_MAX_MS2].
 */

/** How a sit check's verdict was reached (ISSUE-NOV26.0-O163 part 3). */
enum class SitMatch {
    /** No examples learned on this phone for the placement: the fixed rule alone, as before Nov26.27. */
    RULE,
    /** Clearly nearest a learned example of one kind. */
    CLOSE,
    /** A sit by the examples, but as near a non-sit, or only a weak match: it alerts, marked uncertain. */
    UNCERTAIN,
    /** Examples learned, but none decides (none close, or a non-sit only weakly): the fixed rule decides. */
    FIXED_RULE
}

/** A judged sit check: the [verdict] acted on, how it was reached, and [text] for the alert or note (null for [SitMatch.RULE]). */
data class SitJudgement(val verdict: SitVerdict, val match: SitMatch, val text: String? = null)

/**
 * The sit judge of one [placement] (ISSUE-NOV26.0-O163 part 3), from its [examples] learned on this phone, null when
 * none were. Immutable: the Sensor builds a new one when the placement or its examples change.
 */
class SitJudge(val placement: SeatPlacement, val examples: PlacementExamples?) {

    private val learned: List<SitExample> = examples?.examples.orEmpty()

    /** A match this near (in [sitDistance] units) is close; up to [SIT_MATCH_WEAK_FACTOR] times it, weak. */
    val closeLimit: Double = sitCloseLimit(learned)

    /**
     * Judges a sit check of [features], which the fixed rule called [rule]. Only checks under the tilt limit come here
     * (more is the tilt alarm's).
     */
    fun judge(features: SitFeatures, rule: SitVerdict): SitJudgement {
        if (learned.isEmpty()) return SitJudgement(rule, SitMatch.RULE)
        val nearestSit = learned.filter { it.label.isSit }.minByOrNull { sitDistance(features, it.features) }
        val nearestOther = learned.filterNot { it.label.isSit }.minByOrNull { sitDistance(features, it.features) }
        val dSit = nearestSit?.let { sitDistance(features, it.features) } ?: Double.POSITIVE_INFINITY
        val dOther = nearestOther?.let { sitDistance(features, it.features) } ?: Double.POSITIVE_INFINITY
        if (minOf(dSit, dOther) > SIT_MATCH_WEAK_FACTOR * closeLimit) {
            return SitJudgement(rule, SitMatch.FIXED_RULE, "no learned example close, by the fixed rule")
        }
        if (dSit <= dOther) {
            return when {
                nearestOther != null && dOther <= SIT_MATCH_TIE_RATIO * dSit ->
                    SitJudgement(SitVerdict.SIT, SitMatch.UNCERTAIN, "uncertain: as near a learned ${nearestOther.label.singular} as a sit")
                dSit > closeLimit -> SitJudgement(SitVerdict.SIT, SitMatch.UNCERTAIN, "uncertain: only a weak match to a learned sit")
                else -> SitJudgement(SitVerdict.SIT, SitMatch.CLOSE, "close to a learned sit")
            }
        }
        val other = nearestOther!!.label
        return when {
            dSit <= SIT_MATCH_TIE_RATIO * dOther ->
                SitJudgement(SitVerdict.SIT, SitMatch.UNCERTAIN, "uncertain: as near a learned sit as a ${other.singular}")
            dOther > closeLimit -> SitJudgement(rule, SitMatch.FIXED_RULE, "only a weak match to a learned ${other.singular}, by the fixed rule")
            other == SitLabel.STAND_UP -> SitJudgement(SitVerdict.STAND_UP, SitMatch.CLOSE, "close to a learned stand-up")
            else -> SitJudgement(SitVerdict.NONE, SitMatch.CLOSE, "close to a learned ${other.singular}")
        }
    }

    /**
     * Inside a sitting period a vibration hit counts only over this (m/s²): [SIT_ENVELOPE_PEAK_MARGIN] times the seated
     * step's strongest sample, between the slider's [vibrationThreshold] and [SIT_JOLT_MAX_MS2]; without a seated step,
     * [SIT_JOLT_MAX_MS2] as before. The envelope only tightens: a sitter learned moving gently makes a blow count sooner.
     */
    fun seatedVibrationLimit(vibrationThreshold: Double): Double {
        val fixed = maxOf(vibrationThreshold, SIT_JOLT_MAX_MS2)
        val peakMg = examples?.occupied?.windows?.maxOfOrNull { it.peakMg } ?: return fixed
        return (peakMg / 1000.0 * STANDARD_GRAVITY * SIT_ENVELOPE_PEAK_MARGIN).coerceIn(vibrationThreshold, fixed)
    }

    /** For the Sensor's log: what judges sits on this placement. */
    fun describe(): String {
        val seated = examples?.occupied?.let { ", seated vibration from ${SIT_LEARN_OCCUPIED_MS / 60_000} min learned" } ?: ""
        if (learned.isEmpty()) return "Sits on ${placement.label} judged by the fixed rule (no examples learned on this phone)$seated"
        val counts = SitLabel.entries.mapNotNull { label ->
            learned.count { it.label == label }.takeIf { it > 0 }?.let { "$it ${if (it == 1) label.singular else label.plural}" }
        }.joinToString(", ")
        return "Sits on ${placement.label} judged by $counts (close within ${"%.1f".format(java.util.Locale.US, closeLimit)})$seated"
    }

    companion object {
        /** The judge of [placement] from [book], using only examples learned on [phoneModel]. */
        fun of(book: SitExampleBook, placement: SeatPlacement, phoneModel: String): SitJudge =
            SitJudge(placement, book.of(placement.id)?.takeIf { it.isLearnedOn(phoneModel) })
    }
}

/**
 * How near counts as close (ISSUE-NOV26.0-O163 part 3): [SIT_MATCH_CLOSE_SPREADS] times the median distance from each
 * example to the nearest other one of its own kind, at least [SIT_MATCH_CLOSE_MIN]; [SIT_MATCH_CLOSE_DEFAULT] while no
 * kind has two examples. On the A15's chair two sits differed by up to 5 units in their first move alone (Nov25.23).
 */
internal fun sitCloseLimit(examples: List<SitExample>): Double {
    val spreads = examples.indices.mapNotNull { i ->
        examples.indices.filter { it != i && examples[it].label == examples[i].label }
            .minOfOrNull { sitDistance(examples[i].features, examples[it].features) }
    }
    if (spreads.isEmpty()) return SIT_MATCH_CLOSE_DEFAULT
    val median = spreads.sorted()[spreads.size / 2]
    return maxOf(SIT_MATCH_CLOSE_MIN, SIT_MATCH_CLOSE_SPREADS * median)
}

const val SIT_MATCH_CLOSE_SPREADS = 1.5
const val SIT_MATCH_CLOSE_MIN = 2.0
const val SIT_MATCH_CLOSE_DEFAULT = 4.0

/** Beyond this many close limits no example decides: the fixed rule does. */
const val SIT_MATCH_WEAK_FACTOR = 2.0

/** The other kind's nearest example within this ratio of the nearest is a tie: a sit, uncertain. */
const val SIT_MATCH_TIE_RATIO = 1.25

/** A seated hit counts as vibration from this many times the seated step's strongest sample (ISSUE-NOV26.0-O163 part 3). */
const val SIT_ENVELOPE_PEAK_MARGIN = 3.0
