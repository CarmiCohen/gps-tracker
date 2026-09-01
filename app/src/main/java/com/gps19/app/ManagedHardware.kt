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
 * ManagedUnregistrationHelper: Centralized logic for safe, synchronous unregistration
 * of hardware listeners. Ensures consistent 4000ms timeouts and fallback patterns (R889).
 */
object ManagedUnregistrationHelper {
    fun safeUnregister(
        label: String,
        handler: Handler?,
        action: () -> Unit
    ) {
        Timber.d("$label: Starting unregistration...")

        if (handler == null || Looper.myLooper() == handler.looper) {
            try {
                action()
                Timber.d("$label: Immediate unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "$label: Immediate unregistration failed")
            }
            return
        }

        val latch = CountDownLatch(1)
        val posted = handler.post {
            try {
                action()
                Timber.d("$label: Async unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "$label: Async unregistration failed")
            } finally {
                latch.countDown()
            }
        }

        if (!posted) {
            Timber.w("$label: Failed to post unregistration to handler thread")
            try {
                action()
                Timber.d("$label: Fallback unregistration complete.")
            } catch (e: Exception) {
                Timber.e(e, "$label: Fallback unregistration failed")
            }
            return
        }

        try {
            if (!latch.await(4000, TimeUnit.MILLISECONDS)) {
                Timber.w("$label: Unregistration timed out. Forcing direct fallback.")
                try {
                    action()
                    Timber.d("$label: Timeout fallback unregistration complete.")
                } catch (e: Exception) {
                    Timber.e(e, "$label: Timeout fallback unregistration failed")
                }
            }
        } catch (e: InterruptedException) {
            Timber.e("$label: Unregistration interrupted")
            Thread.currentThread().interrupt()
        }
    }
}

/**
 * ManagedNetworkCallback: Encapsulates safe, synchronous unregistration of 
 * ConnectivityManager.NetworkCallback to prevent native BaseEventQueue leaks (R750/R887).
 */
abstract class ManagedNetworkCallback : ConnectivityManager.NetworkCallback() {
    fun unregister(cm: ConnectivityManager) {
        ManagedUnregistrationHelper.safeUnregister(
            "ManagedNetworkCallback",
            Handler(Looper.getMainLooper())
        ) { cm.unregisterNetworkCallback(this) }
    }
}

/**
 * ManagedLocationCallback: Encapsulates safe, synchronous unregistration of
 * FusedLocationProvider location updates to prevent native leaks (R747/R748).
 */
abstract class ManagedLocationCallback : LocationCallback() {
    fun unregister(client: FusedLocationProviderClient) {
        Timber.d("ManagedLocationCallback: Starting unregistration...")
        try {
            val task = client.removeLocationUpdates(this)
            Tasks.await(task, 4000, TimeUnit.MILLISECONDS)
            Timber.d("ManagedLocationCallback: Unregistration complete.")
        } catch (e: Exception) {
            Timber.e(e, "ManagedLocationCallback: Unregistration failed or timed out")
        }
    }
}

/**
 * ManagedGnssStatusCallback: Encapsulates safe, synchronous unregistration of
 * GnssStatus.Callback to prevent native BaseEventQueue leaks (R755/R887).
 */
abstract class ManagedGnssStatusCallback : GnssStatus.Callback() {
    fun unregister(lm: LocationManager, handler: Handler?) {
        ManagedUnregistrationHelper.safeUnregister(
            "ManagedGnssStatusCallback",
            handler
        ) { lm.unregisterGnssStatusCallback(this) }
    }
}

/**
 * ManagedBroadcastReceiver: Standardizes safe unregistration of receivers
 * to ensure deterministic lifecycle management and avoid potential leaks (R753).
 */
abstract class ManagedBroadcastReceiver : BroadcastReceiver() {
    fun unregister(context: Context) {
        Timber.d("ManagedBroadcastReceiver: Starting unregistration...")
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
 * SensorManager listeners to prevent native BaseEventQueue leaks (R745/R746/R888).
 */
abstract class ManagedSensorListener : SensorEventListener {
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun unregister(sm: AndroidSensorManager, handler: Handler?) {
        performUnregistration(sm, null, handler)
    }

    /**
     * Unregisters a specific sensor while maintaining the listener for others (R888).
     */
    fun unregister(sm: AndroidSensorManager, sensor: Sensor, handler: Handler?) {
        performUnregistration(sm, sensor, handler)
    }

    private fun performUnregistration(sm: AndroidSensorManager, sensor: Sensor?, handler: Handler?) {
        val label = if (sensor == null) "global" else "specific (${sensor.name})"
        ManagedUnregistrationHelper.safeUnregister(
            "ManagedSensorListener ($label)",
            handler
        ) {
            if (sensor == null) sm.unregisterListener(this)
            else sm.unregisterListener(this, sensor)
        }
    }
}

/**
 * ManagedDisplayListener: Encapsulates safe, synchronous unregistration of
 * DisplayManager.DisplayListener to prevent resource leaks (R887).
 */
abstract class ManagedDisplayListener : DisplayManager.DisplayListener {
    override fun onDisplayAdded(displayId: Int) {}
    override fun onDisplayRemoved(displayId: Int) {}

    fun unregister(dm: DisplayManager, handler: Handler?) {
        ManagedUnregistrationHelper.safeUnregister(
            "ManagedDisplayListener",
            handler
        ) { dm.unregisterDisplayListener(this) }
    }
}

typealias AndroidSensorManager = SensorManager
