package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.LocationUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlin.time.Duration.Companion.seconds

/** The tracker counts as present while its newest update is younger than this. The TRK LED uses the same limit. */
const val TRACKER_PRESENT_MS = 35_000L

const val LOW_BATTERY_BELOW = 20
const val HIGH_TEMPERATURE_ABOVE = 46f

// A raised alarm needs a clear margin to end, so a value at the limit does not raise it again and again.
private const val LOW_BATTERY_CLEARS_AT = 22
private const val HIGH_TEMPERATURE_CLEARS_AT = 44f

/** The Monitor-side alarms, each switched by the Alert Settings entry named in [AlertSettings.allows]. */
enum class AlarmType(val title: String) {
    TRACKER_OFFLINE("Tracker offline"),
    POWER_DISCONNECTED("Tracker power disconnected"),
    LOW_BATTERY("Tracker battery low"),
    HIGH_TEMPERATURE("Tracker temperature high")
}

fun AlertSettings.allows(type: AlarmType): Boolean = !globalMute && when (type) {
    AlarmType.TRACKER_OFFLINE -> serverConnection // "Peer Connection Offline"
    AlarmType.POWER_DISCONNECTED -> power
    AlarmType.LOW_BATTERY -> lowBattery
    AlarmType.HIGH_TEMPERATURE -> highTemperature
}

/** The newest telemetry from the paired tracker. [receivedAtMs] is Monitor time, so the clocks of the two phones never meet. */
data class TrackerReading(
    val receivedAtMs: Long,
    val sensorTimeMs: Long,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val tempCelsius: Float
)

fun LocationUpdate.toTrackerReading(receivedAtMs: Long) =
    TrackerReading(receivedAtMs, timestamp, batteryLevel, isCharging, tempCelsius)

/** Keeps [current] unless [incoming] is at least as new on the tracker's clock, so a late queued update cannot undo a newer one. */
fun newerReading(current: TrackerReading?, incoming: TrackerReading): TrackerReading =
    if (current == null || incoming.sensorTimeMs >= current.sensorTimeMs) incoming else current

/**
 * The alarm conditions that hold at [nowMs]. With nothing heard yet, offline counts from [watchingSinceMs].
 * [active] holds the conditions of the previous evaluation, which decide the clearing margins.
 */
fun alarmConditions(
    reading: TrackerReading?,
    watchingSinceMs: Long,
    nowMs: Long,
    active: Set<AlarmType>
): Set<AlarmType> = buildSet {
    if (nowMs - (reading?.receivedAtMs ?: watchingSinceMs) >= TRACKER_PRESENT_MS) add(AlarmType.TRACKER_OFFLINE)
    if (reading == null) return@buildSet
    if (!reading.isCharging) add(AlarmType.POWER_DISCONNECTED)
    val batteryLimit = if (AlarmType.LOW_BATTERY in active) LOW_BATTERY_CLEARS_AT else LOW_BATTERY_BELOW
    if (reading.batteryLevel < batteryLimit) add(AlarmType.LOW_BATTERY)
    val temperatureLimit = if (AlarmType.HIGH_TEMPERATURE in active) HIGH_TEMPERATURE_CLEARS_AT else HIGH_TEMPERATURE_ABOVE
    if (reading.tempCelsius > temperatureLimit) add(AlarmType.HIGH_TEMPERATURE)
}

/** One raised alarm. [isActive] is false once its condition has cleared. */
data class Alarm(
    val type: AlarmType,
    val raisedAtMs: Long,
    val isActive: Boolean,
    val isAcknowledged: Boolean
)

/**
 * Every raised alarm. An alarm is raised when its condition starts to hold, and stays until it is acknowledged,
 * even if its condition clears first. An acknowledged alarm is raised again only after its condition has cleared.
 * A switched-off or muted alarm is dropped at once.
 */
data class AlarmBook(val alarms: List<Alarm> = emptyList()) {
    val conditions: Set<AlarmType> get() = alarms.filter { it.isActive }.mapTo(mutableSetOf()) { it.type }
    val pending: List<Alarm> get() = alarms.filter { !it.isAcknowledged }
    val needsSiren: Boolean get() = pending.isNotEmpty()

    fun update(holding: Set<AlarmType>, settings: AlertSettings, nowMs: Long): AlarmBook {
        val allowed = holding.filter(settings::allows).toSet()
        val kept = alarms.filter { settings.allows(it.type) }.mapNotNull { alarm ->
            when {
                alarm.type in allowed -> alarm.copy(isActive = true)
                alarm.isAcknowledged -> null
                else -> alarm.copy(isActive = false)
            }
        }
        val raised = (allowed - kept.mapTo(mutableSetOf()) { it.type })
            .sortedBy { it.ordinal }
            .map { Alarm(it, nowMs, isActive = true, isAcknowledged = false) }
        return AlarmBook(kept + raised)
    }

    fun acknowledge(): AlarmBook = AlarmBook(alarms.filter { it.isActive }.map { it.copy(isAcknowledged = true) })
}

/** Emits once a second, so a tracker that goes silent is noticed without new telemetry. */
fun secondTicks(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(1.seconds)
    }
}

/**
 * The alarm book of the paired tracker, re-evaluated at [clock] time on every reading, settings change, and tick.
 * A new pairing starts an empty book, and nothing is evaluated before the saved settings are known, so a
 * switched-off alarm cannot sound at start. Each [acknowledgements] event acknowledges the book as it stands.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun alarmBooks(
    target: Flow<String?>,
    telemetry: Flow<LocationUpdate>,
    settings: Flow<AlertSettings>,
    ticks: Flow<Unit>,
    acknowledgements: Flow<Unit>,
    clock: () -> Long
): Flow<AlarmBook> = target.flatMapLatest { trackerId ->
    if (trackerId == null) return@flatMapLatest flowOf(AlarmBook())
    val watchingSinceMs = clock()
    merge(
        telemetry.filter { it.deviceId == trackerId }.map { BookEvent.Reading(it.toTrackerReading(clock())) },
        settings.map { BookEvent.Settings(it) },
        ticks.map { BookEvent.Tick },
        acknowledgements.map { BookEvent.Acknowledge }
    ).scan(WatchState()) { state, event ->
        val next = when (event) {
            is BookEvent.Reading -> state.copy(reading = newerReading(state.reading, event.reading))
            is BookEvent.Settings -> state.copy(settings = event.settings)
            BookEvent.Tick -> state
            BookEvent.Acknowledge -> state.copy(book = state.book.acknowledge())
        }
        val alertSettings = next.settings ?: return@scan next
        val nowMs = clock()
        next.copy(
            book = next.book.update(
                alarmConditions(next.reading, watchingSinceMs, nowMs, next.book.conditions),
                alertSettings,
                nowMs
            )
        )
    }.map { it.book }
}.distinctUntilChanged()

private data class WatchState(
    val reading: TrackerReading? = null,
    val settings: AlertSettings? = null,
    val book: AlarmBook = AlarmBook()
)

private sealed interface BookEvent {
    data class Reading(val reading: TrackerReading) : BookEvent
    data class Settings(val settings: AlertSettings) : BookEvent
    data object Tick : BookEvent
    data object Acknowledge : BookEvent
}
