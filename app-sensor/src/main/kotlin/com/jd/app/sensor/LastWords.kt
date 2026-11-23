package com.jd.app.sensor

import android.content.Intent
import com.jd.core.shared.SilenceCause

/** The SIM broadcast Android still sends; hidden from the SDK, so named here. */
const val ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED"
private const val EXTRA_SIM_STATE = "ss"
private const val SIM_STATE_ABSENT = "ABSENT"

/** The system broadcasts that may come right before the Sensor is cut off (ISSUE-NOV22.10-O137). */
val LAST_WORD_ACTIONS = listOf(Intent.ACTION_SHUTDOWN, Intent.ACTION_AIRPLANE_MODE_CHANGED, ACTION_SIM_STATE_CHANGED)

/**
 * The last word a system broadcast calls for, or null (ISSUE-NOV22.10-O137). [isAirplaneModeOn] and [simState] are
 * the broadcast's extras; a removed SIM only cuts the Sensor off while it uses mobile data ([isOnMobileData]).
 */
fun lastWordFor(
    action: String?,
    isAirplaneModeOn: Boolean,
    simState: String?,
    isOnMobileData: Boolean,
    batteryLevel: Int?,
    isPluggedIn: Boolean
): SilenceCause? = when (action) {
    Intent.ACTION_SHUTDOWN -> SilenceCause.ofShutdown(batteryLevel, isPluggedIn)
    Intent.ACTION_AIRPLANE_MODE_CHANGED -> SilenceCause.AIRPLANE_MODE.takeIf { isAirplaneModeOn }
    ACTION_SIM_STATE_CHANGED -> SilenceCause.SIM_REMOVED.takeIf { simState == SIM_STATE_ABSENT && isOnMobileData }
    else -> null
}

/** [lastWordFor] with the extras read from [intent]. */
fun lastWordFor(intent: Intent, isOnMobileData: Boolean, batteryLevel: Int?, isPluggedIn: Boolean): SilenceCause? =
    lastWordFor(
        action = intent.action,
        isAirplaneModeOn = intent.getBooleanExtra("state", false),
        simState = intent.getStringExtra(EXTRA_SIM_STATE),
        isOnMobileData = isOnMobileData,
        batteryLevel = batteryLevel,
        isPluggedIn = isPluggedIn
    )
