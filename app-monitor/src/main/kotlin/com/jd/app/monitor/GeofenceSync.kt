package com.jd.app.monitor

import com.jd.core.shared.GeofenceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/**
 * The geofences the Monitor must send to the tracker (ISSUE-NOV8.13-O51): the stored one each time the tracker
 * comes online, and each edit while it is online. The relay keeps no copy, so a tracker that was offline during
 * an edit receives it when it reconnects; the Sensor stores it and ignores an older or repeated revision.
 */
fun geofenceDeliveries(geofence: Flow<GeofenceConfig>, isTrackerOnline: Flow<Boolean>): Flow<GeofenceConfig> =
    combine(geofence, isTrackerOnline) { config, online -> config.takeIf { online } }
        .distinctUntilChanged()
        .filterNotNull()
