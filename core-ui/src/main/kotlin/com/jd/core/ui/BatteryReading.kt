package com.jd.core.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** The device's own battery state. A null value means the system did not report it. */
data class BatteryReading(
    val level: Int? = null,
    val isCharging: Boolean = false,
    val tempCelsius: Float? = null
)

private const val MISSING = Int.MIN_VALUE

fun batteryReading(level: Int, scale: Int, status: Int, tempTenths: Int) = BatteryReading(
    level = if (level >= 0 && scale > 0) level * 100 / scale else null,
    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
    tempCelsius = if (tempTenths != MISSING) tempTenths / 10f else null
)

fun readBattery(context: Context): BatteryReading {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?: return BatteryReading()
    return batteryReading(
        level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
        scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
        status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1),
        tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, MISSING)
    )
}
