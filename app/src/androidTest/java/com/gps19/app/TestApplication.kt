package com.gps19.app

import dagger.hilt.android.testing.CustomTestApplication

/**
 * GpsTestBaseApplication: Base application for instrumented tests.
 * Oct.28.1:
 * - SIMP-OCT27.4-S1: Inherit from BaseGpsApplication to consolidate WorkManager config.
 * Oct.27.6:
 * - ISSUE-OCT27.6-F1: Refactored to use EntryPoint for HiltWorkerFactory.
 */
open class GpsTestBaseApplication : BaseGpsApplication() {
    override fun onCreate() {
        super.onCreate()
        // WorkManager initialization is deferred until getWorkManagerConfiguration is called by the system.
    }
}

/**
 * GpsTestApplication: Triggers Hilt to generate a test application 
 * based on GpsTestBaseApplication.
 */
@CustomTestApplication(GpsTestBaseApplication::class)
interface GpsTestApplication
