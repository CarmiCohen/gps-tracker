package com.jd.app.monitor

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.shared.ForensicIdentity
import com.jd.core.shared.RelayConfig
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
    @Inject lateinit var alarmEngine: MonitorAlarmEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var signalingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        // startForeground needs its channel even where MonitorApp did not start, such as under a test application.
        createMonitorNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, MonitorApp.SESSION_CHANNEL_ID)
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
        alarmEngine.watch(serviceScope)
        
        return START_STICKY
    }

    /** Runs once per service; a repeated start command must not add another relay collector (ISSUE-NOV8.5-O50). */
    private fun startSignaling() {
        if (signalingJob?.isActive == true) return
        signalingJob = serviceScope.launch {
            val targetId = identityManager.targetId.first() ?: return@launch
            identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_MONITOR) // ISSUE-NOV8.101-O59
            val selfId = identityManager.selfId.first()

            // The relay also pushes the tracker's alarm rows to this phone (ISSUE-NOV10.8-O86).
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { signalingClient.registerPushToken(it) }
                .addOnFailureListener { SystemLogger.w("MonitorService", "No push token, so alarms arrive by the relay connection only: ${it.message}") }

            // The tracker enforces the user's geofence; the Monitor only delivers it (ISSUE-NOV8.13-O51).
            launch {
                geofenceDeliveries(dataStore.geofence, signalingClient.viewerStatusFlow).collect { config ->
                    val sent = signalingClient.sendGeofence(targetId, config)
                    SystemLogger.i("MonitorService", "Geofence rev ${config.revision} (${config.points.size} points, ${config.radiusMeters.toInt()} m) sent to $targetId: $sent")
                }
            }

            // Get last sequence ID for initial backfill handshake (R-ID 768)
            val lastSeq = trailDao.getLastSequenceId(targetId) ?: 0L

            dataStore.relayUrl.collect { rawUrl ->
                val url = RelayConfig.resolve(rawUrl)
                SystemLogger.i("MonitorService", "Connecting to relay $url for target $targetId (Last Seq: $lastSeq)")
                signalingClient.connect(
                    url = url,
                    deviceId = selfId,
                    secret = targetId, // Monitor joins the target's forensic room
                    lastSequenceId = lastSeq
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmEngine.stop()
        signalingClient.disconnect()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
