package com.jd.app.monitor

/** The stored pairing once it has been read; [trackerId] is null when the Monitor is not paired. */
data class Pairing(val trackerId: String?)

/** What the screen does with [MonitorSessionService] for a pairing. */
enum class SessionCommand { START, STOP }

/**
 * A null [pairing] means the stored pairing has not been read yet, and must leave the session alone.
 * Before ISSUE-NOV8.5-O50 the screen read that placeholder as "not paired" on every Activity recreation
 * (rotation, dark mode, font size), stopped the running service, lost acknowledged alarms, and sounded
 * them again when the service restarted a moment later.
 */
fun sessionCommandFor(pairing: Pairing?): SessionCommand? = when {
    pairing == null -> null
    pairing.trackerId != null -> SessionCommand.START
    else -> SessionCommand.STOP
}
