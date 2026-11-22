package com.jd.app.monitor

import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.PushMessage
import com.jd.core.network.PushWire
import com.jd.core.network.SignalingClient
import com.jd.core.network.isPairedTracker
import com.jd.core.shared.SystemLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Receives, as high-priority pushes, the paired tracker's alarm rows and the relay's notice that the tracker went
 * silent: the fast second road beside the relay connection, which a phone may delay in the background, and the only
 * road for what the relay notices by itself (ISSUE-NOV10.8-O86). Each goes to [PushedRows] for [MonitorAlarmEngine],
 * the only place that raises alarms; a push that finds the session dead starts it.
 * The Sensor's changed phone setup report comes at normal priority and goes to [SensorSetupReports], which notifies
 * a new warning without the session (ISSUE-NOV10.8-O86, the O83 warnings).
 * [PushWire] reads every push, and one pairing check comes before the dispatch (SIMP-NOV18.12-S1).
 */
@AndroidEntryPoint
class MonitorMessagingService : FirebaseMessagingService() {

    @Inject lateinit var signalingClient: SignalingClient
    @Inject lateinit var identityManager: ForensicIdentityManager
    @Inject lateinit var pushedRows: PushedRows
    @Inject lateinit var setupReports: SensorSetupReports

    override fun onNewToken(token: String) {
        signalingClient.registerPushToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val nowMs = System.currentTimeMillis()
        val isHighPriority = remoteMessage.priority == RemoteMessage.PRIORITY_HIGH
        val message = PushWire.decode(remoteMessage.data)
        if (message == null) {
            SystemLogger.w("Push", "A push without a readable row, notice or setup report was ignored")
            return
        }
        // Firebase calls this on a worker thread, so reading the stored pairing may block.
        if (!isPairedTracker(message.trackerId, runBlocking { identityManager.targetId.first() })) {
            SystemLogger.w("Push", "A pushed ${message.kind} of '${message.trackerId}' was ignored: it is not the paired tracker")
            return
        }
        when (message) {
            is PushMessage.Setup -> {
                runBlocking { setupReports.take(message.report, "push") }
                return
            }
            is PushMessage.Row -> {
                pushedRows.offer(message.row, nowMs)
                SystemLogger.i("Push", pushArrivalLine(message.row, nowMs, isHighPriority))
            }
            is PushMessage.Silence -> {
                pushedRows.offerSilence(message.silence, nowMs)
                SystemLogger.i("Push", silenceArrivalLine(message.silence, isHighPriority))
            }
        }

        // Android lets a high-priority push start a foreground service from the background; one that it delivered
        // at normal priority may not, and then the row is raised only if the session is already running.
        try {
            ContextCompat.startForegroundService(this, Intent(this, MonitorSessionService::class.java))
        } catch (e: IllegalStateException) {
            SystemLogger.w("Push", "The push was not allowed to start the session: ${e.message}")
        }
    }
}

/** The name of a pushed message in the log lines. */
private val PushMessage.kind: String
    get() = when (this) {
        is PushMessage.Row -> "row"
        is PushMessage.Silence -> "silence notice"
        is PushMessage.Setup -> "setup report"
    }
