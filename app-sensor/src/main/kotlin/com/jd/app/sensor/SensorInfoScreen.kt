package com.jd.app.sensor

import com.jd.core.shared.InfoScreenState
import com.jd.core.shared.SensorInfo
import com.jd.core.shared.sensorInfoSections

/** The Sensor's own Info screen (ISSUE-NOV10.8-O84 part 2); [info] is null while the service is stopped. */
fun sensorInfoScreen(info: SensorInfo?): InfoScreenState =
    if (info == null) InfoScreenState(note = SERVICE_STOPPED_NOTE)
    else InfoScreenState(sections = sensorInfoSections(info), satellites = info.satellites)

internal const val SERVICE_STOPPED_NOTE = "The Sensor service is stopped: start it to see its Info."

/** The Info screen refreshes this often while it is open. */
internal const val INFO_REFRESH_MS = 1_000L
