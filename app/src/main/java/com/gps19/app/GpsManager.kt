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
 * July.25.02:
 * - Issue #550: Refactored SNR history to primitive arrays (LongArray/DoubleArray) 
 *   to eliminate heap churn and achieve zero-allocation forensic buffering.
 * July.24.05:
 * - Issue #538e: Optimized SNR sample retrieval for forensic backfilling. 
 */
@Singleton
class GpsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {

    private val locationManager by lazy { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(context) }
    
    var satellitesInView = 0
        private set
    var satellitesUsed = 0
        private set
    var averageSnr = 0.0
        private set

    // Issue #550: Primitive buffers for zero-churn forensics
    private val snrTsBuffer = LongArray(512)
    private val snrValBuffer = DoubleArray(512)
    private var snrBufferIdx = 0
    private var snrBufferCount = 0

    private val _gnssDetailFlow = MutableStateFlow<GnssDetail?>(null)
    val gnssDetailFlow: StateFlow<GnssDetail?> = _gnssDetailFlow.asStateFlow()

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
            synchronized(snrTsBuffer) {
                snrTsBuffer[snrBufferIdx] = now
                snrValBuffer[snrBufferIdx] = avg
                snrBufferIdx = (snrBufferIdx + 1) % 512
                if (snrBufferCount < 512) snrBufferCount++
            }

            _gnssDetailFlow.value = GnssDetail(satellites = satList.sortedByDescending { it.cn0 })
        }
    }

    /**
     * Returns SNR samples in the given time range for forensic backfilling.
     * Uses sequence generator to eliminate intermediate list allocations.
     */
    fun getSnrSamples(fromTs: Long, toTs: Long): Sequence<Pair<Long, Double>> = sequence {
        // Snapshot the buffer state to avoid holding lock during yield
        val (tsCopy, valCopy, count) = synchronized(snrTsBuffer) {
            val c = snrBufferCount
            val tsArr = LongArray(c)
            val valArr = DoubleArray(c)
            val startIdx = (snrBufferIdx - c + 512) % 512
            for (i in 0 until c) {
                val idx = (startIdx + i) % 512
                tsArr[i] = snrTsBuffer[idx]
                valArr[i] = snrValBuffer[idx]
            }
            Triple(tsArr, valArr, c)
        }
        
        for (i in 0 until count) {
            if (tsCopy[i] in fromTs..toTs) {
                yield(tsCopy[i] to valCopy[i])
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocationFlow(): Flow<Location> = callbackFlow {
        try {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Timber.e(e, "GPS: Failed to register GNSS callback")
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                trySend(loc)
            }
        }

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TICK_INTERVAL_MS)
            .setMinUpdateIntervalMillis(TICK_INTERVAL_MS / 2)
            .setMinUpdateDistanceMeters(0.0f)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Timber.e(e, "CRITICAL: GPS Request failed")
            close(e)
        }

        awaitClose {
            try {
                fusedLocationClient.removeLocationUpdates(fusedCallback)
                locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
            } catch (e: Exception) {}
        }
    }
}
