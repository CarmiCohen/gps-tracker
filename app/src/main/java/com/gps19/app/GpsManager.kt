package com.gps19.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import com.google.android.gms.location.*
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GpsManager: Manages hardware GPS and GNSS status.
 * v9.4.0:
 * - R406a: Unified Heartbeat (Issue #501). Standardized GPS polling to 2s.
 *   Removed pollIntervalFlow and setPollingInterval to simplify logic.
 */
@Singleton
class GpsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    var satellitesInView = 0
        private set
    var satellitesUsed = 0
        private set
    var averageSnr = 0.0
        private set
        
    private var minSnrAccumulator = 100.0
    private var secMinSnrAccumulator = 100.0

    private val snrBuffer = ConcurrentLinkedQueue<Pair<Long, Double>>()
    private var lastBufferRecordTs = 0L

    private val _gnssDetailFlow = MutableStateFlow<GnssDetail?>(null)
    val gnssDetailFlow: StateFlow<GnssDetail?> = _gnssDetailFlow.asStateFlow()

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val now = timeProvider.currentTimeMillis()
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
                    if (snr < minSnrAccumulator) minSnrAccumulator = snr
                    if (snr < secMinSnrAccumulator) secMinSnrAccumulator = snr
                }
                satList.add(SatelliteInfo(
                    svid = status.getSvid(i),
                    cn0 = snr,
                    usedInFix = usedInFix,
                    constellation = status.getConstellationType(i)
                ))
            }
            satellitesUsed = used
            averageSnr = if (snrCount > 0) snrSum / snrCount else 0.0
            
            if (now - lastBufferRecordTs >= TICK_INTERVAL_MS) {
                val snrToStore = if (secMinSnrAccumulator > 99.0) averageSnr else secMinSnrAccumulator
                snrBuffer.add(now to snrToStore)
                lastBufferRecordTs = now
                secMinSnrAccumulator = 100.0

                while (snrBuffer.size > 0 && (now - (snrBuffer.peek()?.first ?: now)) > 1800000L) {
                    snrBuffer.poll()
                }
            }

            _gnssDetailFlow.value = GnssDetail(satellites = satList.sortedByDescending { it.cn0 })
        }
    }

    fun consumeMinSnr(): Double {
        val min = minSnrAccumulator
        minSnrAccumulator = 100.0
        return if (min > 99.0) averageSnr else min
    }

    fun getSnrSamples(fromTs: Long, toTs: Long): List<Pair<Long, Double>> {
        return snrBuffer.filter { it.first in fromTs..toTs }
    }

    fun kickGps() {
        try {
            locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_time_injection", null)
            locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_xtra_injection", null)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "GPS: Warm Kick failed")
        }
    }

    fun reviveGps() {
        Timber.w("GPS: Hardware revival triggered (Full Restart)")
        try {
            locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "delete_aiding_data", null)
            locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_xtra_injection", null)
            locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_time_injection", null)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "GPS: Extra command injection failed")
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocationFlow(): Flow<Location> = callbackFlow {
        try {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "GPS: Failed to register GNSS callback")
        }

        // Try to send last known location immediately
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                Timber.d("GPS: Delivered initial lastLocation (Acc: ${loc.accuracy})")
                trySend(loc)
            }
        }

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                try {
                    result.lastLocation?.let { 
                        Timber.v("GPS: New location received (Acc: ${it.accuracy})")
                        trySend(it) 
                    }
                } catch (e: Exception) {}
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TICK_INTERVAL_MS)
            .setMinUpdateIntervalMillis(TICK_INTERVAL_MS / 2)
            .setMinUpdateDistanceMeters(0.0f)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
            Timber.d("GPS: Location updates requested at ${TICK_INTERVAL_MS}ms interval")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
