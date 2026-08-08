package com.gps19.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.*
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * GpsManager: Hardware GPS and GNSS status provider.
 * Aug.07.07:
 * - Issue #124-Revival: Functional: Hardened GPS Revival loop (R124). 
 *   Resolved lint warnings for stable properties and ensured robust 
 *   permission handling for revival pulses.
 * July.29.00:
 * - Issue #622: Forensic: Location Refresh Reactivity Hardening. Implemented 
 *   debounced recovery logic and forensic gap duration tracking (R613).
 */
@Singleton
class GpsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope,
    private val timeProvider: TimeProvider
) {

    private val locationManager by lazy { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(context) }
    
    private val gpsThread = HandlerThread("GpsHardwareThread").apply { start() }
    private val gpsHandler = Handler(gpsThread.looper)

    var satellitesInView = 0; private set
    var satellitesUsed = 0; private set
    var averageSnr = 0.0; private set

    private var lastFixRt = 0L
    private var lastGnssEmitRt = 0L
    private var lastGnssStatusRt = 0L
    
    private var pendingEnterRt = 0L
    
    // R124: Revival State
    private var revivalAttemptCount = 0
    private val _revivalEvents = MutableSharedFlow<RevivalEvent>(extraBufferCapacity = 8)
    val revivalEvents = _revivalEvents.asSharedFlow()

    sealed class RevivalEvent {
        data class Attempt(val count: Int) : RevivalEvent()
        object HardwareLock : RevivalEvent()
        object Success : RevivalEvent()
    }

    var maxGnssJitterMs = 0L; private set
    fun resetGnssJitter() { maxGnssJitterMs = 0L }

    data class LocationStatus(
        val isPending: Boolean = false,
        val reason: LocationPendingReason = LocationPendingReason.NONE,
        val lastFixRt: Long = 0L,
        val lastPendingDurationMs: Long = 0L,
        val recoveryConfirmed: Boolean = false
    )

    private val _locationStatus = MutableStateFlow(LocationStatus())
    val locationStatusFlow: StateFlow<LocationStatus> = _locationStatus.asStateFlow()

    private val snrTsBuffer = LongArray(512); private val snrRtBuffer = LongArray(512); private val snrValBuffer = DoubleArray(512)
    private var snrBufferIdx = 0; private var snrBufferCount = 0

    private sealed class GpsUpdate {
        data class LocationUpdate(val location: Location) : GpsUpdate()
        data class GnssUpdate(val detail: GnssDetail) : GpsUpdate()
    }

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val nowRt = timeProvider.elapsedRealtime()
            if (lastGnssStatusRt > 0) {
                val interval = nowRt - lastGnssStatusRt
                val jitter = abs(interval - GNSS_EXPECTED_INTERVAL_MS)
                if (jitter > maxGnssJitterMs) maxGnssJitterMs = jitter
            }
            lastGnssStatusRt = nowRt
            satellitesInView = status.satelliteCount
            var used = 0; var snrSum = 0.0; var snrCount = 0
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) used++
                val snr = status.getCn0DbHz(i).toDouble()
                if (snr > 0.0) { snrSum += snr; snrCount++ }
            }
            satellitesUsed = used; averageSnr = if (snrCount > 0) snrSum / snrCount else 0.0
            val now = timeProvider.currentTimeMillis()
            synchronized(snrTsBuffer) {
                snrTsBuffer[snrBufferIdx] = now; snrRtBuffer[snrBufferIdx] = nowRt; snrValBuffer[snrBufferIdx] = averageSnr
                snrBufferIdx = (snrBufferIdx + 1) % 512; if (snrBufferCount < 512) snrBufferCount++
            }
            if (nowRt - lastGnssEmitRt >= GNSS_SAMPLING_INTERVAL_MS) {
                lastGnssEmitRt = nowRt
                val satList = mutableListOf<SatelliteInfo>()
                for (i in 0 until status.satelliteCount) {
                    satList.add(SatelliteInfo(svid = status.getSvid(i), cn0 = status.getCn0DbHz(i).toDouble(), usedInFix = status.usedInFix(i), constellation = status.getConstellationType(i)))
                }
                _internalGpsFlow.tryEmit(GpsUpdate.GnssUpdate(GnssDetail(satellites = satList.sortedByDescending { it.cn0 })))
            }
            updateLocationStatus()
        }
    }

    private val _internalGpsFlow = MutableSharedFlow<GpsUpdate>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        externalScope.launch {
            while (isActive) {
                updateLocationStatus()
                checkRevivalLifecycle()
                delay(2000L)
            }
        }
    }

    private fun checkRevivalLifecycle() {
        val nowRt = timeProvider.elapsedRealtime()
        val currentStatus = _locationStatus.value
        
        if (currentStatus.isPending && currentStatus.reason == LocationPendingReason.GPS_STALL) {
            val stallDuration = nowRt - pendingEnterRt
            if (stallDuration > (revivalAttemptCount + 1) * GPS_REVIVAL_RETRY_INTERVAL_MS) {
                if (revivalAttemptCount < MAX_REVIVAL_ATTEMPTS) {
                    revivalAttemptCount++
                    _revivalEvents.tryEmit(RevivalEvent.Attempt(revivalAttemptCount))
                    restartLocationUpdates()
                } else if (revivalAttemptCount == MAX_REVIVAL_ATTEMPTS) {
                    revivalAttemptCount++ 
                    _revivalEvents.tryEmit(RevivalEvent.HardwareLock)
                    Timber.e("R124: GPS hardware lock confirmed after 3 failed revivals.")
                }
            }
        } else if (!currentStatus.isPending && revivalAttemptCount > 0) {
            _revivalEvents.tryEmit(RevivalEvent.Success)
            revivalAttemptCount = 0
        }
    }

    private fun restartLocationUpdates() {
        Timber.w("R124: Restarting hardware GPS session (Attempt $revivalAttemptCount)")
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.e("R124: Revival aborted - Missing FINE_LOCATION permission")
            return
        }

        externalScope.launch(Dispatchers.Main) {
            val fastRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMaxUpdates(1)
                .build()
            try {
                fusedLocationClient.requestLocationUpdates(fastRequest, object : LocationCallback() {
                    override fun onLocationResult(p0: LocationResult) {}
                }, Looper.getMainLooper())
            } catch (e: SecurityException) {
                Timber.e(e, "R124: SecurityException during revival pulse")
            } catch (e: Exception) {
                Timber.e(e, "R124: Manual revival pulse failed")
            }
        }
    }

    private fun updateLocationStatus() {
        val nowRt = timeProvider.elapsedRealtime()
        val deltaSinceFix = if (lastFixRt > 0) nowRt - lastFixRt else nowRt
        
        _locationStatus.update { current ->
            var nextPending = current.isPending; var nextReason = current.reason
            var recoveryConfirmed = current.recoveryConfirmed; var lastPendingDuration = current.lastPendingDurationMs

            if (deltaSinceFix > GPS_GAP_THRESHOLD_MS) {
                if (!nextPending) { pendingEnterRt = nowRt; nextPending = true; recoveryConfirmed = false }
                nextReason = when {
                    satellitesInView == 0 -> LocationPendingReason.SIGNAL_LOSS
                    satellitesInView >= 4 && satellitesUsed < 4 -> LocationPendingReason.GPS_STALL
                    else -> LocationPendingReason.GPS_GAP
                }
            } 
            else if (nextPending) {
                val recoveryDelta = nowRt - lastFixRt
                if (recoveryDelta < LOCATION_RECOVERY_DEBOUNCE_MS) {
                    if (nowRt - pendingEnterRt > 0) lastPendingDuration = nowRt - pendingEnterRt
                } else {
                    nextPending = false; nextReason = LocationPendingReason.NONE; recoveryConfirmed = true
                }
            } else { recoveryConfirmed = false }

            current.copy(isPending = nextPending, reason = nextReason, lastFixRt = lastFixRt, lastPendingDurationMs = lastPendingDuration, recoveryConfirmed = recoveryConfirmed)
        }
    }

    @SuppressLint("MissingPermission")
    private val hardwareObservationFlow = callbackFlow<GpsUpdate> {
        try { locationManager.registerGnssStatusCallback(gnssStatusCallback, gpsHandler) } catch (e: Exception) { Timber.e(e, "GPS: Failed to register GNSS callback") }
        fusedLocationClient.lastLocation.addOnSuccessListener { loc -> if (loc != null) { lastFixRt = timeProvider.elapsedRealtime(); trySend(GpsUpdate.LocationUpdate(loc)); updateLocationStatus() } }
        val fusedCallback = object : LocationCallback() { override fun onLocationResult(result: LocationResult) { result.lastLocation?.let { lastFixRt = timeProvider.elapsedRealtime(); trySend(GpsUpdate.LocationUpdate(it)); updateLocationStatus() } } }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TICK_INTERVAL_MS).setMinUpdateIntervalMillis(TICK_INTERVAL_MS / 2).setMinUpdateDistanceMeters(0.0f).setWaitForAccurateLocation(false).build()
        try { fusedLocationClient.requestLocationUpdates(request, fusedCallback, gpsThread.looper) } catch (e: Exception) { Timber.e(e, "CRITICAL: GPS Request failed"); close(e) }
        val internalJob = _internalGpsFlow.onEach { trySend(it) }.launchIn(this)
        awaitClose { internalJob.cancel(); try { fusedLocationClient.removeLocationUpdates(fusedCallback); locationManager.unregisterGnssStatusCallback(gnssStatusCallback) } catch (e: Exception) {} }
    }.shareIn(scope = externalScope, started = SharingStarted.WhileSubscribed(5000), replay = 1)

    fun getLocationFlow(): Flow<Location> = hardwareObservationFlow.filterIsInstance<GpsUpdate.LocationUpdate>().map { it.location }
    val gnssDetailFlow: Flow<GnssDetail?> = hardwareObservationFlow.filterIsInstance<GpsUpdate.GnssUpdate>().map { it.detail }

    fun getSnrSamples(fromTs: Long, toTs: Long): Sequence<EngineSnrSample> = sequence {
        val flyweight = EngineSnrSample(); val c: Int; val startIdx: Int
        synchronized(snrTsBuffer) { c = snrBufferCount; startIdx = (snrBufferIdx - c + 512) % 512 }
        for (i in 0 until c) {
            val idx = (startIdx + i) % 512; val ts: Long; val rt: Long; val snr: Double
            synchronized(snrTsBuffer) { ts = snrTsBuffer[idx]; rt = snrRtBuffer[idx]; snr = snrValBuffer[idx] }
            if (ts in fromTs..toTs) { flyweight.ts = ts; flyweight.rt = rt; flyweight.snr = snr; yield(flyweight) }
        }
    }
}
