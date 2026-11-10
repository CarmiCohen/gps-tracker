package com.jd.app.sensor

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tells when the Sensor phone itself rings or plays a notification or alarm, so that its own vibration motor is not
 * taken for a knock (ISSUE-NOV9.2-O62; on the A15 a call shakes the phone by 0.11 g). Needs no permission: the audio
 * mode says an incoming call is ringing, and the active playbacks' usages say a ringtone, notification, or alarm sounds.
 * A notification that only vibrates, without sound, is not visible this way; when the user has granted notification
 * access, [SilentNotificationListener] reports it and the phone counts as vibrating for [NOTIFICATION_VIBRATION_MS]
 * (ISSUE-NOV9.4-O65).
 *
 * The audio mode is followed by a listener from Android 12, and polled every second before it. [onChange] runs on the
 * given handler's thread, or on the poll's coroutine.
 */
class OwnVibrationWatch(context: Context, private val onChange: (Boolean) -> Unit) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    @Volatile private var mode = AudioManager.MODE_NORMAL
    @Volatile private var playbackUsages: List<Int> = emptyList()
    @Volatile private var notificationUntil = 0L // SystemClock.elapsedRealtime() at which the vibrating notification ends.
    private var isReported = false // Guarded by this.
    private var modeListener: Any? = null
    private var pollJob: Job? = null
    private var handler: Handler? = null
    private val notificationEnd = Runnable { update() }

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            playbackUsages = configs.usages()
            update()
        }
    }

    fun start(scope: CoroutineScope, handler: Handler) {
        this.handler = handler
        OwnNotificationVibration.listener = {
            handler.post {
                notificationUntil = SystemClock.elapsedRealtime() + NOTIFICATION_VIBRATION_MS
                handler.removeCallbacks(notificationEnd)
                handler.postDelayed(notificationEnd, NOTIFICATION_VIBRATION_MS)
                update()
            }
        }
        mode = audioManager.mode
        playbackUsages = audioManager.activePlaybackConfigurations.usages()
        audioManager.registerAudioPlaybackCallback(playbackCallback, handler)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val listener = AudioManager.OnModeChangedListener { newMode ->
                mode = newMode
                update()
            }
            audioManager.addOnModeChangedListener({ handler.post(it) }, listener)
            modeListener = listener
        } else {
            pollJob = scope.launch {
                while (isActive) {
                    mode = audioManager.mode
                    update()
                    delay(MODE_POLL_MS)
                }
            }
        }
        update()
    }

    fun stop() {
        OwnNotificationVibration.listener = null
        handler?.removeCallbacks(notificationEnd)
        handler = null
        audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (modeListener as? AudioManager.OnModeChangedListener)?.let { audioManager.removeOnModeChangedListener(it) }
        }
        modeListener = null
        pollJob?.cancel()
        pollJob = null
    }

    @Synchronized
    private fun update() {
        val now = isOwnVibrationSource(mode, playbackUsages, SystemClock.elapsedRealtime() < notificationUntil)
        if (now == isReported) return
        isReported = now
        onChange(now)
    }

    private fun List<AudioPlaybackConfiguration>.usages() = map { it.audioAttributes.usage }

    private companion object {
        const val MODE_POLL_MS = 1_000L
        /** How long a silent notification counts as vibrating; Android's default notification pattern lasts under 1 s. */
        const val NOTIFICATION_VIBRATION_MS = 3_000L
    }
}

/** Audio usages whose sound comes with the phone's own vibration: ringtone, notifications, alarm clock. */
private val OWN_VIBRATION_USAGES = setOf(
    AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
    AudioAttributes.USAGE_NOTIFICATION,
    AudioAttributes.USAGE_NOTIFICATION_EVENT,
    AudioAttributes.USAGE_ALARM
)

/**
 * True while an incoming call rings (audio mode `MODE_RINGTONE`), a ringtone, notification, or alarm plays, or a silent
 * notification vibrates ([isNotificationVibrating], ISSUE-NOV9.4-O65). A call in progress does not count: the phone does
 * not vibrate then, and a knock during it must still alert.
 */
fun isOwnVibrationSource(mode: Int, playbackUsages: Collection<Int>, isNotificationVibrating: Boolean = false): Boolean =
    isNotificationVibrating || mode == AudioManager.MODE_RINGTONE || playbackUsages.any { it in OWN_VIBRATION_USAGES }
