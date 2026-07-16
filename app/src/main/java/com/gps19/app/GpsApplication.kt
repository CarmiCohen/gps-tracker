package com.gps19.app

import android.app.Application
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import android.content.Context
import com.gps19.core.engine.TimeProvider
import org.osmdroid.config.Configuration as OsmConfig
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * GpsApplication: Application entry point and global dependency management.
 * v9.5.0:
 * - Issue #503: Hilt Removal. Reverted to manual Dependency Injection via AppContainer.
 * July.16.24:
 * - Issue #526: Landing Page Hang. Offloaded osmdroid and WorkManager initialization to background threads.
 */
class GpsApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return when (workerClassName) {
                        MaintenanceWorker::class.java.name -> MaintenanceWorker(
                            appContext,
                            workerParameters,
                            container.mainRepository,
                            container.timeProvider
                        )
                        BootServiceStartWorker::class.java.name -> BootServiceStartWorker(
                            appContext,
                            workerParameters,
                            container.mainRepository
                        )
                        else -> null
                    }
                }
            })
            .build()

    override fun onCreate() {
        super.onCreate()
        
        container = AppContainer(this)
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Issue #526: Offload non-critical initialization to background thread
        GlobalScope.launch(Dispatchers.IO) {
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
        }

        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (priority >= Log.ERROR) {
                    try {
                        val fullMessage = if (tag != null) "[$tag] $message" else message
                        val suffix = t?.let { ": ${it.stackTraceToString().take(500)}" } ?: ""
                        // LogManager is lazy-loaded, so this might trigger its creation if an error occurs early
                        container.logManager.logServiceEvent("CRITICAL ERROR: $fullMessage$suffix", true)
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
