package com.jd.core.ui

import android.app.usage.UsageStatsManager
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import com.jd.core.shared.NO_NETWORK
import com.jd.core.shared.PhoneState

/**
 * Reads this phone's state for the Info screens (ISSUE-NOV10.8-O84 part 2). Each value is read on its own, so one the
 * system refuses leaves the others; none needs a permission.
 */
fun readPhoneState(context: Context): PhoneState {
    val battery = readBattery(context)
    val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return PhoneState(
        batteryPercent = battery.level,
        isCharging = battery.isCharging,
        temperatureC = battery.tempCelsius,
        network = networkName(context),
        isPowerSave = power?.isPowerSaveMode == true,
        standbyBucket = standbyBucket(context),
        isBatteryExempt = power?.isIgnoringBatteryOptimizations(context.packageName) == true,
        freeStorageMb = runCatching { StatFs(context.filesDir.absolutePath).availableBytes / (1024L * 1024L) }.getOrNull()
    )
}

/** True when the active network has internet; shared by both apps' INT LED and the Monitor's alarms (SIMP-NOV6.1-S1). */
fun isNetworkAvailable(context: Context): Boolean = networkName(context) != NO_NETWORK

/** True when the GPS or the network location provider is on (the GPS LED, SIMP-NOV6.1-S1). */
fun isLocationProviderEnabled(context: Context): Boolean = try {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
        lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
} catch (_: Exception) {
    false
}

/** `Wi-Fi`, `Mobile`, `Ethernet`, `Other`, or [NO_NETWORK] when no network has internet. */
private fun networkName(context: Context): String = try {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
    when {
        caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NO_NETWORK
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Other"
    }
} catch (_: Exception) {
    NO_NETWORK
}

private fun standbyBucket(context: Context): Int? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return runCatching { (context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager)?.appStandbyBucket }.getOrNull()
}
