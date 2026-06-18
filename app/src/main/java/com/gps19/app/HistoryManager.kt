package com.gps19.app

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.gps19.core.engine.*
import com.gps19.core.engine.LocationProcessor // Explicit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * HistoryManager: Manages the periodic recording of connection metrics (ribbons).
 * v8.9.2:
 * - Issue 182: Synchronized source headers with v8.9.2 baseline.
 * - Issue 135: Added verticalVelocity to updateRibbons and backfillGaps for forensic parity.
 */
class HistoryManager(
    private val context: Context,
    private val repository: MainRepository,
    private val gpsManager: GpsManager?,
    private val sensorManager: AppSensorManager?,
    private val locationProcessor: LocationProcessor,
    private val timeProvider: TimeProvider,
    private val scope: LifecycleCoroutineScope,
    private val onLogEvent: (String, Boolean) -> Unit
) {
    private var lastProcessedHour = -1
    private var lastCleanupDate = ""
    private var lastArchiveDate = ""

    private var clockDriftRef: Long = 0L
    private val aggregator = TelemetryAggregator()
    
    private var backfillAuditCount = 0
    private var hourlyBackfillTotal = 0
    private var lastAuditTs = 0L
    private var lastTimeTriggerTs = 0L

    suspend fun updateRibbons(
        now: Long,
        lastTickTs: Long,
        serviceTickCounter: Int,
        rtt: Int,
        peerSignal: Int,
        peerAvail: Boolean,
        hasGps: Boolean,
        isTrackerMode: Boolean,
        gpsIndex: Float = 0f,
        noiseIdx: Float = 0f,
        luxIdx: Float = 0f,
        vibeIdx: Float = 0f,
        proxIdx: Float = 1f,
        liftIdx: Float = 0f,
        snrIdx: Float = 0f,
        verticalVelocity: Float = 0f,
        sitVz: Float = 0f,
        sitDz: Float = 0f,
        sitBaro: Float = 0f,
        sitTilt: Float = 0f,
        sitShock: Float = 0f,
        isBatterySteepDischarge: Boolean = false,
        isCoolingModeActive: Boolean = false,
        speed: Float = 0f,
        bearing: Float = 0f,
        isSitDetected: Boolean = false,
        isSitActive: Boolean = false
    ) {
        detectClockTampering(now)

        val deltaMs = now - lastTickTs
        
        if (now - lastAuditTs > 60000L) {
            if (backfillAuditCount > 0) {
                onLogEvent("Forensic: 4M Continuity Audit - Backfilled $backfillAuditCount points in last 60s to maintain 1Hz resolution during idle.", false)
                backfillAuditCount = 0
            }
            lastAuditTs = now
        }

        if (lastTickTs > 0 && deltaMs > REAL_TIME_GAP_LIMIT_MS) {
            fillRealGap(lastTickTs, now, isTrackerMode)
        } else if (lastTickTs > 0 && deltaMs > 1500L) {
            backfillAnalyticalGaps(
                lastTickTs = lastTickTs,
                now = now,
                rtt = rtt,
                peerSignal = peerSignal,
                peerAvail = peerAvail,
                hasGps = hasGps,
                isTrackerMode = isTrackerMode,
                gpsIndex = gpsIndex,
                noiseIdx = noiseIdx,
                luxIdx = luxIdx,
                vibeIdx = vibeIdx,
                proxIdx = proxIdx,
                liftIdx = liftIdx,
                snrIdx = snrIdx,
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
                isSitActive = isSitActive
            )
        }

        val currentPoint = EngineConnectionPoint(
            ts = now,
            rtt = rtt,
            remoteSig = peerSignal,
            isConnected = peerAvail,
            isGap = false,
            hasGps = hasGps,
            gpsIndex = gpsIndex,
            noiseIdx = noiseIdx,
            luxIdx = luxIdx,
            vibeIdx = vibeIdx,
            proxIdx = proxIdx,
            liftIdx = liftIdx,
            snrIdx = snrIdx,
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
            isTick = false 
        )

        processResults(aggregator.processPoint(currentPoint))

        // Only perform expensive time-based logic once per minute to avoid 1Hz Calendar allocations
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
        now: Long,
        rtt: Int,
        peerSignal: Int,
        peerAvail: Boolean,
        hasGps: Boolean,
        isTrackerMode: Boolean,
        gpsIndex: Float,
        noiseIdx: Float,
        luxIdx: Float,
        vibeIdx: Float,
        proxIdx: Float,
        liftIdx: Float,
        snrIdx: Float,
        verticalVelocity: Float,
        sitVz: Float,
        sitDz: Float,
        sitBaro: Float,
        sitTilt: Float,
        sitShock: Float,
        isBatterySteepDischarge: Boolean,
        isCoolingModeActive: Boolean,
        speed: Float,
        bearing: Float,
        isSitDetected: Boolean,
        isSitActive: Boolean
    ) {
        val snrSamples = if (isTrackerMode && gpsManager != null) {
            gpsManager.getSnrSamples(lastTickTs + 1, now).map { EngineSnrSample(it.first, it.second) }
        } else emptyList()

        val sensorSamples = if (isTrackerMode && sensorManager != null) {
            sensorManager.getSensorSamples(lastTickTs + 1, now).map { 
                EngineSensorSnapshot(it.ts, it.acoustic, it.lux, it.vibe, it.proxIdx, it.lift, it.isSitDetected) 
            }
        } else emptyList()

        val acousticFloor = if (isTrackerMode) locationProcessor.getAcousticFloorDb() else 0.0

        val baseTemplate = EngineConnectionPoint(
            ts = 0L,
            rtt = rtt,
            remoteSig = peerSignal,
            isConnected = peerAvail,
            hasGps = hasGps,
            gpsIndex = gpsIndex,
            noiseIdx = noiseIdx,
            luxIdx = luxIdx,
            vibeIdx = vibeIdx,
            proxIdx = proxIdx,
            liftIdx = liftIdx,
            snrIdx = snrIdx,
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
            isSitActive = isSitActive
        )

        val results = aggregator.backfillGaps(lastTickTs, now, snrSamples, sensorSamples, acousticFloor, baseTemplate)
        
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

    private fun fillRealGap(lastTickTs: Long, now: Long, isTrackerMode: Boolean) {
        val snrSamples = if (isTrackerMode && gpsManager != null) {
            gpsManager.getSnrSamples(lastTickTs, now).map { EngineSnrSample(it.first, it.second) }
        } else emptyList()

        val sensorSamples = if (isTrackerMode && sensorManager != null) {
            sensorManager.getSensorSamples(lastTickTs, now).map { 
                EngineSensorSnapshot(it.ts, it.acoustic, it.lux, it.vibe, it.proxIdx, it.lift, it.isSitDetected) 
            }
        } else emptyList()

        val acousticFloor = if (isTrackerMode) locationProcessor.getAcousticFloorDb() else 0.0

        RibbonScale.values().forEach { scale ->
            val gapPoints = aggregator.fillRealGap(scale.key, scale.intervalSeconds, lastTickTs, now, snrSamples, sensorSamples, acousticFloor)
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
            return
        }
        
        val delta = abs(currentDrift - clockDriftRef)
        if (delta > DRIFT_TOLERANCE_MS) {
            val jumpSec = delta / 1000
            val direction = if (currentDrift > clockDriftRef) "forward" else "backward"
            onLogEvent("FORENSIC ALERT: System clock jump detected ($direction ${jumpSec}s). Monotonic uptime preserved.", true)
            clockDriftRef = currentDrift
        }
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
            gpsIndex = p.gpsIndex,
            noiseIdx = p.noiseIdx,
            luxIdx = p.luxIdx,
            vibeIdx = p.vibeIdx,
            proxIdx = p.proxIdx,
            liftIdx = p.liftIdx,
            snrIdx = p.snrIdx,
            verticalVelocity = p.verticalVelocity,
            sitVz = p.sitVz,
            sitDz = p.sitDz,
            sitBaro = p.sitBaro,
            sitTilt = p.sitTilt,
            sitShock = p.sitShock,
            isBatterySteepDischarge = p.isBatterySteepDischarge,
            isCoolingModeActive = p.isCoolingModeActive,
            speed = p.speed,
            bearing = p.bearing,
            isSitDetected = p.isSitDetected,
            isSitActive = p.isSitActive
        )
    }

    private fun handleHourlyAutoSave(hour: Int) {
        scope.launch {
            if (lastProcessedHour != hour) {
                val repoHour = repository.getInt("last_auto_save_hour", -1)
                if (repoHour != hour) {
                    lastProcessedHour = hour
                    repository.saveIntSync("last_auto_save_hour", hour)
                    
                    if (hourlyBackfillTotal > 0) {
                        onLogEvent("Forensic: Hourly Continuity Audit - Backfilled $hourlyBackfillTotal points to maintain 1Hz fidelity during power-save ticks.", false)
                        hourlyBackfillTotal = 0
                    }

                    onLogEvent("Hourly auto-export", false)
                    scope.launch(Dispatchers.IO) {
                        MainFileHelper.autoExportData(context, repository, timeProvider)
                    }
                } else {
                    lastProcessedHour = hour
                }
            }
        }
    }

    private fun handleDailyCleanup(calendar: Calendar, hour: Int, minute: Int) {
        scope.launch {
            if (hour == DAILY_CLEANUP_HOUR && minute == DAILY_CLEANUP_MINUTE) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayDate = dateFormat.format(calendar.time)
                if (lastCleanupDate != todayDate) {
                    if (repository.getString("last_daily_cleanup_date", "") != todayDate) {
                        lastCleanupDate = todayDate
                        repository.saveStringSync("last_daily_cleanup_date", todayDate)
                        onLogEvent("Periodic daily cleanup of trails", true)
                        repository.clearTrails()
                    } else {
                        lastCleanupDate = todayDate
                    }
                }
            }
        }
    }

    private fun handleDailyArchiving(calendar: Calendar, hour: Int, minute: Int) {
        scope.launch {
            if (hour == DAILY_ARCHIVE_HOUR && minute == DAILY_ARCHIVE_MINUTE) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayDate = dateFormat.format(calendar.time)
                if (lastArchiveDate != todayDate) {
                    if (repository.getString("last_daily_archive_date", "") != todayDate) {
                        lastArchiveDate = todayDate
                        repository.saveStringSync("last_daily_archive_date", todayDate)
                        onLogEvent("Periodic daily archiving of old files", true)
                        scope.launch(Dispatchers.IO) {
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
