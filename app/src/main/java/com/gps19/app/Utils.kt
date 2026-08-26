package com.gps19.app

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.GnssStatus
import android.os.Build
import android.widget.Toast
import com.gps19.core.engine.*
import org.osmdroid.util.GeoPoint

/**
 * Utils: Android-specific helper functions.
 * Aug.25.05:
 * - Issue #317: Hardware SOT Architectural Decoupling. Delegated hardware 
 *   detection to HardwareSot in core:engine (R313/R212).
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

fun isXiaomiDevice(): Boolean = HardwareSot.isXiaomi(Build.MANUFACTURER)

fun isSamsungDevice(): Boolean = HardwareSot.isSamsung(Build.MANUFACTURER, Build.BRAND)

fun isS21FEDevice(): Boolean = HardwareSot.isS21FE(Build.MANUFACTURER, Build.BRAND, Build.MODEL)

fun isA15Device(): Boolean = HardwareSot.isA15(Build.MANUFACTURER, Build.BRAND, Build.MODEL, Build.PRODUCT, Build.DEVICE)

/**
 * v9.3.11: Now requires non-null pkgName to prevent logcat spillage.
 */
fun isXiaomiSpecialPermissionGranted(context: Context, pkgName: String): XiaomiPermissionStatus {
    if (!isXiaomiDevice()) return XiaomiPermissionStatus.UNKNOWN
    
    val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return try {
        val checkOpMethod = ops.javaClass.getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
        val showOnLock = checkOpMethod.invoke(ops, 10021, android.os.Process.myUid(), pkgName) as Int
        val backgroundPop = checkOpMethod.invoke(ops, 10020, android.os.Process.myUid(), pkgName) as Int
        
        if (showOnLock == AppOpsManager.MODE_ALLOWED && backgroundPop == AppOpsManager.MODE_ALLOWED) {
            XiaomiPermissionStatus.GRANTED
        } else {
            XiaomiPermissionStatus.DENIED
        }
    } catch (e: Exception) {
        XiaomiPermissionStatus.UNKNOWN 
    }
}

fun getXiaomiAutostartStatus(context: Context, pkgName: String): XiaomiPermissionStatus {
    if (!isXiaomiDevice()) return XiaomiPermissionStatus.UNKNOWN
    val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return try {
        val checkOpMethod = ops.javaClass.getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
        val autostart = checkOpMethod.invoke(ops, 10008, android.os.Process.myUid(), pkgName) as Int
        if (autostart == AppOpsManager.MODE_ALLOWED) XiaomiPermissionStatus.GRANTED else XiaomiPermissionStatus.DENIED
    } catch (e: Exception) {
        XiaomiPermissionStatus.UNKNOWN
    }
}

/**
 * openHardwareSettings: Brand-agnostic entry point for hardware-specific permission managers.
 */
fun openHardwareSettings(context: Context, pkgName: String) {
    if (isXiaomiDevice()) {
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                putExtra("extra_pkgname", pkgName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // Fall through to generic details
        }
    }
    
    // Default fallback to standard App Info
    try {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", pkgName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open system settings", Toast.LENGTH_SHORT).show()
    }
}

/**
 * openXiaomiAutostartSettings: Attempts to launch the specific MIUI Autostart settings page.
 */
fun openXiaomiAutostartSettings(context: Context, pkgName: String) {
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
            intent.data = android.net.Uri.fromParts("package", pkgName, null)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

fun isXiaomiAutostartGranted(context: Context, pkgName: String): Boolean {
    return getXiaomiAutostartStatus(context, pkgName) == XiaomiPermissionStatus.GRANTED
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
