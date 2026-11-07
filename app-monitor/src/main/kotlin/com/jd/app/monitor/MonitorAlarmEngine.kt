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
import com.jd.core.shared.SystemLogger
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.seconds

/**
 * Raises the Monitor alarms while a tracker is paired: it keeps the [AlarmBook], sounds the siren while any alarm
 * is unacknowledged, and shows the full-screen alarm notification. Started by [MonitorSessionService], so alarms
 * work with the screen off or the app in the background.
 */
@Singleton
class MonitorAlarmEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signalingClient: SignalingClient,
    private val identityManager: ForensicIdentityManager,
    private val dataStore: MonitorDataStore,
    private val siren: SirenPlayer
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

    private fun isNetworkAvailable(ctx: Context): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    /** Starts watching in [scope]; a second call while watching does nothing. */
    @Synchronized
    fun watch(scope: CoroutineScope) {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            launch {
                _alarms.map { it.needsSiren }.distinctUntilChanged().collect { needsSiren ->
                    sirenTimeoutJob?.cancel()
                    if (needsSiren) {
                        sirenTimedOut.value = false
                        sirenTimeoutJob = scope.launch {
                            delay(30.seconds)
                            sirenTimedOut.value = true
                            SystemLogger.i("Alarm", "Siren auto-stopped after 30s timer (timer or acknowledge)")
                        }
                    } else {
                        sirenTimedOut.value = false
                    }
                }
            }

            alarmBooks(
                target = identityManager.targetId,
                telemetry = signalingClient.telemetryFlow,
                violations = signalingClient.violationFlow,
                settings = dataStore.alertSettings,
                ticks = secondTicks(),
                acknowledgements = acknowledgements,
                isRelayConnected = signalingClient.isConnected,
                isLocalInternet = localInternetFlow(),
                clock = System::currentTimeMillis
            ).onEach { book ->
                if (book.pending.map { it.type } != _alarms.value.pending.map { it.type }) {
                    SystemLogger.w("Alarm", "Pending alarms: ${book.pending.joinToString { it.type.name }}")
                }
                _alarms.value = book
                showNotification(book)
            }.launchIn(this)

            combine(
                _alarms.map { it.needsSiren }.distinctUntilChanged(),
                sirenTimedOut,
                sirenTest,
                dataStore.alertSettings,
                dataStore.sirenType
            ) { needsSiren, timedOut, testing, settings, waveType ->
                if ((needsSiren && !timedOut) || testing) settings.sirenSound(waveType) else null
            }.distinctUntilChanged().onEach { sound ->
                if (sound != null) siren.play(sound) else siren.stop()
            }.launchIn(this)
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
        sirenTimeoutJob?.cancel()
        sirenTimedOut.value = false
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
