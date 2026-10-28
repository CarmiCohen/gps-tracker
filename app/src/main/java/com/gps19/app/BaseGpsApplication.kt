package com.gps19.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * BaseGpsApplication: Shared application logic for both production and test builds.
 * Oct.28.1:
 * - SIMP-OCT27.4-S1: Consolidate WorkManager configuration to reduce duplication.
 */
abstract class BaseGpsApplication : Application(), Configuration.Provider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkManagerEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(EntryPoints.get(this, WorkManagerEntryPoint::class.java).workerFactory())
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()
}
