package com.jd.core.shared

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The lines of the Info screens (ISSUE-NOV10.8-O84 part 2), built here so the screens only draw them and the wording
 * and warning colors are unit-tested. A [InfoTone.WARN] or [InfoTone.BAD] line needs attention.
 */
enum class InfoTone { NORMAL, GOOD, WARN, BAD }

/** One label and value; [opensSatellites] makes the line open the satellite list. */
data class InfoLine(val label: String, val value: String, val tone: InfoTone = InfoTone.NORMAL, val opensSatellites: Boolean = false)

/** A titled group of lines; [isStale] draws it gray, as its values are old. */
data class InfoSection(val title: String, val lines: List<InfoLine>, val isStale: Boolean = false)

/**
 * What an Info screen shows: its sections, the satellites its satellite line opens, and a note above them.
 * [isSatelliteListPending] is true while satellites are in view but their list has not come yet: a Monitor asks for it
 * only while its satellite list is open (ISSUE-NOV15.5-O99 part 1).
 */
data class InfoScreenState(
    val sections: List<InfoSection> = emptyList(),
    val satellites: List<SatelliteReading> = emptyList(),
    val isSatelliteListPending: Boolean = false,
    val note: String? = null
)

/**
 * The Sensor's sections, with ages measured against its own [SensorInfo.capturedAtMs]; the Monitor puts [titlePrefix]
 * before each title, as it also shows its own phone.
 */
fun sensorInfoSections(info: SensorInfo, isStale: Boolean = false, titlePrefix: String = ""): List<InfoSection> {
    val now = info.capturedAtMs
    return listOf(
        InfoSection("${titlePrefix}SERVICE", serviceLines(info, now), isStale),
        InfoSection("${titlePrefix}CONNECTION", connectionLines(info, now), isStale),
        InfoSection("${titlePrefix}POSITION", positionLines(info, now), isStale),
        InfoSection("${titlePrefix}SENSORS", sensorLines(info), isStale),
        InfoSection("${titlePrefix}PHONE", phoneLines(info.phone) + listOf(InfoLine("Version", info.version), InfoLine("Sensor ID", info.sensorId)), isStale)
    )
}

private fun serviceLines(info: SensorInfo, now: Long) = listOf(
    if (info.serviceStartedAtMs > 0L) InfoLine("Running for", durationText(now - info.serviceStartedAtMs), InfoTone.GOOD)
    else InfoLine("Service", "Stopped", InfoTone.BAD),
    InfoLine("Motion", info.motion, if (info.isMotionArmed) InfoTone.GOOD else InfoTone.WARN),
    InfoLine("Microphone", info.microphone, if (info.isMicrophoneFaulty) InfoTone.WARN else InfoTone.NORMAL),
    InfoLine("Detection settings", "rev ${info.behaviourRevision}")
)

private fun connectionLines(info: SensorInfo, now: Long) = linkLines(info.link, now) + listOf(
    InfoLine("Monitor online", if (info.isMonitorOnline) "Yes" else "No", if (info.isMonitorOnline) InfoTone.GOOD else InfoTone.WARN),
    InfoLine("Last Monitor ID", info.lastMonitorId ?: NO_VALUE), // ISSUE-NOV22.7-O135
    InfoLine("Rows held offline", info.queuedRows.toString(), if (info.queuedRows > 0) InfoTone.WARN else InfoTone.NORMAL)
)

/** Clock time of [ms] in the phone's time zone, as `HH:mm` (with the date when not today). */
fun clockText(ms: Long, now: Long = System.currentTimeMillis()): String {
    val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val sameDay = day.format(Date(ms)) == day.format(Date(now))
    return SimpleDateFormat(if (sameDay) "HH:mm" else "yyyy-MM-dd HH:mm", Locale.US).format(Date(ms))
}

/** The relay link lines of both apps. */
fun linkLines(link: LinkStats, now: Long): List<InfoLine> = listOfNotNull(
    InfoLine("Relay", if (link.isConnected) "Connected" else "Disconnected", if (link.isConnected) InfoTone.GOOD else InfoTone.BAD),
    link.changedAtMs.takeIf { it > 0L }?.let {
        InfoLine(if (link.isConnected) "Connected for" else "Disconnected for", durationText(now - it))
    },
    InfoLine(
        "Dropouts",
        if (link.drops == 0) "None" else "${link.drops} (longest ${durationText(link.longestDropMs)}, total ${durationText(link.totalDropMs)})",
        if (link.drops == 0) InfoTone.NORMAL else InfoTone.WARN
    ),
    // The counts start with the app process (ISSUE-NOV15.5-O99 part 3).
    link.countingSinceMs.takeIf { it > 0L }?.let { InfoLine("Counting since", clockText(it, now)) },
    InfoLine("Latency", link.latencyMs?.let { "$it ms" } ?: NO_VALUE, if ((link.latencyMs ?: 0L) > SLOW_LATENCY_MS) InfoTone.WARN else InfoTone.NORMAL)
)

private fun positionLines(info: SensorInfo, now: Long): List<InfoLine> {
    val gnss = info.satelliteSummary()
    val satelliteLines = listOf(
        InfoLine("Satellites", "${gnss.used} used / ${gnss.inView} in view", if (gnss.used < MIN_GOOD_SATELLITES) InfoTone.WARN else InfoTone.NORMAL, opensSatellites = true),
        InfoLine("Signal (mean)", signalText(gnss.meanCn0DbHz), signalTone(gnss.meanCn0DbHz))
    )
    if (info.fixTimeMs <= 0L) return listOf(InfoLine("Fix", "None", InfoTone.BAD)) + satelliteLines
    val ageMs = now - info.fixTimeMs
    return listOf(
        InfoLine("Position", coordinatesText(info.lat, info.lng)),
        InfoLine("Accuracy", accuracyText(info.accuracyM), accuracyTone(info.accuracyM)),
        InfoLine("Speed", info.speedMps?.let { "%.1f km/h".format(Locale.US, it * MPS_TO_KMH) } ?: NO_VALUE),
        InfoLine("Fix age", durationText(ageMs), if (ageMs > OLD_FIX_MS) InfoTone.WARN else InfoTone.NORMAL),
        homeLine(info.homeDistanceM, info.homeRadiusM)
    ) + satelliteLines
}

private fun homeLine(distanceM: Double?, radiusM: Double?): InfoLine = when {
    radiusM == null -> InfoLine("Home", "No home points")
    distanceM == null -> InfoLine("Home", NO_VALUE)
    else -> {
        val isHome = distanceM <= radiusM
        InfoLine(
            "Home",
            "${if (isHome) "Inside" else "Outside"}: ${distanceText(distanceM)} from the nearest point (radius ${radiusM.roundToInt()} m)",
            if (isHome) InfoTone.GOOD else InfoTone.WARN
        )
    }
}

private fun sensorLines(info: SensorInfo) = listOf(
    InfoLine(
        "Vibration",
        info.vibrationG?.let { now ->
            "${gText(now)} (1 min peak ${info.vibrationPeakG?.let(::gText) ?: NO_VALUE}, limit ${info.vibrationLimitG?.let(::gText) ?: NO_VALUE})"
        } ?: "No accelerometer"
    ),
    InfoLine(
        "Tilt",
        info.tiltDeg?.let { "${it.roundToInt()}° (limit ${info.tiltLimitDeg?.roundToInt() ?: NO_VALUE}°)" } ?: "Not armed"
    ),
    InfoLine("Air pressure", info.pressureHpa?.let { "%.1f hPa".format(Locale.US, it) } ?: "No barometer"),
    InfoLine(
        "Noise",
        info.noiseDbfs?.let { "${it.roundToInt()} dBFS (background ${info.noiseFloorDbfs?.roundToInt() ?: NO_VALUE})" } ?: "Not listening"
    ),
    InfoLine(
        "Light",
        when {
            info.lux != null -> "${info.lux.roundToInt()} lx (background ${info.lightFloorLux?.roundToInt() ?: NO_VALUE} lx)"
            info.isLightSensorMissing -> "No light sensor"
            else -> "Not measuring"
        }
    )
)

/** The phone lines of both apps. */
fun phoneLines(phone: PhoneState): List<InfoLine> = listOf(
    batteryLine(phone.batteryPercent, phone.isCharging),
    temperatureLine(phone.temperatureC),
    InfoLine("Network", phone.network, if (phone.network == NO_NETWORK) InfoTone.BAD else InfoTone.NORMAL),
    InfoLine("Power saving", if (phone.isPowerSave) "On" else "Off", if (phone.isPowerSave) InfoTone.WARN else InfoTone.NORMAL),
    InfoLine("Standby bucket", standbyBucketName(phone.standbyBucket), if ((phone.standbyBucket ?: 0) >= RARE_BUCKET) InfoTone.WARN else InfoTone.NORMAL),
    InfoLine(
        "Battery optimization",
        if (phone.isBatteryExempt) "Not optimized (exempt)" else "Optimized (may be stopped)",
        if (phone.isBatteryExempt) InfoTone.GOOD else InfoTone.WARN
    ),
    InfoLine(
        "Free storage",
        phone.freeStorageMb?.let { if (it >= 1024) "%.1f GB".format(Locale.US, it / 1024.0) else "$it MB" } ?: NO_VALUE,
        if ((phone.freeStorageMb ?: Long.MAX_VALUE) < LOW_STORAGE_MB) InfoTone.WARN else InfoTone.NORMAL
    )
)

private fun batteryLine(percent: Int?, isCharging: Boolean) = InfoLine(
    "Battery",
    percent?.let { "$it %" + if (isCharging) ", charging" else "" } ?: NO_VALUE,
    when {
        percent == null -> InfoTone.NORMAL
        percent < CRITICAL_BATTERY_PERCENT -> InfoTone.BAD
        percent < LOW_BATTERY_PERCENT && !isCharging -> InfoTone.WARN
        else -> InfoTone.NORMAL
    }
)

private fun temperatureLine(celsius: Float?) = InfoLine(
    "Temperature",
    celsius?.let { "%.1f °C".format(Locale.US, it) } ?: NO_VALUE,
    if ((celsius ?: 0f) >= HOT_CELSIUS) InfoTone.WARN else InfoTone.NORMAL
)

/**
 * The Monitor's short section about its own phone (user decision 2026-11-15): the relay link, its own GPS, the
 * distance to the Sensor, battery and temperature. [fixAccuracyM] and [fixAgeMs] are null without a fix.
 */
fun monitorPhoneSection(
    link: LinkStats,
    fixAccuracyM: Float?,
    fixAgeMs: Long?,
    distanceToSensorM: Double?,
    phone: PhoneState,
    monitorId: String,
    version: String,
    nowMs: Long
) = InfoSection(
    "THIS PHONE (MONITOR)",
    linkLines(link, nowMs) + listOf(
        if (fixAccuracyM == null || fixAgeMs == null) InfoLine("GPS", "No fix", InfoTone.WARN)
        else InfoLine("GPS", "${accuracyText(fixAccuracyM)}, ${durationText(fixAgeMs)} old", accuracyTone(fixAccuracyM)),
        InfoLine("Distance to Sensor", distanceToSensorM?.let(::distanceText) ?: NO_VALUE),
        batteryLine(phone.batteryPercent, phone.isCharging),
        temperatureLine(phone.temperatureC),
        InfoLine("Network", phone.network, if (phone.network == NO_NETWORK) InfoTone.BAD else InfoTone.NORMAL),
        InfoLine("Version", version),
        InfoLine("Monitor ID", monitorId)
    )
)

/**
 * The Monitor's section from the Sensor's newest row, shown while no Info has come (the Sensor answers only while it is
 * online). Its age uses the Monitor's clock against the Sensor's row time.
 */
fun newestRowSection(row: LocationUpdate, nowMs: Long, isStale: Boolean): InfoSection {
    val hasFix = row.lat != 0.0 || row.lng != 0.0
    return InfoSection(
        "SENSOR'S NEWEST ROW",
        listOfNotNull(
            InfoLine("Sent", "${durationText(nowMs - row.timestamp)} ago", if (isStale) InfoTone.WARN else InfoTone.NORMAL),
            InfoLine("Status", row.status),
            if (hasFix) InfoLine("Position", coordinatesText(row.lat, row.lng)) else InfoLine("Fix", "None", InfoTone.BAD),
            if (hasFix) InfoLine("Accuracy", accuracyText(row.accuracy), accuracyTone(row.accuracy)) else null,
            row.satsUsed.takeIf { it >= 0 }?.let { InfoLine("Satellites", "$it used / ${row.satsInView} in view") },
            InfoLine("Signal (mean)", signalText(row.meanCn0DbHz), signalTone(row.meanCn0DbHz)),
            InfoLine("Noise", row.noiseDbfs?.let { "${it.roundToInt()} dBFS" } ?: NO_VALUE),
            batteryLine(row.batteryLevel.takeIf { it >= 0 }, row.isCharging),
            temperatureLine(row.tempCelsius.takeIf { it > 0f })
        ),
        isStale
    )
}

/** For example `45 s`, `12 min 5 s`, `3 h 07 min`, `2 d 4 h`; a negative length (clock change) counts as 0. */
fun durationText(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000L
    return when {
        s < 60 -> "$s s"
        s < 3600 -> "${s / 60} min ${s % 60} s"
        s < 86_400 -> "${s / 3600} h %02d min".format(Locale.US, (s % 3600) / 60)
        else -> "${s / 86_400} d ${(s % 86_400) / 3600} h"
    }
}

fun distanceText(meters: Double): String =
    if (meters >= 1000) "%.2f km".format(Locale.US, meters / 1000) else "${meters.roundToInt()} m"

fun standbyBucketName(bucket: Int?): String = when (bucket) {
    null -> NO_VALUE
    5 -> "Exempted"
    10 -> "Active"
    20 -> "Working set"
    30 -> "Frequent"
    40 -> "Rare"
    45 -> "Restricted"
    else -> "Bucket $bucket"
}

private fun coordinatesText(lat: Double, lng: Double) = "%.6f, %.6f".format(Locale.US, lat, lng)
private fun accuracyText(meters: Float) = "± ${meters.roundToInt()} m"
private fun accuracyTone(meters: Float) = if (meters > POOR_ACCURACY_M) InfoTone.WARN else InfoTone.NORMAL
private fun signalText(cn0: Float?) = cn0?.let { "%.1f dB-Hz".format(Locale.US, it) } ?: NO_VALUE
private fun signalTone(cn0: Float?) = if (cn0 != null && cn0 < WEAK_SIGNAL_DBHZ) InfoTone.WARN else InfoTone.NORMAL
private fun gText(g: Float) = "%.2f g".format(Locale.US, g)

private const val NO_VALUE = "--"
/** The network name `readPhoneState` gives when there is none. */
const val NO_NETWORK = "None"
private const val MPS_TO_KMH = 3.6f
private const val SLOW_LATENCY_MS = 1_000L
private const val MIN_GOOD_SATELLITES = 4
private const val OLD_FIX_MS = 35_000L
private const val POOR_ACCURACY_M = 50f
private const val WEAK_SIGNAL_DBHZ = 25f
private const val RARE_BUCKET = 40
private const val LOW_STORAGE_MB = 500L
private const val LOW_BATTERY_PERCENT = 20
private const val CRITICAL_BATTERY_PERCENT = 10
private const val HOT_CELSIUS = 45f
