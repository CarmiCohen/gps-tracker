package com.jd.core.shared

import kotlinx.serialization.Serializable

/**
 * The detailed Info of the menu's Info screen (ISSUE-NOV10.8-O84 part 2, user decisions 2026-11-15): what each phone
 * knows about itself. The Sensor builds a [SensorInfo] every second for its own screen, and sends one every
 * [SENSOR_INFO_SEND_INTERVAL_MS] while a Monitor's Info screen asks for it ([SENSOR_INFO_REQUEST_INTERVAL_MS]); the
 * satellite list goes with it only while a Monitor's satellite list is open ([SENSOR_INFO_SATELLITE_WINDOW_MS],
 * ISSUE-NOV15.5-O99 part 1).
 */

/** One satellite as the phone's GNSS chip reports it. [constellation] is an Android `GnssStatus` constellation type. */
@Serializable
data class SatelliteReading(val svid: Int, val constellation: Int, val cn0DbHz: Float, val usedInFix: Boolean)

/** The satellite counts and the mean signal of the satellites used in the fix, null while none is used. */
data class GnssSummary(val used: Int, val inView: Int, val meanCn0DbHz: Float?)

fun gnssSummary(satellites: List<SatelliteReading>): GnssSummary {
    val used = satellites.filter { it.usedInFix }
    return GnssSummary(
        used = used.size,
        inView = satellites.size,
        meanCn0DbHz = if (used.isEmpty()) null else used.map { it.cn0DbHz }.average().toFloat()
    )
}

/** The names of Android's `GnssStatus.CONSTELLATION_*` values. */
fun constellationName(type: Int): String = when (type) {
    1 -> "GPS"
    2 -> "SBAS"
    3 -> "GLONASS"
    4 -> "QZSS"
    5 -> "BeiDou"
    6 -> "Galileo"
    7 -> "NavIC"
    else -> "Other"
}

/**
 * The relay link since the app started: [changedAtMs] is the wall-clock time of the last connect or disconnect (0
 * before the first connect), a drop is a disconnection after a connect, and its length counts once the link is back.
 * [latencyMs] is the newest round trip to the relay and back, null while unknown or disconnected.
 * [countingSinceMs] is when the counting started, the app process's start; Info shows it, so counts that a restart
 * set back to zero are marked as such (ISSUE-NOV15.5-O99 part 3, user decision 2026-11-17). 0 from Sensors before Nov17.3.
 */
@Serializable
data class LinkStats(
    val isConnected: Boolean = false,
    val changedAtMs: Long = 0L,
    val drops: Int = 0,
    val longestDropMs: Long = 0L,
    val totalDropMs: Long = 0L,
    val latencyMs: Long? = null,
    val countingSinceMs: Long = 0L
) {
    /** These statistics after the link became [connected] at [nowMs]; a repeated state changes nothing. */
    fun afterLink(connected: Boolean, nowMs: Long): LinkStats = when {
        connected == isConnected -> this
        connected -> {
            val dropMs = if (changedAtMs > 0L) (nowMs - changedAtMs).coerceAtLeast(0L) else 0L
            copy(
                isConnected = true,
                changedAtMs = nowMs,
                longestDropMs = maxOf(longestDropMs, dropMs),
                totalDropMs = totalDropMs + dropMs
            )
        }
        else -> copy(isConnected = false, changedAtMs = nowMs, drops = drops + 1, latencyMs = null)
    }
}

/** The phone's own state, read by `readPhoneState` in core-ui. Null values were not reported by the system. */
@Serializable
data class PhoneState(
    val batteryPercent: Int? = null,
    val isCharging: Boolean = false,
    val temperatureC: Float? = null,
    val network: String = "None",
    val isPowerSave: Boolean = false,
    /** Android's app standby bucket (10 active ... 45 restricted), null before Android 9. */
    val standbyBucket: Int? = null,
    val isBatteryExempt: Boolean = false,
    val freeStorageMb: Long? = null
)

/**
 * Everything the Sensor tells about itself on the Info screens. Times are the Sensor's wall clock; ages inside are
 * measured against [capturedAtMs], so a Monitor whose clock differs still shows them right.
 */
@Serializable
data class SensorInfo(
    val capturedAtMs: Long = 0L,
    val sensorId: String = "",
    val version: String = "",
    // Service
    val serviceStartedAtMs: Long = 0L,
    val motion: String = "",
    val isMotionArmed: Boolean = false,
    val microphone: String = "",
    /** True when noise detection should run but cannot (not when it is switched off on the Monitor). */
    val isMicrophoneFaulty: Boolean = false,
    val behaviourRevision: Long = 0L,
    // Connection
    val link: LinkStats = LinkStats(),
    val isMonitorOnline: Boolean = false,
    val queuedRows: Int = 0,
    // Position: no fix while [fixTimeMs] is 0
    val fixTimeMs: Long = 0L,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracyM: Float = 0f,
    val speedMps: Float? = null,
    /** Distance to the nearest home point, null without home points or a fix. */
    val homeDistanceM: Double? = null,
    val homeRadiusM: Double? = null,
    /** The satellites in view; sent to a Monitor only while its satellite list is open (ISSUE-NOV15.5-O99 part 1). */
    val satellites: List<SatelliteReading> = emptyList(),
    /** The summary of [satellites], always sent, so the Monitor shows it without the list; read by [satelliteSummary]. */
    val satellitesUsed: Int = 0,
    val satellitesInView: Int = 0,
    val meanCn0DbHz: Float? = null,
    // Sensors: null when the phone lacks the sensor or it is off
    val vibrationG: Float? = null,
    val vibrationPeakG: Float? = null,
    val vibrationLimitG: Float? = null,
    /** The tilt from the resting position, null while tilt detection is not armed. */
    val tiltDeg: Float? = null,
    val tiltLimitDeg: Float? = null,
    val pressureHpa: Float? = null,
    val noiseDbfs: Float? = null,
    val noiseFloorDbfs: Float? = null,
    /** The light and its learned background, null while light detection is off or the phone has no light sensor. */
    val lux: Float? = null,
    val lightFloorLux: Float? = null,
    /** The phone has no light sensor (ISSUE-NOV22.4-O130). */
    val isLightSensorMissing: Boolean = false,
    // Phone
    val phone: PhoneState = PhoneState()
)

/** This Info with its satellite list left out, keeping the list's summary (ISSUE-NOV15.5-O99 part 1). */
fun SensorInfo.withoutSatelliteList(): SensorInfo = copy(satellites = emptyList())

/**
 * The satellite summary of this Info: from the list when it came, else from the summary fields; an Info from a Sensor
 * older than Nov16.4 has only the list.
 */
fun SensorInfo.satelliteSummary(): GnssSummary =
    if (satellites.isNotEmpty()) gnssSummary(satellites) else GnssSummary(satellitesUsed, satellitesInView, meanCn0DbHz)

/** A [SensorInfo] with the receiving phone's own clock time, so its age needs no clock agreement. */
data class ReceivedSensorInfo(val info: SensorInfo, val receivedAtMs: Long)

/** A Monitor's Info screen asks the Sensor for its Info this often while it is open. */
const val SENSOR_INFO_REQUEST_INTERVAL_MS = 10_000L

/** The Sensor sends its Info this often after a request ... */
const val SENSOR_INFO_SEND_INTERVAL_MS = 5_000L

/** ... for this long, so a closed screen costs at most this much more data. */
const val SENSOR_INFO_ANSWER_WINDOW_MS = 30_000L

/**
 * After a request that asks for the satellite list, the Sensor adds the list for this long: longer than
 * [SENSOR_INFO_REQUEST_INTERVAL_MS], so an open list never misses it, and short, so a closed list soon stops costing
 * data (about 4 KB per Info with 55 satellites in view, ISSUE-NOV15.5-O99 part 1).
 */
const val SENSOR_INFO_SATELLITE_WINDOW_MS = 15_000L

/** A Monitor shows the Sensor's Info as old (gray) when none arrived for this long. */
const val SENSOR_INFO_STALE_MS = 20_000L

/** The relay latency is measured this often while connected. */
const val LATENCY_PING_INTERVAL_MS = 30_000L
