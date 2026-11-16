package com.jd.app.sensor

import android.app.ApplicationExitInfo
import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.PastEvent
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

const val SERVICE_STARTED = "Sensor service started"
const val SERVICE_STOPPED = "Sensor service stopped"

fun serviceStartedMessage(versionName: String): String = "$SERVICE_STARTED (version $versionName)"

/** How the previous Sensor process ended, as Android recorded it (`ApplicationExitInfo`, API 30+). */
data class ProcessExit(val timestampMs: Long, val reason: Int)

/**
 * The stop the previous run did not record (ISSUE-NOV15.4-O98), or null when it did: only `onDestroy` records
 * [SERVICE_STOPPED], so a process killed by Android, a crash, an app update or a phone shutdown left the log's last
 * start without a stop. Called on the next start, before it records its own. With an Android exit record after that
 * start, the first one gives the event its reason and time (a later one may be a run without the service); otherwise
 * the event follows the last one the run recorded, as the phone's restart when that came before [bootTimeMs].
 * [exits] is asked only when a stop is missing.
 */
fun unrecordedStopEvent(events: List<EventEntry>, bootTimeMs: Long, exits: () -> List<ProcessExit>): PastEvent? {
    val lastLifecycle = events.lastOrNull { it.message.startsWith(SERVICE_STARTED) || it.message.startsWith(SERVICE_STOPPED) }
    if (lastLifecycle == null || lastLifecycle.message.startsWith(SERVICE_STOPPED)) return null
    val exit = exits().filter { it.timestampMs >= lastLifecycle.timestampMs }.minByOrNull { it.timestampMs }
    if (exit != null) return PastEvent(EventLevel.WARNING, "$SERVICE_STOPPED: ${exitReasonText(exit.reason)}", exit.timestampMs)
    val lastSeenMs = events.last().timestampMs
    val cause = if (lastSeenMs < bootTimeMs) "phone shut down or restarted" else "reason unknown"
    return PastEvent(EventLevel.WARNING, "$SERVICE_STOPPED: $cause (after this, exact time unknown)", lastSeenMs + 1)
}

/** Android's exit reason in words; the constants are compile-time values, so this also runs below API 30. */
fun exitReasonText(reason: Int): String = when (reason) {
    ApplicationExitInfo.REASON_LOW_MEMORY -> "killed by Android for memory"
    ApplicationExitInfo.REASON_CRASH, ApplicationExitInfo.REASON_CRASH_NATIVE -> "app crashed"
    ApplicationExitInfo.REASON_ANR -> "app not responding, closed"
    ApplicationExitInfo.REASON_USER_REQUESTED -> "force-stopped or app reinstalled"
    ApplicationExitInfo.REASON_USER_STOPPED -> "phone user profile stopped"
    ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "app updated"
    ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "app disabled or changed"
    ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "a permission was withdrawn"
    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "killed by Android for resource use"
    ApplicationExitInfo.REASON_SIGNALED -> "killed by the system"
    ApplicationExitInfo.REASON_FREEZER -> "killed while frozen"
    ApplicationExitInfo.REASON_DEPENDENCY_DIED, ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
    ApplicationExitInfo.REASON_EXIT_SELF -> "process ended (Android reason $reason)"
    else -> "killed by Android (reason $reason)"
}
