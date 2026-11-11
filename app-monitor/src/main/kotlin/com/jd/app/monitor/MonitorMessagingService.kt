package com.jd.app.monitor

import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.LocationRowWire
import com.jd.core.network.SignalingClient
import com.jd.core.shared.SystemLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Receives the paired tracker's alarm rows as high-priority pushes, the fast second road beside the relay
 * connection, which a phone may delay in the background (ISSUE-NOV10.8-O86). The row goes to [PushedRows] for
 * [MonitorAlarmEngine], the only place that raises alarms; a push that finds the session dead starts it.
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
        val row = remoteMessage.data[ROW_KEY]?.let(LocationRowWire::decodeText)
        if (row == null) {
            SystemLogger.w("Push", "A push without a readable row was ignored")
            return
        }
        // Firebase calls this on a worker thread, so reading the stored pairing may block.
        val trackerId = runBlocking { identityManager.targetId.first() }
        if (!pushedRows.offer(row, trackerId, nowMs)) {
            SystemLogger.w("Push", "A pushed row of '${row.deviceId}' was ignored: it is not the paired tracker")
            return
        }
        SystemLogger.i("Push", pushArrivalLine(row, nowMs, remoteMessage.priority == RemoteMessage.PRIORITY_HIGH))

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
    }
}
