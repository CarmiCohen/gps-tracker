package com.gps19.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import com.gps19.core.engine.TimeProvider
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration as OsmConfig
import timber.log.Timber
import javax.inject.Inject
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * GpsApplication: Application entry point and global dependency management.
 * v8.9.51:
 * - Issue #456: Resilience Hardening. Scheduled MaintenanceWorker on startup 
 *   as the 3rd layer of the High-Resilience Watchdog system. (Formerly #366-W)
 * v8.9.2:
 * - Issue 182: Synchronized source headers with v8.9.2 baseline.
 */
@HiltAndroidApp
class GpsApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GpsApplicationEntryPoint {
        fun logManager(): LogManager
        fun networkManagerWrapper(): RemoteUpdateWrapper
        fun repository(): MainRepository
        fun timeProvider(): TimeProvider
    }

    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Issue #456: Layer 3 Watchdog - WorkManager persistence
        MaintenanceWorker.schedule(this)

        // Issue 146: Move OsmDroid config to background to prevent main thread stall
        GlobalScope.launch(Dispatchers.IO) {
            OsmConfig.getInstance().load(this@GpsApplication, PreferenceManager.getDefaultSharedPreferences(this@GpsApplication))
            OsmConfig.getInstance().userAgentValue = "GpsTracker/${BuildConfig.VERSION_NAME} (${packageName})"
        }

        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (priority >= Log.ERROR) {
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            this@GpsApplication,
                            GpsApplicationEntryPoint::class.java
                        )
                        val logManager = entryPoint.logManager()
                        val fullMessage = if (tag != null) "[$tag] $message" else message
                        val suffix = t?.let { ": ${it.stackTraceToString().take(500)}" } ?: ""
                        logManager.logServiceEvent("CRITICAL ERROR: $fullMessage$suffix", true)
                    } catch (e: Exception) {
                    }
                }
            }
        })

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.tag("CRASH").e(throwable, "Uncaught Exception in thread ${thread.name}")
            Thread.sleep(200) 
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
