package com.jd.app.monitor

import com.jd.core.shared.EventLevel

/** An event to record: its level and text (ISSUE-NOV10.8-O84). */
data class LoggedEvent(val level: EventLevel, val message: String)

/**
 * The events between two alarm books (ISSUE-NOV10.8-O84): each newly raised alarm, and each condition that no longer
 * holds, whether it cleared or its alarm was switched off. Acknowledgements are recorded where the user gives them.
 */
fun alarmEvents(previous: AlarmBook, current: AlarmBook): List<LoggedEvent> {
    val known = previous.alarms.mapTo(HashSet()) { it.type to it.raisedAtMs }
    val raised = current.alarms.filter { (it.type to it.raisedAtMs) !in known }
        .map { LoggedEvent(EventLevel.ALARM, "Alarm: ${it.type.title}") }
    val ended = (previous.conditions - current.conditions).sortedBy { it.ordinal }
        .map { LoggedEvent(EventLevel.INFO, "Ended: ${it.title}") }
    return raised + ended
}

/** The event of an acknowledgement, or null when no alarm was waiting for one. */
fun acknowledgementEvent(book: AlarmBook): LoggedEvent? =
    book.pending.takeIf { it.isNotEmpty() }
        ?.let { pending -> LoggedEvent(EventLevel.INFO, "Acknowledged: ${pending.joinToString { it.type.title }}") }

fun relayLinkEvent(isConnected: Boolean): LoggedEvent =
    if (isConnected) LoggedEvent(EventLevel.INFO, "Relay connected") else LoggedEvent(EventLevel.WARNING, "Relay connection lost")
