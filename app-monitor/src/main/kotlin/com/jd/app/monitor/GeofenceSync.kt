package com.jd.app.monitor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/**
 * The settings the Monitor must send to the tracker: the geofence (ISSUE-NOV8.13-O51) and the Sensor's detection
 * settings (ISSUE-NOV10.8-O83). The stored value is sent each time the tracker comes online, and each edit while it is
 * online. The relay keeps no copy, so a tracker that was offline during an edit receives it when it reconnects; the
 * Sensor stores it and ignores an older or repeated revision.
 */
fun <T : Any> trackerDeliveries(settings: Flow<T>, isTrackerOnline: Flow<Boolean>): Flow<T> =
    combine(settings, isTrackerOnline) { value, online -> value.takeIf { online } }
        .distinctUntilChanged()
        .filterNotNull()
