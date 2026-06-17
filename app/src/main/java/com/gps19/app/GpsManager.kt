package com.gps19.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import com.google.android.gms.location.*
import com.gps19.core.engine.GnssDetail
import com.gps19.core.engine.SatelliteInfo
import com.gps19.core.engine.TimeProvider
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
 * v8.8.21: Migrated to TimeProvider for all timing logic.
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
    var averageSnr = 0f
        private set
        
    private var minSnrAccumulator = 100f
    private var secMinSnrAccumulator = 100f

    private val snrBuffer = ConcurrentLinkedQueue<Pair<Long, Float>>()
    private var lastBufferRecordTs = 0L

    private val _gnssDetailFlow = MutableStateFlow<GnssDetail?>(null)
    val gnssDetailFlow: StateFlow<GnssDetail?> = _gnssDetailFlow.asStateFlow()

    private val pollIntervalFlow = MutableStateFlow(1000L)

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val now = timeProvider.currentTimeMillis()
            satellitesInView = status.satelliteCount
            var used = 0
            var snrSum = 0f
            var snrCount = 0
            val satList = mutableListOf<SatelliteInfo>()

            for (i in 0 until status.satelliteCount) {
                val usedInFix = status.usedInFix(i)
                if (usedInFix) used++
                val snr = status.getCn0DbHz(i)
                if (snr > 0) {
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
            averageSnr = if (snrCount > 0) snrSum / snrCount else 0f
            
            if (now - lastBufferRecordTs >= 1000L) {
                val snrToStore = if (secMinSnrAccumulator > 99f) averageSnr else secMinSnrAccumulator
                snrBuffer.add(now to snrToStore)
                lastBufferRecordTs = now
                secMinSnrAccumulator = 100f

                while (snrBuffer.size > 0 && (now - (snrBuffer.peek()?.first ?: now)) > 1800000L) {
                    snrBuffer.poll()
                }
            }

            _gnssDetailFlow.value = GnssDetail(satellites = satList.sortedByDescending { it.cn0 })
        }
    }

    fun consumeMinSnr(): Float {
        val min = minSnrAccumulator
        minSnrAccumulator = 100f
        return if (min > 99f) averageSnr else min
    }

    fun getSnrSamples(fromTs: Long, toTs: Long): List<Pair<Long, Float>> {
        return snrBuffer.filter { it.first in fromTs..toTs }
    }

    fun setPollingInterval(intervalMs: Long) {
        if (pollIntervalFlow.value != intervalMs) {
            Timber.d("GPS: Polling interval changed to ${intervalMs}ms")
            pollIntervalFlow.value = intervalMs
        }
    }

    fun reviveGps() {
        Timber.w("GPS: Hardware revival triggered")
        try {
            locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "delete_aiding_data", null)
            locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_xtra_injection", null)
            locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_time_injection", null)
        } catch (e: Exception) {
            Timber.e(e, "GPS: Extra command injection failed")
        }
        
        val currentInterval = pollIntervalFlow.value
        pollIntervalFlow.value = currentInterval + 1
        pollIntervalFlow.value = currentInterval
    }

    @SuppressLint("MissingPermission")
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getLocationFlow(): Flow<Location> = channelFlow {
        try {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Timber.e(e, "GPS: Failed to register GNSS callback")
        }

        val locationJob = launch {
            pollIntervalFlow.flatMapLatest { interval ->
                callbackFlow {
                    val fusedCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            try {
                                result.lastLocation?.let { trySend(it) }
                            } catch (e: Exception) {
                            }
                        }
                    }

                    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                        .setMinUpdateIntervalMillis(interval / 2)
                        .setMinUpdateDistanceMeters(0.0f)
                        .setWaitForAccurateLocation(true)
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
                        } catch (e: Exception) {}
                    }
                }
            }.collect { 
                send(it)
            }
        }

        awaitClose {
            locationJob.cancel()
            try {
                locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
            } catch (e: Exception) {
            }
        }
    }
}
