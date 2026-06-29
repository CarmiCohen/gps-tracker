package com.gps19.app

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.GnssStatus
import android.os.Build
import com.gps19.core.engine.*
import org.osmdroid.util.GeoPoint

/**
 * Utils: Android-specific helper functions.
 * v8.9.51:
 * - Issue #455: Xiaomi Autostart & Boot Resilience. (Formerly #190)
 * v8.9.48:
 * - Issue #421: Role Identity Prefix Mismatch. (Formerly #182)
 */

enum class XiaomiPermissionStatus {
    GRANTED,
    DENIED,
    UNKNOWN // Reflection failed or non-Xiaomi device
}

/**
 * Checks if a GeoPoint is valid.
 */
fun isValidLocation(p: GeoPoint?): Boolean {
    return p != null && PhysicsUtils.isValidLocation(p.latitude, p.longitude)
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

fun getXiaomiAutostartStatus(context: Context): XiaomiPermissionStatus {
    if (!isXiaomiDevice()) return XiaomiPermissionStatus.UNKNOWN
    val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return try {
        val checkOpMethod = ops.javaClass.getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
        val autostart = checkOpMethod.invoke(ops, 10008, android.os.Process.myUid(), context.packageName) as Int
        if (autostart == AppOpsManager.MODE_ALLOWED) XiaomiPermissionStatus.GRANTED else XiaomiPermissionStatus.DENIED
    } catch (e: Exception) {
        XiaomiPermissionStatus.UNKNOWN
    }
}

/**
 * openXiaomiAutostartSettings: Attempts to launch the specific MIUI Autostart settings page.
 * Falls back to app info page if the specific activity is not found.
 */
fun openXiaomiAutostartSettings(context: Context) {
    if (!isXiaomiDevice()) return
    try {
        val intent = Intent()
        intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent("miui.intent.action.OP_AUTO_START")
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e2: Exception) {
            // Fallback to app details
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.fromParts("package", context.packageName, null)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

fun isXiaomiAutostartGranted(context: Context): Boolean {
    return getXiaomiAutostartStatus(context) == XiaomiPermissionStatus.GRANTED
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
