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
 * v8.9.91:
 * - Issue #005 Hardening: Moved osmdroid path configuration to a synchronous block 
 *   before load() to ensure zero getPackageName() calls during early initialization.
 * v8.9.90:
 * - Issue #005: Advanced log spillage hardening. Manually set osmdroid Base/Cache 
 *   paths using static strings to eliminate repetitive getPackageName() lookups.
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

        // Issue #005: Deep silence for osmdroid
        // Rationale: We MUST set these synchronously before any OsmDroid component 
        // can trigger the default path discovery logic which calls getPackageName().
        val osmConfig = OsmConfig.getInstance()
        osmConfig.userAgentValue = "GpsTracker/8.9.91"
        
        val baseDir = File(filesDir, "osmdroid")
        if (!baseDir.exists()) baseDir.mkdirs()
        osmConfig.osmdroidBasePath = baseDir
        
        val cacheDir = File(baseDir, "tiles")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        osmConfig.osmdroidTileCache = cacheDir

        // Background the actual preference loading as it involves I/O
        GlobalScope.launch(Dispatchers.IO) {
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
