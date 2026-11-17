package com.jd.core.shared

/**
 * The Sensor's per-kind cool-down for event alerts (ISSUE-NOV17.10-O111): after an alert of one kind (noise, tilt,
 * lift, vibration), the next alert of the same kind passes only [coolDownMs] later; other kinds are not held back.
 * The detectors' own debouncing (persistence, hit count) comes first; this is the last step before an alert is sent.
 *
 * Times are monotonic milliseconds. Thread-safe: the microphone and the motion sensor threads both ask.
 */
class AlertCoolDown(private val coolDownMs: Long = ALERT_COOL_DOWN_MS) {
    private val lastPassedMs = HashMap<String, Long>()

    /** True, and the cool-down of [kind] starts again, when its last alert passed at least [coolDownMs] before [nowMs]. */
    @Synchronized
    fun tryPass(kind: String, nowMs: Long): Boolean {
        val last = lastPassedMs[kind]
        if (last != null && nowMs - last < coolDownMs) return false
        lastPassedMs[kind] = nowMs
        return true
    }
}

/** The time after an event alert in which the same kind raises no new alert (ISSUE-NOV17.10-O111). */
const val ALERT_COOL_DOWN_MS = 60_000L
