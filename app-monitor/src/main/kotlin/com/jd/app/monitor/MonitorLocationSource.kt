package com.jd.app.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import com.jd.core.ui.toFixReading
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Streams the Monitor device's own fixes, starting from the freshest last-known fix.
 * Callers must hold a location permission before collecting [fixes].
 */
class MonitorLocationSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(): Boolean = LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun fixes(): Flow<LocalFix> = callbackFlow {
        val locationManager = context.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { it in locationManager.allProviders }

        var currentBest: Location? = null

        providers.mapNotNull { locationManager.getLastKnownLocation(it) }
            .sortedBy { it.time }
            .forEach { loc ->
                if (isBetterLocation(loc, currentBest)) {
                    currentBest = loc
                    trySend(loc.toLocalFix())
                }
            }

        val listener = LocationListenerCompat { location ->
            if (isBetterLocation(location, currentBest)) {
                currentBest = location
                trySend(location.toLocalFix())
            }
        }
        providers.forEach {
            locationManager.requestLocationUpdates(
                it, UPDATE_INTERVAL_MS, UPDATE_DISTANCE_M, listener, Looper.getMainLooper()
            )
        }
        awaitClose { locationManager.removeUpdates(listener) }
    }

    private fun Location.toLocalFix() = LocalFix(MapPosition(latitude, longitude), toFixReading())

    companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val UPDATE_INTERVAL_MS = 10_000L
        private const val UPDATE_DISTANCE_M = 10f
        const val TWO_MINUTES_MS = 120_000L

        /**
         * Determines whether a new [location] fix is better than the [currentBestLocation].
         * Prevents a coarse Network fix from replacing a fresh, more accurate GPS fix (ISSUE-NOV6.1-O28).
         */
        fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
            if (currentBestLocation == null) return true

            val timeDeltaMs = if (location.elapsedRealtimeNanos > 0L && currentBestLocation.elapsedRealtimeNanos > 0L) {
                (location.elapsedRealtimeNanos - currentBestLocation.elapsedRealtimeNanos) / 1_000_000L
            } else {
                location.time - currentBestLocation.time
            }

            val isSignificantlyNewer = timeDeltaMs > TWO_MINUTES_MS
            val isSignificantlyOlder = timeDeltaMs < -TWO_MINUTES_MS
            val isNewer = timeDeltaMs > 0

            if (isSignificantlyNewer) return true
            if (isSignificantlyOlder) return false

            val locationAccuracy = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE
            val currentAccuracy = if (currentBestLocation.hasAccuracy()) currentBestLocation.accuracy else Float.MAX_VALUE
            val accuracyDelta = (locationAccuracy - currentAccuracy).toInt()

            val isMoreAccurate = accuracyDelta < 0
            val isLessAccurate = accuracyDelta > 0
            val isSignificantlyLessAccurate = accuracyDelta > 200

            val isFromSameProvider = location.provider == currentBestLocation.provider

            return when {
                isMoreAccurate -> true
                isNewer && !isLessAccurate -> true
                isNewer && !isSignificantlyLessAccurate && isFromSameProvider -> true
                else -> false
            }
        }
    }
}
