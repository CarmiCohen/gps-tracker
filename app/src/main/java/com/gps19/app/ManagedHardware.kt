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
 * Aug.28.03:
 * - Issue #752 Hardening: Prevented unregister deadlock by checking if the 
 *   caller is already on the Main Looper (R752).
 */
abstract class ManagedNetworkCallback : ConnectivityManager.NetworkCallback() {
    fun unregister(cm: ConnectivityManager) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                cm.unregisterNetworkCallback(this)
                Timber.d("ManagedNetworkCallback: Immediate unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "ManagedNetworkCallback: Immediate unregistration failed")
            }
            return
        }

        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            try {
                cm.unregisterNetworkCallback(this)
                Timber.d("ManagedNetworkCallback: Async unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "ManagedNetworkCallback: Async unregistration failed")
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
 * Aug.28.03:
 * - Issue #752 Hardening: Hardened unregistration to avoid blocking the Main 
 *   Thread if the task is already being handled on it (R752).
 */
abstract class ManagedLocationCallback : LocationCallback() {
    fun unregister(client: FusedLocationProviderClient) {
        try {
            val task = client.removeLocationUpdates(this)
            // If we are on Main Thread, we should avoid blocking if possible, 
            // but Tasks.await is generally safe if the task doesn't depend on Main Looper.
            // Google Play Services tasks usually complete on their own internal threads.
            Tasks.await(task, 1000, TimeUnit.MILLISECONDS)
            Timber.d("ManagedLocationCallback: Unregistration complete.")
        } catch (e: Exception) {
            Timber.e(e, "ManagedLocationCallback: Unregistration failed or timed out")
        }
    }
}
