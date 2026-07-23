package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * HistoryManager: Manages the periodic recording of connection metrics (ribbons).
 * July.23.02:
 * - Issue #525: State Audit. Fixed missing forensic mapping in mapToAppPoint.
 * July.21.00:
 * - Issue #105: Forensic Ribbon Continuity Verification.
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
    interface Listener {
        fun onLogEvent(message: String, important: Boolean)
    }

    private var scope: CoroutineScope? = null
    private var listener: Listener? = null

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

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    suspend fun initialize(scope: CoroutineScope) {
        this.scope = scope
        
        withContext(Dispatchers.IO) {
            val lastSitTs = repository.getLong(MainRepository.LAST_HISTORY_SIT_TS_KEY, 0L)
            if (lastSitTs > 0) {
                 lastSitDetectedRt = timeProvider.elapsedRealtime() - (timeProvider.currentTimeMillis() - lastSitTs)
            }
            clockDriftRef = repository.getLong(MainRepository.CLOCK_DRIFT_REF_KEY, 0L)
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
        locationPendingReason: LocationPendingReason = LocationPendingReason.NONE
    ) {
        detectClockTampering(now)

        val deltaRt = if (lastTickRt > 0) nowRt - lastTickRt else 0L
        
        if (now - lastAuditTs > 60000L) {
            if (backfillAuditCount > 0) {
                listener?.onLogEvent("Forensic: 4M Continuity Audit - Backfilled $backfillAuditCount points in last 60s to maintain 1Hz resolution during idle.", false)
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
                locationPendingReason = locationPendingReason
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
            locationPendingReason = locationPendingReason
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
        results.forEach { (scale, point) ->
            repository.addHistoryPoint(scale.key, mapToAppPoint(point))
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
        locationPendingReason: LocationPendingReason
    ) {
        val snrSamples = if (isTrackerMode) {
            gpsManager.getSnrSamples(lastTickTs + 1, now).map { EngineSnrSample(it.first, it.first - (now - nowRt), it.second) }
        } else emptyList()

        val sensorSamples = if (isTrackerMode) {
            sensorManager.getSensorSamples(lastTickTs + 1, now).map { 
                EngineSensorSnapshot(it.ts, it.rt, it.acoustic, it.lux, it.vibe, it.proxIdx, it.lift, it.tilt, it.isSitDetected) 
            }
        } else emptyList()

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
            locationPendingReason = locationPendingReason
        )

        val results = aggregator.backfillGaps(lastTickRt, nowRt, lastTickTs, now, snrSamples, sensorSamples, locationProcessor.getAcousticFloorDb(), baseTemplate)
        
        val fourMPoints = results.filter { it.first == RibbonScale.FOUR_MIN }.map { mapToAppPoint(it.second) }
        if (fourMPoints.isNotEmpty()) {
            repository.addHistoryPoints("4M", fourMPoints)
            backfillAuditCount += fourMPoints.size
            hourlyBackfillTotal += fourMPoints.size
        }

        results.filter { it.first != RibbonScale.FOUR_MIN }.forEach { (scale, point) ->
            repository.addHistoryPoint(scale.key, mapToAppPoint(point))
        }
    }

    private fun fillRealGap(lastTickTs: Long, lastTickRt: Long, now: Long, nowRt: Long, isTrackerMode: Boolean) {
        val snrSamples = if (isTrackerMode) {
            gpsManager.getSnrSamples(lastTickTs, now).map { EngineSnrSample(it.first, it.first - (now - nowRt), it.second) }
        } else emptyList()

        val sensorSamples = if (isTrackerMode) {
            sensorManager.getSensorSamples(lastTickTs, now).map { 
                EngineSensorSnapshot(it.ts, it.rt, it.acoustic, it.lux, it.vibe, it.proxIdx, it.lift, it.tilt, it.isSitDetected) 
            }
        } else emptyList()

        RibbonScale.entries.forEach { scale ->
            val gapPoints = aggregator.fillRealGap(scale.key, scale.intervalSeconds, lastTickRt, nowRt, lastTickTs, now, snrSamples, sensorSamples, locationProcessor.getAcousticFloorDb())
            if (gapPoints.isNotEmpty()) {
                repository.addHistoryPoints(scale.key, gapPoints.map { mapToAppPoint(it) })
            }
        }
    }

    private fun detectClockTampering(nowWall: Long) {
        val monotonic = timeProvider.elapsedRealtime()
        val currentDrift = nowWall - monotonic
        
        if (clockDriftRef == 0L) {
            clockDriftRef = currentDrift
            scope?.launch { repository.saveLong(MainRepository.CLOCK_DRIFT_REF_KEY, currentDrift) }
            return
        }
        
        val delta = abs(currentDrift - clockDriftRef)
        if (delta > DRIFT_TOLERANCE_MS) {
            val jumpSec = delta / 1000
            val direction = if (currentDrift > clockDriftRef) "forward" else "backward"
            listener?.onLogEvent("FORENSIC ALERT: System clock jump detected ($direction ${jumpSec}s). Monotonic uptime preserved.", true)
            clockDriftRef = currentDrift
            scope?.launch { repository.saveLong(MainRepository.CLOCK_DRIFT_REF_KEY, currentDrift) }
        }
    }

    private fun applySitDuplicateGuard(isDetected: Boolean, ts: Long, rt: Long): Boolean {
        if (!isDetected) return false
        if (abs(rt - lastSitDetectedRt) < SIT_DUPLICATE_GUARD_MS) {
            return false
        }
        lastSitDetectedRt = rt
        scope?.launch {
            repository.saveLong(MainRepository.LAST_HISTORY_SIT_TS_KEY, ts)
        }
        return true
    }

    private fun mapToAppPoint(p: EngineConnectionPoint): ConnectionPoint {
        return ConnectionPoint(
            ts = p.ts,
            rtt = p.rtt,
            localSig = DEFAULT_SIGNAL_STRENGTH,
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
            sitDz = p.sitDz,
            sitBaro = p.sitBaro,
            sitTilt = p.sitTilt,
            sitShock = p.sitShock
        )
    }

    private fun handleHourlyAutoSave(hour: Int) {
        scope?.launch {
            if (lastProcessedHour != hour) {
                val repoHour = repository.getInt("last_auto_save_hour", -1)
                if (repoHour != hour) {
                    lastProcessedHour = hour
                    repository.saveIntSync("last_auto_save_hour", hour)
                    
                    if (hourlyBackfillTotal > 0) {
                        listener?.onLogEvent("Forensic: Hourly Continuity Audit - Backfilled $hourlyBackfillTotal points to maintain 1Hz fidelity during power-save ticks.", false)
                        hourlyBackfillTotal = 0
                    }

                    listener?.onLogEvent("Hourly auto-export", false)
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
                    if (repository.getString("last_daily_cleanup_date", "") != todayDate) {
                        lastCleanupDate = todayDate
                        repository.saveStringSync("last_daily_cleanup_date", todayDate)
                        listener?.onLogEvent("Periodic daily cleanup of trails", true)
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
                    if (repository.getString("last_daily_archive_date", "") != todayDate) {
                        lastArchiveDate = todayDate
                        repository.saveStringSync("last_daily_archive_date", todayDate)
                        listener?.onLogEvent("Periodic daily archiving of old files", true)
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
