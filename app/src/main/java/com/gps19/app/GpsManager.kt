package com.gps19.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.*
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
 * July.29.00:
 * - Issue #622: Forensic: Location Refresh Reactivity Hardening. Implemented 
 *   debounced recovery logic and forensic gap duration tracking (R613).
 * July.28.22:
 * - Issue #617: Global SharedFlow Audit. Hardened _internalGpsFlow with 
 *   BufferOverflow.DROP_OLDEST to ensure non-blocking hardware callbacks (R617).
 */
@Singleton
class GpsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope,
    private val timeProvider: TimeProvider
) {

    private val locationManager by lazy { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(context) }
    
    // Dedicated thread for high-frequency hardware callbacks to prevent ANR on A15
    private val gpsThread = HandlerThread("GpsHardwareThread").apply { start() }
    private val gpsHandler = Handler(gpsThread.looper)

    var satellitesInView = 0
        private set
    var satellitesUsed = 0
        private set
    var averageSnr = 0.0
        private set

    private var lastFixRt = 0L
    private var lastGnssEmitRt = 0L
    private var lastGnssStatusRt = 0L
    
    private var pendingEnterRt = 0L
    private var lastGapReason = LocationPendingReason.NONE
    
    var maxGnssJitterMs = 0L
        private set

    fun resetGnssJitter() {
        maxGnssJitterMs = 0L
    }

    data class LocationStatus(
        val isPending: Boolean = false,
        val reason: LocationPendingReason = LocationPendingReason.NONE,
        val lastFixRt: Long = 0L,
        val lastPendingDurationMs: Long = 0L,
        val recoveryConfirmed: Boolean = false
    )

    private val _locationStatus = MutableStateFlow(LocationStatus())
    val locationStatusFlow: StateFlow<LocationStatus> = _locationStatus.asStateFlow()

    private val snrTsBuffer = LongArray(512)
    private val snrRtBuffer = LongArray(512)
    private val snrValBuffer = DoubleArray(512)
    private var snrBufferIdx = 0
    private var snrBufferCount = 0

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
            var used = 0
            var snrSum = 0.0
            var snrCount = 0

            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) used++
                val snr = status.getCn0DbHz(i).toDouble()
                if (snr > 0.0) {
                    snrSum += snr
                    snrCount++
                }
            }
            
            satellitesUsed = used
            val avg = if (snrCount > 0) snrSum / snrCount else 0.0
            averageSnr = avg
            
            val now = timeProvider.currentTimeMillis()
            synchronized(snrTsBuffer) {
                snrTsBuffer[snrBufferIdx] = now
                snrRtBuffer[snrBufferIdx] = nowRt
                snrValBuffer[snrBufferIdx] = avg
                snrBufferIdx = (snrBufferIdx + 1) % 512
                if (snrBufferCount < 512) snrBufferCount++
            }

            if (nowRt - lastGnssEmitRt >= GNSS_SAMPLING_INTERVAL_MS) {
                lastGnssEmitRt = nowRt
                
                val satList = mutableListOf<SatelliteInfo>()
                for (i in 0 until status.satelliteCount) {
                    satList.add(SatelliteInfo(
                        svid = status.getSvid(i),
                        cn0 = status.getCn0DbHz(i).toDouble(),
                        usedInFix = status.usedInFix(i),
                        constellation = status.getConstellationType(i)
                    ))
                }
                
                _internalGpsFlow.tryEmit(GpsUpdate.GnssUpdate(
                    GnssDetail(satellites = satList.sortedByDescending { it.cn0 })
                ))
            }

            updateLocationStatus()
        }
    }

    private val _internalGpsFlow = MutableSharedFlow<GpsUpdate>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        // Heartbeat for status flow to catch timeouts even when no hardware callbacks arrive
        externalScope.launch {
            while (isActive) {
                updateLocationStatus()
                delay(2000L) // Faster pulse for recovery reactivity
            }
        }
    }

    private fun updateLocationStatus() {
        val nowRt = timeProvider.elapsedRealtime()
        val deltaSinceFix = if (lastFixRt > 0) nowRt - lastFixRt else nowRt
        
        _locationStatus.update { current ->
            var nextPending = current.isPending
            var nextReason = current.reason
            var recoveryConfirmed = current.recoveryConfirmed
            var lastPendingDuration = current.lastPendingDurationMs

            // Gap Detection
            if (deltaSinceFix > GPS_GAP_THRESHOLD_MS) {
                if (!nextPending) {
                    pendingEnterRt = nowRt
                    nextPending = true
                    recoveryConfirmed = false
                }
                nextReason = when {
                    satellitesInView == 0 -> LocationPendingReason.SIGNAL_LOSS
                    satellitesInView >= 4 && satellitesUsed < 4 -> LocationPendingReason.GPS_STALL
                    else -> LocationPendingReason.GPS_GAP
                }
                lastGapReason = nextReason
            } 
            // Recovery Logic with Debounce (Issue #622)
            else if (nextPending) {
                // We have a fix (deltaSinceFix <= threshold), but we wait for stabilization
                val recoveryDelta = nowRt - lastFixRt
                if (recoveryDelta < LOCATION_RECOVERY_DEBOUNCE_MS) {
                    // Fix is very fresh, check if it's stable or we just started recovering
                    if (nowRt - pendingEnterRt > 0) {
                        lastPendingDuration = nowRt - pendingEnterRt
                    }
                    // We don't clear isPending yet to prevent flickering if fix is lost immediately
                    // But we can mark it as "recovery in progress" implicitly by nextPending remaining true
                    // until the next heartbeat or fix update confirms stability.
                    // Actually, for UI flickering, we want to stay in "Pending" until it's really back.
                } else {
                    // Recovery confirmed
                    nextPending = false
                    nextReason = LocationPendingReason.NONE
                    recoveryConfirmed = true
                }
            } else {
                recoveryConfirmed = false
            }

            current.copy(
                isPending = nextPending,
                reason = nextReason,
                lastFixRt = lastFixRt,
                lastPendingDurationMs = lastPendingDuration,
                recoveryConfirmed = recoveryConfirmed
            )
        }
    }

    @SuppressLint("MissingPermission")
    private val hardwareObservationFlow = callbackFlow<GpsUpdate> {
        try {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, gpsHandler)
        } catch (e: Exception) {
            Timber.e(e, "GPS: Failed to register GNSS callback")
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                lastFixRt = timeProvider.elapsedRealtime()
                trySend(GpsUpdate.LocationUpdate(loc))
                updateLocationStatus()
            }
        }

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { 
                    lastFixRt = timeProvider.elapsedRealtime()
                    trySend(GpsUpdate.LocationUpdate(it))
                    updateLocationStatus()
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TICK_INTERVAL_MS)
            .setMinUpdateIntervalMillis(TICK_INTERVAL_MS / 2)
            .setMinUpdateDistanceMeters(0.0f)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, fusedCallback, gpsThread.looper)
        } catch (e: Exception) {
            Timber.e(e, "CRITICAL: GPS Request failed")
            close(e)
        }

        val internalJob = _internalGpsFlow.onEach { trySend(it) }.launchIn(this)

        awaitClose {
            internalJob.cancel()
            try {
                fusedLocationClient.removeLocationUpdates(fusedCallback)
                locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
            } catch (e: Exception) {}
        }
    }.shareIn(
        scope = externalScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
    )

    fun getLocationFlow(): Flow<Location> = hardwareObservationFlow
        .filterIsInstance<GpsUpdate.LocationUpdate>()
        .map { it.location }

    val gnssDetailFlow: Flow<GnssDetail?> = hardwareObservationFlow
        .filterIsInstance<GpsUpdate.GnssUpdate>()
        .map { it.detail }

    fun getSnrSamples(fromTs: Long, toTs: Long): Sequence<EngineSnrSample> = sequence {
        val flyweight = EngineSnrSample()
        val c: Int
        val startIdx: Int
        synchronized(snrTsBuffer) {
            c = snrBufferCount
            startIdx = (snrBufferIdx - c + 512) % 512
        }

        for (i in 0 until c) {
            val idx = (startIdx + i) % 512
            val ts: Long
            val rt: Long
            val snr: Double
            
            synchronized(snrTsBuffer) {
                ts = snrTsBuffer[idx]
                rt = snrRtBuffer[idx]
                snr = snrValBuffer[idx]
            }
            
            if (ts in fromTs..toTs) {
                flyweight.ts = ts
                flyweight.rt = rt
                flyweight.snr = snr
                yield(flyweight)
            }
        }
    }
}
