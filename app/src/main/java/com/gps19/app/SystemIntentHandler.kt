package com.gps19.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SIMP-OCT13.30-S1: Activity Result Contracts Consolidation.
 * Decouples MainActivity from system-level intent orchestration.
 */
@Singleton
class SystemIntentHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getAppInfoIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun getExactAlarmIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else null
    }

    fun getBatteryExemptionIntent(): Intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun getOverlayPermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else null
    }

    fun launchAppInfo(launcher: ActivityResultLauncher<Intent>) {
        launcher.launch(getAppInfoIntent())
    }

    fun launchExactAlarmSetting(launcher: ActivityResultLauncher<Intent>) {
        getExactAlarmIntent()?.let { launcher.launch(it) }
    }

    fun launchBatteryExemptionSetting(launcher: ActivityResultLauncher<Intent>) {
        launcher.launch(getBatteryExemptionIntent())
    }

    fun launchOverlayPermissionSetting(launcher: ActivityResultLauncher<Intent>) {
        getOverlayPermissionIntent()?.let { launcher.launch(it) }
    }
}
