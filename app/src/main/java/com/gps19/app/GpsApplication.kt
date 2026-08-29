package com.gps19.app

import android.app.Application
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import android.content.Context
import android.content.ComponentCallbacks2
import android.os.Process
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
import java.util.concurrent.atomic.AtomicBoolean
import com.gps19.core.engine.ShadowCache

/**
 * GpsApplication: Application entry point and global dependency management.
 * Aug.29.13:
 * - Issue #759 Hardening: Added MY_UID shadow-cache to eliminate repetitive 
 *   Process.myUid() IPC calls that trigger system diagnostic logs (R759).
 * Aug.28.10:
 * - Issue #758 Optimization: Implemented pre-warming of SqlTileWriter on IO 
 *   thread and added isOsmReady signal (R758).
 */
@HiltAndroidApp
class GpsApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope
    @Inject lateinit var logManager: LogManager

    companion object {
        private val stringCache = ShadowCache<String, String>(100)
        private val intCache = ShadowCache<String, Int>(10)

        /**
         * Shadow-cache for the package name to avoid triggering Samsung's repetitive 
         * 'getPackageName' logcat spam.
         */
        val PACKAGE_NAME: String by lazy { stringCache.getOrPut("pkg") { "" } }

        /**
         * Shadow-cache for the process UID to avoid triggering system-level IPC 
         * diagnostic logs during high-frequency permission checks.
         */
        val MY_UID: Int by lazy { intCache.getOrPut("uid") { Process.myUid() } }
        
        /**
         * Issue #758: Signal for map hydration to start.
         */
        val isOsmReady = AtomicBoolean(false)

        /**
         * Generic access for high-frequency string lookups that trigger OS diagnostic logs.
         */
        fun getCachedString(key: String, provider: () -> String): String {
            return stringCache.getOrPut(key, provider)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        stringCache.put("pkg", super.getPackageName())
        intCache.put("uid", Process.myUid())

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

        applicationScope.launch(Dispatchers.IO) {
            try {
                val osmConfig = OsmConfig.getInstance()
                osmConfig.userAgentValue = "GpsTracker/8.10.10"
                
                val baseDir = File(filesDir, "osmdroid")
                if (!baseDir.exists()) baseDir.mkdirs()
                osmConfig.osmdroidBasePath = baseDir
                
                val cacheDir = File(baseDir, "tiles")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                osmConfig.osmdroidTileCache = cacheDir

                osmConfig.load(this@GpsApplication, PreferenceManager.getDefaultSharedPreferences(this@GpsApplication))
                osmConfig.isDebugMode = false
                osmConfig.isDebugTileProviders = false
                
                try {
                    SqlTileWriter()
                    Timber.d("Issue #758: SqlTileWriter pre-warmed on IO thread.")
                } catch (e: Exception) {
                    Timber.e(e, "Issue #758: SqlTileWriter pre-warm failed")
                }
                
                isOsmReady.set(true)

                MaintenanceWorker.schedule(this@GpsApplication)
                delay(3000)
                
                Timber.d("Issue #721: Shadow-cache synchronized with LRU strategy.")
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
                stringCache.clear()
                intCache.clear()
                System.gc()
            } catch (e: Exception) {
                Timber.e(e, "Issue #656: Cache trim failed")
            }
        }
    }
}
