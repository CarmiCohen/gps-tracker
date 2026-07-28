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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * GpsManager: Hardware GPS and GNSS status provider.
 * July.28.21:
 * - Issue #615: Forensic: Stability Audit Metric Expansion. Implemented 
 *   GNSS callback jitter tracking to detect hardware-level timing 
 *   inconsistencies. maxGnssJitterMs is exposed for forensic auditing.
 * July.28.20:
 * - Issue #614: Structural: GNSS Callback Overhead Monitoring. Implemented 
 *   sampling for high-frequency GNSS status callbacks to prevent Main Thread 
 *   starvation on budget hardware. Scalars are updated live, but detailed 
 *   flow emissions are throttled by GNSS_SAMPLING_INTERVAL_MS.
 * July.28.18:
 * - Issue #613: Forensic: Location Refresh Reactivity. Added reactive 
 *   locationStatusFlow to monitor pending fixes and stalls without polling.
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
    
    var maxGnssJitterMs = 0L
        private set

    fun resetGnssJitter() {
        maxGnssJitterMs = 0L
    }

    data class LocationStatus(
        val isPending: Boolean = false,
        val reason: LocationPendingReason = LocationPendingReason.NONE,
        val lastFixRt: Long = 0L
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

            // Basic status updates are lightweight and needed for real-time health checks
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

            // Issue #614: Throttle high-frequency hardware chatter to prevent downstream 
            // flow processing overhead on budget hardware.
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

    private val _internalGpsFlow = MutableSharedFlow<GpsUpdate>(replay = 1)

    init {
        // Heartbeat for status flow to catch timeouts even when no hardware callbacks arrive
        externalScope.launch {
            while (isActive) {
                updateLocationStatus()
                delay(5000L)
            }
        }
    }

    private fun updateLocationStatus() {
        val nowRt = timeProvider.elapsedRealtime()
        val delta = if (lastFixRt > 0) nowRt - lastFixRt else nowRt // Treat uptime as initial delta
        
        var isPending = false
        var reason = LocationPendingReason.NONE
        
        if (delta > GPS_GAP_THRESHOLD_MS) {
            isPending = true
            reason = when {
                satellitesInView == 0 -> LocationPendingReason.SIGNAL_LOSS
                satellitesInView >= 4 && satellitesUsed < 4 -> LocationPendingReason.GPS_STALL
                else -> LocationPendingReason.GPS_GAP
            }
        }
        
        _locationStatus.update { it.copy(isPending = isPending, reason = reason, lastFixRt = lastFixRt) }
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
