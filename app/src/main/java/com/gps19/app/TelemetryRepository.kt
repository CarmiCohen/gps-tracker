package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TelemetryRepository: In-memory store for live system status.
 * Sep.06.02:
 * - Issue #924 RESOLVED: Watchdog Safe-Mode. Added isSafeMode to prevent 
 *   signaling loops during hydration failures (R-ID 271).
 * Aug.01.10:
 * - Issue #668: Performance: Object Churn. Implemented double-buffering for 
 *   LocationUpdate and SystemHealthState to ensure zero-allocation StateFlow 
 *   emissions (R-HARDWARE-01). Refactored updateLocation to use mergeInto().
 */
@Singleton
class TelemetryRepository @Inject constructor() {
    private val _isRelayConnected = MutableStateFlow(false)
    val isRelayConnected = _isRelayConnected.asStateFlow()

    private val _lastRtt = MutableStateFlow(0)
    val lastRtt = _lastRtt.asStateFlow()

    private val _isSafeMode = MutableStateFlow(false)
    val isSafeMode = _isSafeMode.asStateFlow()

    // Flyweight buffers for zero-churn StateFlow emissions
    private val healthBuffers = listOf(SystemHealthState(), SystemHealthState())
    private var healthBufferIdx = 0

    private val localLocBuffers = listOf(LocationUpdate(isMe = true), LocationUpdate(isMe = true))
    private var localLocBufferIdx = 0

    private val trackerLocBuffers = listOf(LocationUpdate(isMe = false), LocationUpdate(isMe = false))
    private var trackerLocBufferIdx = 0

    private val _systemHealth = MutableStateFlow(healthBuffers[0])
    val systemHealth = _systemHealth.asStateFlow()

    private val _localLocation = MutableStateFlow(localLocBuffers[0])
    val localLocation = _localLocation.asStateFlow()

    private val _trackerLocation = MutableStateFlow(trackerLocBuffers[0])
    val trackerLocation = _trackerLocation.asStateFlow()

    private val _connectedViewers = MutableStateFlow<List<String>>(emptyList())
    val connectedViewers = _connectedViewers.asStateFlow()

    private val _lastRemoteActivityTs = MutableStateFlow(0L)
    val lastRemoteActivityTs = _lastRemoteActivityTs.asStateFlow()

    private val _gnssDetail = MutableStateFlow<GnssDetail?>(null)
    val gnssDetail = _gnssDetail.asStateFlow()

    fun updateRelayStatus(connected: Boolean) { _isRelayConnected.value = connected }
    fun updateLastRtt(rtt: Int) { _lastRtt.value = rtt }
    fun setSafeMode(enabled: Boolean) { _isSafeMode.value = enabled }

    /**
     * updateHealth: Swaps buffers to force StateFlow emission while reusing objects.
     */
    fun updateHealth(state: SystemHealthState) { 
        val nextIdx = (healthBufferIdx + 1) % 2
        val next = healthBuffers[nextIdx]
        next.copyFrom(state)
        healthBufferIdx = nextIdx
        _systemHealth.value = next
    }

    fun updateGnssDetail(detail: GnssDetail?) { _gnssDetail.value = detail }

    /**
     * updateLocation: Swaps buffers and uses mergeInto to eliminate per-tick allocations.
     */
    fun updateLocation(update: LocationUpdate) {
        if (update.isMe) {
            val nextIdx = (localLocBufferIdx + 1) % 2
            val current = localLocBuffers[localLocBufferIdx]
            val next = localLocBuffers[nextIdx]
            
            next.copyFrom(current)
            TelemetryMerger.mergeInto(next, update)
            
            localLocBufferIdx = nextIdx
            _localLocation.value = next
        } else {
            val nextIdx = (trackerLocBufferIdx + 1) % 2
            val current = trackerLocBuffers[trackerLocBufferIdx]
            val next = trackerLocBuffers[nextIdx]
            
            next.copyFrom(current)
            TelemetryMerger.mergeInto(next, update)
            
            trackerLocBufferIdx = nextIdx
            _trackerLocation.value = next
        }
    }

    fun updateConnectedViewers(viewers: List<String>) { _connectedViewers.value = viewers }
    fun updateRemoteActivity(ts: Long) { _lastRemoteActivityTs.value = ts }

    fun clear() {
        val h = healthBuffers[0]
        h.copyFrom(SystemHealthState())
        _systemHealth.value = h

        val l = localLocBuffers[0]
        l.copyFrom(LocationUpdate(isMe = true))
        _localLocation.value = l

        val t = trackerLocBuffers[0]
        t.copyFrom(LocationUpdate(isMe = false))
        _trackerLocation.value = t

        _connectedViewers.value = emptyList()
        _lastRemoteActivityTs.value = 0L
        _gnssDetail.value = null
        _isSafeMode.value = false
    }
}
