package com.gps19.app

import com.gps19.core.engine.GnssDetail
import com.gps19.core.engine.LocationUpdate
import com.gps19.core.engine.TelemetryMerger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TelemetryRepository: In-memory store for live system status.
 * v9.5.0: Hilt removed. Manual DI.
 */
class TelemetryRepository() {
    private val _isRelayConnected = MutableStateFlow(false)
    val isRelayConnected = _isRelayConnected.asStateFlow()

    private val _lastRtt = MutableStateFlow(0)
    val lastRtt = _lastRtt.asStateFlow()

    private val _integrityState = MutableStateFlow(IntegrityState())
    val integrityState = _integrityState.asStateFlow()

    private val _localLocation = MutableStateFlow(LocationUpdate())
    val localLocation = _localLocation.asStateFlow()

    private val _trackerLocation = MutableStateFlow(LocationUpdate())
    val trackerLocation = _trackerLocation.asStateFlow()

    private val _connectedViewers = MutableStateFlow<List<String>>(emptyList())
    val connectedViewers = _connectedViewers.asStateFlow()

    private val _lastRemoteActivityTs = MutableStateFlow(0L)
    val lastRemoteActivityTs = _lastRemoteActivityTs.asStateFlow()

    private val _gnssDetail = MutableStateFlow<GnssDetail?>(null)
    val gnssDetail = _gnssDetail.asStateFlow()

    fun updateRelayStatus(connected: Boolean) { _isRelayConnected.value = connected }
    fun updateLastRtt(rtt: Int) { _lastRtt.value = rtt }
    fun updateIntegrity(state: IntegrityState) { _integrityState.value = state }
    fun updateGnssDetail(detail: GnssDetail?) { _gnssDetail.value = detail }

    fun updateLocation(update: LocationUpdate) {
        val current = if (update.isMe) _localLocation.value else _trackerLocation.value
        
        // v8.8.21: Delegated complex merging logic to TelemetryMerger (engine)
        val merged = TelemetryMerger.merge(current, update)

        if (update.isMe) {
            _localLocation.value = merged
        } else {
            _trackerLocation.value = merged
        }
    }

    fun updateConnectedViewers(viewers: List<String>) { _connectedViewers.value = viewers }
    fun updateRemoteActivity(ts: Long) { _lastRemoteActivityTs.value = ts }

    fun clear() {
        _localLocation.value = LocationUpdate()
        _trackerLocation.value = LocationUpdate()
        _integrityState.value = IntegrityState()
        _connectedViewers.value = emptyList()
        _lastRemoteActivityTs.value = 0L
        _gnssDetail.value = null
    }
}
