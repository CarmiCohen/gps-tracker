package com.gps19.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.CustomTestApplication
import dagger.hilt.components.SingletonComponent

/**
 * GpsTestBaseApplication: Base application for instrumented tests.
 * Implements Configuration.Provider to support WorkManager initialization.
 * Oct.27.6:
 * - ISSUE-OCT27.6-F1: Refactored to use EntryPoint for HiltWorkerFactory to 
 *   circumvent Hilt's restriction on @Inject fields in @CustomTestApplication 
 *   base classes.
 */
open class GpsTestBaseApplication : Application(), Configuration.Provider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkManagerEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(EntryPoints.get(this, WorkManagerEntryPoint::class.java).workerFactory())
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        // WorkManager initialization is deferred until getWorkManagerConfiguration is called by the system,
        // or we can explicitly initialize it here if needed. 
        // Room/Hilt environment in tests usually needs this to be ready.
    }
}

/**
 * GpsTestApplication: Triggers Hilt to generate a test application 
 * based on GpsTestBaseApplication.
 */
@CustomTestApplication(GpsTestBaseApplication::class)
interface GpsTestApplication
