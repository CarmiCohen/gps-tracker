package com.jd.app.monitor

import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.shared.DriveWatch
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventLog
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.activePlacement
import com.jd.core.shared.asksAfterDrive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** A drive of [trackerId]'s Sensor that ended with the car parked since [parkedSinceMs] (its clock). */
data class DriveEnd(val trackerId: String, val parkedSinceMs: Long)

/**
 * Asks after a drive whether to switch the seat placement (ISSUE-NOV26.0-O163 part 1): fed the paired Sensor's live rows
 * by [MonitorSessionService], so it hears a drive with the app in the background too. It never switches by itself; the
 * question waits in [pending] until answered, and goes to the Events list too. Nothing is asked while a car seat is in use.
 */
@Singleton
class DriveQuestion @Inject constructor(
    private val signalingClient: SignalingClient,
    private val identityManager: ForensicIdentityManager,
    private val dataStore: MonitorDataStore,
    private val eventLog: EventLog
) {
    private val _pending = MutableStateFlow<DriveEnd?>(null)
    val pending: StateFlow<DriveEnd?> = _pending.asStateFlow()

    private var watchJob: Job? = null

    /** Starts watching in [scope]; a second call while watching does nothing. */
    @Synchronized
    fun watch(scope: CoroutineScope) {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            driveEnds(signalingClient.telemetryFlow, { identityManager.targetId.first() }).collect { drive ->
                val settings = dataStore.alertSettings.first()
                if (!settings.asksAfterDrive()) return@collect
                _pending.value = drive
                eventLog.record(
                    EventLevel.INFO,
                    "Sensor seems to be in a car: parked since ${clockText(drive.parkedSinceMs)}, placement ${settings.activePlacement().label}"
                )
            }
        }
    }

    /** The question answered: switched on the SEAT PLACEMENT choice, or kept. A new target's [DriveEnd] is not shown. */
    fun dismiss() {
        _pending.value = null
    }
}

/**
 * The drives of the paired Sensor ([target], read per row) in [rows]: one [DriveWatch], started over for a new target;
 * other trackers' rows and rows without a fix (0, 0) are skipped (ISSUE-NOV26.0-O163 part 1).
 */
internal fun driveEnds(rows: Flow<LocationUpdate>, target: suspend () -> String?): Flow<DriveEnd> = flow {
    var drives = DriveWatch()
    var watched: String? = null
    rows.collect { row ->
        val paired = target()
        if (paired != watched) {
            drives = DriveWatch()
            watched = paired
        }
        if (paired == null || row.deviceId != paired || (row.lat == 0.0 && row.lng == 0.0)) return@collect
        drives.onFix(row.timestamp, row.lat, row.lng, row.speed.takeIf { it >= 0f })?.let { emit(DriveEnd(paired, it)) }
    }
}

/** A drive's parking time as the question and the event show it. */
fun clockText(timeMs: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(timeMs))
