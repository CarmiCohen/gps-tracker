package com.jd.app.monitor

import com.jd.core.network.ForensicIdentityManager
import com.jd.core.shared.SystemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records this Monitor phone's own fixes as its trail (ISSUE-NOV27.2-O170). Started by [MonitorSessionService], so the
 * trail grows with the app in the background or the screen off too; it was recorded by the screen's ViewModel before,
 * which Android stops feeding locations once the app leaves the foreground.
 */
@Singleton
class OwnTrailRecorder @Inject constructor(
    private val locationSource: MonitorLocationSource,
    private val identityManager: ForensicIdentityManager,
    private val repository: MonitorRepository
) {
    private var watchJob: Job? = null

    /** Starts recording in [scope]; a second call while recording, or a call without location permission, does nothing. */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Synchronized
    fun watch(scope: CoroutineScope) {
        if (watchJob?.isActive == true || !locationSource.hasPermission()) return
        watchJob = scope.launch {
            identityManager.selfId
                .filter(::isRealMonitorId)
                .distinctUntilChanged()
                .flatMapLatest { id -> locationSource.fixes().map { fix -> id to fix } }
                .catch { e -> SystemLogger.w("OwnTrail", "The Monitor's own trail stopped: ${e.message}") }
                .collect { (id, fix) -> repository.insertLocalTrail(id, fix) }
        }
    }

    companion object {
        /** False for the placeholders shown before this Monitor has its ID. */
        fun isRealMonitorId(id: String): Boolean = id.isNotBlank() && id != "PENDING_IDENTITY" && id != "LOADING..."
    }
}
