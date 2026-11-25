package com.jd.app.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventLog
import com.jd.core.shared.SystemLogger
import com.jd.core.ui.isNetworkAvailable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.seconds

/**
 * Raises the Monitor alarms while a tracker is paired: it keeps the [AlarmBook], sounds the siren while any alarm
 * is unacknowledged, and shows the full-screen alarm notification. Started by [MonitorSessionService], so alarms
 * work with the screen off or the app in the background.
 *
 * It reads the tracker's rows from the relay connection and, for alarm rows, from [PushedRows] as well
 * (ISSUE-NOV10.8-O86). The same row may come by both; the second copy changes nothing, see [newerReading].
 * The relay's pushed notice that the tracker went silent raises the same tracker-offline alarm as its own count.
 */
@Singleton
class MonitorAlarmEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signalingClient: SignalingClient,
    private val identityManager: ForensicIdentityManager,
    private val dataStore: MonitorDataStore,
    private val siren: SirenPlayer,
    private val pushedRows: PushedRows,
    private val eventLog: EventLog
) {
    private val _alarms = MutableStateFlow(AlarmBook())
    val alarms: StateFlow<AlarmBook> = _alarms.asStateFlow()

    private val sirenTest = MutableStateFlow(false)
    val isSirenOn: StateFlow<Boolean> = siren.isPlaying

    private val sirenTimedOut = MutableStateFlow(false)
    private var sirenTimeoutJob: Job? = null

    private val acknowledgements = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private var watchJob: Job? = null

    private fun localInternetFlow(): Flow<Boolean> = flow {
        while (true) {
            emit(isNetworkAvailable(context))
            delay(1.seconds)
        }
    }.distinctUntilChanged()

    /** Starts watching in [scope]; a second call while watching does nothing. */
    @Synchronized
    fun watch(scope: CoroutineScope) {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            alarmBooks(
                target = identityManager.targetId,
                telemetry = merge(signalingClient.telemetryFlow, pushedRows.fresh(System::currentTimeMillis)),
                silences = pushedRows.freshSilences(System::currentTimeMillis),
                settings = dataStore.alertSettings,
                ticks = secondTicks(),
                acknowledgements = acknowledgements,
                isRelayConnected = signalingClient.isConnected,
                isLocalInternet = localInternetFlow(),
                clock = System::currentTimeMillis
            ).onEach { book ->
                val previous = _alarms.value
                if (book.pending.map { it.type } != previous.pending.map { it.type }) {
                    SystemLogger.w("Alarm", "Pending alarms: ${book.pending.joinToString { it.type.name }}")
                }
                alarmEvents(previous, book).forEach { eventLog.record(it.level, it.message) }
                _alarms.value = book
                if (book.needsSiren != previous.needsSiren) sirenEpisode(book.needsSiren, this@launch)
                showNotification(book)
            }.launchIn(this)

            combine(
                _alarms.map { it.needsSiren }.distinctUntilChanged(),
                sirenTimedOut,
                sirenTest,
                dataStore.alertSettings
            ) { needsSiren, timedOut, testing, settings ->
                if ((needsSiren && !timedOut) || testing) settings.sirenSound() else null
            }.distinctUntilChanged().onEach { sound ->
                if (sound != null) siren.play(sound) else siren.stop()
            }.launchIn(this)
        }
    }

    /**
     * Starts or ends a siren episode: each one sounds for at most 30 s. The timed-out flag is cleared only when an
     * episode starts, never when one ends, so the siren never sees "needed and not timed out" from a book that is
     * about to be acknowledged (ISSUE-NOV10.7-O75: an acknowledgement after the 30 s used to chirp the siren).
     */
    private fun sirenEpisode(needsSiren: Boolean, watching: CoroutineScope) {
        sirenTimeoutJob?.cancel()
        if (!needsSiren) return
        sirenTimedOut.value = false
        sirenTimeoutJob = watching.launch {
            delay(30.seconds)
            sirenTimedOut.value = true
            eventLog.record(EventLevel.DETAIL, "Siren stopped after 30 s")
        }
    }

    /** Stops watching and silences everything, as when the tracker is unpaired. */
    @Synchronized
    fun stop() {
        watchJob?.cancel()
        watchJob = null
        sirenTimeoutJob?.cancel()
        sirenTimedOut.value = false
        sirenTest.value = false
        siren.stop()
        _alarms.value = AlarmBook()
        NotificationManagerCompat.from(context).cancel(ALARM_NOTIFICATION_ID)
    }

    fun acknowledge() {
        acknowledgementEvent(_alarms.value)?.let { eventLog.record(it.level, it.message) }
        acknowledgements.tryEmit(Unit)
    }

    fun toggleSirenTest() {
        sirenTest.value = !sirenTest.value
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(book: AlarmBook) {
        val manager = NotificationManagerCompat.from(context)
        if (book.pending.isEmpty()) {
            manager.cancel(ALARM_NOTIFICATION_ID)
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) return
        val openAlarm = PendingIntent.getActivity(
            context, 0,
            Intent(context, AlarmOverlayActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, MonitorApp.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("ALARM")
            .setContentText(book.pending.joinToString { it.type.title })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAlarm)
            .setFullScreenIntent(openAlarm, true)
            .build()
        manager.notify(ALARM_NOTIFICATION_ID, notification)
    }

    private companion object {
        const val ALARM_NOTIFICATION_ID = 2001
    }
}
