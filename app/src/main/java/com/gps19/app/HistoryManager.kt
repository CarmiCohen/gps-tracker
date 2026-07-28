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
    data class LogEvent(val message: String, val important: Boolean) : HistoryEvent()
}

/**
 * HistoryManager: Manages the periodic recording of connection metrics (ribbons).
 * July.28.22:
 * - Issue #617: Global SharedFlow Audit. Hardened _historyEvents with 
 *   BufferOverflow.DROP_OLDEST to ensure non-blocking telemetry recording (R617).
 * July.27.07:
 * - Issue #602: SIT Timestamp Parity Logic. Integrated sitVzTs/Rt into 
 *   updateRibbons and mapping to restore full forensic parity.
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
        now: Long,
        nowRt: Long,
        lastTickTs: Long,
        lastTickRt: Long,
        serviceTickCounter: Int,
        rtt: Int,
        peerSignal: Int,
        peerAvail: Boolean,
        hasGps: Boolean,
        isTrackerMode: Boolean,
        accuracy: Double = 0.0,
        maxAccuracy: Double = 0.0,
        noiseIdx: Double = 0.0,
        luxIdx: Double = 0.0,
        vibeIdx: Double = 0.0,
        proxIdx: Double = 1.0,
        liftIdx: Double = 0.0,
        snrIdx: Double = 0.0,
        tiltIdx: Double = 0.0,
        baroIdx: Double = 0.0,
        verticalVelocity: Double = 0.0,
        sitVz: Double = 0.0,
        sitVzTs: Long = 0L,
        sitVzRt: Long = 0L,
        sitDz: Double = 0.0,
        sitBaro: Double = 0.0,
        sitTilt: Double = 0.0,
        sitShock: Double = 0.0,
        isBatterySteepDischarge: Boolean = false,
        isCoolingModeActive: Boolean = false,
        speed: Double = 0.0,
        bearing: Double = 0.0,
        isSitDetected: Boolean = false,
        isSitActive: Boolean = false,
        currentMa: Int = 0,
        locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
        kineticEnergy: Double = 0.0
    ) {
        detectClockTampering(now)

        val deltaRt = if (lastTickRt > 0) nowRt - lastTickRt else 0L
        
        if (now - lastAuditTs > 60000L) {
            if (backfillAuditCount > 0) {
                _historyEvents.tryEmit(HistoryEvent.LogEvent("Forensic: 4M Continuity Audit - Backfilled $backfillAuditCount points in last 60s to maintain 1Hz resolution during idle.", false))
                backfillAuditCount = 0
            }
            lastAuditTs = now
        }

        if (lastTickRt > 0 && deltaRt > REAL_TIME_GAP_LIMIT_MS) {
            fillRealGap(lastTickTs, lastTickRt, now, nowRt, isTrackerMode)
        } else if (lastTickRt > 0 && deltaRt > 1500L) {
            backfillAnalyticalGaps(
                lastTickTs = lastTickTs,
                lastTickRt = lastTickRt,
                now = now,
                nowRt = nowRt,
                rtt = rtt,
                peerSignal = peerSignal,
                peerAvail = peerAvail,
                hasGps = hasGps,
                isTrackerMode = isTrackerMode,
                accuracy = accuracy,
                maxAccuracy = maxAccuracy,
                noiseIdx = noiseIdx,
                luxIdx = luxIdx,
                vibeIdx = vibeIdx,
                proxIdx = proxIdx,
                liftIdx = liftIdx,
                snrIdx = snrIdx,
                tiltIdx = tiltIdx,
                baroIdx = baroIdx,
                verticalVelocity = verticalVelocity,
                sitVz = sitVz,
                sitVzTs = sitVzTs,
                sitVzRt = sitVzRt,
                sitDz = sitDz,
                sitBaro = sitBaro,
                sitTilt = sitTilt,
                sitShock = sitShock,
                isBatterySteepDischarge = isBatterySteepDischarge,
                isCoolingModeActive = isCoolingModeActive,
                speed = speed,
                bearing = bearing,
                isSitDetected = isSitDetected,
                isSitActive = isSitActive,
                currentMa = currentMa,
                locationPendingReason = locationPendingReason,
                kineticEnergy = kineticEnergy
            )
        }

        val currentPoint = EngineConnectionPoint(
            ts = now,
            rt = nowRt,
            rtt = rtt,
            remoteSig = peerSignal,
            isConnected = peerAvail,
            isGap = false,
            hasGps = hasGps,
            accuracy = accuracy,
            maxAccuracy = maxAccuracy,
            gpsIndex = 0.0,
            noiseIdx = noiseIdx,
            luxIdx = luxIdx,
            vibeIdx = vibeIdx,
            proxIdx = proxIdx,
            liftIdx = liftIdx,
            snrIdx = snrIdx,
            tiltIdx = tiltIdx,
            baroIdx = baroIdx,
            isSitDetected = applySitDuplicateGuard(isSitDetected, now, nowRt),
            isSitActive = isSitActive,
            verticalVelocity = verticalVelocity,
            sitVz = sitVz,
            sitVzTs = sitVzTs,
            sitVzRt = sitVzRt,
            sitDz = sitDz,
            sitBaro = sitBaro,
            sitTilt = sitTilt,
            sitShock = sitShock,
            isBatterySteepDischarge = isBatterySteepDischarge,
            isCoolingModeActive = isCoolingModeActive,
            speed = speed,
            bearing = bearing,
            isTick = false,
            currentMa = currentMa,
            locationPendingReason = locationPendingReason,
            kineticEnergy = kineticEnergy
        )

        processResults(aggregator.processPoint(currentPoint))

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

    private fun processResults(results: List<Pair<RibbonScale, EngineConnectionPoint>>) {
        LatencyMonitor.measure(
            timeProvider,
            LATENCY_THRESHOLD_DB_WRITE_MS,
            { duration ->
                _historyEvents.tryEmit(HistoryEvent.LogEvent("Forensic Warning: processResults DB spike (${duration}ms)", false))
            }
        ) {
            results.forEach { (scale, point) ->
                repository.addHistoryPoint(scale.key, mapToAppPoint(point))
            }
        }
    }

    private fun backfillAnalyticalGaps(
        lastTickTs: Long,
        lastTickRt: Long,
        now: Long,
        nowRt: Long,
        rtt: Int,
        peerSignal: Int,
        peerAvail: Boolean,
        hasGps: Boolean,
        isTrackerMode: Boolean,
        accuracy: Double,
        maxAccuracy: Double,
        noiseIdx: Double,
        luxIdx: Double,
        vibeIdx: Double,
        proxIdx: Double,
        liftIdx: Double,
        snrIdx: Double,
        tiltIdx: Double,
        baroIdx: Double,
        verticalVelocity: Double,
        sitVz: Double,
        sitVzTs: Long,
        sitVzRt: Long,
        sitDz: Double,
        sitBaro: Double,
        sitTilt: Double,
        sitShock: Double,
        isBatterySteepDischarge: Boolean,
        isCoolingModeActive: Boolean,
        speed: Double,
        bearing: Double,
        isSitDetected: Boolean,
        isSitActive: Boolean,
        currentMa: Int,
        locationPendingReason: LocationPendingReason,
        kineticEnergy: Double
    ) {
        LatencyMonitor.measure(
            timeProvider,
            LATENCY_THRESHOLD_SENSOR_PROCESS_MS,
            { duration ->
                _historyEvents.tryEmit(HistoryEvent.LogEvent("Forensic Warning: backfillAnalyticalGaps logic spike (${duration}ms)", false))
            }
        ) {
            val snrSamples = if (isTrackerMode) gpsManager.getSnrSamples(lastTickTs + 1, now) else emptySequence()
            val sensorSamples = if (isTrackerMode) sensorManager.getSensorSamples(lastTickTs + 1, now) else emptySequence()

            val baseTemplate = EngineConnectionPoint(
                ts = 0L,
                rt = 0L,
                rtt = rtt,
                remoteSig = peerSignal,
                isConnected = peerAvail,
                hasGps = hasGps,
                accuracy = accuracy,
                maxAccuracy = maxAccuracy,
                noiseIdx = noiseIdx,
                luxIdx = luxIdx,
                vibeIdx = vibeIdx,
                proxIdx = proxIdx,
                liftIdx = liftIdx,
                snrIdx = snrIdx,
                tiltIdx = tiltIdx,
                baroIdx = baroIdx,
                verticalVelocity = verticalVelocity,
                sitVz = sitVz,
                sitVzTs = sitVzTs,
                sitVzRt = sitVzRt,
                sitDz = sitDz,
                sitBaro = sitBaro,
                sitTilt = sitTilt,
                sitShock = sitShock,
                isSitDetected = applySitDuplicateGuard(isSitDetected, now, nowRt),
                isSitActive = isSitActive,
                isBatterySteepDischarge = isBatterySteepDischarge,
                isCoolingModeActive = isCoolingModeActive,
                speed = speed,
                bearing = bearing,
                currentMa = currentMa,
                locationPendingReason = locationPendingReason,
                kineticEnergy = kineticEnergy
            )

            val results = aggregator.backfillGaps(lastTickRt, nowRt, lastTickTs, now, snrSamples, sensorSamples, locationProcessor.getAcousticFloorDb(), baseTemplate)
            
            val fourMPoints = ArrayList<ConnectionPoint>()
            results.forEach { (scale, point) ->
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
    }

    private fun fillRealGap(lastTickTs: Long, lastTickRt: Long, now: Long, nowRt: Long, isTrackerMode: Boolean) {
        LatencyMonitor.measure(
            timeProvider,
            LATENCY_THRESHOLD_DB_WRITE_MS,
            { duration ->
                _historyEvents.tryEmit(HistoryEvent.LogEvent("Forensic Warning: fillRealGap cycle spike (${duration}ms)", false))
            }
        ) {
            val snrSamples = if (isTrackerMode) gpsManager.getSnrSamples(lastTickTs, now) else emptySequence()
            val sensorSamples = if (isTrackerMode) sensorManager.getSensorSamples(lastTickTs, now) else emptySequence()

            RibbonScale.entries.forEach { scale ->
                val gapPoints = aggregator.fillRealGap(scale.key, scale.intervalSeconds, lastTickRt, nowRt, lastTickTs, now, snrSamples, sensorSamples, locationProcessor.getAcousticFloorDb())
                if (gapPoints.isNotEmpty()) {
                    repository.addHistoryPoints(scale.key, gapPoints.map { mapToAppPoint(it) })
                }
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
            val jumpSec = delta / 1000
            val direction = if (currentDrift > clockDriftRef) "forward" else "backward"
            _historyEvents.tryEmit(HistoryEvent.LogEvent("FORENSIC ALERT: System clock jump detected ($direction ${jumpSec}s). Monotonic uptime preserved.", true))
            clockDriftRef = currentDrift
            scope?.launch { repository.saveLong(CLOCK_DRIFT_REF_KEY, currentDrift) }
        }
    }

    private fun applySitDuplicateGuard(isDetected: Boolean, ts: Long, rt: Long): Boolean {
        if (!isDetected) return false
        if (abs(rt - lastSitDetectedRt) < SIT_DUPLICATE_GUARD_MS) {
            return false
        }
        lastSitDetectedRt = rt
        scope?.launch {
            repository.saveLong(LAST_HISTORY_SIT_TS_KEY, ts)
        }
        return true
    }

    private fun mapToAppPoint(p: EngineConnectionPoint): ConnectionPoint {
        return ConnectionPoint(
            ts = p.ts,
            rt = p.rt,
            rtt = p.rtt,
            localSig = 10,
            remoteSig = p.remoteSig,
            isConnected = p.isConnected,
            isGap = p.isGap,
            hasGps = p.hasGps,
            isTick = p.isTick,
            gpsAccuracy = p.accuracy,
            maxAccuracy = p.maxAccuracy,
            isBatterySteepDischarge = p.isBatterySteepDischarge,
            isCoolingModeActive = p.isCoolingModeActive,
            speed = p.speed,
            bearing = p.bearing,
            currentMa = p.currentMa,
            locationPendingReason = p.locationPendingReason,
            gpsIndex = p.gpsIndex,
            noiseIdx = p.noiseIdx,
            luxIdx = p.luxIdx,
            vibeIdx = p.vibeIdx,
            proxIdx = p.proxIdx,
            liftIdx = p.liftIdx,
            snrIdx = p.snrIdx,
            tiltIdx = p.tiltIdx,
            baroIdx = p.baroIdx,
            isSitDetected = p.isSitDetected,
            isSitActive = p.isSitActive,
            sitVz = p.sitVz,
            sitVzTs = p.sitVzTs,
            sitVzRt = p.sitVzRt,
            sitDz = p.sitDz,
            sitBaro = p.sitBaro,
            sitTilt = p.sitTilt,
            sitShock = p.sitShock,
            kineticEnergy = p.kineticEnergy
        )
    }

    private fun handleHourlyAutoSave(hour: Int) {
        scope?.launch {
            if (lastProcessedHour != hour) {
                val repoHour = repository.getInt(LAST_AUTO_SAVE_HOUR_KEY, -1)
                if (repoHour != hour) {
                    lastProcessedHour = hour
                    repository.saveIntSync(LAST_AUTO_SAVE_HOUR_KEY, hour)
                    
                    if (hourlyBackfillTotal > 0) {
                        _historyEvents.tryEmit(HistoryEvent.LogEvent("Forensic: Hourly Continuity Audit - Backfilled $hourlyBackfillTotal points to maintain 1Hz fidelity during power-save ticks.", false))
                        hourlyBackfillTotal = 0
                    }

                    _historyEvents.tryEmit(HistoryEvent.LogEvent("Hourly auto-export", false))
                    scope?.launch(Dispatchers.IO) {
                        MainFileHelper.autoExportData(context, repository, timeProvider)
                    }
                } else {
                    lastProcessedHour = hour
                }
            }
        }
    }

    private fun handleDailyCleanup(calendar: Calendar, hour: Int, minute: Int) {
        scope?.launch {
            if (hour == DAILY_CLEANUP_HOUR && minute == DAILY_CLEANUP_MINUTE) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayDate = dateFormat.format(calendar.time)
                if (lastCleanupDate != todayDate) {
                    if (repository.getString(LAST_DAILY_CLEANUP_DATE_KEY, "") != todayDate) {
                        lastCleanupDate = todayDate
                        repository.saveStringSync(LAST_DAILY_CLEANUP_DATE_KEY, todayDate)
                        _historyEvents.tryEmit(HistoryEvent.LogEvent("Periodic daily cleanup of trails", true))
                        repository.clearTrails()
                    } else {
                        lastCleanupDate = todayDate
                    }
                }
            }
        }
    }

    private fun handleDailyArchiving(calendar: Calendar, hour: Int, minute: Int) {
        scope?.launch {
            if (hour == DAILY_ARCHIVE_HOUR && minute == DAILY_ARCHIVE_MINUTE) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayDate = dateFormat.format(calendar.time)
                if (lastArchiveDate != todayDate) {
                    if (repository.getString(LAST_DAILY_ARCHIVE_DATE_KEY, "") != todayDate) {
                        lastArchiveDate = todayDate
                        repository.saveStringSync(LAST_DAILY_ARCHIVE_DATE_KEY, todayDate)
                        _historyEvents.tryEmit(HistoryEvent.LogEvent("Periodic daily archiving of old files", true))
                        scope?.launch(Dispatchers.IO) {
                            MainFileHelper.performDailyArchiving(context, timeProvider)
                        }
                    } else {
                        lastArchiveDate = todayDate
                    }
                }
            }
        }
    }
}
