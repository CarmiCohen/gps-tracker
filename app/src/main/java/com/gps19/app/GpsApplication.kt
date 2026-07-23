package com.gps19.app

import android.app.Application
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import org.osmdroid.config.Configuration as OsmConfig
import timber.log.Timber
import javax.inject.Inject
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * GpsApplication: Application entry point and global dependency management.
 * July.22.04:
 * - Hilt Hardening: Standardized dependency graph.
 * July.21.00:
 * - Build Hardening: Added missing HiltWorkerFactory import.
 * July.20.07:
 * - Issue #115: Startup Hardening. Migrated from GlobalScope to managed @ApplicationScope.
 */
@HiltAndroidApp
class GpsApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope
    @Inject lateinit var logManager: LogManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Standardized Timber logging for critical errors
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (priority >= Log.ERROR) {
                    try {
                        val fullMessage = if (tag != null) "[$tag] $message" else message
                        val suffix = t?.let { ": ${it.stackTraceToString().take(500)}" } ?: ""
                        logManager.logServiceEvent("CRITICAL ERROR: $fullMessage$suffix", true)
                    } catch (e: Exception) {
                        // Fail-safe to prevent logging loops
                    }
                }
            }
        })

        // Issue #115: Startup ANR Hardening - Offload I/O intensive setup to managed scope
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Issue #456: Layer 3 Watchdog - WorkManager persistence
                MaintenanceWorker.schedule(this@GpsApplication)

                // Issue #005: Deep silence for osmdroid
                val osmConfig = OsmConfig.getInstance()
                osmConfig.userAgentValue = "GpsTracker/8.9.91"
                
                val baseDir = File(filesDir, "osmdroid")
                if (!baseDir.exists()) baseDir.mkdirs()
                osmConfig.osmdroidBasePath = baseDir
                
                val cacheDir = File(baseDir, "tiles")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                osmConfig.osmdroidTileCache = cacheDir

                osmConfig.load(this@GpsApplication, PreferenceManager.getDefaultSharedPreferences(this@GpsApplication))
                osmConfig.isDebugMode = false
                osmConfig.isDebugTileProviders = false
                
                Timber.d("Issue #115: Managed startup initialization complete.")
            } catch (e: Exception) {
                Timber.e(e, "Issue #115: Managed startup failed")
            }
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.tag("CRASH").e(throwable, "Uncaught Exception in thread ${thread.name}")
            Thread.sleep(200) 
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
