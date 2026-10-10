package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * HistoryManager: Manages the periodic recording of connection metrics (ribbons).
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to use property-based 
 *   TimeProvider API.
 */
@Singleton
class HistoryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val domainEventBus: DomainEventBus
) {
    private var scope: CoroutineScope? = null
    private val isInitialized = AtomicBoolean(false)

    private var lastProcessedHour = -1
    private var lastCleanupDate = ""
    private var lastArchiveDate = ""

    private var clockDriftRef: Long = 0L
    private val aggregator = TelemetryAggregator()
    
    private val currentPointFlyweight = EngineConnectionPoint()
    private val appPointPool = Array(RibbonScale.entries.size) { ConnectionPoint() }
    
    private var lastTimeTriggerRt = 0L
    private var lastSitDetectedRt = 0L
    private var currentRole: AppRole = AppRole.TRACKER

    private val ribbonMutex = Mutex()

    /**
     * initialize: Binds the manager to an active service scope and hydrates role-aware state.
     */
    suspend fun initialize(scope: CoroutineScope, role: AppRole = AppRole.TRACKER) {
        this.scope = scope
        this.currentRole = role
        
        withContext(Dispatchers.IO) {
            val lastSitTs = repository.getLong(currentRole, LAST_SIT_TS_KEY, 0L)
            if (lastSitTs > 0) {
                 lastSitDetectedRt = timeProvider.elapsedRealtime - (timeProvider.currentTimeMillis - lastSitTs)
            }
            clockDriftRef = repository.getLong(currentRole, CLOCK_DRIFT_REF_KEY, 0L)
            lastProcessedHour = repository.getInt(LAST_AUTO_SAVE_HOUR_KEY, -1)
        }
        isInitialized.set(true)
    }

    /**
     * recoverLastRealtime: Provides reboot-aware monotonic clock recovery.
     */
    fun recoverLastRealtime(lastTs: Long, recoveredDrift: Long): Long {
        val now = timeProvider.currentTimeMillis
        val nowRt = timeProvider.elapsedRealtime
        val currentDrift = now - nowRt
        
        val effectiveDrift = if (recoveredDrift != 0L && abs(currentDrift - recoveredDrift) < DRIFT_TOLERANCE_MS) {
            recoveredDrift
        } else {
            currentDrift
        }
        
        return lastTs - effectiveDrift
    }

    /**
     * reset: Clears all forensic counters and transient state.
     */
    fun reset() {
        lastProcessedHour = -1
        lastCleanupDate = ""
        lastArchiveDate = ""
        lastTimeTriggerRt = 0L
        aggregator.reset()
    }
    
    /**
     * trimMemory: Issue #1416 Hardening.
     */
    suspend fun trimMemory() = ribbonMutex.withLock {
        appPointPool.forEach { it.reset() }
    }

    /**
     * pruneStorage: Issue #SIMP-1014-2. Triggers proactive database pruning 
     * to alleviate storage pressure.
     */
    suspend fun pruneStorage() = withContext(Dispatchers.IO) {
        repository.proactivePruning()
    }

    private fun emitSanitizedLog(message: String, isImportant: Boolean = false) {
        val sanitized = ForensicSanitizer.sanitizeMessage(message)
        domainEventBus.emit(HistoryEvent.LogEvent(sanitized, isImportant))
    }

    /**
     * updateRibbons: Unified entry point for ribbon updates from TickEvaluated events.
     */
    suspend fun updateRibbons(event: DomainEvent.TickEvaluated) {
        val proc = event.processed
        val snapshot = event.snapshot
        
        updateRibbons(
            now = event.now,
            nowRt = event.nowRt,
            lastTickTs = event.lastTickTs,
            lastTickRt = event.lastTickRt,
            serviceTickCounter = event.serviceTickCounter,
            rtt = event.rtt,
            peerSignal = if (event.isPeerActive) 10 else 0,
            peerAvail = event.isSocketConnected && event.isPeerActive,
            hasGps = (proc?.timestamp ?: 0L) > 0,
            isTrackerMode = event.isTrackerMode,
            accuracy = proc?.currentAccuracy ?: 0.0,
            maxAccuracy = proc?.maxAccuracy ?: 0.0,
            noiseIdx = snapshot.atmospheric.noiseIdx,
            luxIdx = snapshot.atmospheric.luxIdx,
            vibeIdx = snapshot.atmospheric.vibeIdx,
            proxIdx = snapshot.atmospheric.proxIdx,
            liftIdx = snapshot.atmospheric.liftIdx,
            snrIdx = snapshot.integrity.snrIdx,
            tiltIdx = snapshot.atmospheric.tiltIdx,
            baroIdx = snapshot.atmospheric.baroIdx,
            verticalVelocity = snapshot.kinetic.verticalVelocity,
            sitVz = snapshot.integrity.sitVz,
            sitVzTs = snapshot.integrity.sitVzTs,
            sitVzRt = snapshot.integrity.sitVzRt,
            sitDz = snapshot.integrity.sitDz,
            sitBaro = snapshot.integrity.sitBaro,
            sitTilt = snapshot.integrity.sitTilt,
            sitShock = snapshot.integrity.sitShock,
            isBatterySteepDischarge = snapshot.integrity.isBatterySteepDischarge,
            isCoolingModeActive = snapshot.integrity.isCoolingModeActive,
            speed = snapshot.kinetic.speed,
            bearing = snapshot.kinetic.bearing,
            isSitDetected = if (event.isTrackerMode) event.isSuspiciousMode else false,
            isSitActive = false,
            currentMa = snapshot.integrity.currentMa,
            locationPendingReason = snapshot.integrity.locationPendingReason,
            kineticEnergy = snapshot.kinetic.kineticEnergy,
            isRecoveryEvent = event.recoveryFlagged,
            cpuLoad = snapshot.integrity.cpuLoad,
            ioWait = snapshot.integrity.ioWait,
            maxIoLatency = snapshot.integrity.maxIoLatency,
            isSilentFailure = snapshot.integrity.isSilentFailure,
            isBatteryLow = snapshot.integrity.isBatteryLow,
            isBatteryCritical = snapshot.integrity.isBatteryCritical,
            isUltraLongStationary = snapshot.integrity.isUltraLongStationary,
            thermalSnapshot = snapshot.thermalSnapshot,
            heapSnapshot = snapshot.heapSnapshot
        )
    }

    suspend fun updateRibbons(
        now: Long, nowRt: Long, lastTickTs: Long, lastTickRt: Long,
        serviceTickCounter: Long, rtt: Int, peerSignal: Int, peerAvail: Boolean,
        hasGps: Boolean, isTrackerMode: Boolean, accuracy: Double = 0.0,
        maxAccuracy: Double = 0.0, noiseIdx: Double = 0.0, luxIdx: Double = 0.0,
        vibeIdx: Double = 0.0, proxIdx: Double = 1.0, liftIdx: Double = 0.0,
        snrIdx: Double = 0.0, tiltIdx: Double = 0.0, baroIdx: Double = 0.0,
        verticalVelocity: Double = 0.0, sitVz: Double = 0.0, sitVzTs: Long = 0L,
        sitVzRt: Long = 0L, sitDz: Double = 0.0, sitBaro: Double = 0.0,
        sitTilt: Double = 0.0, sitShock: Double = 0.0,
        isBatterySteepDischarge: Boolean = false, isCoolingModeActive: Boolean = false,
        speed: Double = 0.0, bearing: Double = 0.0, isSitDetected: Boolean = false,
        isSitActive: Boolean = false, currentMa: Int = 0,
        locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
        kineticEnergy: Double = 0.0,
        isRecoveryEvent: Boolean = false,
        cpuLoad: Double = 0.0,
        ioWait: Double = 0.0,
        maxIoLatency: Long = 0L,
        isSilentFailure: Boolean = false,
        isBatteryLow: Boolean = false,
        isBatteryCritical: Boolean = false,
        isUltraLongStationary: Boolean = false,
        thermalSnapshot: Double? = null,
        heapSnapshot: Double? = null
    ) = ribbonMutex.withLock {
        detectClockTampering(now)

        currentPointFlyweight.apply {
            ts = now; rt = nowRt; this.rtt = rtt; remoteSig = peerSignal; isConnected = peerAvail; isGap = false
            this.isRecoveryEvent = isRecoveryEvent
            this.hasGps = hasGps; 
            this.accuracy = PhysicsUtils.safeDouble(accuracy)
            this.maxAccuracy = PhysicsUtils.safeDouble(maxAccuracy)
            this.isSitDetected = applySitDuplicateGuard(isSitDetected, now, nowRt)
            this.isSitActive = isSitActive; 
            this.verticalVelocity = PhysicsUtils.safeDouble(verticalVelocity)
            this.sitVz = PhysicsUtils.safeDouble(sitVz)
            this.sitVzTs = sitVzTs; this.sitVzRt = sitVzRt; 
            this.sitDz = PhysicsUtils.safeDouble(sitDz)
            this.sitBaro = PhysicsUtils.safeDouble(sitBaro)
            this.sitTilt = PhysicsUtils.safeDouble(sitTilt)
            this.sitShock = PhysicsUtils.safeDouble(sitShock)
            this.isBatterySteepDischarge = isBatterySteepDischarge
            this.isCoolingModeActive = isCoolingModeActive; 
            this.speed = PhysicsUtils.safeDouble(speed)
            this.bearing = PhysicsUtils.safeDouble(bearing)
            isTick = false
            this.currentMa = currentMa; this.locationPendingReason = locationPendingReason; 
            this.kineticEnergy = PhysicsUtils.safeDouble(kineticEnergy)
            this.cpuLoad = PhysicsUtils.safeDouble(cpuLoad)
            this.ioWait = PhysicsUtils.safeDouble(ioWait)
            this.maxIoLatency = maxIoLatency; this.isSilentFailure = isSilentFailure
            this.isBatteryLow = isBatteryLow; this.isBatteryCritical = isBatteryCritical
            this.noiseIdx = PhysicsUtils.safeDouble(noiseIdx)
            this.luxIdx = PhysicsUtils.safeDouble(luxIdx)
            this.vibeIdx = PhysicsUtils.safeDouble(vibeIdx)
            this.proxIdx = PhysicsUtils.safeDouble(proxIdx)
            this.initLiftIdx(PhysicsUtils.safeDouble(liftIdx))
            this.snrIdx = PhysicsUtils.safeDouble(snrIdx)
            this.tiltIdx = PhysicsUtils.safeDouble(tiltIdx)
            this.baroIdx = PhysicsUtils.safeDouble(baroIdx)
            this.isUltraLongStationary = isUltraLongStationary
            this.thermalSnapshot = thermalSnapshot?.let { PhysicsUtils.safeDouble(it) }
            this.heapSnapshot = heapSnapshot?.let { PhysicsUtils.safeDouble(it) }
        }
        
        aggregator.processPoint(currentPointFlyweight) { scale, point ->
            val flyweight = appPointPool[scale.ordinal]
            TelemetryMapper.mapEngineToApp(point, flyweight)
            repository.addHistoryPoint(scale.key, flyweight)
        }

        if (nowRt - lastTimeTriggerRt >= 60000L || lastTimeTriggerRt == 0L) {
            lastTimeTriggerRt = nowRt
            val calendar = Calendar.getInstance().apply { timeInMillis = now }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            handleHourlyAutoSave(hour)
            handleDailyCleanup(calendar, hour, minute)
            handleDailyArchiving(calendar, hour, minute)
        }
    }

    private fun detectClockTampering(nowWall: Long) {
        val monotonic = timeProvider.elapsedRealtime
        val currentDrift = nowWall - monotonic
        if (clockDriftRef == 0L) {
            clockDriftRef = currentDrift
            scope?.launch { repository.saveLong(currentRole, CLOCK_DRIFT_REF_KEY, currentDrift) }
            return
        }
        val delta = abs(currentDrift - clockDriftRef)
        if (delta > DRIFT_TOLERANCE_MS) {
            val direction = if (currentDrift > clockDriftRef) "forward" else "backward"
            emitSanitizedLog("FORENSIC ALERT: System clock jump detected ($direction ${delta / 1000}s).", true)
            clockDriftRef = currentDrift
            scope?.launch { repository.saveLong(currentRole, CLOCK_DRIFT_REF_KEY, currentDrift) }
        }
    }

    private fun applySitDuplicateGuard(isDetected: Boolean, ts: Long, rt: Long): Boolean {
        if (!isDetected) return false
        if (abs(rt - lastSitDetectedRt) < SIT_DUPLICATE_GUARD_MS) return false
        lastSitDetectedRt = rt
        scope?.launch { repository.saveLong(currentRole, LAST_HISTORY_SIT_TS_KEY, ts) }
        return true
    }

    private fun handleHourlyAutoSave(hour: Int) {
        scope?.launch {
            if (lastProcessedHour != hour) {
                if (repository.getInt(LAST_AUTO_SAVE_HOUR_KEY, -1) != hour) {
                    lastProcessedHour = hour
                    repository.saveIntSync(LAST_AUTO_SAVE_HOUR_KEY, hour)
                    emitSanitizedLog("Hourly auto-export")
                    scope?.launch(Dispatchers.IO) { 
                        MainFileHelper.autoExportData(context, repository, timeProvider) 
                    }
                } else { lastProcessedHour = hour }
            }
        }
    }

    private fun handleDailyCleanup(calendar: Calendar, hour: Int, minute: Int) {
        scope?.launch {
            if (hour == DAILY_CLEANUP_HOUR && minute == DAILY_CLEANUP_MINUTE) {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                if (lastCleanupDate != todayDate) {
                    if (repository.getString(LAST_DAILY_CLEANUP_DATE_KEY, "") != todayDate) {
                        lastCleanupDate = todayDate
                        repository.saveStringSync(LAST_DAILY_CLEANUP_DATE_KEY, todayDate)
                        emitSanitizedLog("Periodic daily cleanup of trails", true)
                        repository.clearTrails()
                    } else { lastCleanupDate = todayDate }
                }
            }
        }
    }

    private fun handleDailyArchiving(calendar: Calendar, hour: Int, minute: Int) {
        scope?.launch {
            if (hour == DAILY_ARCHIVE_HOUR && minute == DAILY_ARCHIVE_MINUTE) {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                if (lastArchiveDate != todayDate) {
                    if (repository.getString(LAST_DAILY_ARCHIVE_DATE_KEY, "") != todayDate) {
                        lastArchiveDate = todayDate
                        repository.saveStringSync(LAST_DAILY_ARCHIVE_DATE_KEY, todayDate)
                        emitSanitizedLog("Periodic daily archiving of old files", true)
                        scope?.launch(Dispatchers.IO) { 
                            MainFileHelper.performDailyArchiving(context, timeProvider) 
                        }
                    } else { lastArchiveDate = todayDate }
                }
            }
        }
    }

    private fun EngineConnectionPoint.initLiftIdx(value: Double) {
        this.liftIdx = value
    }
}
