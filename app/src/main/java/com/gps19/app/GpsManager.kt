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

/**
 * GpsManager: Hardware GPS and GNSS status provider.
 * July.27.12:
 * - A15 Hardening: Migrated all hardware callbacks to a dedicated HandlerThread 
 *   to ensure GNSS status chatter does not block the Main Looper.
 * July.26.03:
 * - Issue #545c: Flow Architecture Standardization.
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
            satellitesInView = status.satelliteCount
            var used = 0
            var snrSum = 0.0
            var snrCount = 0
            val satList = mutableListOf<SatelliteInfo>()

            for (i in 0 until status.satelliteCount) {
                val usedInFix = status.usedInFix(i)
                if (usedInFix) used++
                val snr = status.getCn0DbHz(i).toDouble()
                if (snr > 0.0) {
                    snrSum += snr
                    snrCount++
                }
                satList.add(SatelliteInfo(
                    svid = status.getSvid(i),
                    cn0 = snr,
                    usedInFix = usedInFix,
                    constellation = status.getConstellationType(i)
                ))
            }
            satellitesUsed = used
            val avg = if (snrCount > 0) snrSum / snrCount else 0.0
            averageSnr = avg
            
            val now = timeProvider.currentTimeMillis()
            val nowRt = timeProvider.elapsedRealtime()
            synchronized(snrTsBuffer) {
                snrTsBuffer[snrBufferIdx] = now
                snrRtBuffer[snrBufferIdx] = nowRt
                snrValBuffer[snrBufferIdx] = avg
                snrBufferIdx = (snrBufferIdx + 1) % 512
                if (snrBufferCount < 512) snrBufferCount++
            }

            _internalGpsFlow.tryEmit(GpsUpdate.GnssUpdate(GnssDetail(satellites = satList.sortedByDescending { it.cn0 })))
        }
    }

    private val _internalGpsFlow = MutableSharedFlow<GpsUpdate>(replay = 1)

    @SuppressLint("MissingPermission")
    private val hardwareObservationFlow = callbackFlow<GpsUpdate> {
        try {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, gpsHandler)
        } catch (e: Exception) {
            Timber.e(e, "GPS: Failed to register GNSS callback")
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                trySend(GpsUpdate.LocationUpdate(loc))
            }
        }

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(GpsUpdate.LocationUpdate(it)) }
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
