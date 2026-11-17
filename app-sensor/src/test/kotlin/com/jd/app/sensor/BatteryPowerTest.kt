package com.jd.app.sensor

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Test

/** Chapter 214.1 (ISSUE-NOV17.15-O114): plugged in is read apart from charging. */
class BatteryPowerTest {

    @Test
    fun aHeldBatteryOnAChargerIsPluggedInButNotCharging() {
        assertEquals(
            BatteryPower(isCharging = false, isPluggedIn = true),
            batteryPower(BatteryManager.BATTERY_STATUS_NOT_CHARGING, BatteryManager.BATTERY_PLUGGED_USB)
        )
    }

    @Test
    fun chargingAndFullAreBothPluggedIn() {
        val charging = BatteryPower(isCharging = true, isPluggedIn = true)
        assertEquals(charging, batteryPower(BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_PLUGGED_AC))
        assertEquals(charging, batteryPower(BatteryManager.BATTERY_STATUS_FULL, BatteryManager.BATTERY_PLUGGED_WIRELESS))
    }

    @Test
    fun onBatteryIsNeither() {
        assertEquals(BatteryPower(isCharging = false, isPluggedIn = false), batteryPower(BatteryManager.BATTERY_STATUS_DISCHARGING, 0))
        assertEquals(BatteryPower(isCharging = false, isPluggedIn = false), batteryPower(-1, 0))
    }

    @Test
    fun chargingWithoutAPluggedValueStillCountsAsPluggedIn() {
        assertEquals(BatteryPower(isCharging = true, isPluggedIn = true), batteryPower(BatteryManager.BATTERY_STATUS_CHARGING, 0))
    }
}
