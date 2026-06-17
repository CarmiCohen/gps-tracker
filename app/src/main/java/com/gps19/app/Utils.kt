package com.gps19.app

import android.app.AppOpsManager
import android.content.Context
import android.location.GnssStatus
import android.os.Build
import com.gps19.core.engine.*
import org.osmdroid.util.GeoPoint
import java.util.Locale
import kotlin.math.*

/**
 * Utils: Project-wide helper functions.
 * v8.7.0:
 * - Modularization: Delegated core math to :core:engine.
 */

enum class XiaomiPermissionStatus {
    GRANTED,
    DENIED,
    UNKNOWN // Reflection failed or non-Xiaomi device
}

/**
 * Calculates the distance between two points in meters using the Haversine formula.
 * Delegated to PhysicsUtils in :core:engine for consistency.
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    return PhysicsUtils.calculateDistance(lat1, lon1, lat2, lon2)
}

/**
 * Checks if a location is valid (not (0,0), not NaN, and within global bounds).
 * Delegated to PhysicsUtils in :core:engine.
 */
fun isValidLocation(lat: Double, lng: Double): Boolean {
    return PhysicsUtils.isValidLocation(lat, lng)
}

/**
 * Checks if a location is the default coordinate.
 */
fun isDefaultLocation(lat: Double, lng: Double): Boolean {
    return abs(lat - DEFAULT_LAT) < 0.0001 && abs(lng - DEFAULT_LNG) < 0.0001
}

/**
 * Checks if a GeoPoint is valid.
 */
fun isValidLocation(p: GeoPoint?): Boolean {
    return p != null && isValidLocation(p.latitude, p.longitude)
}

/**
 * Unified duration formatting with day support.
 */
fun formatDurationUnified(ms: Long): String {
    if (ms <= 0) return "00:00:00"
    val totalSeconds = ms / 1000
    val s = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val m = totalMinutes % 60
    val totalHours = totalMinutes / 60
    
    if (totalHours >= 24) {
        val days = totalHours / 24
        val hours = totalHours % 24
        return "${days}d ${hours}h ${m}m"
    }
    
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", totalHours, m, s)
}

/**
 * Concise duration formatting for logs (R845).
 */
fun formatDurationSimple(ms: Long): String {
    val totalSec = ms / 1000
    if (totalSec <= 0L) return "0s"
    if (totalSec < 60) return "${totalSec}s"
    val m = totalSec / 60
    val s = totalSec % 60
    if (m < 60) {
        return if (s == 0L) "${m}m" else "${m}m ${s}s"
    }
    val h = m / 60
    val mm = m % 60
    return if (mm == 0L) "${h}h" else "${h}h ${mm}m"
}

/**
 * Formats duration in "Xh YYm" or "Xd Yh ZZm" format for heartbeat logs.
 */
fun formatDurationHoursMinutes(ms: Long): String {
    val totalMinutes = ms / 60000
    val m = totalMinutes % 60
    val totalHours = totalMinutes / 60
    val h = totalHours % 24
    val d = totalHours / 24

    return if (d > 0) {
        String.format(Locale.getDefault(), "%dd %02dh %02dm", d, h, m)
    } else {
        String.format(Locale.getDefault(), "%dh %02dm", h, m)
    }
}

/**
 * Cleans a log message for display.
 */
fun cleanLogDisplayMessage(message: String): String {
    var m = message
    // R860: Strip leading tags in brackets for cleaner UI
    m = m.replace(Regex("^\\[[^\\]]+\\]\\s*"), "")
    m = m.replace(Regex("\\s*\\((Sustained|Interruption|Duration):[^)]+\\)", RegexOption.IGNORE_CASE), "")
    m = m.replace(Regex("\\s*after an interruption of[^.]+", RegexOption.IGNORE_CASE), "")
    m = m.replace(Regex("(?i)\\b(INFRA|SYSTEM|ALARM|EVENT):?\\s*"), "")
    return m.trim()
}

/**
 * Calculates a communication quality index (0-10) based on RTT and signal levels.
 */
fun calculateCommIndex(rtt: Int, remoteSig: Int, localSig: Int): Int {
    return TelemetryUtils.calculateCommIndex(rtt, remoteSig, localSig)
}

/**
 * Calculates the GPS-Index.
 */
fun calculateGpsIndex(gpsAgeMs: Long, maxAccuracy: Float, satsUsed: Int): Float {
    return TelemetryUtils.calculateGpsIndex(gpsAgeMs, maxAccuracy, satsUsed).totalIndex
}

fun smoothCoordinate(last: Double, current: Double, alpha: Double = 0.3): Double {
    if (last == 0.0) return current
    return last + alpha * (current - last)
}

fun smoothBearing(last: Float, current: Float, alpha: Float = 0.2f): Float {
    var delta = current - last
    while (delta < -180) delta += 360
    while (delta > 180) delta -= 360
    return (last + delta * alpha + 360) % 360
}

fun getConstellationName(type: Int): String {
    return when (type) {
        GnssStatus.CONSTELLATION_GPS -> "GPS"
        GnssStatus.CONSTELLATION_SBAS -> "SBAS"
        GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
        GnssStatus.CONSTELLATION_QZSS -> "QZSS"
        GnssStatus.CONSTELLATION_BEIDOU -> "BEIDOU"
        GnssStatus.CONSTELLATION_GALILEO -> "GALILEO"
        GnssStatus.CONSTELLATION_IRNSS -> "IRNSS"
        else -> "UNKNOWN"
    }
}

fun isXiaomiDevice(): Boolean {
    val m = Build.MANUFACTURER.uppercase()
    return m.contains("XIAOMI") || m.contains("REDMI") || m.contains("POCO")
}

fun isXiaomiSpecialPermissionGranted(context: Context): XiaomiPermissionStatus {
    if (!isXiaomiDevice()) return XiaomiPermissionStatus.UNKNOWN
    
    val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return try {
        val checkOpMethod = ops.javaClass.getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
        val showOnLock = checkOpMethod.invoke(ops, 10021, android.os.Process.myUid(), context.packageName) as Int
        val backgroundPop = checkOpMethod.invoke(ops, 10020, android.os.Process.myUid(), context.packageName) as Int
        
        if (showOnLock == AppOpsManager.MODE_ALLOWED && backgroundPop == AppOpsManager.MODE_ALLOWED) {
            XiaomiPermissionStatus.GRANTED
        } else {
            XiaomiPermissionStatus.DENIED
        }
    } catch (e: Exception) {
        XiaomiPermissionStatus.UNKNOWN 
    }
}

fun isXiaomiAutostartGranted(context: Context): Boolean {
    if (!isXiaomiDevice()) return true
    val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return try {
        val checkOpMethod = ops.javaClass.getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
        val autostart = checkOpMethod.invoke(ops, 10008, android.os.Process.myUid(), context.packageName) as Int
        autostart == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        true
    }
}

fun isSamsungDevice(): Boolean {
    return Build.MANUFACTURER.uppercase().contains("SAMSUNG")
}

fun isS21FEDevice(): Boolean {
    if (!isSamsungDevice()) return false
    val m = Build.MODEL.uppercase()
    return m.contains("G990B") || m.contains("G990E") || m.contains("S21FE")
}

fun isA15Device(): Boolean {
    val m = Build.MODEL.uppercase()
    val p = Build.PRODUCT.uppercase()
    return m.contains("A15") || p.contains("A15")
}

fun getRecentsLockDescription(): String {
    return if (isS21FEDevice()) {
        "Open the recent apps screen and 'Lock' this app. \n\u2022 NOTE: Verified NOT strictly required for reliability on S21 FE."
    } else {
        "Open the recent apps screen and 'Lock' this app to prevent it from being closed when you clear all apps."
    }
}

fun getBatteryOptimizationDescription(): String {
    return when {
        isSamsungDevice() -> "Set to UNRESTRICTED. App Info \u2192 Battery \u2192 Unrestricted. This is CRITICAL for background tracking."
        isXiaomiDevice() -> "Set to 'No restrictions'. App Info \u2192 Battery saver \u2192 No restrictions."
        else -> "Set to UNRESTRICTED or 'No optimization' in system battery settings."
    }
}

fun getAutoStartDescription(): String {
    return when {
        isSamsungDevice() -> {
            val hardwareNote = when {
                isA15Device() -> "\n\u2022 NOTE: A15 virtual proximity requires 'Unrestricted' for reliable debounce."
                isS21FEDevice() -> "\n\u2022 NOTE: S21FE G990B/E requires background activity for 10Hz GPS polling."
                else -> ""
            }
            "Ensure persistent background execution:\n\u2022 App Info \u2192 Battery \u2192 select 'Unrestricted'.\n\u2022 App Info \u2192 ensure 'Allow background activity' is ON.\n\u2022 Disable 'Pause app activity if unused'.$hardwareNote"
        }
        isXiaomiDevice() -> "Ensure persistent background execution:\n\u2022 App Info \u2192 enable 'Autostart'.\n\u2022 App Info \u2192 Notifications \u2192 enable all permissions."
        else -> "Enable 'Autostart' and ensure 'Background activity' are allowed in system settings."
    }
}
