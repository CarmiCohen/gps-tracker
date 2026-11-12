package com.jd.app.monitor

import com.jd.core.network.SensorSilence
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

/**
 * Before the relay has been connected once, it counts as lost only this long after watching starts: longer than the
 * 20 s socket timeout and a relay cold start (ISSUE-NOV5.1-O9), so the first connection never sounds the siren (ISSUE-NOV7.17-O42).
 */
const val RELAY_FIRST_CONNECT_GRACE_MS = 30_000L

/** The tracker's GPS counts as lost once it has reported no position this long, so the time to a first fix does not sound the siren. */
const val GPS_SIGNAL_LOSS_AFTER_MS = 30_000L

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
    HIGH_TEMPERATURE("Tracker temperature high"),
    RELAY_DISCONNECTED("Relay disconnected"),
    LOCAL_INTERNET_OFFLINE("Monitor internet lost"),
    GPS_SIGNAL_LOSS("Tracker GPS signal loss"),
    GPS_JUMP("Tracker GPS jump"),
    GEOFENCE_BREACH("Geofence boundary breach"),
    ACOUSTIC_ALERT("Tracker noise detected"),
    TILT_ALERT("Tracker tilted"),
    LIFT_ALERT("Tracker lifted"),
    VIBRATION_ALERT("Tracker vibration detected"),
    NOISE_DETECTION_OFF("Tracker noise detection off")
}

fun AlertSettings.allows(type: AlarmType): Boolean = !globalMute && when (type) {
    AlarmType.TRACKER_OFFLINE -> serverConnection // "Peer Connection Offline"
    AlarmType.POWER_DISCONNECTED -> power
    AlarmType.LOW_BATTERY -> lowBattery
    AlarmType.HIGH_TEMPERATURE -> highTemperature
    AlarmType.RELAY_DISCONNECTED -> relayConnection
    AlarmType.LOCAL_INTERNET_OFFLINE -> localInternet
    AlarmType.GPS_SIGNAL_LOSS -> signalLoss
    AlarmType.GPS_JUMP -> jammerDetection
    AlarmType.GEOFENCE_BREACH -> distance
    AlarmType.ACOUSTIC_ALERT -> acousticAlert // "Acoustic / Microphone Noise Sensor"; the Sensor listens either way.
    AlarmType.TILT_ALERT -> tiltAlert // "Tilt Angle Detection Sensor"; the angle is set on the Sensor.
    AlarmType.LIFT_ALERT -> liftAlert // "Lift / Height Change Sensor"
    AlarmType.VIBRATION_ALERT -> tamperAlert // "Tamper / Vibration Sensor"; the limit is set on the Sensor.
    AlarmType.NOISE_DETECTION_OFF -> acousticAlert // A noise alarm the Sensor cannot raise (ISSUE-NOV8.103-O60).
}

/** The newest telemetry from the paired tracker. [receivedAtMs] is Monitor time, so the clocks of the two phones never meet. */
data class TrackerReading(
    val receivedAtMs: Long,
    val sensorTimeMs: Long,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val tempCelsius: Float,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracy: Float = 0f,
    val status: String = "VALID",
    val isNoiseDetectionOff: Boolean = false
)

fun LocationUpdate.toTrackerReading(receivedAtMs: Long) =
    TrackerReading(
        receivedAtMs = receivedAtMs,
        sensorTimeMs = timestamp,
        batteryLevel = batteryLevel,
        isCharging = isCharging,
        tempCelsius = tempCelsius,
        lat = lat,
        lng = lng,
        accuracy = accuracy,
        status = status,
        isNoiseDetectionOff = isNoiseDetectionOff
    )

fun TrackerReading.hasGpsSignal(): Boolean =
    (lat != 0.0 || lng != 0.0) && accuracy > 0f && status != "NO_FIX" && status != "GPS_SIGNAL_LOSS"

/** Keeps [current] unless [incoming] is at least as new on the tracker's clock, so a late queued update cannot undo a newer one. */
fun newerReading(current: TrackerReading?, incoming: TrackerReading): TrackerReading =
    if (current == null || incoming.sensorTimeMs >= current.sensorTimeMs) incoming else current

/**
 * The alarm conditions that hold at [nowMs]. With nothing heard yet, offline counts from [watchingSinceMs].
 * [active] holds the conditions of the previous evaluation, which decide the clearing margins.
 * [hasRelayConnected] tells whether the relay has been connected since [watchingSinceMs]; until then a relay that is
 * not connected counts as lost only after [RELAY_FIRST_CONNECT_GRACE_MS].
 * [noGpsSignalSinceMs] is when the tracker started to report no position, or null while it reports one.
 * [isReportedSilent] is true while the relay's notice that the tracker went silent holds (ISSUE-NOV10.8-O86).
 */
fun alarmConditions(
    reading: TrackerReading?,
    watchingSinceMs: Long,
    nowMs: Long,
    active: Set<AlarmType>,
    isRelayConnected: Boolean = true,
    hasRelayConnected: Boolean = true,
    isLocalInternet: Boolean = true,
    hasViolation: Boolean = false,
    violationType: String? = null,
    noGpsSignalSinceMs: Long? = null,
    isReportedSilent: Boolean = false
): Set<AlarmType> = buildSet {
    if (isReportedSilent || nowMs - (reading?.receivedAtMs ?: watchingSinceMs) >= TRACKER_PRESENT_MS) {
        add(AlarmType.TRACKER_OFFLINE)
    }
    if (!isRelayConnected && (hasRelayConnected || nowMs - watchingSinceMs >= RELAY_FIRST_CONNECT_GRACE_MS)) {
        add(AlarmType.RELAY_DISCONNECTED)
    }
    if (!isLocalInternet) add(AlarmType.LOCAL_INTERNET_OFFLINE)
    if (reading != null && nowMs - reading.receivedAtMs < TRACKER_PRESENT_MS && !reading.hasGpsSignal() &&
        noGpsSignalSinceMs != null && nowMs - noGpsSignalSinceMs >= GPS_SIGNAL_LOSS_AFTER_MS
    ) {
        add(AlarmType.GPS_SIGNAL_LOSS)
    }
    // The Sensor sends each violation twice: as a violation event and as the status of its telemetry row.
    val flagged = setOfNotNull(violationType, reading?.status)
    flagged.mapNotNullTo(this) { SENSOR_VIOLATION_ALARMS[it] }
    val isOtherViolation = hasViolation && violationType !in SENSOR_VIOLATION_ALARMS
    if ("JUMP" in flagged || "OUTLIER" in flagged || isOtherViolation) add(AlarmType.GPS_JUMP)
    if (reading == null) return@buildSet
    if (!reading.isCharging) add(AlarmType.POWER_DISCONNECTED)
    if (reading.isNoiseDetectionOff) add(AlarmType.NOISE_DETECTION_OFF)
    val batteryLimit = if (AlarmType.LOW_BATTERY in active) LOW_BATTERY_CLEARS_AT else LOW_BATTERY_BELOW
    if (reading.batteryLevel >= 0 && reading.batteryLevel < batteryLimit) add(AlarmType.LOW_BATTERY)
    val temperatureLimit = if (AlarmType.HIGH_TEMPERATURE in active) HIGH_TEMPERATURE_CLEARS_AT else HIGH_TEMPERATURE_ABOVE
    if (reading.tempCelsius > 0f && reading.tempCelsius > temperatureLimit) add(AlarmType.HIGH_TEMPERATURE)
}

/** Violation types with an alarm of their own; any other violation is a GPS jump. */
private val SENSOR_VIOLATION_ALARMS = mapOf(
    com.jd.core.shared.GEOFENCE_BREACH_STATUS to AlarmType.GEOFENCE_BREACH,
    com.jd.core.shared.ACOUSTIC_ALERT_STATUS to AlarmType.ACOUSTIC_ALERT,
    com.jd.core.shared.TILT_ALERT_STATUS to AlarmType.TILT_ALERT,
    com.jd.core.shared.LIFT_ALERT_STATUS to AlarmType.LIFT_ALERT,
    com.jd.core.shared.VIBRATION_ALERT_STATUS to AlarmType.VIBRATION_ALERT
)

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
 *
 * Each of [silences], the relay's notice that the tracker went silent, holds the tracker offline until a row newer
 * than the notice's last row arrives, on the tracker's clock (ISSUE-NOV10.8-O86). So the relay's replay of that last
 * row on joining does not end it, and a notice about rows this Monitor has already outrun is ignored.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun alarmBooks(
    target: Flow<String?>,
    telemetry: Flow<LocationUpdate>,
    violations: Flow<com.jd.core.shared.ViolationEvent> = flowOf(),
    settings: Flow<AlertSettings>,
    ticks: Flow<Unit>,
    acknowledgements: Flow<Unit>,
    isRelayConnected: Flow<Boolean> = flowOf(true),
    isLocalInternet: Flow<Boolean> = flowOf(true),
    silences: Flow<SensorSilence> = flowOf(),
    clock: () -> Long
): Flow<AlarmBook> = target.flatMapLatest { trackerId ->
    if (trackerId == null) return@flatMapLatest flowOf(AlarmBook())
    val watchingSinceMs = clock()
    merge(
        telemetry.filter { it.deviceId == trackerId }.map { BookEvent.Reading(it.toTrackerReading(clock())) },
        violations.filter { it.location.deviceId == trackerId || it.location.deviceId.isEmpty() }.map { BookEvent.Violation(it) },
        silences.filter { it.trackerId == trackerId }.map { BookEvent.Silence(it) },
        settings.map { BookEvent.Settings(it) },
        ticks.map { BookEvent.Tick },
        acknowledgements.map { BookEvent.Acknowledge },
        isRelayConnected.map { BookEvent.RelayConnection(it) },
        isLocalInternet.map { BookEvent.LocalInternet(it) }
    ).scan(WatchState()) { state, event ->
        val next = when (event) {
            is BookEvent.Reading -> {
                val reading = newerReading(state.reading, event.reading)
                state.copy(
                    reading = reading,
                    hasViolation = false,
                    violationType = null,
                    noGpsSignalSinceMs = if (reading.hasGpsSignal()) null else state.noGpsSignalSinceMs ?: reading.receivedAtMs,
                    silentAfterSensorMs = state.silentAfterSensorMs?.takeIf { reading.sensorTimeMs <= it }
                )
            }
            is BookEvent.Violation -> state.copy(hasViolation = true, violationType = event.violation.type)
            is BookEvent.Silence -> {
                val lastHeardMs = event.silence.lastSensorTimeMs
                if (state.reading != null && state.reading.sensorTimeMs > lastHeardMs) state
                else state.copy(silentAfterSensorMs = lastHeardMs)
            }
            is BookEvent.Settings -> state.copy(settings = event.settings)
            BookEvent.Tick -> state
            BookEvent.Acknowledge -> state.copy(book = state.book.acknowledge(), hasViolation = false, violationType = null)
            is BookEvent.RelayConnection -> state.copy(
                isRelayConnected = event.isConnected,
                hasRelayConnected = state.hasRelayConnected || event.isConnected
            )
            is BookEvent.LocalInternet -> state.copy(isLocalInternet = event.isConnected)
        }
        val alertSettings = next.settings ?: return@scan next
        val nowMs = clock()
        next.copy(
            book = next.book.update(
                alarmConditions(
                    next.reading,
                    watchingSinceMs,
                    nowMs,
                    next.book.conditions,
                    isRelayConnected = next.isRelayConnected,
                    hasRelayConnected = next.hasRelayConnected,
                    isLocalInternet = next.isLocalInternet,
                    hasViolation = next.hasViolation,
                    violationType = next.violationType,
                    noGpsSignalSinceMs = next.noGpsSignalSinceMs,
                    isReportedSilent = next.silentAfterSensorMs != null
                ),
                alertSettings,
                nowMs
            )
        )
    }.map { it.book }
}.distinctUntilChanged()

private data class WatchState(
    val reading: TrackerReading? = null,
    val settings: AlertSettings? = null,
    val book: AlarmBook = AlarmBook(),
    val isRelayConnected: Boolean = true,
    val hasRelayConnected: Boolean = false,
    val isLocalInternet: Boolean = true,
    val hasViolation: Boolean = false,
    val violationType: String? = null,
    val noGpsSignalSinceMs: Long? = null,
    /** While the relay's silence notice holds: the tracker time of the last row it had; a newer row ends it. */
    val silentAfterSensorMs: Long? = null
)

private sealed interface BookEvent {
    data class Reading(val reading: TrackerReading) : BookEvent
    data class Violation(val violation: com.jd.core.shared.ViolationEvent) : BookEvent
    data class Silence(val silence: SensorSilence) : BookEvent
    data class Settings(val settings: AlertSettings) : BookEvent
    data object Tick : BookEvent
    data object Acknowledge : BookEvent
    data class RelayConnection(val isConnected: Boolean) : BookEvent
    data class LocalInternet(val isConnected: Boolean) : BookEvent
}

