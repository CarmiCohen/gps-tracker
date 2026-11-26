package com.jd.core.shared

import kotlinx.serialization.Serializable
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * How still the phone was over each [windowMs] window (ISSUE-NOV25.23-O155): an empty seat rests, a seated person's
 * small movements (breathing, shifting) move the phone a little. The per-minute vibration maximum could not tell them
 * apart (Nov26.1: 0.02 g in every empty minute and in 6 seated ones), so a window gives several finer measures:
 * the samples' deviation from the estimated gravity (root mean square, and how many exceed 10 and 20 mg), which is
 * mostly the sensor's own noise at rest, and the slow movement, from the 1 s means (sensor noise averaged out): the
 * spread of their magnitude and the largest angle between their directions and the window's first.
 *
 * Not thread-safe: the sensor thread feeds it through [MotionSentinel].
 */
class StillnessMeter(private val windowMs: Long = STILLNESS_WINDOW_MS) {

    /** Serializable: a learning round's seated step keeps its windows as the placement's occupied envelope (ISSUE-NOV26.0-O163). */
    @Serializable
    data class Window(
        val samples: Int,
        val rmsMg: Double,
        val over10Mg: Int,
        val over20Mg: Int,
        val peakMg: Double,
        val slowSdMg: Double,
        val slowTiltDeg: Double
    ) {
        /** A sitter's movement, not the resting seat's noise (ISSUE-NOV25.23-O155: [SIT_MOVEMENT_TILT_DEG], [SIT_MOVEMENT_OVER_20MG]). */
        val isMovement: Boolean get() = slowTiltDeg >= SIT_MOVEMENT_TILT_DEG || over20Mg >= SIT_MOVEMENT_OVER_20MG

        fun describe(): String = "n $samples, rms ${f1(rmsMg)} mg, over 10/20 mg $over10Mg/$over20Mg, peak ${peakMg.toInt()} mg, " +
            "slow sd ${f2(slowSdMg)} mg, slow tilt ${"%.3f".format(java.util.Locale.US, slowTiltDeg)} deg"

        private fun f1(v: Double) = "%.1f".format(java.util.Locale.US, v)
        private fun f2(v: Double) = "%.2f".format(java.util.Locale.US, v)
    }

    private var windowStartMs: Long? = null
    private var samples = 0
    private var sumSquares = 0.0
    private var over10 = 0
    private var over20 = 0
    private var peak = 0.0

    private var secondStartMs = 0L
    private var secondX = 0.0
    private var secondY = 0.0
    private var secondZ = 0.0
    private var secondCount = 0
    private val secondMagnitudes = ArrayList<Double>()
    private var firstX = 0.0
    private var firstY = 0.0
    private var firstZ = 0.0
    private var hasFirst = false
    private var maxTilt = 0.0

    /** Feeds one sample in m/s² and its [deviationMs2] from the estimated gravity; returns a window when one ends. */
    fun onSample(x: Double, y: Double, z: Double, deviationMs2: Double, nowMs: Long): Window? {
        // A sample at or after a boundary opens the next second or window.
        val start = windowStartMs ?: nowMs.also { begin(it) }
        if (nowMs - secondStartMs >= 1_000L) closeSecond(nowMs)
        var window: Window? = null
        if (nowMs - start >= windowMs) {
            val mean = if (secondMagnitudes.isEmpty()) 0.0 else secondMagnitudes.average()
            val sd = if (secondMagnitudes.size < 2) 0.0 else sqrt(secondMagnitudes.sumOf { (it - mean) * (it - mean) } / (secondMagnitudes.size - 1))
            window = Window(samples, sqrt(sumSquares / samples.coerceAtLeast(1)), over10, over20, peak, sd / STANDARD_GRAVITY * 1000.0, maxTilt)
            begin(nowMs)
        }
        val mg = deviationMs2 / STANDARD_GRAVITY * 1000.0
        samples++
        sumSquares += mg * mg
        if (mg > 10.0) over10++
        if (mg > 20.0) over20++
        if (mg > peak) peak = mg
        secondX += x; secondY += y; secondZ += z
        secondCount++
        return window
    }

    /** Samples stopped: the next sample starts a new window. */
    fun reset() {
        windowStartMs = null
    }

    private fun begin(nowMs: Long) {
        windowStartMs = nowMs
        samples = 0
        sumSquares = 0.0
        over10 = 0
        over20 = 0
        peak = 0.0
        secondMagnitudes.clear()
        hasFirst = false
        maxTilt = 0.0
        startSecond(nowMs)
    }

    private fun startSecond(nowMs: Long) {
        secondStartMs = nowMs
        secondX = 0.0; secondY = 0.0; secondZ = 0.0
        secondCount = 0
    }

    private fun closeSecond(nowMs: Long) {
        if (secondCount == 0) return startSecond(nowMs)
        val mx = secondX / secondCount
        val my = secondY / secondCount
        val mz = secondZ / secondCount
        val magnitude = sqrt(mx * mx + my * my + mz * mz)
        secondMagnitudes.add(magnitude)
        if (!hasFirst) {
            firstX = mx; firstY = my; firstZ = mz
            hasFirst = true
        } else {
            val lengths = magnitude * sqrt(firstX * firstX + firstY * firstY + firstZ * firstZ)
            if (lengths > 1e-6) {
                val angle = Math.toDegrees(acos(((mx * firstX + my * firstY + mz * firstZ) / lengths).coerceIn(-1.0, 1.0)))
                if (angle > maxTilt) maxTilt = angle
            }
        }
        startSecond(nowMs)
    }
}

/** The stillness window (ISSUE-NOV25.23-O155): 10 s, short enough to follow a stand-up, long enough to see a sitter. */
const val STILLNESS_WINDOW_MS = 10_000L
