package com.gps19.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication
import androidx.work.Configuration
import androidx.work.WorkManager

/**
 * HiltTestRunner: Custom test runner to enable Hilt in instrumentation tests.
 * Manually initializes WorkManager to unblock components like BootReceiver 
 * that depend on it during test execution.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        val app = super.newApplication(cl, HiltTestApplication::class.java.name, context)
        try {
            WorkManager.initialize(app, Configuration.Builder().build())
        } catch (e: Exception) {
            // WorkManager may already be initialized in some test environments
        }
        return app
    }
}
