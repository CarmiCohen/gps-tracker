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
 * TrailPruningWorker: Bounds Monitor trail storage (ISSUE-NOV5.2-O14).
 * Keeps the newest [MAX_STORED_TRAIL_POINTS] rows of each tracker and deletes
 * local user rows older than 48 hours (Point 5).
 */
@HiltWorker
class TrailPruningWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val trailDao: TrailDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            trailDao.pruneLocalTrail(System.currentTimeMillis() - LOCAL_TRAIL_TTL_MS)
            val removed = trailDao.getTrackerIds().sumOf { trackerId ->
                trailDao.pruneOldestRows(trackerId, MAX_STORED_TRAIL_POINTS)
            }
            SystemLogger.i("Pruning", "Trail cleanup finished. Rows removed: $removed")
            Result.success()
        } catch (e: Exception) {
            SystemLogger.e("Pruning", "Pruning failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "trail_pruning"
        private const val LOCAL_TRAIL_TTL_MS = 48 * 3600 * 1000L
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
