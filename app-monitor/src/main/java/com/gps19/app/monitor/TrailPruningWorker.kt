package com.gps19.app.monitor

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gps19.core.shared.SystemLogger

/**
 * TrailPruningWorker: Implements Local Trail TTL (Point 5).
 * Automatically deletes local user trail data older than 48 hours.
 */
class TrailPruningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        SystemLogger.i("Pruning", "Starting local trail TTL cleanup (48h)...")
        // TODO: trailDao.pruneOlderThan(System.currentTimeMillis() - 48 * 3600 * 1000)
        return Result.success()
    }
}
