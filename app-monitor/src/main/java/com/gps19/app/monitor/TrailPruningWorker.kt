package com.gps19.app.monitor

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gps19.core.shared.SystemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * TrailPruningWorker: Implements Local Trail TTL (Point 5).
 * Automatically deletes local user trail data older than 48 hours.
 * Hardened Revol30.3: Integrated with Hilt and Room TrailDao.
 */
@HiltWorker
class TrailPruningWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val trailDao: TrailDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val threshold = System.currentTimeMillis() - 48 * 3600 * 1000
        SystemLogger.i("Pruning", "Starting local trail TTL cleanup (Threshold: $threshold)...")
        
        return try {
            trailDao.pruneLocalTrail(threshold)
            Result.success()
        } catch (e: Exception) {
            SystemLogger.e("Pruning", "Pruning failed: ${e.message}")
            Result.retry()
        }
    }
}
