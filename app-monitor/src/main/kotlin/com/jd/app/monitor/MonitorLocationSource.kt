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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Streams the Monitor device's own position, starting from the freshest last-known fix.
 * Callers must hold a location permission before collecting [positions].
 */
class MonitorLocationSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(): Boolean = LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun positions(): Flow<MapPosition> = callbackFlow {
        val locationManager = context.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { it in locationManager.allProviders }

        providers.mapNotNull { locationManager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?.let { trySend(it.toMapPosition()) }

        val listener = LocationListenerCompat { trySend(it.toMapPosition()) }
        providers.forEach {
            locationManager.requestLocationUpdates(
                it, UPDATE_INTERVAL_MS, UPDATE_DISTANCE_M, listener, Looper.getMainLooper()
            )
        }
        awaitClose { locationManager.removeUpdates(listener) }
    }

    private fun Location.toMapPosition() = MapPosition(latitude, longitude)

    companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val UPDATE_INTERVAL_MS = 10_000L
        private const val UPDATE_DISTANCE_M = 10f
    }
}
