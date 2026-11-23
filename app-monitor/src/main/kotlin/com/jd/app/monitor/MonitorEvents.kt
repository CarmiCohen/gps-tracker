package com.jd.app.monitor

import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventSource

/** An event to record: its level and text (ISSUE-NOV10.8-O84). */
data class LoggedEvent(val level: EventLevel, val message: String)

/**
 * The events between two alarm books (ISSUE-NOV10.8-O84): each newly raised alarm, and each condition that no longer
 * holds, whether it cleared or its alarm was switched off; and the tracker going offline or returning, also with its
 * alarm switched off or muted (ISSUE-NOV15.4-O98 part 2). Acknowledgements are recorded where the user gives them.
 * An end is a detail (ISSUE-NOV23.4-O139): the list shows the alarm, and its end only with Details.
 */
fun alarmEvents(previous: AlarmBook, current: AlarmBook): List<LoggedEvent> {
    val known = previous.alarms.mapTo(HashSet()) { it.type to it.raisedAtMs }
    val raised = current.alarms.filter { (it.type to it.raisedAtMs) !in known }
        .map { LoggedEvent(EventLevel.ALARM, alarmMessage(it.type)) }
    val ended = (previous.conditions - current.conditions).sortedBy { it.ordinal }
        .map { LoggedEvent(EventLevel.DETAIL, "Ended: ${it.title}") }
    return raised + ended + listOfNotNull(trackerLinkEvent(previous.isTrackerOnline, current.isTrackerOnline))
}

/** A change of the tracker being heard; none before it is first heard, nor when a new pairing starts over. */
fun trackerLinkEvent(previous: Boolean?, current: Boolean?): LoggedEvent? = when {
    current == null || current == previous -> null
    current -> LoggedEvent(EventLevel.INFO, "Sensor online")
    else -> LoggedEvent(EventLevel.WARNING, "Sensor offline: nothing received for ${TRACKER_PRESENT_MS / 1000} s or the relay reported it silent")
}

fun alarmMessage(type: AlarmType): String = "Alarm: ${type.title}"

/** The event of an acknowledgement, a detail (ISSUE-NOV23.4-O139), or null when no alarm was waiting for one. */
fun acknowledgementEvent(book: AlarmBook): LoggedEvent? =
    book.pending.takeIf { it.isNotEmpty() }
        ?.let { pending -> LoggedEvent(EventLevel.DETAIL, "Acknowledged: ${pending.joinToString { it.type.title }}") }

/** A Monitor alarm answers a Sensor alarm raised up to this long before it, on the Sensor's clock... */
const val ECHO_AFTER_MS = 5 * 60_000L

/** ...or this long after it, for the difference of the two phones' clocks. */
const val ECHO_BEFORE_MS = 60_000L

/**
 * The Sensor alarms the Monitor answered with its own alarm of the same kind (ISSUE-NOV23.4-O139), listed only with
 * Details, so an alarm shows as one line, the Monitor's. A Sensor alarm the Monitor did not answer (switched off, muted,
 * or learned after the alarm window) stays listed, and so does one from a Sensor before Nov23.4, which names no kind.
 */
fun echoedSensorAlarms(events: List<EventEntry>): Set<String> {
    val raised = events.filter { it.source == EventSource.MONITOR && it.level == EventLevel.ALARM }
    if (raised.isEmpty()) return emptySet()
    return events.filter { sensor ->
        val kind = sensor.kind
        sensor.source == EventSource.SENSOR && sensor.level == EventLevel.ALARM && kind != null &&
            alarmMessage(violationAlarm(kind)).let { message ->
                raised.any { it.message == message && it.timestampMs - sensor.timestampMs in -ECHO_BEFORE_MS..ECHO_AFTER_MS }
            }
    }.mapTo(HashSet()) { it.id }
}

fun relayLinkEvent(isConnected: Boolean): LoggedEvent =
    if (isConnected) LoggedEvent(EventLevel.INFO, "Relay connected") else LoggedEvent(EventLevel.WARNING, "Relay connection lost")
