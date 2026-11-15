package com.jd.app.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jd.core.shared.SystemLogger

/**
 * BootReceiver: starts [SensorService] after a reboot and after an app update, both of which leave it stopped.
 * Updated Nov1.0: Added permission guard to prevent SecurityException on API 34+ (R-ID 10).
 * Updated Nov12.7: also on `MY_PACKAGE_REPLACED` (ISSUE-NOV12.6-O92), with the same permission list as the app and
 * the service. Both broadcasts are exempt from Android 12's background start restriction; location and the microphone
 * may still be refused from the background, which the service handles with its restart notification (ISSUE-NOV8.103-O60).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!startsSensorOn(intent.action)) return
        val reason = if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) "App update" else "System reboot"
        if (hasRequiredSensorPermissions(context)) {
            SystemLogger.i("BootReceiver", "$reason detected. Permissions verified. Starting SensorService...")
            startSensorService(context)
        } else {
            SystemLogger.w("BootReceiver", "$reason detected. Critical permissions missing. Service start aborted.")
        }
    }

    companion object {
        /** The broadcasts after which the Sensor starts itself (ISSUE-NOV12.6-O92); the manifest declares the same. */
        val START_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)

        fun startsSensorOn(action: String?): Boolean = action in START_ACTIONS
    }
}
