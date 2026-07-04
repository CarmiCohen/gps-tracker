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
import java.io.File

/**
 * GpsApplication: Application entry point and global dependency management.
 * v8.9.90:
 * - Issue #005: Advanced log spillage hardening. Manually set osmdroid Base/Cache 
 *   paths using static strings to eliminate repetitive getPackageName() lookups.
 * v8.9.89:
 * - Issue #005: Deep hardening for osmdroid. Set userAgentValue BEFORE load() 
 *   to ensure zero getPackageName() calls during map setup.
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
        // Issue #005: Deep silence for osmdroid
        GlobalScope.launch(Dispatchers.IO) {
            val osmConfig = OsmConfig.getInstance()
            
            // Rationale: Set static identifiers BEFORE load() to preempt automatic system lookups.
            osmConfig.userAgentValue = "GpsTracker/8.9.90"
            
            // Rationale: Manually define paths to avoid internal library getPackageName() queries.
            val baseDir = File(filesDir, "osmdroid")
            if (!baseDir.exists()) baseDir.mkdirs()
            osmConfig.osmdroidBasePath = baseDir
            
            val cacheDir = File(baseDir, "tiles")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            osmConfig.osmdroidTileCache = cacheDir

            osmConfig.load(this@GpsApplication, PreferenceManager.getDefaultSharedPreferences(this@GpsApplication))
            osmConfig.isDebugMode = false
            osmConfig.isDebugTileProviders = false
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
