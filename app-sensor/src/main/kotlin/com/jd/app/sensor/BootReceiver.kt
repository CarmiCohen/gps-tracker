package com.jd.app.sensor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.jd.core.shared.SystemLogger

/**
 * BootReceiver: Ensures the SensorService starts automatically after device reboot.
 * Updated Nov1.0: Added permission guard to prevent SecurityException on API 34+ (R-ID 10).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SystemLogger.i("BootReceiver", "System reboot detected. Checking permissions...")
            
            if (hasRequiredPermissions(context)) {
                SystemLogger.i("BootReceiver", "Permissions verified. Starting SensorService...")
                val serviceIntent = Intent(context, SensorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                SystemLogger.w("BootReceiver", "Critical permissions missing after boot. Service start aborted.")
            }
        }
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        return locationGranted && notificationGranted
    }
}
