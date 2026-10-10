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
 * Oct.10.9:
 * - Issue #SIMP-1014-3: Connectivity Jitter. Migrated to saveLocationUpdateDebounced 
 *   to handle high-frequency JNI telemetry bursts without IO saturation.
 * Oct.2.9:
 * - Issue #1314: TrackerStatus Convergence. Migrated from TrackerStatus 
 *   to unified LocationUpdate monolith.
 */
@Singleton
class RemoteStatusRepository @Inject constructor(
    private val mainRepository: MainRepository,
    private val timeProvider: TimeProvider
) {
    private val _remoteStatus = MutableStateFlow(LocationUpdate())
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
            // R-ID 453/565: Standardized Role Identity Authority
            mainRepository.loadLocationUpdate(AppRole.VIEWER_REMOTE)?.let { savedStatus ->
                _remoteStatus.value = savedStatus
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize RemoteStatusRepository")
        }
    }

    fun updateStatus(status: LocationUpdate) {
        _remoteStatus.value = status
        mainRepository.saveLocationUpdateDebounced(status, AppRole.VIEWER_REMOTE)
    }

    fun updateStatusAtomic(action: (LocationUpdate) -> LocationUpdate) {
        _remoteStatus.update { current ->
            val next = action(current)
            mainRepository.saveLocationUpdateDebounced(next, AppRole.VIEWER_REMOTE)
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

    /**
     * shouldProcessPacket: Determines if a packet is fresh enough to process.
     * R171: Relaxed to allow jitter. Only drops if packet is older than 2 seconds 
     * relative to the newest packet received (MONOTONIC_JITTER_TOLERANCE_MS).
     */
    fun shouldProcessPacket(remoteTs: Long): Boolean {
        if (remoteTs <= 0) return true
        
        // Drop if it's a severe regression (e.g. historical data re-sending)
        if (remoteTs < lastRemotePacketTs - MONOTONIC_JITTER_TOLERANCE_MS) return false
        
        // Update high-water mark
        if (remoteTs > lastRemotePacketTs) {
            lastRemotePacketTs = remoteTs
        }
        return true
    }

    fun reset() {
        _remoteStatus.value = LocationUpdate()
        _isTrackerConnected.value = false
        _lastPeerActivityTs.value = 0L
        _peerSignal.value = 0
        lastRemotePacketTs = 0L
        isInitialized.set(false)
    }
}
