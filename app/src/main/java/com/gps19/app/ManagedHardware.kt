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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.tasks.Tasks
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * ManagedUnregistrationHelper: Centralized logic for safe unregistration
 * of hardware listeners. 
 * Sep.14.10 Audit (#1022):
 * - Simplification Idea #17: Converted to fire-and-forget asynchronous 
 *   unregistration. Removed CountDownLatch wait blocks to ensure rapid 
 *   teardown and prevent stalling signaling disconnects during mode switches.
 */
object ManagedUnregistrationHelper {
    fun safeUnregister(
        label: String,
        handler: Handler?,
        action: () -> Unit
    ) {
        Timber.d("$label: Starting unregistration...")

        if (handler == null || Looper.myLooper() == handler.looper) {
            val startTime = SystemClock.elapsedRealtime()
            try {
                action()
                Timber.d("$label: Immediate unregistration complete in ${SystemClock.elapsedRealtime() - startTime}ms.")
            } catch (e: Exception) {
                Timber.e(e, "$label: Immediate unregistration failed")
            }
            return
        }

        // Idea #17 Implementation: Fire-and-forget to avoid blocking the caller (ConnectivitySuite).
        handler.post {
            val taskStartTime = SystemClock.elapsedRealtime()
            try {
                action()
                val duration = SystemClock.elapsedRealtime() - taskStartTime
                Timber.d("$label: Async unregistration complete in ${duration}ms.")
            } catch (e: Exception) {
                Timber.e(e, "$label: Async unregistration failed after ${SystemClock.elapsedRealtime() - taskStartTime}ms")
            }
        }
    }
}

/**
 * ManagedNetworkCallback: Encapsulates safe unregistration of 
 * ConnectivityManager.NetworkCallback.
 */
abstract class ManagedNetworkCallback : ConnectivityManager.NetworkCallback() {
    fun unregister(cm: ConnectivityManager, handler: Handler? = Handler(Looper.getMainLooper())) {
        ManagedUnregistrationHelper.safeUnregister(
            "ManagedNetworkCallback",
            handler
        ) { 
            try {
                cm.unregisterNetworkCallback(this) 
            } catch (e: Exception) {
                Timber.w("ManagedNetworkCallback: Unregister failed (likely already gone)")
            }
        }
    }
}

/**
 * ManagedLocationCallback: Encapsulates safe unregistration of
 * FusedLocationProvider location updates.
 */
abstract class ManagedLocationCallback : LocationCallback() {
    fun unregister(client: FusedLocationProviderClient, handler: Handler?) {
        ManagedUnregistrationHelper.safeUnregister(
            "ManagedLocationCallback",
            handler
        ) {
            // Sep.14.10: Removed Tasks.await and unused task variable (Idea #17).
            client.removeLocationUpdates(this)
            Timber.d("ManagedLocationCallback: Task submitted.")
        }
    }
}

/**
 * ManagedGnssStatusCallback: Encapsulates safe unregistration of
 * GnssStatus.Callback.
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
 * ManagedLocationListener: Encapsulates safe unregistration of
 * android.location.LocationListener.
 */
abstract class ManagedLocationListener : android.location.LocationListener {
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in API 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    fun unregister(lm: LocationManager, handler: Handler?) {
        ManagedUnregistrationHelper.safeUnregister(
            "ManagedLocationListener",
            handler
        ) { lm.removeUpdates(this) }
    }
}

/**
 * ManagedBroadcastReceiver: Standardizes safe unregistration of receivers.
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
 * ManagedSensorListener: Encapsulates safe unregistration of
 * SensorManager listeners.
 */
abstract class ManagedSensorListener : SensorEventListener {
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun unregister(sm: AndroidSensorManager, handler: Handler?) {
        performUnregistration(sm, null, handler)
    }

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
 * ManagedDisplayListener: Encapsulates safe unregistration of
 * DisplayManager.DisplayListener.
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
