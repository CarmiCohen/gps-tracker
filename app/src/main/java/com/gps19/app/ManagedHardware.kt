package com.gps19.app

import android.content.BroadcastReceiver
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.location.GnssStatus
import android.location.LocationManager
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
 * Aug.28.06:
 * - Issue #755 Hardening: Increased timeout to 2000ms to allow for higher 
 *   Main Looper congestion during teardown.
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
            if (!latch.await(2000, TimeUnit.MILLISECONDS)) {
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
            Tasks.await(task, 2000, TimeUnit.MILLISECONDS)
            Timber.d("ManagedLocationCallback: Unregistration complete.")
        } catch (e: Exception) {
            Timber.e(e, "ManagedLocationCallback: Unregistration failed or timed out")
        }
    }
}

/**
 * ManagedGnssStatusCallback: Encapsulates safe, synchronous unregistration of
 * GnssStatus.Callback to prevent native BaseEventQueue leaks (R755).
 */
abstract class ManagedGnssStatusCallback : GnssStatus.Callback() {
    fun unregister(lm: LocationManager, handler: Handler?) {
        if (handler == null) {
            try {
                lm.unregisterGnssStatusCallback(this)
                Timber.d("ManagedGnssStatusCallback: Unregistration complete (no handler).")
            } catch (e: Exception) {
                Timber.e(e, "ManagedGnssStatusCallback: Unregistration failed (no handler)")
            }
            return
        }

        if (Looper.myLooper() == handler.looper) {
            try {
                lm.unregisterGnssStatusCallback(this)
                Timber.d("ManagedGnssStatusCallback: Immediate unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "ManagedGnssStatusCallback: Immediate unregistration failed")
            }
            return
        }

        val latch = CountDownLatch(1)
        handler.post {
            try {
                lm.unregisterGnssStatusCallback(this)
                Timber.d("ManagedGnssStatusCallback: Async unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "ManagedGnssStatusCallback: Async unregistration failed")
            } finally {
                latch.countDown()
            }
        }
        try {
            if (!latch.await(2000, TimeUnit.MILLISECONDS)) {
                Timber.w("ManagedGnssStatusCallback: Unregistration timed out")
            }
        } catch (e: InterruptedException) {
            Timber.e("ManagedGnssStatusCallback: Unregistration interrupted")
            Thread.currentThread().interrupt()
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
            if (!latch.await(2000, TimeUnit.MILLISECONDS)) {
                Timber.w("ManagedSensorListener: Unregistration timed out")
            }
        } catch (e: InterruptedException) {
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
            if (!latch.await(2000, TimeUnit.MILLISECONDS)) {
                Timber.w("ManagedDisplayListener: Unregistration timed out")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
