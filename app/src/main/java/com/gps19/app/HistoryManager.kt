package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * HistoryEvent: Reactive event container for history-related logs and triggers.
 */
sealed class HistoryEvent {
    data class LogEvent(val message: String, val isImportant: Boolean) : HistoryEvent()
}

/**
 * HistoryManager: Manages the periodic recording of connection metrics (ribbons).
 * Aug.01.10:
 * - Issue #668: Performance: Object Churn. Implemented flyweight for EngineConnectionPoint 
 *   to eliminate per-tick allocations in updateRibbons (R-HARDWARE-01).
 * July.30.48:
 * - Issue #653: Performance: GC Churn Optimization.
 */
@Singleton
class HistoryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val gpsManager: GpsManager,
    private val sensorManager: AppSensorManager,
    private val locationProcessor: LocationProcessor
) {
    private val _historyEvents = MutableSharedFlow<HistoryEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val historyEvents: SharedFlow<HistoryEvent> = _historyEvents.asSharedFlow()

    private var scope: CoroutineScope? = null
    private val isInitialized = AtomicBoolean(false)

    private var lastProcessedHour = -1
    private var lastCleanupDate = ""
    private var lastArchiveDate = ""

    private var clockDriftRef: Long = 0L
    private val aggregator = TelemetryAggregator()
    
    // Issue #668: Flyweight to eliminate tick-level allocation
    private val currentPointFlyweight = EngineConnectionPoint()
    private val baseTemplateFlyweight = EngineConnectionPoint()

    private var backfillAuditCount = 0
    private var hourlyBackfillTotal = 0
    private var lastAuditTs = 0L
    private var lastTimeTriggerTs = 0L
    private var lastSitDetectedRt = 0L

    suspend fun initialize(scope: CoroutineScope) {
        if (isInitialized.getAndSet(true)) return
        this.scope = scope
        withContext(Dispatchers.IO) {
            val lastSitTs = repository.getLong(LAST_HISTORY_SIT_TS_KEY, 0L)
            if (lastSitTs > 0) {
                 lastSitDetectedRt = timeProvider.elapsedRealtime() - (timeProvider.currentTimeMillis() - lastSitTs)
            }
            clockDriftRef = repository.getLong(CLOCK_DRIFT_REF_KEY, 0L)
        }
    }

    suspend fun updateRibbons(
        now: Long, nowRt: Long, lastTickTs: Long, lastTickRt: Long,
        serviceTickCounter: Int, rtt: Int, peerSignal: Int, peerAvail: Boolean,
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
        isRecoveryEvent: Boolean = false
    ) {
        detectClockTampering(now)
        val deltaRt = if (lastTickRt > 0) nowRt - lastTickRt else 0L
        
        if (now - lastAuditTs > 60000L) {
            if (backfillAuditCount > 0) {
                _historyEvents.tryEmit(HistoryEvent.LogEvent("Forensic: 4M Continuity Audit - Backfilled $backfillAuditCount points", false))
                backfillAuditCount = 0
            }
            lastAuditTs = now
        }

        if (lastTickRt > 0 && deltaRt > REAL_TIME_GAP_LIMIT_MS) {
            fillRealGap(lastTickTs, lastTickRt, now, nowRt, isTrackerMode)
        } else if (lastTickRt > 0 && deltaRt > 1500L) {
            backfillAnalyticalGaps(
                lastTickTs, lastTickRt, now, nowRt, rtt, peerSignal, peerAvail, hasGps, isTrackerMode,
                accuracy, maxAccuracy, noiseIdx, luxIdx, vibeIdx, proxIdx, liftIdx, snrIdx, tiltIdx, baroIdx,
                verticalVelocity, sitVz, sitVzTs, sitVzRt, sitDz, sitBaro, sitTilt, sitShock,
                isBatterySteepDischarge, isCoolingModeActive, speed, bearing, isSitDetected, isSitActive,
                currentMa, locationPendingReason, kineticEnergy, isRecoveryEvent
            )
        }

        // Issue #668: Reusing flyweight
        currentPointFlyweight.apply {
            ts = now; rt = nowRt; this.rtt = rtt; remoteSig = peerSignal; isConnected = peerAvail; isGap = false
            this.isRecoveryEvent = isRecoveryEvent
            this.hasGps = hasGps; this.accuracy = accuracy; this.maxAccuracy = maxAccuracy; gpsIndex = 0.0
            this.noiseIdx = noiseIdx; this.luxIdx = luxIdx; this.vibeIdx = vibeIdx; this.proxIdx = proxIdx
            this.liftIdx = liftIdx; this.snrIdx = snrIdx; this.tiltIdx = tiltIdx; this.baroIdx = baroIdx
            this.isSitDetected = applySitDuplicateGuard(isSitDetected, now, nowRt)
            this.isSitActive = isSitActive; this.verticalVelocity = verticalVelocity; this.sitVz = sitVz
            this.sitVzTs = sitVzTs; this.sitVzRt = sitVzRt; this.sitDz = sitDz; this.sitBaro = sitBaro
            this.sitTilt = sitTilt; this.sitShock = sitShock; this.isBatterySteepDischarge = isBatterySteepDischarge
            this.isCoolingModeActive = isCoolingModeActive; this.speed = speed; this.bearing = bearing; isTick = false
            this.currentMa = currentMa; this.locationPendingReason = locationPendingReason; this.kineticEnergy = kineticEnergy
        }
        
        aggregator.processPoint(currentPointFlyweight) { scale, point ->
            repository.addHistoryPoint(scale.key, mapToAppPoint(point))
        }

        if (now - lastTimeTriggerTs >= 60000L || lastTimeTriggerTs == 0L) {
            lastTimeTriggerTs = now
            val calendar = Calendar.getInstance().apply { timeInMillis = now }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            handleHourlyAutoSave(hour)
            handleDailyCleanup(calendar, hour, minute)
            handleDailyArchiving(calendar, hour, minute)
        }
    }

    private fun backfillAnalyticalGaps(
        lastTickTs: Long, lastTickRt: Long, now: Long, nowRt: Long, rtt: Int,
        peerSignal: Int, peerAvail: Boolean, hasGps: Boolean, isTrackerMode: Boolean,
        accuracy: Double, maxAccuracy: Double, noiseIdx: Double, luxIdx: Double,
        vibeIdx: Double, proxIdx: Double, liftIdx: Double, snrIdx: Double,
        tiltIdx: Double, baroIdx: Double, verticalVelocity: Double, sitVz: Double,
        sitVzTs: Long, sitVzRt: Long, sitDz: Double, sitBaro: Double,
        sitTilt: Double, sitShock: Double, isBatterySteepDischarge: Boolean,
        isCoolingModeActive: Boolean, speed: Double, bearing: Double,
        isSitDetected: Boolean, isSitActive: Boolean, currentMa: Int,
        locationPendingReason: LocationPendingReason, kineticEnergy: Double,
        isRecoveryEvent: Boolean
    ) {
        val snrSamples = if (isTrackerMode) gpsManager.getSnrSamples(lastTickTs + 1, now) else emptySequence()
        val sensorSamples = if (isTrackerMode) sensorManager.getSensorSamples(lastTickTs + 1, now) else emptySequence()
        
        // Issue #668: Reusing baseTemplate flyweight
        baseTemplateFlyweight.apply {
            ts = 0L; rt = 0L; this.rtt = rtt; remoteSig = peerSignal; isConnected = peerAvail; this.hasGps = hasGps
            this.isRecoveryEvent = isRecoveryEvent
            this.accuracy = accuracy; this.maxAccuracy = maxAccuracy; this.noiseIdx = noiseIdx; this.luxIdx = luxIdx
            this.vibeIdx = vibeIdx; this.proxIdx = proxIdx; this.liftIdx = liftIdx; this.snrIdx = snrIdx; this.tiltIdx = tiltIdx
            this.baroIdx = baroIdx; this.verticalVelocity = verticalVelocity; this.sitVz = sitVz; this.sitVzTs = sitVzTs
            this.sitVzRt = sitVzRt; this.sitDz = sitDz; this.sitBaro = sitBaro; this.sitTilt = sitTilt; this.sitShock = sitShock
            this.isSitDetected = applySitDuplicateGuard(isSitDetected, now, nowRt)
            this.isSitActive = isSitActive; this.isBatterySteepDischarge = isBatterySteepDischarge
            this.isCoolingModeActive = isCoolingModeActive; this.speed = speed; this.bearing = bearing
            this.currentMa = currentMa; this.locationPendingReason = locationPendingReason; this.kineticEnergy = kineticEnergy
        }
        
        val fourMPoints = ArrayList<ConnectionPoint>()
        
        aggregator.backfillGaps(lastTickRt, nowRt, lastTickTs, now, snrSamples, sensorSamples, locationProcessor.getAcousticFloorDb(), baseTemplateFlyweight) { scale, point ->
            val appPoint = mapToAppPoint(point)
            if (scale == RibbonScale.FOUR_MIN) { 
                fourMPoints.add(appPoint) 
            } else { 
                repository.addHistoryPoint(scale.key, appPoint) 
            }
        }
        
        if (fourMPoints.isNotEmpty()) {
            repository.addHistoryPoints("4M", fourMPoints)
            backfillAuditCount += fourMPoints.size
            hourlyBackfillTotal += fourMPoints.size
        }
    }

    private fun fillRealGap(lastTickTs: Long, lastTickRt: Long, now: Long, nowRt: Long, isTrackerMode: Boolean) {
        val snrSamples = if (isTrackerMode) gpsManager.getSnrSamples(lastTickTs, now) else emptySequence()
        val sensorSamples = if (isTrackerMode) sensorManager.getSensorSamples(lastTickTs, now) else emptySequence()
        
        RibbonScale.entries.forEach { scale ->
            val gapPoints = ArrayList<ConnectionPoint>()
            aggregator.fillRealGap(scale, lastTickRt, nowRt, lastTickTs, snrSamples, sensorSamples, locationProcessor.getAcousticFloorDb()) { point ->
                gapPoints.add(mapToAppPoint(point))
            }
            if (gapPoints.isNotEmpty()) { 
                repository.addHistoryPoints(scale.key, gapPoints)
            }
        }
    }

    private fun detectClockTampering(nowWall: Long) {
        val monotonic = timeProvider.elapsedRealtime()
        val currentDrift = nowWall - monotonic
        if (clockDriftRef == 0L) {
            clockDriftRef = currentDrift
            scope?.launch { repository.saveLong(CLOCK_DRIFT_REF_KEY, currentDrift) }
            return
        }
        val delta = abs(currentDrift - clockDriftRef)
        if (delta > DRIFT_TOLERANCE_MS) {
            val direction = if (currentDrift > clockDriftRef) "forward" else "backward"
            _historyEvents.tryEmit(HistoryEvent.LogEvent("FORENSIC ALERT: System clock jump detected ($direction ${delta / 1000}s).", true))
            clockDriftRef = currentDrift
            scope?.launch { repository.saveLong(CLOCK_DRIFT_REF_KEY, currentDrift) }
        }
    }

    private fun applySitDuplicateGuard(isDetected: Boolean, ts: Long, rt: Long): Boolean {
        if (!isDetected) return false
        if (abs(rt - lastSitDetectedRt) < SIT_DUPLICATE_GUARD_MS) return false
        lastSitDetectedRt = rt
        scope?.launch { repository.saveLong(LAST_HISTORY_SIT_TS_KEY, ts) }
        return true
    }

    private fun mapToAppPoint(p: EngineConnectionPoint) = ConnectionPoint(
        ts = p.ts, rt = p.rt, rtt = p.rtt, localSig = 10, remoteSig = p.remoteSig, isConnected = p.isConnected,
        isGap = p.isGap, isRecoveryEvent = p.isRecoveryEvent, hasGps = p.hasGps, isTick = p.isTick, gpsAccuracy = p.accuracy, maxAccuracy = p.maxAccuracy,
        isBatterySteepDischarge = p.isBatterySteepDischarge, isCoolingModeActive = p.isCoolingModeActive,
        speed = p.speed, bearing = p.bearing, currentMa = p.currentMa, locationPendingReason = p.locationPendingReason,
        gpsIndex = p.gpsIndex, noiseIdx = p.noiseIdx, luxIdx = p.luxIdx, vibeIdx = p.vibeIdx, proxIdx = p.proxIdx,
        liftIdx = p.liftIdx, snrIdx = p.snrIdx, tiltIdx = p.tiltIdx, baroIdx = p.baroIdx, isSitDetected = p.isSitDetected,
        isSitActive = p.isSitActive, sitVz = p.sitVz, sitVzTs = p.sitVzTs, sitVzRt = p.sitVzRt, sitDz = p.sitDz,
        sitBaro = p.sitBaro, sitTilt = p.sitTilt, sitShock = p.sitShock, kineticEnergy = p.kineticEnergy
    )

    private fun handleHourlyAutoSave(hour: Int) {
        scope?.launch {
            if (lastProcessedHour != hour) {
                if (repository.getInt(LAST_AUTO_SAVE_HOUR_KEY, -1) != hour) {
                    lastProcessedHour = hour
                    repository.saveIntSync(LAST_AUTO_SAVE_HOUR_KEY, hour)
                    if (hourlyBackfillTotal > 0) {
                        _historyEvents.tryEmit(HistoryEvent.LogEvent("Forensic: Hourly Continuity Audit - Backfilled $hourlyBackfillTotal points.", false))
                        hourlyBackfillTotal = 0
                    }
                    _historyEvents.tryEmit(HistoryEvent.LogEvent("Hourly auto-export", false))
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
                        _historyEvents.tryEmit(HistoryEvent.LogEvent("Periodic daily cleanup of trails", true))
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
                        _historyEvents.tryEmit(HistoryEvent.LogEvent("Periodic daily archiving of old files", true))
                        scope?.launch(Dispatchers.IO) { 
                            MainFileHelper.performDailyArchiving(context, timeProvider) 
                        }
                    } else { lastArchiveDate = todayDate }
                }
            }
        }
    }
}
