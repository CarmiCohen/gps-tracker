package com.gps19.app

import com.gps19.core.engine.*
import org.json.JSONObject
import timber.log.Timber

/**
 * TelemetryMapper: Centralized authority for telemetry data transformation.
 * Sep.28.1:
 * - Issue #1205: Context-Aware Power Optimization. Propagated activityType 
 *   completely across all mapping paths, including mapSnapshotToHealth.
 * Sep.27.17:
 * - Issue #1160: Flyweight & Pooling Expansion. Refactored mapping logic to 
 *   support zero-allocation "out" parameters for high-frequency evaluation paths.
 * Sep.26.12:
 * - Issue #1344: Added thermalHeadroom and heapAllocatedMb to all mapping paths.
 */
object TelemetryMapper {

    /**
     * mapSnapshotToUpdate: Authority for converting an engine snapshot into a 
     * persistence-ready LocationUpdate.
     */
    fun mapSnapshotToUpdate(
        snapshot: SystemEvaluationSnapshot,
        processed: ProcessedLocation?,
        isMe: Boolean,
        ts: Long,
        out: LocationUpdate
    ): LocationUpdate {
        val speed = processed?.filteredSpeed ?: snapshot.kinetic.speed
        val trackerState = when {
            speed > 0.5 -> TrackerState.MOVING
            else -> TrackerState.PARKING
        }

        return out.apply {
            kinetic.copyFrom(snapshot.kinetic)
            kinetic.activityType = snapshot.activityType
            atmospheric.copyFrom(snapshot.atmospheric)
            integrity.copyFrom(snapshot.integrity)
            this.status = snapshot.status
            this.ts = ts
            this.isMe = isMe
            this.trackerState = trackerState
            this.isClockRegression = snapshot.isClockRegression
            this.lastValidFixRt = snapshot.lastValidFixRt
        }
    }

    /**
     * mapSnapshotToStatus: Authority for converting an engine snapshot and 
     * event context into a signaling-ready TrackerStatus DTO.
     */
    fun mapSnapshotToStatus(
        snapshot: SystemEvaluationSnapshot,
        processed: ProcessedLocation?,
        deviceId: String,
        viewerId: String,
        now: Long,
        nowRt: Long,
        gnssDetail: GnssDetail? = null,
        isSuspiciousMode: Boolean = false,
        lastSitTs: Long = 0L,
        out: TrackerStatus
    ): TrackerStatus {
        val speed = processed?.filteredSpeed ?: snapshot.kinetic.speed
        val trackerState = when {
            speed > 0.5 -> TrackerState.MOVING
            else -> TrackerState.PARKING
        }

        return out.apply {
            this.deviceId = deviceId
            this.viewerId = viewerId
            
            kinetic.apply {
                copyFrom(snapshot.kinetic)
                lat = processed?.optimizedPoint?.lat ?: snapshot.kinetic.lat
                lng = processed?.optimizedPoint?.lng ?: snapshot.kinetic.lng
                alt = processed?.optimizedPoint?.alt ?: snapshot.kinetic.alt
                this.speed = speed
                accuracy = processed?.currentAccuracy ?: snapshot.kinetic.accuracy
                maxAccuracy = processed?.maxAccuracy ?: snapshot.kinetic.maxAccuracy
                gpsTs = processed?.timestamp ?: snapshot.kinetic.gpsTs
                isJump = snapshot.kinetic.isJump
                kineticEnergy = snapshot.kinetic.kineticEnergy
                isAdaptiveJump = snapshot.isAdaptiveJump
                this.activityType = snapshot.activityType
            }

            atmospheric.copyFrom(snapshot.atmospheric)

            integrity.apply {
                copyFrom(snapshot.integrity)
                this.gnssDetail = gnssDetail ?: snapshot.integrity.gnssDetail
                isSitActive = snapshot.integrity.isSitActive
                isSitDetected = isSuspiciousMode
                this.lastSitTs = lastSitTs
                tamperNote = snapshot.suppressionNote
                thermalHeadroom = snapshot.thermalHeadroom
                heapAllocatedMb = snapshot.heapAllocatedMb
            }

            this.status = snapshot.status
            this.ts = now
            this.rt = nowRt
            this.trackerState = trackerState
            this.isClockRegression = snapshot.isClockRegression
            this.lastValidFixRt = snapshot.lastValidFixRt
            this.isSilentFailure = snapshot.isSilentFailure
            this.isBatteryWhitelisted = snapshot.integrity.battery > 0
        }
    }

    /**
     * mapStatusToUpdate: Authority for converting a TrackerStatus DTO into a 
     * persistence-ready LocationUpdate.
     */
    fun mapStatusToUpdate(s: TrackerStatus, isMe: Boolean, out: LocationUpdate): LocationUpdate {
        return out.apply {
            kinetic.copyFrom(s.kinetic)
            atmospheric.copyFrom(s.atmospheric)
            integrity.copyFrom(s.integrity)
            status = s.status
            ts = s.ts
            this.isMe = isMe
            trackerState = s.trackerState
            isClockRegression = s.isClockRegression
            lastValidFixRt = s.lastValidFixRt
        }
    }

    /**
     * mapProtoToSnapshot: Converts an incoming Proto DTO into an engine snapshot.
     */
    fun mapProtoToSnapshot(proto: RealtimeStatus, now: Long, nowRt: Long, out: SystemEvaluationSnapshot): SystemEvaluationSnapshot {
        return out.apply {
            reset()
            kinetic.apply {
                lat = proto.lat; lng = proto.lng; alt = proto.alt
                speed = proto.speed.coerceAtLeast(0.0)
                gpsTs = proto.gpsTs; accuracy = proto.accuracy.coerceAtLeast(0.0)
                bearing = proto.bearing; maxAccuracy = proto.maxAccuracy
                kineticEnergy = proto.kineticEnergy
            }
            integrity.apply {
                isJammer = proto.isJammer
                isStalled = proto.isStalled
                isTamperDetected = proto.isTamperDetected || proto.isLocationPending
                satsUsed = proto.satsUsed
                satsView = proto.satsView
                snrIdx = proto.snrIdx
            }
            snrSnapshot = proto.snrIdx * 5.0
            jumpTier = proto.jumpTier
            isJammer = proto.isJammer
            isStalled = proto.isStalled
            tamperDetected = proto.isTamperDetected || proto.isLocationPending
            nowTs = now; this.nowRt = nowRt
            
            activityType = try { 
                ActivityType.valueOf(proto.activityType) 
            } catch (e: Exception) { ActivityType.UNKNOWN }
        }
    }

    /**
     * mapProtoToStatus: Updates tracker status with Proto data and processor results.
     */
    fun mapProtoToStatus(
        proto: RealtimeStatus,
        current: TrackerStatus,
        processed: ProcessedLocation,
        now: Long,
        lastFixRt: Long,
        out: TrackerStatus
    ): TrackerStatus {
        out.copyFrom(current)
        return out.apply {
            kinetic.apply {
                lat = if (processed.optimizedPoint.lat != 0.0) processed.optimizedPoint.lat else current.lat
                lng = if (processed.optimizedPoint.lng != 0.0) processed.optimizedPoint.lng else current.lng
                gpsTs = if (processed.optimizedPoint.ts != 0L) processed.optimizedPoint.ts else current.gpsTs
                speed = processed.filteredSpeed
                bearing = proto.bearing
                accuracy = proto.accuracy
                maxAccuracy = proto.maxAccuracy
                isJump = processed.status == SentinelStatus.JUMP
                kineticEnergy = proto.kineticEnergy
                isAdaptiveJump = proto.isAdaptiveJump
                jumpTier = proto.jumpTier
                activityType = try { 
                    ActivityType.valueOf(proto.activityType) 
                } catch (e: Exception) { ActivityType.UNKNOWN }
            }

            atmospheric.apply {
                temp = proto.temp
                noiseIdx = proto.noiseIdx
                luxIdx = proto.luxIdx
                vibeIdx = proto.vibeIdx
                liftIdx = proto.liftIdx
                tiltIdx = proto.tiltIdx
                baroIdx = proto.baroIdx
            }

            integrity.apply {
                battery = proto.battery
                isCharging = proto.isCharging
                satsView = proto.satsView
                satsUsed = proto.satsUsed
                snrIdx = proto.snrIdx
                isLocationPending = proto.isLocationPending
                locationPendingReason = TrackerStatus.mapProtoToPendingReason(proto.pendingReason.name)
                isBatterySteepDischarge = proto.isBatterySteepDischarge
                isCoolingModeActive = proto.isCoolingModeActive
                isBatteryLow = proto.isBatteryLow
                isBatteryCritical = proto.isBatteryCritical
                isSitDetected = proto.isSitDetected
                lastSitTs = proto.lastSitTs
                sitVz = proto.sitVz
                sitVzTs = proto.sitVzTs
                sitVzRt = proto.sitVzRt
                sitDz = proto.sitDz
                sitBaro = proto.sitBaro
                sitTilt = proto.sitTilt
                sitShock = proto.sitShock
                isSitActive = proto.isSitActive
                uptimeMs = proto.uptimeMs
                totalConnectedMs = proto.totalConnectedMs
                sessionConnectedMs = proto.sessionConnectedMs
                totalDropMs = proto.totalDropMs
                maxDropMs = proto.maxDropMs
                lastConnTs = proto.lastConnTs
                lastDiscTs = proto.lastDiscTs
                isJammer = proto.isJammer
                isStalled = proto.isStalled
                isTamperDetected = proto.isTamperDetected
                isPowerTamper = proto.isPowerTamper
                violationUptimeMs = proto.violationUptimeMs
                isUltraLongStationary = proto.isUltraLongStationary
                gpsHardwareLock = proto.gpsHardwareLock
                isGnssThrottled = proto.isGnssThrottled
                tamperNote = if (proto.hasTamperNote()) proto.tamperNote else null
            }

            status = processed.status
            ts = now
            trackerState = TrackerStatus.mapProtoToTrackerState(proto.state.name)
            isClockRegression = proto.isClockRegression
            lastValidFixRt = lastFixRt
        }
    }

    /**
     * mapJsonToSnapshot: Converts an incoming JSON payload into an engine snapshot.
     */
    fun mapJsonToSnapshot(data: JSONObject, current: TrackerStatus, now: Long, nowRt: Long, out: SystemEvaluationSnapshot): SystemEvaluationSnapshot {
        val incomingGpsTs = data.optLong("gps_ts", 0L)
        val gpsAgeMs = if (data.has("gps_age_ms")) data.optLong("gps_age_ms") else (if (incomingGpsTs > 0) maxOf(0L, now - incomingGpsTs) else 0L)
        val candidateTs = if (gpsAgeMs > 0 || incomingGpsTs > 0) now - gpsAgeMs else 0L
        
        val statusStr = data.optString("status", current.status.name)
        val statusVar = try { SentinelStatus.valueOf(statusStr) } catch(e: Exception) { current.status }

        return out.apply {
            reset()
            kinetic.apply {
                lat = data.optDouble("lat", 0.0); lng = data.optDouble("lng", 0.0); alt = data.optDouble("alt", 0.0)
                speed = data.optDouble("speed", 0.0).coerceAtLeast(0.0)
                gpsTs = candidateTs; accuracy = data.optDouble("accuracy", 0.0).coerceAtLeast(0.0)
                bearing = data.optDouble("bearing", 0.0)
                maxAccuracy = data.optDouble("max_accuracy", 0.0)
                kineticEnergy = data.optDouble("kinetic_energy", current.kineticEnergy)
            }
            integrity.apply {
                satsUsed = data.optInt("sats_used", -1)
                satsView = data.optInt("sats_view", -1)
                snrIdx = data.optDouble("snr_idx", current.snrIdx)
            }
            atmospheric.apply {
                noiseIdx = data.optDouble("noise_idx", current.noiseIdx)
                luxIdx = data.optDouble("lux_idx", current.luxIdx)
                vibeIdx = data.optDouble("vibe_idx", current.vibeIdx)
                liftIdx = data.optDouble("lift_idx", current.liftIdx)
                tiltIdx = data.optDouble("tilt_idx", current.tiltIdx)
                baroIdx = data.optDouble("baro_idx", current.baroIdx)
            }
            jumpTier = data.optInt("jump_tier", 0)
            isJammer = data.optBoolean("is_jammer", false)
            isStalled = data.optDouble("is_stalled", 0.0) != 0.0 || data.optBoolean("is_stalled", false)
            tamperDetected = data.optBoolean("is_tamper_detected", current.isTamperDetected) || 
                             data.optBoolean("is_location_pending", false) || 
                             statusVar == SentinelStatus.TAMPER
            nowTs = now; this.nowRt = nowRt
            thermalHeadroom = data.optDouble("thermal_headroom", 0.0)
            heapAllocatedMb = data.optDouble("heap_allocated_mb", 0.0)
            
            activityType = try { 
                ActivityType.valueOf(data.optString("activity_type", current.activityType.name)) 
            } catch (e: Exception) { ActivityType.UNKNOWN }
        }
    }

    /**
     * mapJsonToStatus: Updates tracker status with JSON data and processor results.
     */
    fun mapJsonToStatus(
        data: JSONObject,
        current: TrackerStatus,
        processed: ProcessedLocation,
        now: Long,
        lastFixRt: Long,
        gnssDetail: GnssDetail?,
        out: TrackerStatus
    ): TrackerStatus {
        val statusStr = data.optString("status", current.status.name)
        val statusVar = try { SentinelStatus.valueOf(statusStr) } catch(e: Exception) { current.status }

        out.copyFrom(current)
        return out.apply {
            kinetic.apply {
                lat = if (processed.optimizedPoint.lat != 0.0) processed.optimizedPoint.lat else current.lat
                lng = if (processed.optimizedPoint.lng != 0.0) processed.optimizedPoint.lng else current.lng
                gpsTs = if (processed.optimizedPoint.ts != 0L) processed.optimizedPoint.ts else current.gpsTs
                speed = processed.filteredSpeed
                bearing = data.optDouble("bearing", current.bearing)
                accuracy = data.optDouble("accuracy", current.accuracy)
                maxAccuracy = data.optDouble("max_accuracy", current.maxAccuracy)
                isJump = processed.status == SentinelStatus.JUMP
                kineticEnergy = data.optDouble("kinetic_energy", current.kineticEnergy)
                isAdaptiveJump = data.optBoolean("is_adaptive_jump", current.isAdaptiveJump)
                jumpTier = data.optInt("jump_tier", current.jumpTier)
                verticalVelocity = data.optDouble("vertical_velocity", current.verticalVelocity)
                activityType = try { 
                    ActivityType.valueOf(data.optString("activity_type", current.activityType.name)) 
                } catch (e: Exception) { ActivityType.UNKNOWN }
            }

            atmospheric.apply {
                temp = data.optDouble("temp", current.temp)
                maxTemp = data.optDouble("max_temp", current.maxTemp)
                vibration = data.optDouble("vibration", current.vibration)
                heading = data.optDouble("heading", current.heading)
                baroAlt = data.optDouble("baro_alt", current.baroAlt)
                lux = data.optDouble("lux", current.lux)
                isNear = data.optBoolean("is_near", current.isNear)
                tiltDegrees = data.optDouble("tilt_degrees", current.tiltDegrees)
                acousticDb = data.optDouble("acoustic_db", current.acousticDb)
                peakVibrationShock = data.optDouble("peak_vibration_shock", current.peakVibrationShock)
                peakVibrationShockTs = data.optLong("peak_shock_ts", current.peakVibrationShockTs)
                luxBaseline = data.optDouble("lux_baseline", current.luxBaseline)
                acousticFloorDb = data.optDouble("acoustic_floor_db", current.acousticFloorDb)
                adaptiveVibrationFloor = data.optDouble("adaptive_vibration_floor", current.adaptiveVibrationFloor)
                proxIdx = data.optDouble("prox_idx", current.proxIdx)
                proximityCm = data.optDouble("proximity_cm", current.proximityCm)
                proximityDebounceMs = data.optLong("proximity_debounce_ms", current.proximityDebounceMs)
                vibrationRollingSum = data.optDouble("vibration_rolling_sum", current.vibrationRollingSum)
                noiseIdx = data.optDouble("noise_idx", current.noiseIdx)
                luxIdx = data.optDouble("lux_idx", current.luxIdx)
                vibeIdx = data.optDouble("vibe_idx", current.vibeIdx)
                liftIdx = data.optDouble("lift_idx", current.liftIdx)
                tiltIdx = data.optDouble("tilt_idx", current.tiltIdx)
                baroIdx = data.optDouble("baro_idx", current.baroIdx)
            }

            integrity.apply {
                battery = data.optInt("battery", current.battery)
                currentMa = data.optInt("current_ma", current.currentMa)
                isCharging = data.optBoolean("is_charging", current.isCharging)
                satsView = data.optInt("sats_view", current.satsView)
                satsUsed = data.optInt("sats_used", current.satsUsed)
                isTamperDetected = data.optBoolean("is_tamper_detected", current.isTamperDetected)
                isPowerTamper = data.optBoolean("is_power_tamper", current.isPowerTamper)
                isLocationPending = data.optBoolean("is_location_pending", false)
                locationPendingReason = try { LocationPendingReason.valueOf(data.optString("location_pending_reason", "NONE")) } catch(e: Exception) { LocationPendingReason.NONE }
                isBatterySteepDischarge = data.optBoolean("is_battery_steep_discharge", false)
                isCoolingModeActive = data.optBoolean("is_cooling_mode_active", false)
                isBatteryLow = data.optBoolean("is_battery_low", false)
                isBatteryCritical = data.optBoolean("is_battery_critical", false)
                isPowerSaveMode = data.optBoolean("is_power_save_mode", current.isPowerSaveMode)
                standbyBucket = data.optInt("standby_bucket", current.standbyBucket)
                netInterface = data.optString("net_interface", current.netInterface)
                isStorageLow = data.optBoolean("is_storage_low", current.isStorageLow)
                isStorageCritical = data.optBoolean("is_storage_critical", current.isStorageCritical)
                this.gnssDetail = gnssDetail
                uptimeMs = data.optLong("uptime_ms", current.uptimeMs)
                totalDropMs = data.optLong("total_drop_ms", current.totalDropMs)
                maxDropMs = data.optLong("max_drop_ms", current.maxDropMs)
                maxDropTs = data.optLong("max_drop_ts", current.maxDropTs)
                totalConnectedMs = data.optLong("total_connected_ms", current.totalConnectedMs)
                sessionConnectedMs = data.optLong("session_connected_ms", current.sessionConnectedMs)
                lastConnTs = data.optLong("last_conn_ts", current.lastConnTs)
                lastDiscTs = data.optLong("last_disc_ts", current.lastDiscTs)
                snrIdx = data.optDouble("snr_idx", current.snrIdx)
                isSitDetected = data.optBoolean("is_sit_detected", current.isSitDetected)
                lastSitTs = data.optLong("last_sit_ts", current.lastSitTs)
                isSuspicious = data.optBoolean("is_suspicious", current.isSuspicious)
                isAnchorLocked = data.optBoolean("is_anchor_locked", current.isAnchorLocked)
                sitVz = data.optDouble("sit_vz", current.sitVz)
                sitDz = data.optDouble("sit_dz", current.sitDz)
                sitBaro = data.optDouble("sit_baro", current.sitBaro)
                sitTilt = data.optDouble("sit_tilt", current.sitTilt)
                sitShock = data.optDouble("sit_shock", current.sitShock)
                isSitActive = data.optBoolean("is_sit_active", current.isSitActive)
                violationUptimeMs = data.optLong("violation_uptime_ms", current.violationUptimeMs)
                isUltraLongStationary = data.optBoolean("is_ultra_long_stationary", current.isUltraLongStationary)
                gpsHardwareLock = data.optBoolean("gps_hw_lock", current.gpsHardwareLock)
                isGnssThrottled = data.optBoolean("is_gnss_throttled", current.isGnssThrottled)
                tamperNote = if (data.has("tamper_note")) data.getString("tamper_note") else null
                thermalHeadroom = data.optDouble("thermal_headroom", 0.0)
                heapAllocatedMb = data.optDouble("heap_allocated_mb", 0.0)
            }

            this.status = statusVar
            this.ts = now
            this.trackerState = try { TrackerState.valueOf(data.optString("tracker_state", "UNKNOWN")) } catch(e: Exception) { current.trackerState }
            this.isClockRegression = processed.isClockRegression
            this.lastValidFixRt = lastFixRt
        }
    }

    /**
     * mapSnapshotToHealth: Synchronizes evaluation health state from engine snapshot.
     */
    fun mapSnapshotToHealth(snapshot: SystemEvaluationSnapshot, health: SystemHealthState) {
        health.update(
            signalLoss = snapshot.integrity.isLocationPending && snapshot.integrity.locationPendingReason == LocationPendingReason.SIGNAL_LOSS, 
            gpsStalled = snapshot.integrity.isStalled, 
            gpsHardwareLock = snapshot.integrity.gpsHardwareLock, 
            localInternetLoss = snapshot.localInternetLoss,
            isHardwareOnline = snapshot.isHardwareOnline, 
            batteryLevel = snapshot.integrity.battery, 
            batteryTemp = snapshot.atmospheric.temp,
            isCharging = snapshot.integrity.isCharging, 
            currentMa = snapshot.integrity.currentMa, 
            status = snapshot.status, 
            isJammer = snapshot.integrity.isJammer,
            isTamperDetected = snapshot.tamperDetected,
            tiltDegrees = snapshot.atmospheric.tiltDegrees, 
            acousticDb = snapshot.atmospheric.acousticDb, 
            baroAlt = snapshot.atmospheric.baroAlt, 
            lux = snapshot.atmospheric.lux, 
            isNear = snapshot.atmospheric.isNear, 
            luxBaseline = snapshot.atmospheric.luxBaseline, 
            acousticFloorDb = snapshot.atmospheric.acousticFloorDb, 
            adaptiveVibrationFloor = snapshot.atmospheric.adaptiveVibrationFloor, 
            peakVibrationShock = snapshot.atmospheric.peakVibrationShock,
            isPowerSaveMode = snapshot.integrity.isPowerSaveMode, 
            standbyBucket = snapshot.integrity.standbyBucket, 
            netInterface = snapshot.integrity.netInterface,
            isStorageLow = snapshot.integrity.isStorageLow, 
            isStorageCritical = snapshot.integrity.isStorageCritical,
            isBatterySteepDischarge = snapshot.integrity.isBatterySteepDischarge, 
            isCoolingModeActive = snapshot.integrity.isCoolingModeActive,
            vibration = snapshot.vibeSnapshot ?: snapshot.atmospheric.vibration, 
            cpuLoad = snapshot.cpuLoad, 
            ioWait = snapshot.ioWait, 
            maxIoLatency = snapshot.maxIoLatency, 
            isSilentFailure = snapshot.isSilentFailure, 
            isMaliAnomaly = snapshot.isMaliAnomaly, 
            isUltraLongStationary = snapshot.integrity.isUltraLongStationary,
            isBatteryLow = snapshot.integrity.isBatteryLow, 
            isBatteryCritical = snapshot.integrity.isBatteryCritical,
            tamperNote = snapshot.suppressionNote,
            isPowerTamper = snapshot.integrity.isPowerTamper,
            isLocationPending = snapshot.integrity.isLocationPending,
            locationPendingReason = snapshot.integrity.locationPendingReason,
            coolingEnteredRt = snapshot.nowRt,
            thermalHeadroom = snapshot.thermalHeadroom,
            heapAllocatedMb = snapshot.heapAllocatedMb,
            activityType = snapshot.activityType
        )
    }

    /**
     * mapProcessedToSnapshot: Refines engine snapshot with local processor results.
     */
    fun mapProcessedToSnapshot(
        snapshot: SystemEvaluationSnapshot,
        processed: ProcessedLocation,
        rawGpsTs: Long,
        lastValidFixRt: Long,
        snrSnapshot: Double?,
        out: SystemEvaluationSnapshot
    ): SystemEvaluationSnapshot {
        out.copyFrom(snapshot)
        return out.apply {
            this.status = processed.status
            this.isJammer = processed.jammerDetected
            this.jumpTier = processed.jumpTier
            this.isAdaptiveJump = processed.isAdaptiveJump
            kinetic.apply {
                lat = processed.optimizedPoint.lat
                lng = processed.optimizedPoint.lng
                accuracy = processed.currentAccuracy
                maxAccuracy = processed.maxAccuracy
                gpsTs = rawGpsTs
                speed = processed.filteredSpeed
                kineticEnergy = processed.kineticEnergy
                this.activityType = snapshot.activityType
            }
            this.lastValidFixRt = lastValidFixRt
            tamperDetected = processed.tamperDetected
            suppressionNote = processed.suppressionNote
            this.snrSnapshot = snrSnapshot
        }
    }

    /**
     * mapStatusToSnapshot: Authority for converting remote tracker status into 
     * a local evaluation snapshot.
     */
    fun mapStatusToSnapshot(
        s: TrackerStatus,
        base: SystemEvaluationSnapshot,
        nowRt: Long,
        out: SystemEvaluationSnapshot
    ): SystemEvaluationSnapshot {
        out.copyFrom(base)
        return out.apply {
            this.status = s.status
            this.isJammer = s.isJammer
            this.jumpTier = s.jumpTier
            this.isAdaptiveJump = s.isAdaptiveJump
            kinetic.copyFrom(s.kinetic)
            integrity.copyFrom(s.integrity)
            atmospheric.copyFrom(s.atmospheric)
            this.lastValidFixRt = s.lastValidFixRt
            tamperDetected = s.isTamperDetected
            suppressionNote = s.tamperNote
            this.isStalled = s.isStalled
            this.isClockRegression = s.isClockRegression || (nowRt - s.lastValidFixRt > 30000L)
            snrSnapshot = s.snrIdx * 5.0
            thermalHeadroom = s.integrity.thermalHeadroom
            heapAllocatedMb = s.integrity.heapAllocatedMb
            this.activityType = s.activityType
        }
    }

    /**
     * Maps core and forensic fields from EngineConnectionPoint to ConnectionPoint.
     */
    fun mapEngineToApp(p: EngineConnectionPoint, out: ConnectionPoint) {
        out.apply {
            ts = p.ts; rt = p.rt; rtt = p.rtt; localSig = 10; remoteSig = p.remoteSig
            isConnected = p.isConnected; isGap = p.isGap; isRecoveryEvent = p.isRecoveryEvent
            hasGps = p.hasGps; isTick = p.isTick; gpsAccuracy = p.accuracy; maxAccuracy = p.maxAccuracy
            speed = p.speed; bearing = p.bearing; currentMa = p.currentMa
            locationPendingReason = p.locationPendingReason; isUltraLongStationary = p.isUltraLongStationary
            violationUptimeMs = p.violationUptimeMs; gpsHardwareLock = p.gpsHardwareLock

            // Forensic Parity
            snrIdx = p.snrIdx; noiseIdx = p.noiseIdx; luxIdx = p.luxIdx; vibeIdx = p.vibeIdx
            proxIdx = p.proxIdx; liftIdx = p.liftIdx; tiltIdx = p.tiltIdx; baroIdx = p.baroIdx
            isSitDetected = p.isSitDetected; isSitActive = p.isSitActive; verticalVelocity = p.verticalVelocity
            sitVz = p.sitVz; sitVzTs = p.sitVzTs; sitVzRt = p.sitVzRt; sitDz = p.sitDz
            sitBaro = p.sitBaro; sitTilt = p.sitTilt; sitShock = p.sitShock; kineticEnergy = p.kineticEnergy
            isBatterySteepDischarge = p.isBatterySteepDischarge; isCoolingModeActive = p.isCoolingModeActive
            isBatteryLow = p.isBatteryLow; isBatteryCritical = p.isBatteryCritical; cpuLoad = p.cpuLoad
            ioWait = p.ioWait; maxIoLatency = p.maxIoLatency; isSilentFailure = p.isSilentFailure
            thermalHeadroom = p.thermalHeadroom; heapAllocatedMb = p.heapAllocatedMb
            activityType = p.activityType
        }
    }

    /**
     * mapEntityToApp: Authority for converting a HistoryEntity into a 
     * UI-ready ConnectionPoint.
     */
    fun mapEntityToApp(entity: HistoryEntity, out: ConnectionPoint) {
        out.apply {
            ts = entity.ts; rt = entity.rt; rtt = entity.rtt; isConnected = entity.isConnected
            isGap = entity.isGap; isRecoveryEvent = entity.isRecoveryEvent; hasGps = entity.hasGps
            isTick = entity.isTick; gpsAccuracy = entity.accuracy; maxAccuracy = entity.maxAccuracy
            speed = entity.speed; bearing = entity.bearing; currentMa = entity.currentMa
            locationPendingReason = try { LocationPendingReason.valueOf(entity.locationPendingReason) } catch(e: Exception) { LocationPendingReason.NONE }

            // Forensic Parity
            gpsIndex = entity.gpsIndex; snrIdx = entity.snrIdx; noiseIdx = entity.noiseIdx
            luxIdx = entity.luxIdx; vibeIdx = entity.vibeIdx; proxIdx = entity.proxIdx
            liftIdx = entity.liftIdx; tiltIdx = entity.tiltIdx; baroIdx = entity.baroIdx
            isSitDetected = entity.isSitDetected; isSitActive = entity.isSitActive
            verticalVelocity = entity.verticalVelocity; sitVz = entity.sitVz; sitVzTs = entity.sitVzTs
            sitVzRt = entity.sitVzRt; sitDz = entity.sitDz; sitBaro = entity.sitBaro
            sitTilt = entity.sitTilt; sitShock = entity.sitShock
            isBatterySteepDischarge = entity.isBatterySteepDischarge; isCoolingModeActive = entity.isCoolingModeActive
            isBatteryLow = entity.isBatteryLow; isBatteryCritical = entity.isBatteryCritical
            violationUptimeMs = entity.violationUptimeMs; isUltraLongStationary = entity.isUltraLongStationary
            gpsHardwareLock = entity.gpsHardwareLock; isAnchorLocked = entity.isAnchorLocked
            thermalHeadroom = entity.thermalHeadroom; heapAllocatedMb = entity.heapAllocatedMb
            activityType = try { 
                ActivityType.valueOf(entity.activityType) 
            } catch (e: Exception) { ActivityType.UNKNOWN }
        }
    }

    /**
     * mapAppToEntity: Authority for converting a ConnectionPoint into a 
     * persistence-ready HistoryEntity.
     */
    fun mapAppToEntity(p: ConnectionPoint, ribbonKey: String): HistoryEntity {
        return HistoryEntity(
            ts = p.ts, rt = p.rt, rtt = p.rtt, isConnected = p.isConnected, isGap = p.isGap,
            isRecoveryEvent = p.isRecoveryEvent, hasGps = p.hasGps, isTick = p.isTick, ribbonKey = ribbonKey,
            gpsIndex = p.gpsIndex, noiseIdx = p.noiseIdx, luxIdx = p.luxIdx, vibeIdx = p.vibeIdx,
            proxIdx = p.proxIdx, liftIdx = p.liftIdx, snrIdx = p.snrIdx, tiltIdx = p.tiltIdx,
            baroIdx = p.baroIdx, verticalVelocity = p.verticalVelocity, sitVz = p.sitVz,
            sitVzTs = p.sitVzTs, sitVzRt = p.sitVzRt, sitDz = p.sitDz,
            isBatterySteepDischarge = p.isBatterySteepDischarge, remoteSig = p.remoteSig,
            isCoolingModeActive = p.isCoolingModeActive, speed = p.speed, bearing = p.bearing,
            isSitDetected = p.isSitDetected, isSitActive = p.isSitActive, sitBaro = p.sitBaro,
            sitTilt = p.sitTilt, sitShock = p.sitShock, currentMa = p.currentMa,
            locationPendingReason = p.locationPendingReason.name, accuracy = p.gpsAccuracy,
            maxAccuracy = p.maxAccuracy, isAnchorLocked = p.isAnchorLocked, isBatteryLow = p.isBatteryLow,
            isBatteryCritical = p.isBatteryCritical, violationUptimeMs = p.violationUptimeMs,
            isUltraLongStationary = p.isUltraLongStationary, gpsHardwareLock = p.gpsHardwareLock,
            thermalHeadroom = p.thermalHeadroom, heapAllocatedMb = p.heapAllocatedMb,
            activityType = p.activityType.name
        )
    }

    /**
     * mapStatusToPending: Authority for converting a TrackerStatus into a 
     * persistence-ready PendingStatusEntity.
     */
    fun mapStatusToPending(status: TrackerStatus): PendingStatusEntity {
        return PendingStatusEntity(
            lat = status.lat, lng = status.lng, speed = status.speed, accuracy = status.accuracy,
            bearing = status.bearing, battery = status.battery, temp = status.temp,
            isCharging = status.isCharging, currentMa = status.currentMa, timestamp = status.ts,
            gpsTs = status.gpsTs, satsView = status.satsView, satsUsed = status.satsUsed,
            maxAccuracy = status.maxAccuracy, snrIdx = status.snrIdx, noiseIdx = status.noiseIdx,
            luxIdx = status.luxIdx, vibeIdx = status.vibeIdx, proxIdx = status.proxIdx,
            liftIdx = status.liftIdx, tiltIdx = status.tiltIdx, baroIdx = status.baroIdx,
            isBatterySteepDischarge = status.isBatterySteepDischarge, isCoolingModeActive = status.isCoolingModeActive,
            isSitDetected = status.isSitDetected, isSitActive = status.isSitActive, sitVz = status.sitVz,
            sitVzTs = status.sitVzTs, sitVzRt = status.sitVzRt, sitDz = status.sitDz,
            verticalVelocity = status.verticalVelocity, sitBaro = status.sitBaro, sitTilt = status.sitTilt,
            sitShock = status.sitShock, isStorageLow = status.isStorageLow,
            isStorageCritical = status.isStorageCritical, isPowerSaveMode = status.isPowerSaveMode,
            standbyBucket = status.standbyBucket, netInterface = status.netInterface,
            lastValidFixRt = status.lastValidFixRt, locationPendingReason = status.locationPendingReason.name,
            isAnchorLocked = status.isAnchorLocked, trackerState = status.trackerState.name,
            status = status.status.name, isBatteryLow = status.isBatteryLow,
            isBatteryCritical = status.isBatteryCritical, isUltraLongStationary = status.isUltraLongStationary,
            violationUptimeMs = status.violationUptimeMs, gpsHardwareLock = status.gpsHardwareLock,
            isGnssThrottled = status.isGnssThrottled, thermalHeadroom = status.integrity.thermalHeadroom,
            heapAllocatedMb = status.integrity.heapAllocatedMb, activityType = status.activityType.name
        )
    }

    /**
     * mapPendingToStatus: Authority for converting a PendingStatusEntity back 
     * into a domain TrackerStatus.
     */
    fun mapPendingToStatus(entity: PendingStatusEntity, deviceId: String, viewerId: String, out: TrackerStatus): TrackerStatus {
        out.apply {
            reset()
            this.deviceId = deviceId
            this.viewerId = viewerId
            
            kinetic.apply {
                lat = entity.lat; lng = entity.lng; speed = entity.speed
                accuracy = entity.accuracy; maxAccuracy = entity.maxAccuracy
                bearing = entity.bearing; gpsTs = entity.gpsTs
                verticalVelocity = entity.verticalVelocity; kineticEnergy = 0.0
                isAdaptiveJump = false
                activityType = try { 
                    ActivityType.valueOf(entity.activityType) 
                } catch (e: Exception) { ActivityType.UNKNOWN }
            }

            atmospheric.apply {
                temp = entity.temp; noiseIdx = entity.noiseIdx; luxIdx = entity.luxIdx
                vibeIdx = entity.vibeIdx; liftIdx = entity.liftIdx; tiltIdx = entity.tiltIdx
                baroIdx = entity.baroIdx; proxIdx = entity.proxIdx
            }

            integrity.apply {
                battery = entity.battery; isCharging = entity.isCharging; currentMa = entity.currentMa
                satsView = entity.satsView; satsUsed = entity.satsUsed; snrIdx = entity.snrIdx
                isBatterySteepDischarge = entity.isBatterySteepDischarge
                isCoolingModeActive = entity.isCoolingModeActive
                isSitDetected = entity.isSitDetected; isSitActive = entity.isSitActive
                sitVz = entity.sitVz; sitVzTs = entity.sitVzTs; sitVzRt = entity.sitVzRt
                sitDz = entity.sitDz; sitBaro = entity.sitBaro; sitTilt = entity.sitTilt
                sitShock = entity.sitShock; isStorageLow = entity.isStorageLow
                isStorageCritical = entity.isStorageCritical; isPowerSaveMode = entity.isPowerSaveMode
                standbyBucket = entity.standbyBucket; netInterface = entity.netInterface
                locationPendingReason = try { LocationPendingReason.valueOf(entity.locationPendingReason) } catch(e: Exception) { LocationPendingReason.NONE }
                isAnchorLocked = entity.isAnchorLocked
                isBatteryLow = entity.isBatteryLow; isBatteryCritical = entity.isBatteryCritical
                isUltraLongStationary = entity.isUltraLongStationary
                violationUptimeMs = entity.violationUptimeMs
                gpsHardwareLock = entity.gpsHardwareLock
                isGnssThrottled = entity.isGnssThrottled
                thermalHeadroom = entity.thermalHeadroom
                heapAllocatedMb = entity.heapAllocatedMb
            }

            ts = entity.timestamp
            status = try { SentinelStatus.valueOf(entity.status) } catch(e: Exception) { SentinelStatus.VALID }
            trackerState = try { TrackerState.valueOf(entity.trackerState) } catch(e: Exception) { TrackerState.UNKNOWN }
            lastValidFixRt = entity.lastValidFixRt
        }
        return out
    }
}
