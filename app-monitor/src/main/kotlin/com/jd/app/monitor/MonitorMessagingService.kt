package com.jd.app.monitor

import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.LocationRowWire
import com.jd.core.network.SensorSilenceWire
import com.jd.core.network.SignalingClient
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
 */
@AndroidEntryPoint
class MonitorMessagingService : FirebaseMessagingService() {

    @Inject lateinit var signalingClient: SignalingClient
    @Inject lateinit var identityManager: ForensicIdentityManager
    @Inject lateinit var pushedRows: PushedRows

    override fun onNewToken(token: String) {
        signalingClient.registerPushToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val nowMs = System.currentTimeMillis()
        val isHighPriority = remoteMessage.priority == RemoteMessage.PRIORITY_HIGH
        val row = remoteMessage.data[ROW_KEY]?.let(LocationRowWire::decodeText)
        val silence = remoteMessage.data[NOTICE_KEY]?.let(SensorSilenceWire::decodeText)
        if (row == null && silence == null) {
            SystemLogger.w("Push", "A push without a readable row or notice was ignored")
            return
        }
        // Firebase calls this on a worker thread, so reading the stored pairing may block.
        val trackerId = runBlocking { identityManager.targetId.first() }
        if (row != null) {
            if (!pushedRows.offer(row, trackerId, nowMs)) {
                SystemLogger.w("Push", "A pushed row of '${row.deviceId}' was ignored: it is not the paired tracker")
                return
            }
            SystemLogger.i("Push", pushArrivalLine(row, nowMs, isHighPriority))
        } else if (silence != null) {
            if (!pushedRows.offerSilence(silence, trackerId, nowMs)) {
                SystemLogger.w("Push", "A silence notice of '${silence.trackerId}' was ignored: it is not the paired tracker")
                return
            }
            SystemLogger.i("Push", silenceArrivalLine(silence, isHighPriority))
        }

        // Android lets a high-priority push start a foreground service from the background; one that it delivered
        // at normal priority may not, and then the row is raised only if the session is already running.
        try {
            ContextCompat.startForegroundService(this, Intent(this, MonitorSessionService::class.java))
        } catch (e: IllegalStateException) {
            SystemLogger.w("Push", "The push was not allowed to start the session: ${e.message}")
        }
    }

    private companion object {
        /** The relay sends the row as JSON text under this data key. */
        const val ROW_KEY = "row"

        /** The relay sends what it noticed by itself as JSON text under this data key. */
        const val NOTICE_KEY = "notice"
    }
}
