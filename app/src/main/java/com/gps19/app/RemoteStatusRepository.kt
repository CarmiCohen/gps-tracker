package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RemoteStatusRepository: Single Source of Truth for Remote Peer Telemetry.
 * July.26.02:
 * - Issue #545b: Lifecycle Idempotency. Added isInitialized AtomicBoolean guard 
 *   to initialize() to prevent redundant state restoration from MainRepository 
 *   during service re-attachment or multi-mode transitions.
 * July.23.01:
 * - SIT Hardening (Issue #522): Consolidated forensic state authority.
 * - Deep Purge: Removed references to the obsolete RemoteHandler.
 */
@Singleton
class RemoteStatusRepository @Inject constructor(
    private val mainRepository: MainRepository,
    private val timeProvider: TimeProvider
) {
    private val _remoteStatus = MutableStateFlow(TrackerStatus())
    val remoteStatus = _remoteStatus.asStateFlow()

    private val _isTrackerConnected = MutableStateFlow(false)
    val isTrackerConnected = _isTrackerConnected.asStateFlow()

    private val _lastPeerActivityTs = MutableStateFlow(0L)
    val lastPeerActivityTs = _lastPeerActivityTs.asStateFlow()

    private val _peerSignal = MutableStateFlow(0)
    val peerSignal = _peerSignal.asStateFlow()

    private var lastRemotePacketTs = 0L
    private val isInitialized = AtomicBoolean(false)

    suspend fun initialize() {
        if (isInitialized.getAndSet(true)) return

        try {
            mainRepository.loadTrackerState()?.let { savedStatus ->
                _remoteStatus.value = savedStatus
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize RemoteStatusRepository")
        }
    }

    fun updateStatus(status: TrackerStatus) {
        _remoteStatus.value = status
        mainRepository.saveTrackerState(status)
    }

    fun updateStatusAtomic(action: (TrackerStatus) -> TrackerStatus) {
        _remoteStatus.update { current ->
            val next = action(current)
            mainRepository.saveTrackerState(next)
            next
        }
    }

    fun setTrackerConnected(connected: Boolean) {
        _isTrackerConnected.value = connected
    }

    fun updatePeerActivity(ts: Long) {
        _lastPeerActivityTs.value = ts
    }

    fun setPeerSignal(signal: Int) {
        _peerSignal.value = signal
    }

    fun shouldProcessPacket(remoteTs: Long): Boolean {
        if (remoteTs > 0 && remoteTs < lastRemotePacketTs) return false
        if (remoteTs > 0) lastRemotePacketTs = remoteTs
        return true
    }

    fun reset() {
        _remoteStatus.value = TrackerStatus()
        _isTrackerConnected.value = false
        _lastPeerActivityTs.value = 0L
        _peerSignal.value = 0
        lastRemotePacketTs = 0L
        isInitialized.set(false)
    }
}
