package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISSUE-NOV26.0-O163 part 3: judging a sit check by the placement's learned examples. */
class SitJudgeTest {

    private val car = SeatPlacement(3L, "Driver's seat", SeatType.CAR_SEAT)
    private val sit = SitFeatures(joltG = 0.45, firstMoveMs2 = -0.15, tiltDeg = 0.8, settleMs = 600L)
    private val standUp = SitFeatures(joltG = 0.5, firstMoveMs2 = 0.3, tiltDeg = 1.0, settleMs = 700L)
    private val bump = SitFeatures(joltG = 0.2, firstMoveMs2 = null, tiltDeg = 0.1, settleMs = 300L)

    private fun judge(vararg examples: Pair<SitLabel, SitFeatures>, occupied: OccupiedEnvelope? = null) = SitJudge(
        car,
        PlacementExamples(car.id, "SM-A155F", false, examples.mapIndexed { i, (label, f) -> SitExample(label, i.toLong(), f, SitVerdict.NONE) }, occupied)
    )

    private val learned = judge(SitLabel.SIT to sit, SitLabel.STAND_UP to standUp, SitLabel.BUMP to bump)

    @Test
    fun withoutExamplesTheFixedRuleDecides() {
        val none = SitJudge(car, null)
        assertEquals(SitJudgement(SitVerdict.SIT, SitMatch.RULE), none.judge(sit, SitVerdict.SIT))
        assertEquals(SitJudgement(SitVerdict.NONE, SitMatch.RULE), none.judge(sit, SitVerdict.NONE))
        assertNull(none.judge(sit, SitVerdict.NONE).text)
    }

    /** A gentle sit the fixed rule misses (under 0.25 g), learned at this seat. */
    @Test
    fun nearALearnedSitIsASitWhateverTheRuleSaid() {
        val j = learned.judge(sit.copy(joltG = 0.47), SitVerdict.NONE)
        assertEquals(SitVerdict.SIT, j.verdict)
        assertEquals(SitMatch.CLOSE, j.match)
        assertEquals("close to a learned sit", j.text)
    }

    @Test
    fun nearALearnedBumpIsNoSitWhateverTheRuleSaid() {
        val j = learned.judge(bump.copy(joltG = 0.22), SitVerdict.SIT)
        assertEquals(SitJudgement(SitVerdict.NONE, SitMatch.CLOSE, "close to a learned bump"), j)
    }

    @Test
    fun nearALearnedStandUpIsAStandUp() {
        assertEquals(SitVerdict.STAND_UP, learned.judge(standUp.copy(tiltDeg = 1.2), SitVerdict.NONE).verdict)
    }

    /** A security app: what lies as near a sit as a bump alerts, marked uncertain. */
    @Test
    fun aTieAlertsUncertain() {
        val between = SitFeatures(joltG = 0.325, firstMoveMs2 = -0.075, tiltDeg = 0.45, settleMs = 450L) // Halfway.
        val j = learned.judge(between, SitVerdict.NONE)
        assertEquals(SitVerdict.SIT, j.verdict)
        assertEquals(SitMatch.UNCERTAIN, j.match)
        val text = j.text.orEmpty()
        assertTrue(text, text.startsWith("uncertain: as near a learned "))
    }

    @Test
    fun aWeakMatchToASitAlertsUncertain() {
        // 5 units from the sit (0.5 g more), far further from the others; the close limit is the default 4.
        val j = learned.judge(sit.copy(joltG = 0.95), SitVerdict.NONE)
        assertEquals(SitJudgement(SitVerdict.SIT, SitMatch.UNCERTAIN, "uncertain: only a weak match to a learned sit"), j)
    }

    @Test
    fun farFromEveryExampleTheFixedRuleDecides() {
        val blow = SitFeatures(joltG = 3.0, firstMoveMs2 = 2.0, tiltDeg = 4.0, settleMs = 5_000L)
        assertEquals(SitJudgement(SitVerdict.NONE, SitMatch.FIXED_RULE, "no learned example close, by the fixed rule"), learned.judge(blow, SitVerdict.NONE))
        assertEquals(SitVerdict.SIT, learned.judge(blow, SitVerdict.SIT).verdict)
    }

    @Test
    fun aWeakMatchToABumpLeavesItToTheFixedRule() {
        val door = SitFeatures(joltG = 1.5, firstMoveMs2 = null, tiltDeg = 0.1, settleMs = 300L) // A car door, far from the sit.
        val j = judge(SitLabel.SIT to sit, SitLabel.BUMP to door).judge(door.copy(joltG = 2.0), SitVerdict.SIT) // 5 units from the door.
        assertEquals(SitJudgement(SitVerdict.SIT, SitMatch.FIXED_RULE, "only a weak match to a learned bump, by the fixed rule"), j)
    }

    @Test
    fun theCloseLimitFollowsTheSpreadOfEachKind() {
        assertEquals(SIT_MATCH_CLOSE_DEFAULT, sitCloseLimit(emptyList()), 1e-9)
        val wide = judge(SitLabel.SIT to sit, SitLabel.SIT to sit.copy(joltG = 0.85), SitLabel.BUMP to bump)
        assertEquals("1.5 times the 4 units between the sits", 6.0, wide.closeLimit, 1e-6)
        val tight = judge(SitLabel.SIT to sit, SitLabel.SIT to sit.copy(joltG = 0.46), SitLabel.BUMP to bump)
        assertEquals(SIT_MATCH_CLOSE_MIN, tight.closeLimit, 1e-9)
    }

    /** User agreed 2026-11-26: another phone's examples do not count here. */
    @Test
    fun onlyExamplesOfThisPhoneJudge() {
        val book = SitExampleBook().withExample(car.id, SitExample(SitLabel.BUMP, 1L, sit, SitVerdict.SIT), "Redmi Note 9", false)
        assertEquals(SitMatch.RULE, SitJudge.of(book, car, "SM-A155F").judge(sit, SitVerdict.SIT).match)
        assertEquals(SitVerdict.NONE, SitJudge.of(book, car, "Redmi Note 9").judge(sit, SitVerdict.SIT).verdict)
        assertEquals("Another placement's examples do not count", SitMatch.RULE, SitJudge.of(book, DEFAULT_SEAT_PLACEMENT, "Redmi Note 9").judge(sit, SitVerdict.SIT).match)
    }

    @Test
    fun theSeatedStepSetsTheSeatedVibrationLimitWithinTheFixedOnes() {
        val slider = vibrationThresholdForSensitivity(DEFAULT_VIBRATION_SENSITIVITY) // 0.13 g
        fun limitG(peakMg: Double?) = (peakMg?.let { judge(occupied = OccupiedEnvelope(0L, listOf(window(it)))) } ?: judge())
            .seatedVibrationLimit(slider) / STANDARD_GRAVITY
        assertEquals("No seated step: the fixed 1 g", 1.0, limitG(null), 1e-9)
        assertEquals("Three times the seated peak", 0.3, limitG(100.0), 1e-9)
        assertEquals("Never under the slider", 0.13, limitG(10.0), 1e-9)
        assertEquals("Never over the fixed 1 g", 1.0, limitG(800.0), 1e-9)
    }

    @Test
    fun theLogNamesWhatJudges() {
        assertEquals(
            "Sits on Driver's seat (car seat) judged by the fixed rule (no examples learned on this phone)",
            SitJudge(car, null).describe()
        )
        assertEquals("Sits on Driver's seat (car seat) judged by 1 sit, 1 stand-up, 1 bump (close within 4.0)", learned.describe())
    }

    private fun window(peakMg: Double) = StillnessMeter.Window(500, 5.0, 20, 6, peakMg, 1.0, 0.5)
}
