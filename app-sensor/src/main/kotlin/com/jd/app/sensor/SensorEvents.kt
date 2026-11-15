package com.jd.app.sensor

import com.jd.core.shared.EventLevel
import com.jd.core.shared.SensorSetupReport

/** An event to record: its level and text (ISSUE-NOV10.8-O84). */
data class LoggedEvent(val level: EventLevel, val message: String)

/**
 * The event for a change of the phone setup the Sensor reports (ISSUE-NOV10.8-O84), or null when the missing items and
 * faults are unchanged: a new detection settings revision alone is recorded where it is applied.
 */
fun setupChangeEvent(previous: SensorSetupReport?, current: SensorSetupReport): LoggedEvent? {
    if (previous != null && previous.missing == current.missing && previous.faults == current.faults) return null
    val problems = current.missing.map { "${it.label} missing" } + current.faults.map { it.text }
    return if (problems.isEmpty()) LoggedEvent(EventLevel.INFO, "Phone setup complete")
    else LoggedEvent(EventLevel.WARNING, "Phone setup needs attention: ${problems.joinToString("; ")}")
}

fun relayLinkEvent(isConnected: Boolean): LoggedEvent =
    if (isConnected) LoggedEvent(EventLevel.INFO, "Relay connected") else LoggedEvent(EventLevel.WARNING, "Relay connection lost")

fun monitorLinkEvent(isOnline: Boolean): LoggedEvent =
    if (isOnline) LoggedEvent(EventLevel.INFO, "A Monitor is connected") else LoggedEvent(EventLevel.INFO, "No Monitor connected")
