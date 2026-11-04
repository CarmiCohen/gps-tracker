package com.jd.app.monitor

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.shared.SystemLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * MonitorSessionService: Active only while the user is actively tracking.
 * Hardened Revol30.3: Manages SignalingClient connection and triggers MonitorRepository ingestion.
 */
@AndroidEntryPoint
class MonitorSessionService : Service() {

    @Inject lateinit var signalingClient: SignalingClient
    @Inject lateinit var identityManager: ForensicIdentityManager
    @Inject lateinit var repository: MonitorRepository
    @Inject lateinit var trailDao: TrailDao
    @Inject lateinit var dataStore: MonitorDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                MonitorApp.ALARM_CHANNEL_ID,
                "Critical Theft Alerts",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for high-priority theft and sensor violation alerts."
            }
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, MonitorApp.ALARM_CHANNEL_ID)
            .setContentTitle("Active Tracking Session")
            .setContentText("Monitoring remote sensor traces...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notification)
        }
        
        startSignaling()
        
        return START_NOT_STICKY
    }

    private fun startSignaling() {
        serviceScope.launch {
            val targetId = identityManager.targetId.first() ?: return@launch
            val selfId = identityManager.selfId.first()
            val url = dataStore.relayUrl.first() ?: "http://relay.revolution.io"
            
            // Get last sequence ID for initial backfill handshake (R-ID 768)
            val lastSeq = trailDao.getLastSequenceId(targetId) ?: 0L
            
            SystemLogger.i("MonitorService", "Connecting to relay for target $targetId (Last Seq: $lastSeq)")
            signalingClient.connect(
                url = url,
                deviceId = selfId,
                secret = targetId, // Monitor joins the target's forensic room
                lastSequenceId = lastSeq
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        signalingClient.disconnect()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
