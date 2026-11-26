package com.jd.core.shared

/**
 * Whether the Sensor was just driven and parked, from its fixes (ISSUE-NOV26.0-O163 part 1): after a drive the Monitor
 * asks whether to switch the seat placement and never switches by itself (user 2026-11-26).
 *
 * A drive needs [DRIVE_MIN_FIXES] fixes faster than [DRIVE_ABOVE_MPS] (not a walk or a run), the newest one farther than
 * [DRIVE_MIN_DISTANCE_M] from the first, so a resting phone's GPS speed spikes, repeated by its rows at one place,
 * never make one. Car-fast fixes [DRIVE_PARKED_MS] apart start over. It is parked once its position stayed within
 * [DRIVE_PARKED_RADIUS_M] for [DRIVE_PARKED_MS], whatever speed the fixes report: rows repeat the last fix's speed when
 * the GPS is lost (an underground car park), and a red light is shorter. A fix not newer than the last is ignored. Not
 * thread-safe: one thread feeds it.
 */
class DriveWatch {
    private class Point(val timeMs: Long, val lat: Double, val lng: Double)

    var isDriving = false
        private set

    private var lastTimeMs = Long.MIN_VALUE
    private var carStart: Point? = null // The first car-fast fix of a drive not yet confirmed.
    private var carFixes = 0
    private var lastCarFixMs = 0L
    private var anchor: Point? = null // While driving: where the position has stayed since.

    /** Feeds one fix ([speedMps] null when not reported); returns the time the car parked when a drive has just ended. */
    fun onFix(timeMs: Long, lat: Double, lng: Double, speedMps: Float?): Long? {
        if (timeMs <= lastTimeMs) return null
        lastTimeMs = timeMs
        val here = Point(timeMs, lat, lng)
        if (isDriving) {
            val still = anchor ?: here.also { anchor = it }
            if (distance(still, here) > DRIVE_PARKED_RADIUS_M) {
                anchor = here
            } else if (timeMs - still.timeMs >= DRIVE_PARKED_MS) {
                isDriving = false
                anchor = null
                return still.timeMs
            }
            return null
        }
        if (carStart != null && timeMs - lastCarFixMs >= DRIVE_PARKED_MS) carStart = null
        if (speedMps == null || speedMps <= DRIVE_ABOVE_MPS) return null
        val start = carStart ?: here.also { carStart = it; carFixes = 0 }
        carFixes++
        lastCarFixMs = timeMs
        if (carFixes >= DRIVE_MIN_FIXES && distance(start, here) >= DRIVE_MIN_DISTANCE_M) {
            isDriving = true
            carStart = null
            anchor = here
        }
        return null
    }

    private fun distance(a: Point, b: Point) = SpatialMath.calculateDistance(a.lat, a.lng, b.lat, b.lng)
}

/** A fix faster than this (30 km/h) is a car's, beyond a walk, a run or most bicycles (ISSUE-NOV26.0-O163 part 1). */
const val DRIVE_ABOVE_MPS = 30f / 3.6f

/** Car-fast fixes that may make a drive. */
const val DRIVE_MIN_FIXES = 5

/** ...the newest at least this far (m) from the first. */
const val DRIVE_MIN_DISTANCE_M = 500.0

/** Parked: the position stayed within this radius (m)... */
const val DRIVE_PARKED_RADIUS_M = 50.0

/** ...for this long (5 min, beyond a red light); also the gap that starts the car-fast count over. */
const val DRIVE_PARKED_MS = 5 * 60_000L

/** The Monitor's question after a drive (ISSUE-NOV26.0-O163 part 1). */
const val DRIVE_QUESTION_TITLE = "Sensor seems to be in a car: switch placement?"

/** The question's text: the placement in use, and what a switch does. */
fun driveQuestionText(settings: AlertSettings, parkedSinceText: String): String {
    val carSeats = settings.carSeatPlacements()
    return "It was driven and has been parked since $parkedSinceText. In use: ${settings.activePlacement().label}. " +
        if (carSeats.isEmpty()) "There is no car seat placement yet: add one on the SEAT PLACEMENT screen."
        else "Sit checks use the chosen placement's learned examples."
}

/** The car seat placements a drive question offers. */
fun AlertSettings.carSeatPlacements(): List<SeatPlacement> = seatPlacements.filter { it.type == SeatType.CAR_SEAT }

/** Whether a drive should ask at all: not while a car seat is already in use. */
fun AlertSettings.asksAfterDrive(): Boolean = activePlacement().type != SeatType.CAR_SEAT
