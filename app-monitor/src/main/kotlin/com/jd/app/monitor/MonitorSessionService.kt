package com.jd.app.monitor

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.network.peerDeliveries
import com.jd.core.shared.EventLog
import com.jd.core.shared.ForensicIdentity
import com.jd.core.shared.LinkStats
import com.jd.core.shared.PeerEventCatchUp
import com.jd.core.shared.graphSampleTimes
import com.jd.core.shared.linkUpPercent
import com.jd.core.shared.linkChanges
import com.jd.core.shared.RelayConfig
import com.jd.core.shared.SystemLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    @Inject lateinit var eventLog: EventLog
    @Inject lateinit var graphs: MonitorGraphs

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
                peerDeliveries(dataStore.geofence, signalingClient.viewerStatusFlow).collect { config ->
                    val sent = signalingClient.sendGeofence(targetId, config)
                    SystemLogger.i("MonitorService", "Geofence rev ${config.revision} (${config.points.size} points, ${config.radiusMeters.toInt()} m) sent to $targetId: $sent")
                }
            }
            // The Sensor's detection switches and limits are set here and sent the same way (ISSUE-NOV10.8-O83).
            launch {
                peerDeliveries(dataStore.alertSettings.map { it.sensorBehaviour() }, signalingClient.viewerStatusFlow).collect { behaviour ->
                    val sent = signalingClient.sendSensorBehaviour(targetId, behaviour)
                    SystemLogger.i("MonitorService", "Sensor behaviour rev ${behaviour.revision} sent to $targetId: $sent ($behaviour)")
                }
            }
            // The Sensor answers with what its phone setup lacks; the screen warns from the client's copy (ISSUE-NOV10.8-O83 part 2).
            launch {
                signalingClient.sensorSetupFlow.filterNotNull().collect { report ->
                    SystemLogger.i("MonitorService", "Sensor setup report from ${report.trackerId}: missing ${report.missing}, faults ${report.faults}, behaviour rev ${report.behaviourRevision}")
                }
            }

            // The Monitor's events list also holds the paired Sensor's events (ISSUE-NOV10.8-O84); those it missed while
            // offline it asks for, however many (ISSUE-NOV15.4-O98 part 4).
            launch {
                var catchUp: PeerEventCatchUp? = null
                signalingClient.sensorEventsFlow.filter { it.trackerId == targetId }.collect { batch ->
                    // Read before the first batch merges, so its newest events do not hide the ones missed before them.
                    val sync = catchUp
                        ?: PeerEventCatchUp(PeerEventCatchUp.syncedFrom(eventLog.events.value, targetId), System::currentTimeMillis).also { catchUp = it }
                    eventLog.merge(batch.events)
                    sync.received(batch)?.let { afterMs ->
                        val sent = signalingClient.requestSensorEvents(targetId, afterMs)
                        SystemLogger.i("MonitorService", "Sensor events missed: asked for those after $afterMs: $sent")
                    }
                }
            }
            launch {
                signalingClient.isConnected.linkChanges().collect { relayLinkEvent(it).let { event -> eventLog.record(event.level, event.message) } }
            }

            // The paired Sensor's samples arrive while the Graphs screen asks for them; this phone samples its own link (ISSUE-NOV10.8-O84 part 3).
            launch {
                signalingClient.graphSamplesFlow.filter { it.trackerId == targetId }.collect { graphs.sensor.add(it.samples) }
            }
            launch {
                var previousLink: LinkStats? = null
                var previousMs = 0L
                graphSampleTimes().collect { timeMs ->
                    val link = signalingClient.linkStats.value
                    graphs.own.add(monitorGraphSample(timeMs, linkUpPercent(previousLink, previousMs, link, timeMs), link.latencyMs, repository.takeRowCounts()))
                    previousLink = link
                    previousMs = timeMs
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
