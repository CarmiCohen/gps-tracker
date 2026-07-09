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
 * v9.3.6:
 * - Issue #058: Hilt Migration. Injected LogManager directly to remove 
 *   EntryPointAccessors from Timber tree.
 */
@HiltAndroidApp
class GpsApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var logManager: LogManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GpsApplicationEntryPoint {
        fun logManager(): LogManager
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
        val osmConfig = OsmConfig.getInstance()
        osmConfig.userAgentValue = "GpsTracker/8.9.91"
        
        val baseDir = File(filesDir, "osmdroid")
        if (!baseDir.exists()) baseDir.mkdirs()
        osmConfig.osmdroidBasePath = baseDir
        
        val cacheDir = File(baseDir, "tiles")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        osmConfig.osmdroidTileCache = cacheDir

        GlobalScope.launch(Dispatchers.IO) {
            osmConfig.load(this@GpsApplication, PreferenceManager.getDefaultSharedPreferences(this@GpsApplication))
            osmConfig.isDebugMode = false
            osmConfig.isDebugTileProviders = false
        }

        // Issue #058: Eliminated EntryPointAccessors by using injected logManager
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (priority >= Log.ERROR) {
                    try {
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
