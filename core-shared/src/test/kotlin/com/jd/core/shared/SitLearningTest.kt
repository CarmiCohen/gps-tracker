package com.jd.core.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISSUE-NOV26.0-O163 part 2: the learned sit examples, the Sensor's learning steps and the Monitor's texts. */
class SitLearningTest {

    private val sitFeatures = SitFeatures(joltG = 0.45, firstMoveMs2 = -0.15, tiltDeg = 0.8, settleMs = 600L)
    private val standFeatures = SitFeatures(joltG = 0.5, firstMoveMs2 = 0.12, tiltDeg = 1.0, settleMs = 700L)
    private val bumpFeatures = SitFeatures(joltG = 1.4, firstMoveMs2 = null, tiltDeg = 0.1, settleMs = 300L)

    private fun example(label: SitLabel, atMs: Long, features: SitFeatures = sitFeatures) =
        SitExample(label, atMs, features, SitVerdict.NONE)

    private fun SitExampleBook.add(label: SitLabel, atMs: Long, features: SitFeatures = sitFeatures, model: String = "SM-A155F") =
        withExample(7L, example(label, atMs, features), model, hasBarometer = false)

    @Test
    fun anExampleIsKeptPerPlacementWithItsPhone() {
        val book = SitExampleBook().add(SitLabel.SIT, 1L).add(SitLabel.STAND_UP, 2L, standFeatures)
        val placement = book.of(7L)!!
        assertEquals("SM-A155F", placement.phoneModel)
        assertEquals(listOf(SitLabel.SIT, SitLabel.STAND_UP), placement.examples.map { it.label })
        assertNull(book.of(8L))
        assertEquals(2L, placement.newest(SitLabel.STAND_UP)?.atMs)
    }

    @Test
    fun eachLabelKeepsItsNewestExamples() {
        var book = SitExampleBook().add(SitLabel.BUMP, 0L, bumpFeatures)
        for (t in 1L..(SIT_EXAMPLES_PER_LABEL_MAX + 3L)) book = book.add(SitLabel.SIT, t)
        val placement = book.of(7L)!!
        assertEquals(SIT_EXAMPLES_PER_LABEL_MAX, placement.count(SitLabel.SIT))
        assertEquals("The oldest sits went", 4L, placement.examples.filter { it.label == SitLabel.SIT }.minOf { it.atMs })
        assertEquals("Another label is kept", 1, placement.count(SitLabel.BUMP))
    }

    /** User agreed 2026-11-26: another phone's accelerometer, rate, barometer and weight differ. */
    @Test
    fun learningOnAnotherPhoneStartsOver() {
        val book = SitExampleBook().add(SitLabel.SIT, 1L).add(SitLabel.SIT, 2L, model = "Redmi Note 9")
        val placement = book.of(7L)!!
        assertEquals("Redmi Note 9", placement.phoneModel)
        assertEquals(listOf(2L), placement.examples.map { it.atMs })
    }

    @Test
    fun theMonitorsPlacementsDecideWhatIsKept() {
        val book = SitExampleBook().add(SitLabel.SIT, 1L).withExample(9L, example(SitLabel.SIT, 2L), "SM-A155F", false)
        assertEquals(listOf(9L), book.keeping(listOf(1L, 9L)).placements.map { it.placementId })
        assertEquals("A request naming none keeps all", 2, book.keeping(emptyList()).placements.size)
        assertEquals(listOf(9L), book.withoutPlacement(7L).placements.map { it.placementId })
    }

    @Test
    fun sitsApartFromNonSitsDoNotOverlap() {
        val book = SitExampleBook()
            .add(SitLabel.SIT, 1L).add(SitLabel.SIT, 2L, sitFeatures.copy(joltG = 0.4))
            .add(SitLabel.STAND_UP, 3L, standFeatures).add(SitLabel.STAND_UP, 4L, standFeatures.copy(joltG = 0.55))
            .add(SitLabel.BUMP, 5L, bumpFeatures)
        assertEquals(0, book.of(7L)!!.overlapCount())
        assertNull(sitOverlapWarning(book.of(7L)))
        assertNull("One side only", sitOverlapWarning(SitExampleBook().add(SitLabel.SIT, 1L).of(7L)))
    }

    /** A stand-up measured like a sit lies nearer the sits: the round must warn. */
    @Test
    fun aStandUpLikeASitOverlaps() {
        val book = SitExampleBook()
            .add(SitLabel.SIT, 1L).add(SitLabel.SIT, 2L, sitFeatures.copy(joltG = 0.4))
            .add(SitLabel.STAND_UP, 3L, sitFeatures.copy(joltG = 0.46))
        val placement = book.of(7L)!!
        assertTrue(placement.overlapCount() >= 1)
        val warning = sitOverlapWarning(placement)
        assertNotNull(warning)
        assertTrue(warning!!, warning.contains("of 3 examples"))
    }

    @Test
    fun theDistanceWeighsTheFirstMoveAndTheBarometerOnlyWhenBothHaveIt() {
        assertEquals(0.0, sitDistance(sitFeatures, sitFeatures), 1e-9)
        assertTrue(sitDistance(sitFeatures, standFeatures) > 2.0) // Down 0.15 against up 0.12.
        val withBaro = sitFeatures.copy(baroDropCm = 20.0)
        assertEquals(sitDistance(sitFeatures, sitFeatures), sitDistance(withBaro, sitFeatures), 1e-9)
        assertEquals(4.0, sitDistance(withBaro, sitFeatures.copy(baroDropCm = 0.0)), 1e-9)
    }

    @Test
    fun aStepTakesTheNextCheckWithinItsWindow() {
        val learner = SitLearner()
        val check = SitCheck(sitFeatures, SitVerdict.SIT)
        assertNull("No step asked", learner.onSitCheck(check, 0L, 100L))
        assertFalse(learner.isHoldingAlerts(0L))

        learner.onRequest(SitLearnRequest(placementId = 7L, label = SitLabel.SIT), nowMs = 1_000L)
        assertEquals(SitLabel.SIT, learner.waitingFor(1_000L))
        assertTrue("The round's own sit does not alarm", learner.isHoldingAlerts(30_000L))
        val (placementId, example) = learner.onSitCheck(check, 30_000L, 5_000L)!!
        assertEquals(7L, placementId)
        assertEquals(SitExample(SitLabel.SIT, 5_000L, sitFeatures, SitVerdict.SIT), example)
        assertNull("One example per step", learner.onSitCheck(check, 31_000L, 5_100L))
        assertNull(learner.waitingFor(31_000L))
        assertTrue("Held until the next step's request", learner.isHoldingAlerts(30_000L + SIT_LEARN_TAIL_MS - 1))
        assertFalse(learner.isHoldingAlerts(30_000L + SIT_LEARN_TAIL_MS))
    }

    @Test
    fun aStepEndsAfterItsWindowOrWhenTheMonitorStops() {
        val learner = SitLearner()
        val check = SitCheck(sitFeatures, SitVerdict.NONE)
        learner.onRequest(SitLearnRequest(placementId = 7L, label = SitLabel.BUMP), nowMs = 0L)
        assertNull("Too late", learner.onSitCheck(check, SIT_LEARN_STEP_MS, 1L))
        assertFalse(learner.isHoldingAlerts(SIT_LEARN_STEP_MS))

        learner.onRequest(SitLearnRequest(placementId = 7L, label = SitLabel.BUMP), nowMs = 100_000L)
        learner.onRequest(SitLearnRequest(placementId = 7L), nowMs = 101_000L) // STOP: only asks.
        assertNull(learner.waitingFor(101_000L))
        assertNull(learner.onSitCheck(check, 102_000L, 1L))
        assertFalse("Held only a tail after the stop", learner.isHoldingAlerts(101_000L + SIT_LEARN_TAIL_MS))
    }

    @Test
    fun theMonitorsTexts() {
        assertEquals("No examples learned", sitExamplesSummary(null, "SM-A155F"))
        val book = SitExampleBook().add(SitLabel.SIT, 1L).add(SitLabel.SIT, 2L).add(SitLabel.STAND_UP, 3L, standFeatures)
            .add(SitLabel.BUMP, 4L, bumpFeatures)
        assertEquals("2 sits, 1 stand-up, 1 bump, learned on SM-A155F", sitExamplesSummary(book.of(7L), "SM-A155F"))
        assertEquals(
            "2 sits, 1 stand-up, 1 bump, learned on another phone (SM-A155F): learn again",
            sitExamplesSummary(book.of(7L), "Redmi Note 9")
        )
        assertEquals(
            "first move down 0.15 m/s², jolt 0.45 g, tilt 0.8 deg, settled in 0.6 s",
            sitFeatures.describe()
        )
        assertEquals(
            "no clear first move, jolt 1.40 g, tilt 0.1 deg, settled in 0.3 s, barometer rise 12 cm",
            bumpFeatures.copy(baroDropCm = -12.0).describe()
        )
        assertTrue(sitLearnInstruction(SitLabel.SIT, SeatType.CAR_SEAT).startsWith("Get in"))
        assertEquals(listOf(3, 3, 1), SitLabel.entries.map { label -> SIT_LEARN_STEPS.count { it == label } })
    }

    // ---- Nov26.26: the seated step and the occupied envelope ----

    private val stillWindow = StillnessMeter.Window(500, 3.0, 2, 0, 15.0, 0.1, 0.05)
    private val movingWindow = StillnessMeter.Window(500, 6.1, 40, 12, 180.0, 0.9, 0.42)

    @Test
    fun aWindowIsMovementByTheFixedRule() {
        assertFalse(stillWindow.isMovement)
        assertTrue(movingWindow.isMovement)
        assertTrue("Slow tilt alone", stillWindow.copy(slowTiltDeg = SIT_MOVEMENT_TILT_DEG).isMovement)
        assertTrue("Samples over 20 mg alone", stillWindow.copy(over20Mg = SIT_MOVEMENT_OVER_20MG).isMovement)
    }

    @Test
    fun theSeatedStepKeepsTheWindowsOfItsTwoMinutes() {
        val learner = SitLearner()
        assertNull("No step asked", learner.onWindow(movingWindow, 10_000L, 1L))
        learner.onRequest(SitLearnRequest(placementId = 7L, occupied = true), nowMs = 1_000L)
        assertNull("Not a labelled step", learner.waitingFor(1_000L))
        assertTrue("The sitter's own sits and jolts do not alarm", learner.isHoldingAlerts(100_000L))
        assertNull("Begun before the step", learner.onWindow(stillWindow, 5_000L, 1L))
        assertNull("A sit check during the step is part of its windows", learner.onSitCheck(SitCheck(sitFeatures, SitVerdict.SIT), 6_000L, 1L))
        var result: Pair<Long, OccupiedEnvelope>? = null
        var t = 15_000L
        while (result == null) {
            result = learner.onWindow(if (t % 20_000L == 15_000L) movingWindow else stillWindow, t, 9_000L)
            t += STILLNESS_WINDOW_MS
        }
        val (placementId, envelope) = result
        assertEquals(7L, placementId)
        assertEquals(9_000L, envelope.atMs)
        assertEquals("The windows closing at 15 to 125 s", 12, envelope.windows.size)
        assertEquals(6, envelope.movingWindows)
        assertNull("The step ended", learner.onWindow(movingWindow, t, 1L))
        val endMs = t - STILLNESS_WINDOW_MS
        assertTrue(learner.isHoldingAlerts(endMs + SIT_LEARN_TAIL_MS - 1))
        assertFalse(learner.isHoldingAlerts(endMs + SIT_LEARN_TAIL_MS))
    }

    @Test
    fun theSeatedStepGivesUpWithoutWindowsOrWhenTheMonitorStops() {
        val learner = SitLearner()
        learner.onRequest(SitLearnRequest(placementId = 7L, occupied = true), nowMs = 0L)
        assertNull("Too late", learner.onWindow(movingWindow, SIT_LEARN_OCCUPIED_WAIT_MS, 1L))
        assertFalse(learner.isHoldingAlerts(SIT_LEARN_OCCUPIED_WAIT_MS))

        learner.onRequest(SitLearnRequest(placementId = 7L, occupied = true), nowMs = 200_000L)
        learner.onRequest(SitLearnRequest(placementId = 7L), nowMs = 210_000L) // STOP.
        assertNull(learner.onWindow(movingWindow, 330_000L, 1L))
        assertFalse(learner.isHoldingAlerts(210_000L + SIT_LEARN_TAIL_MS))

        learner.onRequest(SitLearnRequest(placementId = 7L, label = SitLabel.SIT, occupied = true), nowMs = 400_000L)
        assertEquals("A label makes it a labelled step", SitLabel.SIT, learner.waitingFor(400_000L))
    }

    @Test
    fun theEnvelopeLivesBesideTheExamples() {
        val envelope = OccupiedEnvelope(5L, listOf(stillWindow, movingWindow))
        val book = SitExampleBook().add(SitLabel.SIT, 1L).withOccupied(7L, envelope, "SM-A155F", hasBarometer = false)
        assertEquals(envelope, book.of(7L)!!.occupied)
        assertEquals("The examples are kept", 1, book.of(7L)!!.count(SitLabel.SIT))
        assertEquals("A new example keeps the envelope", envelope, book.add(SitLabel.BUMP, 6L, bumpFeatures).of(7L)!!.occupied)
        assertNull("Another phone starts over", book.add(SitLabel.SIT, 7L, model = "Redmi Note 9").of(7L)!!.occupied)
        val other = book.withOccupied(7L, envelope.copy(atMs = 8L), "Redmi Note 9", hasBarometer = true).of(7L)!!
        assertTrue("Another phone's envelope drops this phone's examples", other.examples.isEmpty())
        assertTrue(other.hasBarometer)
        assertEquals("1 sit, 2 min seated, learned on SM-A155F", sitExamplesSummary(book.of(7L), "SM-A155F"))
        assertEquals(
            "2 min seated, learned on SM-A155F",
            sitExamplesSummary(SitExampleBook().withOccupied(7L, envelope, "SM-A155F", false).of(7L), "SM-A155F")
        )
    }

    @Test
    fun theEnvelopesTexts() {
        val envelope = OccupiedEnvelope(5L, listOf(stillWindow, movingWindow))
        assertEquals(
            "movement in 1 of 2 windows of 10 s; peak 0.18 g, slow tilt up to 0.42 deg, rms up to 6.1 mg",
            envelope.describe(SeatType.CHAIR)
        )
        assertTrue(envelope.describe(SeatType.CAR_SEAT).contains("roll up to 0.42 deg"))
        assertTrue(OccupiedEnvelope(5L, listOf(stillWindow)).describe(SeatType.SOFA).endsWith("a still sitter looks like an empty seat"))
        assertEquals("nothing measured", OccupiedEnvelope(5L, emptyList()).describe(SeatType.CHAIR))
        assertTrue(sitOccupiedInstruction(SeatType.CAR_SEAT).contains("engine running"))
        assertTrue(sitOccupiedInstruction(SeatType.CHAIR).startsWith("Sit on the seat for 2 min"))
        assertEquals(SIT_LEARN_STEPS.size + 1, SIT_LEARN_STEP_COUNT)
        assertEquals("Keep going: 120 s", sitLearnWaitText(isSeated = true, isTimedOut = false, secondsLeft = 140))
        assertEquals("Finishing on the Sensor: 20 s", sitLearnWaitText(isSeated = true, isTimedOut = false, secondsLeft = 20))
        assertEquals("Nothing received in 140 s: TRY AGAIN, or SKIP the step.", sitLearnWaitText(isSeated = true, isTimedOut = true, secondsLeft = 0))
        assertEquals("Waiting for the Sensor: 42 s", sitLearnWaitText(isSeated = false, isTimedOut = false, secondsLeft = 42))
        assertEquals("Nothing felt in 60 s: TRY AGAIN, or SKIP the step.", sitLearnWaitText(isSeated = false, isTimedOut = true, secondsLeft = 0))
    }

    /** The sentinel hands every stillness window to its owner, the seated step's source. */
    @Test
    fun theSentinelReportsEachStillnessWindow() {
        val windows = mutableListOf<StillnessMeter.Window>()
        val sentinel = MotionSentinel(onWindow = { windows += it })
        var t = 0L
        while (t <= 2 * STILLNESS_WINDOW_MS + 100L) { // The first sample only starts the gravity estimate.
            sentinel.onAccelerometer(0.0, 0.0, STANDARD_GRAVITY, t)
            t += 20L
        }
        assertEquals(2, windows.size)
        assertFalse(windows.first().isMovement)
    }

    /** A file or report before Nov26.26 has no envelope; one after it keeps it. */
    @Test
    fun theEnvelopeSurvivesJson() {
        val book = SitExampleBook().add(SitLabel.SIT, 1L).withOccupied(7L, OccupiedEnvelope(5L, listOf(movingWindow)), "SM-A155F", false)
        assertEquals(book, Json.decodeFromString<SitExampleBook>(Json.encodeToString(book)))
        val before = Json.decodeFromString<SitExampleBook>(Json.encodeToString(SitExampleBook().add(SitLabel.SIT, 1L)))
        assertNull(before.of(7L)!!.occupied)
        val request = SitLearnRequest(7L, occupied = true)
        assertEquals(request, Json.decodeFromString<SitLearnRequest>(Json.encodeToString(request)))
        assertFalse("isMovement is derived, not stored", Json.encodeToString(movingWindow).contains("isMovement"))
    }

    /** The settings file and the wire carry the book as JSON; a file before Nov26.25 has none. */
    @Test
    fun theBookSurvivesJson() {
        val book = SitExampleBook().add(SitLabel.SIT, 1L).add(SitLabel.BUMP, 2L, bumpFeatures.copy(baroDropCm = 3.0))
        assertEquals(book, Json.decodeFromString<SitExampleBook>(Json.encodeToString(book)))
        val request = SitLearnRequest(7L, SitLabel.STAND_UP, clear = true, keepIds = listOf(1L, 7L))
        assertEquals(request, Json.decodeFromString<SitLearnRequest>(Json.encodeToString(request)))
    }
}
