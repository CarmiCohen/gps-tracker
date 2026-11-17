package com.jd.app.monitor

import com.jd.core.shared.EventLevel

/** An event to record: its level and text (ISSUE-NOV10.8-O84). */
data class LoggedEvent(val level: EventLevel, val message: String)

/**
 * The events between two alarm books (ISSUE-NOV10.8-O84): each newly raised alarm, and each condition that no longer
 * holds, whether it cleared or its alarm was switched off; and the tracker going offline or returning, also with its
 * alarm switched off or muted (ISSUE-NOV15.4-O98 part 2). Acknowledgements are recorded where the user gives them.
 */
fun alarmEvents(previous: AlarmBook, current: AlarmBook): List<LoggedEvent> {
    val known = previous.alarms.mapTo(HashSet()) { it.type to it.raisedAtMs }
    val raised = current.alarms.filter { (it.type to it.raisedAtMs) !in known }
        .map { LoggedEvent(EventLevel.ALARM, "Alarm: ${it.type.title}") }
    val ended = (previous.conditions - current.conditions).sortedBy { it.ordinal }
        .map { LoggedEvent(EventLevel.INFO, "Ended: ${it.title}") }
    return raised + ended + listOfNotNull(trackerLinkEvent(previous.isTrackerOnline, current.isTrackerOnline))
}

/** A change of the tracker being heard; none before it is first heard, nor when a new pairing starts over. */
fun trackerLinkEvent(previous: Boolean?, current: Boolean?): LoggedEvent? = when {
    current == null || current == previous -> null
    current -> LoggedEvent(EventLevel.INFO, "Sensor online")
    else -> LoggedEvent(EventLevel.WARNING, "Sensor offline: nothing received for ${TRACKER_PRESENT_MS / 1000} s or the relay reported it silent")
}

/** The event of an acknowledgement, or null when no alarm was waiting for one. */
fun acknowledgementEvent(book: AlarmBook): LoggedEvent? =
    book.pending.takeIf { it.isNotEmpty() }
        ?.let { pending -> LoggedEvent(EventLevel.INFO, "Acknowledged: ${pending.joinToString { it.type.title }}") }

fun relayLinkEvent(isConnected: Boolean): LoggedEvent =
    if (isConnected) LoggedEvent(EventLevel.INFO, "Relay connected") else LoggedEvent(EventLevel.WARNING, "Relay connection lost")
