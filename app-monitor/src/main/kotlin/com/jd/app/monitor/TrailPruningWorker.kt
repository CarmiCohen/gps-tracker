package com.jd.app.monitor

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jd.core.shared.SystemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * TrailPruningWorker: Bounds Monitor trail storage (ISSUE-NOV5.2-O14): thins the Sensor's and this phone's own trail
 * by age and keeps each under [MAX_STORED_TRAIL_POINTS] ([MonitorRepository.thinTrails], ISSUE-NOV27.1-O168).
 */
@HiltWorker
class TrailPruningWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MonitorRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val removed = repository.thinTrails()
            SystemLogger.i("Pruning", "Trail cleanup finished. Rows removed: $removed")
            Result.success()
        } catch (e: Exception) {
            SystemLogger.e("Pruning", "Pruning failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "trail_pruning"
        private const val INTERVAL_MINUTES = 15L

        /** Safe on every app start: an existing schedule is kept, so the interval does not restart. */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<TrailPruningWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES).build()
            )
        }
    }
}
