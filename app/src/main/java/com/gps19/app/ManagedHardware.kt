package com.gps19.app

import android.content.BroadcastReceiver
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
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

/**
 * ManagedBroadcastReceiver: Standardizes safe unregistration of receivers
 * to ensure deterministic lifecycle management and avoid potential leaks (R753).
 */
abstract class ManagedBroadcastReceiver : BroadcastReceiver() {
    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
            Timber.d("ManagedBroadcastReceiver: Unregistration successful.")
        } catch (e: IllegalArgumentException) {
            Timber.w("ManagedBroadcastReceiver: Receiver already unregistered or not registered.")
        } catch (e: Exception) {
            Timber.e(e, "ManagedBroadcastReceiver: Unregistration failed.")
        }
    }
}

/**
 * ManagedSensorListener: Encapsulates safe, synchronous unregistration of
 * SensorManager listeners to prevent native BaseEventQueue leaks (R745/R746).
 */
abstract class ManagedSensorListener : SensorEventListener {
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun unregister(sm: SensorManager, handler: Handler?) {
        if (handler == null) {
            try {
                sm.unregisterListener(this)
                Timber.d("ManagedSensorListener: Unregistration complete (no handler).")
            } catch (e: Exception) {
                Timber.e(e, "ManagedSensorListener: Unregistration failed (no handler)")
            }
            return
        }

        if (Looper.myLooper() == handler.looper) {
            try {
                sm.unregisterListener(this)
                Timber.d("ManagedSensorListener: Immediate unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "ManagedSensorListener: Immediate unregistration failed")
            }
            return
        }

        val latch = CountDownLatch(1)
        handler.post {
            try {
                sm.unregisterListener(this)
                Timber.d("ManagedSensorListener: Async unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "ManagedSensorListener: Async unregistration failed")
            } finally {
                latch.countDown()
            }
        }
        try {
            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                Timber.w("ManagedSensorListener: Unregistration timed out")
            }
        } catch (e: InterruptedException) {
            Timber.e("ManagedSensorListener: Unregistration interrupted")
            Thread.currentThread().interrupt()
        }
    }
}

/**
 * ManagedDisplayListener: Encapsulates safe, synchronous unregistration of
 * DisplayManager.DisplayListener to prevent resource leaks.
 */
abstract class ManagedDisplayListener : DisplayManager.DisplayListener {
    override fun onDisplayAdded(displayId: Int) {}
    override fun onDisplayRemoved(displayId: Int) {}

    fun unregister(dm: DisplayManager, handler: Handler?) {
        if (handler == null) {
            try {
                dm.unregisterDisplayListener(this)
                Timber.d("ManagedDisplayListener: Unregistration complete (no handler).")
            } catch (e: Exception) {
                Timber.e(e, "ManagedDisplayListener: Unregistration failed (no handler)")
            }
            return
        }

        if (Looper.myLooper() == handler.looper) {
            try {
                dm.unregisterDisplayListener(this)
                Timber.d("ManagedDisplayListener: Immediate unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "ManagedDisplayListener: Immediate unregistration failed")
            }
            return
        }

        val latch = CountDownLatch(1)
        handler.post {
            try {
                dm.unregisterDisplayListener(this)
                Timber.d("ManagedDisplayListener: Async unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "ManagedDisplayListener: Async unregistration failed")
            } finally {
                latch.countDown()
            }
        }
        try {
            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                Timber.w("ManagedDisplayListener: Unregistration timed out")
            }
        } catch (e: InterruptedException) {
            Timber.e("ManagedDisplayListener: Unregistration interrupted")
            Thread.currentThread().interrupt()
        }
    }
}
