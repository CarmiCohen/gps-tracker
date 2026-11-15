package com.jd.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/**
 * What one phone must send to its peer through the relay, which keeps no copy: the Monitor's geofence
 * (ISSUE-NOV8.13-O51) and detection settings (ISSUE-NOV10.8-O83) for the Sensor, and the Sensor's setup report for the
 * Monitor (ISSUE-NOV10.8-O83 part 2). The current value is sent each time the peer comes online, and each change while
 * it is online, so a peer that was offline during a change receives it when it reconnects. The receiver ignores a
 * repeated value.
 */
fun <T : Any> peerDeliveries(values: Flow<T>, isPeerOnline: Flow<Boolean>): Flow<T> =
    combine(values, isPeerOnline) { value, online -> value.takeIf { online } }
        .distinctUntilChanged()
        .filterNotNull()
