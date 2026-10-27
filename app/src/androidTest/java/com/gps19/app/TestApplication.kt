package com.gps19.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.testing.CustomTestApplication
import javax.inject.Inject

/**
 * GpsTestBaseApplication: Base application for instrumented tests.
 * Implements Configuration.Provider to support WorkManager initialization.
 * Oct.27.2:
 * - ISSUE-OCT27.2-F1: Integrated HiltWorkerFactory and explicit WorkManager 
 *   initialization to support @HiltWorker instantiation in tests.
 */
open class GpsTestBaseApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Ensure WorkManager is initialized with the Hilt factory
        WorkManager.initialize(this, workManagerConfiguration)
    }
}

/**
 * GpsTestApplication: Triggers Hilt to generate a test application 
 * based on GpsTestBaseApplication.
 */
@CustomTestApplication(GpsTestBaseApplication::class)
interface GpsTestApplication
