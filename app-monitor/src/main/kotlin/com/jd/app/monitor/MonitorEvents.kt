package com.jd.app.monitor

import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** An event to record: its level and text (ISSUE-NOV10.8-O84). */
data class LoggedEvent(val level: EventLevel, val message: String)

/**
 * The events between two alarm books (ISSUE-NOV10.8-O84): each newly raised alarm, and each condition that no longer
 * holds, whether it cleared or its alarm was switched off; and the tracker going offline or returning, also with its
 * alarm switched off or muted (ISSUE-NOV15.4-O98 part 2). Acknowledgements are recorded where the user gives them.
 * An end is a detail (ISSUE-NOV23.4-O139): the list shows the alarm, and its end only with More.
 */
fun alarmEvents(previous: AlarmBook, current: AlarmBook): List<LoggedEvent> {
    val known = previous.alarms.mapTo(HashSet()) { it.type to it.raisedAtMs }
    val raised = current.alarms.filter { (it.type to it.raisedAtMs) !in known }
        .map { LoggedEvent(EventLevel.ALARM, alarmMessage(it.type)) }
    val ended = (previous.conditions - current.conditions).sortedBy { it.ordinal }
        .map { LoggedEvent(EventLevel.DETAIL, "$ENDED_PREFIX${it.title}") }
    return raised + ended + listOfNotNull(trackerLinkEvent(previous.isTrackerOnline, current.isTrackerOnline))
}

/** A change of the tracker being heard; none before it is first heard, nor when a new pairing starts over. */
fun trackerLinkEvent(previous: Boolean?, current: Boolean?): LoggedEvent? = when {
    current == null || current == previous -> null
    current -> LoggedEvent(EventLevel.INFO, "Sensor online")
    else -> LoggedEvent(EventLevel.WARNING, "Sensor offline: nothing received for ${TRACKER_PRESENT_MS / 1000} s or the relay reported it silent")
}

fun alarmMessage(type: AlarmType): String = "$ALARM_PREFIX${type.title}"

/** The event of an acknowledgement, a detail (ISSUE-NOV23.4-O139), or null when no alarm was waiting for one. */
fun acknowledgementEvent(book: AlarmBook): LoggedEvent? =
    book.pending.takeIf { it.isNotEmpty() }
        ?.let { pending -> LoggedEvent(EventLevel.DETAIL, "$ACKNOWLEDGED_PREFIX${pending.joinToString { it.type.title }}") }

/** A Monitor alarm answers a Sensor alarm raised up to this long before it, on the Sensor's clock... */
const val ECHO_AFTER_MS = 5 * 60_000L

/** ...or this long after it, for the difference of the two phones' clocks. */
const val ECHO_BEFORE_MS = 60_000L

/**
 * The Sensor alarms the Monitor answered with its own alarm of the same kind (ISSUE-NOV23.4-O139), listed only with
 * More, so an alarm shows as one line, the Monitor's. A Sensor alarm the Monitor did not answer (switched off, muted,
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

/**
 * The one-line text of each Monitor alarm that has both ended and been acknowledged (SIMP-NOV25.0-S1), by event id:
 * `Alarm: <type>, ended hh:mm, acknowledged hh:mm`. Its end and acknowledgement are the first of each, for its type,
 * after it and before the next alarm of that type; an alarm still waiting for either keeps its own text. Display only:
 * without More the list shows this text, with More the separate events as recorded.
 */
fun finishedAlarmTexts(events: List<EventEntry>, formatTime: (Long) -> String = ::shortTime): Map<String, String> {
    val monitor = events.filter { it.source == EventSource.MONITOR }.sortedBy { it.timestampMs }
    val result = HashMap<String, String>()
    monitor.forEachIndexed { index, alarm ->
        if (alarm.level != EventLevel.ALARM || !alarm.message.startsWith(ALARM_PREFIX)) return@forEachIndexed
        val title = alarm.message.removePrefix(ALARM_PREFIX)
        val after = monitor.drop(index + 1).takeWhile { it.level != EventLevel.ALARM || it.message != alarm.message }
        val ended = after.firstOrNull { it.level == EventLevel.DETAIL && it.message == "$ENDED_PREFIX$title" }
        val acknowledged = after.firstOrNull {
            it.level == EventLevel.DETAIL && it.message.startsWith(ACKNOWLEDGED_PREFIX) &&
                title in it.message.removePrefix(ACKNOWLEDGED_PREFIX).split(", ")
        }
        if (ended != null && acknowledged != null) {
            result[alarm.id] = "${alarm.message}, ended ${formatTime(ended.timestampMs)}, acknowledged ${formatTime(acknowledged.timestampMs)}"
        }
    }
    return result
}

private fun shortTime(timeMs: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))

private const val ALARM_PREFIX = "Alarm: "
private const val ENDED_PREFIX = "Ended: "
private const val ACKNOWLEDGED_PREFIX = "Acknowledged: "

fun relayLinkEvent(isConnected: Boolean): LoggedEvent =
    if (isConnected) LoggedEvent(EventLevel.INFO, "Relay connected") else LoggedEvent(EventLevel.WARNING, "Relay connection lost")
