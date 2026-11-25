package com.jd.app.sensor

/**
 * The status sent while the Sensor has no current fix (`com.jd.core.ui.isFixFresh`, ISSUE-NOV7.19-O43). The Monitor's
 * `hasGpsSignal` reads it as no position.
 */
const val NO_FIX_STATUS = "NO_FIX"
