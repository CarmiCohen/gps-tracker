package com.gps19.app

import android.app.Application
import androidx.work.Configuration
import dagger.hilt.android.testing.CustomTestApplication

/**
 * GpsTestBaseApplication: Base application for instrumented tests.
 * Implements Configuration.Provider to support WorkManager initialization.
 */
open class GpsTestBaseApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}

/**
 * GpsTestApplication: Triggers Hilt to generate a test application 
 * based on GpsTestBaseApplication.
 */
@CustomTestApplication(GpsTestBaseApplication::class)
interface GpsTestApplication
