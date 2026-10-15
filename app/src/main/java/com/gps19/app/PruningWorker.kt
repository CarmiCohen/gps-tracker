package com.gps19.app

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.gps19.core.engine.MAINTENANCE_WORK_NAME
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * PruningWorker: Dedicated background task for database maintenance and log pruning.
 * Oct.13.8:
 * - Issue #SIMP-OCT12.18-S1: Pruning Scheduler. Initial implementation.
 * Oct.14.2:
 * - Fixed IllegalArgumentException: Removed backoff criteria for idle-mode job.
 */
@HiltWorker
class PruningWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MainRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val PRUNING_WORK_NAME = "Database_Pruning"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .setRequiresStorageNotLow(true)
                .build()

            // Note: Removed setBackoffCriteria to avoid crash with setRequiresDeviceIdle(true)
            val request = PeriodicWorkRequestBuilder<PruningWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PRUNING_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Timber.d("PRUNING: Scheduled periodic database maintenance.")
        }

        fun triggerNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<PruningWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${PRUNING_WORK_NAME}_NOW",
                ExistingWorkPolicy.REPLACE,
                request
            )
            Timber.d("PRUNING: Triggered immediate database maintenance.")
        }
    }

    override suspend fun doWork(): Result {
        Timber.i("PRUNING: Starting background database maintenance task.")
        return try {
            repository.proactivePruning()
            Timber.i("PRUNING: Database maintenance task completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "PRUNING: Database maintenance task failed.")
            Result.retry()
        }
    }
}
