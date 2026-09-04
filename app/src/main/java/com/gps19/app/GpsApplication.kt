
package com.gps19.app

import android.app.Application
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import androidx.work.WorkManager
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
 * Sep.03.132:
 * - Issue #901 Fix: Hardened trimCaches() to preserve "pkg" and "uid" identity 
 *   tokens. Previous implementation cleared these tokens during memory pressure, 
 *   leading to persistent IPC log spam regression (R759).
 * Sep.01.24:
 * - Issue #892 Fix: Manually initialize WorkManager in onCreate() to resolve 
 *   IllegalStateException in BootReceiver, as auto-init is disabled in manifest.
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
         * R759: Direct shadow-cache authority for high-frequency identifiers.
         */
        val PACKAGE_NAME: String get() = stringCache.get("pkg") ?: ""
        val MY_UID: Int get() = intCache.get("uid") ?: 0

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

    /**
     * Issue #876 Fix: Direct cache query to avoid lazy initialization race 
     * conditions. Ensures framework log spam is eliminated as soon as 
     * the cache is populated in onCreate() (R759).
     */
    override fun getPackageName(): String {
        return stringCache.get("pkg") ?: super.getPackageName()
    }

    override fun onCreate() {
        super.onCreate()
        
        // Populate caches immediately to silence framework IPC logs
        stringCache.put("pkg", super.getPackageName())
        intCache.put("uid", Process.myUid())

        // Issue #892: Manual WorkManager initialization required because 
        // WorkManagerInitializer was removed from manifest to support custom factories.
        WorkManager.initialize(this, workManagerConfiguration)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (priority >= Log.ERROR) {
                    try {
                        val fullMessage = if (tag != null) "[$tag] $message" else message
                        val sanitizedMessage = ForensicSanitizer.sanitizeMessage(fullMessage)
                        val suffix = t?.let { ": ${ForensicSanitizer.sanitizeStackTrace(it, 500)}" } ?: ""
                        logManager.logServiceEvent("CRITICAL ERROR: $sanitizedMessage$suffix", true)
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
                // Issue #901: Preserve identity tokens to prevent log spam regression
                val pkg = stringCache.get("pkg")
                val uid = intCache.get("uid")
                
                stringCache.clear()
                intCache.clear()
                
                pkg?.let { stringCache.put("pkg", it) }
                uid?.let { intCache.put("uid", it) }

                System.gc()
            } catch (e: Exception) {
                Timber.e(e, "Issue #656: Cache trim failed")
            }
        }
    }
}
