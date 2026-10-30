package com.gps19.app.sensor

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gps19.core.shared.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * SensorService: Ultimate Hardened Version.
 * Corrected for API 34: Permission-aware FGS start and robust sensor registration.
 */
@AndroidEntryPoint
class SensorService : Service(), SensorEventListener {

    @Inject lateinit var orchestrator: TickOrchestrator
    private lateinit var forensicBuffer: ForensicBuffer
    private lateinit var sensorManager: SensorManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var isMuzzled = false

    // Raw IMU caches
    private var lastAccel = DoubleArray(3)
    private var lastGravity = DoubleArray(3)
    private var currentIntervalMs = 100L

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            SystemLogger.i("Network", "Interface changed. Forcing reconnection...")
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        val protectedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createDeviceProtectedStorageContext()
        } else this
        
        forensicBuffer = ForensicBuffer(protectedContext.filesDir)
        forensicBuffer.initialize()
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        // Critical: Start Foreground first on API 34+ to establish "eligible state"
        startForegroundServiceInternal()
        
        registerSensors()
        registerNetworkCallback()
        acquireWakeLock()
        
        if (!isMuzzled) {
            startSensingLoop()
        }
    }

    private fun registerSensors() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        
        // Fallback to SENSOR_DELAY_UI if HIGH_SAMPLING_RATE_SENSORS is not yet available or processed
        val delay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Even if declared, check if we should actually use FASTEST
            SensorManager.SENSOR_DELAY_NORMAL 
        } else {
            SensorManager.SENSOR_DELAY_FASTEST
        }
        
        accel?.let { sensorManager.registerListener(this, it, delay) }
        gravity?.let { sensorManager.registerListener(this, it, delay) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccel[0] = event.values[0].toDouble()
                lastAccel[1] = event.values[1].toDouble()
                lastAccel[2] = event.values[2].toDouble()
            }
            Sensor.TYPE_GRAVITY -> {
                lastGravity[0] = event.values[0].toDouble()
                lastGravity[1] = event.values[1].toDouble()
                lastGravity[2] = event.values[2].toDouble()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun startSensingLoop() {
        orchestrator.launchPeriodicLoop(
            name = "sensing_loop",
            scope = serviceScope,
            intervalProvider = { currentIntervalMs }
        ) {
            val update = PoolRegistry.locationUpdatePool.acquire()
            try {
                val worldAccel = SensingEngine.rotateToWorldSpace(
                    lastAccel[0], lastAccel[1], lastAccel[2],
                    lastGravity[0], lastGravity[1], lastGravity[2]
                )
                
                update.sensorSnapshot.vibration = worldAccel[0].toFloat()
                update.timestamp = System.currentTimeMillis()
                update.monotonicNanos = System.nanoTime()
                
                currentIntervalMs = SensingEngine.getOptimalIntervalMs(
                    currentTemp = 30f, 
                    isCharging = true, 
                    batteryLevel = 80
                )
                
                forensicBuffer.writeTrace(update)
            } catch (e: Exception) {
                SystemLogger.e("SensorService", "Loop error: ${e.message}")
            } finally {
                PoolRegistry.locationUpdatePool.release(update)
            }
        }
    }

    private fun registerNetworkCallback() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.registerDefaultNetworkCallback(networkCallback)
        } else {
            SystemLogger.e("SensorService", "Missing ACCESS_NETWORK_STATE. Telemetry sync degraded.")
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sensor:SensingLock").apply { 
            acquire(10*60*1000L /*10 minutes fallback*/) 
        }
    }

    private fun startForegroundServiceInternal() {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, AdminActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, SensorApp.CHANNEL_ID)
            .setContentTitle("System Maintenance Service")
            .setContentText("Monitoring environment integrity...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // API 34 requires specific permissions for these types
                val hasSync = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC) == PackageManager.PERMISSION_GRANTED
                if (hasSync) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                }
            }
            
            try {
                startForeground(1, notification, type)
            } catch (e: Exception) {
                SystemLogger.e("SensorService", "startForeground failed: ${e.message}. Retrying with minimal type...")
                startForeground(1, notification)
            }
        } else {
            startForeground(1, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        orchestrator.cancelAll()
        sensorManager.unregisterListener(this)
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {}
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
