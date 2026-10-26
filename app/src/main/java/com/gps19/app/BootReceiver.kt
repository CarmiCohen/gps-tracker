package com.gps19.app

import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.content.BroadcastReceiver
import androidx.work.OutOfQuotaPolicy
import timber.log.Timber

/**
 * BootReceiver: Triggered when the device restarts.
 * Oct.26.0:
 * - Isolation Hardening: Removed worker definition to BootServiceStartWorker.kt.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || 
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Timber.d("Boot detected, scheduling expedited service start via WorkManager")
            val workRequest = OneTimeWorkRequestBuilder<BootServiceStartWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
