package com.jd.app.sensor

import android.os.BatteryManager

/**
 * The phone's power as read from one battery broadcast (ISSUE-NOV17.15-O114).
 * [isCharging]: the battery is charging or full, for the graphs' Charging band and the Info line.
 * [isPluggedIn]: a charger is connected, whether or not the battery takes charge; a phone that holds its battery at a
 * limit (battery protection) reports "not charging" while plugged in, and must not count as power disconnected.
 */
data class BatteryPower(val isCharging: Boolean, val isPluggedIn: Boolean)

/** [status] is `BatteryManager.EXTRA_STATUS`, [plugged] is `BatteryManager.EXTRA_PLUGGED` (0 on battery). */
fun batteryPower(status: Int, plugged: Int): BatteryPower {
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryPower(isCharging = isCharging, isPluggedIn = plugged > 0 || isCharging)
}
