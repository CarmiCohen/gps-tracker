package com.gps19.app

import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.tasks.Tasks
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * ManagedNetworkCallback: Encapsulates safe, synchronous unregistration of 
 * ConnectivityManager.NetworkCallback to prevent native BaseEventQueue leaks (R750).
 */
abstract class ManagedNetworkCallback : ConnectivityManager.NetworkCallback() {
    fun unregister(cm: ConnectivityManager) {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            try {
                cm.unregisterNetworkCallback(this)
                Timber.d("ManagedNetworkCallback: Unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "ManagedNetworkCallback: Unregistration failed")
            } finally {
                latch.countDown()
            }
        }
        try {
            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                Timber.w("ManagedNetworkCallback: Unregistration timed out")
            }
        } catch (e: InterruptedException) {
            Timber.e("ManagedNetworkCallback: Unregistration interrupted")
            Thread.currentThread().interrupt()
        }
    }
}

/**
 * ManagedLocationCallback: Encapsulates safe, synchronous unregistration of
 * FusedLocationProvider location updates to prevent native leaks (R747/R748).
 */
abstract class ManagedLocationCallback : LocationCallback() {
    fun unregister(client: FusedLocationProviderClient) {
        try {
            val task = client.removeLocationUpdates(this)
            Tasks.await(task, 1000, TimeUnit.MILLISECONDS)
            Timber.d("ManagedLocationCallback: Unregistration complete.")
        } catch (e: Exception) {
            Timber.e(e, "ManagedLocationCallback: Unregistration failed or timed out")
        }
    }
}
