package com.gps19.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.*
import com.google.android.gms.location.*
import com.gps19.core.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * GpsManager: Streamlined hardware GPS and GNSS status provider.
 * v9.5.0: Issue #514 - Simplified to rely on FusedLocationProvider and immediate GNSS status.
 * Removed legacy kick/revive commands and complex SNR buffering.
 */
class GpsManager(
    private val context: Context,
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
            averageSnr = if (snrCount > 0) snrSum / snrCount else 0.0
            _gnssDetailFlow.value = GnssDetail(satellites = satList.sortedByDescending { it.cn0 })
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
            Timber.d("GPS: Location updates requested at ${TICK_INTERVAL_MS}ms interval")
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
