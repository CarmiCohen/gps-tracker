package com.gps19.app

import android.app.Application
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import android.content.Context
import android.content.ComponentCallbacks2
import androidx.hilt.work.HiltWorkerFactory
import org.osmdroid.config.Configuration as OsmConfig
import org.osmdroid.tileprovider.modules.SqlTileWriter
import timber.log.Timber
import javax.inject.Inject
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * GpsApplication: Application entry point and global dependency management.
 * Aug.01.00:
 * - Issue #664: Forensic Audit: Startup Davey Stalls. Deferring osmdroid setup by 3s 
 *   to clear the main-thread critical path during first-frame rendering.
 * July.31.00:
 * - Issue #656: userfaultfd mitigation. Added aggressive onTrimMemory handling.
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

        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (priority >= Log.ERROR) {
                    try {
                        val fullMessage = if (tag != null) "[$tag] $message" else message
                        val suffix = t?.let { ": ${it.stackTraceToString().take(500)}" } ?: ""
                        logManager.logServiceEvent("CRITICAL ERROR: $fullMessage$suffix", true)
                    } catch (e: Exception) {}
                }
            }
        })

        // Issue #664: Startup ANR Hardening - Defer I/O intensive setup to avoid Davey stalls
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Issue #456: Layer 3 Watchdog - WorkManager persistence
                MaintenanceWorker.schedule(this@GpsApplication)

                // Issue #664: Defer heavy osmdroid and SharedPreferences access to avoid 
                // contention with DataStore and Compose initialization.
                delay(3000)

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
                
                Timber.d("Issue #664: Deferred startup initialization complete.")
            } catch (e: Exception) {
                Timber.e(e, "Issue #664: Deferred startup failed")
            }
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.tag("CRASH").e(throwable, "Uncaught Exception in thread ${thread.name}")
            Thread.sleep(200) 
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            trimCaches()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        trimCaches()
    }

    private fun trimCaches() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                System.gc()
            } catch (e: Exception) {
                Timber.e(e, "Issue #656: Cache trim failed")
            }
        }
    }
}
