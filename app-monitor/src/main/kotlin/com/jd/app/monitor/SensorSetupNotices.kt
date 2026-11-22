package com.jd.app.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.isPairedTracker
import com.jd.core.shared.SensorSetupReport
import com.jd.core.shared.SystemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * True when [candidate] replaces [current]: the first report, another tracker's, or one the Sensor sent no earlier.
 * The same report comes by the relay connection and by push, and a push held while the phone dozed may arrive after a
 * newer report (ISSUE-NOV10.8-O86). Reports of a Sensor before Nov18.12 have time 0, so the later arrival wins.
 */
fun isNewerSetupReport(current: SensorSetupReport?, candidate: SensorSetupReport): Boolean =
    current == null || current.trackerId != candidate.trackerId || candidate.sentAtMs >= current.sentAtMs

/** The ID of the Sensor phone warnings notification; the alarm notification is 2001. */
const val SENSOR_SETUP_NOTIFICATION_ID = 2002

/** What the setup notification does with a new report's warnings. */
enum class SetupNotice { SHOW, CANCEL, KEEP }

/**
 * The step for a report's [warnings] after the ones the notification already covered, [notified]. A warning not notified before shows the notification with all current warnings; no warning removes it; fewer or the
 * same warnings leave it as the user last saw or dismissed it (user decision 2026-11-18: each warning notifies once).
 */
fun setupNotice(warnings: List<String>, notified: Set<String>): SetupNotice = when {
    warnings.isEmpty() -> SetupNotice.CANCEL
    warnings.any { it !in notified } -> SetupNotice.SHOW
    else -> SetupNotice.KEEP
}

/**
 * The paired Sensor's newest setup report, from either road: the relay connection, while the session runs, and the
 * relay's push, which reaches a Monitor frozen in the background (ISSUE-NOV10.8-O86). A new warning is shown as a plain
 * notification on its own channel, never with the siren (user decision 2026-11-18); the amber line of the screen reads
 * [latest].
 */
@Singleton
class SensorSetupReports @Inject constructor(
    @ApplicationContext private val context: Context,
    private val identityManager: ForensicIdentityManager,
    private val dataStore: MonitorDataStore
) {
    private val _latest = MutableStateFlow<SensorSetupReport?>(null)
    val latest: StateFlow<SensorSetupReport?> = _latest.asStateFlow()

    private val mutex = Mutex()

    /** Takes [report], received by [road], when it is the paired tracker's and not older than the one held. */
    suspend fun take(report: SensorSetupReport, road: String) = mutex.withLock {
        if (!isPairedTracker(report.trackerId, identityManager.targetId.first())) {
            SystemLogger.w("SensorSetup", "A setup report of '${report.trackerId}' by $road was ignored: it is not the paired tracker")
            return@withLock
        }
        if (!isNewerSetupReport(_latest.value, report)) {
            SystemLogger.i("SensorSetup", "An older setup report by $road was ignored (sent at ${report.sentAtMs})")
            return@withLock
        }
        _latest.value = report
        SystemLogger.i(
            "SensorSetup",
            "Sensor setup report from ${report.trackerId} by $road: missing ${report.missing}, faults ${report.faults}, " +
                "behaviour rev ${report.behaviourRevision}"
        )
        val warnings = sensorSetupWarnings(report, dataStore.alertSettings.first())
        val notified = dataStore.notifiedSetupWarnings.first()
        when (setupNotice(warnings, notified)) {
            SetupNotice.SHOW -> showNotification(warnings)
            SetupNotice.CANCEL -> NotificationManagerCompat.from(context).cancel(SENSOR_SETUP_NOTIFICATION_ID)
            SetupNotice.KEEP -> Unit
        }
        if (warnings.toSet() != notified) dataStore.saveNotifiedSetupWarnings(warnings.toSet())
    }

    /** Forgets the report and removes its notification, as when the tracker is unpaired. */
    suspend fun forget() = mutex.withLock {
        _latest.value = null
        NotificationManagerCompat.from(context).cancel(SENSOR_SETUP_NOTIFICATION_ID)
        dataStore.saveNotifiedSetupWarnings(emptySet())
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(warnings: List<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            SystemLogger.w("SensorSetup", "No notification permission: the Sensor phone warnings show on the Monitor screen only")
            return
        }
        val open = PendingIntent.getActivity(
            context, SENSOR_SETUP_NOTIFICATION_ID, monitorOpenIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = warnings.joinToString("\n") { "- $it" } + "\nFix these on the Sensor phone: open the Sensor app, then PHONE SETUP."
        val notification = NotificationCompat.Builder(context, MonitorApp.SETUP_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Sensor phone needs attention")
            .setContentText(sensorSetupLineText(warnings.size))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(SENSOR_SETUP_NOTIFICATION_ID, notification)
    }
}
