package com.gps19.app

import com.gps19.core.engine.*
import org.json.JSONObject
import timber.log.Timber

/**
 * TelemetryMapper: Centralized authority for telemetry data transformation.
 * Oct.8.2:
 * - Issue #SIMP-1007-17: Behavioral Reason Promotion. Verified parity in 
 *   priority-based health reason mapping.
 * Oct.7.11:
 * - Issue #SIMP-1007-17: Strategic Simplification. Updated mapTickToOutputs and 
 *   mapProcessedToSnapshot to promote behavioral pending reasons from the processor.
 * Oct.7.7:
 * - Issue #SIMP-1007-16: Flag Propagation. Updated mapping to handle 
 *   isSuspiciousNoise and isMemoryPressureThrottled across Proto and JSON layers.
 */
object TelemetryMapper {

    /**
     * mapTickToOutputs: Consolidated authority for preparing persistence and 
     * signaling DTOs from a tick event.
     */
    fun mapTickToOutputs(
        event: DomainEvent.TickEvaluated,
        deviceId: String,
        viewerId: String,
        lastAlarmAckTs: Long,
        violationStartTs: Long,
        updateOut: LocationUpdate
    ) {
        val snapshot = event.snapshot
        val proc = event.processed
        val now = event.now
        val nowRt = event.nowRt

        snapshot.apply {
            this.ts = now
            this.rt = nowRt
            this.deviceId = deviceId
            this.viewerId = viewerId
            this.isMe = true
            
            if (event.isTrackerMode) {
                this.lastAlarmAckTs = lastAlarmAckTs
                this.violationStartTs = violationStartTs
            }

            proc?.let {
                kinetic.lat = it.optimizedPoint.lat
                kinetic.lng = it.optimizedPoint.lng
                kinetic.alt = it.optimizedPoint.alt
                kinetic.speed = it.filteredSpeed
                kinetic.accuracy = it.currentAccuracy
                kinetic.maxAccuracy = it.maxAccuracy
                kinetic.gpsTs = it.timestamp
                this.status = it.status

                // Oct.7.11: Promote behavioral health reason from processor
                if (it.locationPendingReason != LocationPendingReason.NONE) {
                    this.integrity.locationPendingReason = SentinelValidator.getHigherPriorityReason(
                        this.integrity.locationPendingReason,
                        it.locationPendingReason
                    )
                    this.integrity.isLocationPending = true
                }
            }

            integrity.gnssDetail = event.gnssDetail ?: integrity.gnssDetail
            integrity.isSitDetected = event.isSuspiciousMode
            integrity.lastSitTs = event.lastSitTs
            
            this.trackerState = snapshot.trackerState
            this.isClockRegression = snapshot.isClockRegression
            this.lastValidFixRt = snapshot.lastValidFixRt
            this.integrity.isSilentFailure = snapshot.integrity.isSilentFailure
            this.integrity.isBatteryWhitelisted = integrity.battery > 0
        }

        updateOut.copyFrom(snapshot)
    }

    /**
     * mapProtoToStatus: Updates location update with Proto data and processor results.
     */
    fun mapProtoToStatus(
        proto: RealtimeStatus,
        current: LocationUpdate,
        processed: ProcessedLocation,
        now: Long,
        lastFixRt: Long,
        out: LocationUpdate
    ): LocationUpdate {
        out.copyFrom(current)
        return out.apply {
            deviceId = proto.id
            viewerId = proto.viewerId
            
            kinetic.apply {
                lat = if (processed.optimizedPoint.lat != 0.0) processed.optimizedPoint.lat else current.kinetic.lat
                lng = if (processed.optimizedPoint.lng != 0.0) processed.optimizedPoint.lng else current.kinetic.lng
                gpsTs = if (processed.optimizedPoint.ts != 0L) processed.optimizedPoint.ts else current.kinetic.gpsTs
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
                locationPendingReason = try { LocationPendingReason.valueOf(proto.pendingReason.name.removePrefix("LPR_")) } catch(e: Exception) { LocationPendingReason.NONE }
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
                thermalHeadroom = proto.thermalHeadroom
                heapAllocatedMb = proto.heapAllocatedMb

                // Oct.7.11 behavioral health priority resolution
                if (processed.locationPendingReason != LocationPendingReason.NONE) {
                    locationPendingReason = SentinelValidator.getHigherPriorityReason(
                        locationPendingReason,
                        processed.locationPendingReason
                    )
                    isLocationPending = true
                }
            }

            this.thermalSnapshot = if (proto.hasThermalSnapshot()) proto.thermalSnapshot else null
            this.heapSnapshot = if (proto.hasHeapSnapshot()) proto.heapSnapshot else null
            
            // Oct.7.7 Anomaly Flags
            this.isSuspiciousNoise = proto.isSuspiciousNoise
            this.isMemoryPressureThrottled = proto.isMemoryPressureThrottled

            status = processed.status
            ts = now
            trackerState = try { TrackerState.valueOf(proto.state.name.removePrefix("TS_")) } catch(e: Exception) { TrackerState.UNKNOWN }
            isClockRegression = proto.isClockRegression
            lastValidFixRt = lastFixRt
            lastAlarmAckTs = proto.lastAlarmAckTs
            violationStartTs = proto.violationStartTs
        }
    }

    /**
     * mapJsonToStatus: Updates location update with JSON data and processor results.
     */
    fun mapJsonToStatus(
        data: JSONObject,
        current: LocationUpdate,
        processed: ProcessedLocation,
        now: Long,
        lastFixRt: Long,
        gnssDetail: GnssDetail?,
        out: LocationUpdate
    ): LocationUpdate {
        val statusStr = data.optString("status", current.status.name)
        val statusVar = try { SentinelStatus.valueOf(statusStr) } catch(e: Exception) { current.status }

        out.copyFrom(current)
        return out.apply {
            deviceId = data.optString("id", current.deviceId)
            viewerId = data.optString("viewer_id", current.viewerId)

            kinetic.apply {
                lat = if (processed.optimizedPoint.lat != 0.0) processed.optimizedPoint.lat else current.kinetic.lat
                lng = if (processed.optimizedPoint.lng != 0.0) processed.optimizedPoint.lng else current.kinetic.lng
                gpsTs = if (processed.optimizedPoint.ts != 0L) processed.optimizedPoint.ts else current.kinetic.gpsTs
                speed = processed.filteredSpeed
                bearing = data.optDouble("bearing", current.kinetic.bearing)
                accuracy = data.optDouble("accuracy", current.kinetic.accuracy)
                maxAccuracy = data.optDouble("max_accuracy", current.kinetic.maxAccuracy)
                isJump = processed.status == SentinelStatus.JUMP
                kineticEnergy = data.optDouble("kinetic_energy", current.kinetic.kineticEnergy)
                isAdaptiveJump = data.optBoolean("is_adaptive_jump", current.kinetic.isAdaptiveJump)
                jumpTier = data.optInt("jump_tier", current.kinetic.jumpTier)
                verticalVelocity = data.optDouble("vertical_velocity", current.kinetic.verticalVelocity)
                activityType = try { 
                    ActivityType.valueOf(data.optString("activity_type", current.kinetic.activityType.name)) 
                } catch (e: Exception) { ActivityType.UNKNOWN }
            }

            atmospheric.apply {
                temp = data.optDouble("temp", current.atmospheric.temp)
                maxTemp = data.optDouble("max_temp", current.atmospheric.maxTemp)
                vibration = data.optDouble("vibration", current.atmospheric.vibration)
                heading = data.optDouble("heading", current.atmospheric.heading)
                baroAlt = data.optDouble("baro_alt", current.atmospheric.baroAlt)
                lux = data.optDouble("lux", current.atmospheric.lux)
                isNear = data.optBoolean("is_near", current.atmospheric.isNear)
                tiltDegrees = data.optDouble("tilt_degrees", current.atmospheric.tiltDegrees)
                acousticDb = data.optDouble("acoustic_db", current.atmospheric.acousticDb)
                peakVibrationShock = data.optDouble("peak_vibration_shock", current.atmospheric.peakVibrationShock)
                peakVibrationShockTs = data.optLong("peak_shock_ts", current.atmospheric.peakVibrationShockTs)
                luxBaseline = data.optDouble("lux_baseline", current.atmospheric.luxBaseline)
                acousticFloorDb = data.optDouble("acoustic_floor_db", current.atmospheric.acousticFloorDb)
                adaptiveVibrationFloor = data.optDouble("adaptive_vibration_floor", current.atmospheric.adaptiveVibrationFloor)
                proxIdx = data.optDouble("prox_idx", current.atmospheric.proxIdx)
                proximityCm = data.optDouble("proximity_cm", current.atmospheric.proximityCm)
                proximityDebounceMs = data.optLong("proximity_debounce_ms", current.atmospheric.proximityDebounceMs)
                vibrationRollingSum = data.optDouble("vibration_rolling_sum", current.atmospheric.vibrationRollingSum)
                noiseIdx = data.optDouble("noise_idx", current.atmospheric.noiseIdx)
                luxIdx = data.optDouble("lux_idx", current.atmospheric.luxIdx)
                vibeIdx = data.optDouble("vibe_idx", current.atmospheric.vibeIdx)
                liftIdx = data.optDouble("lift_idx", current.atmospheric.liftIdx)
                tiltIdx = data.optDouble("tilt_idx", current.atmospheric.tiltIdx)
                baroIdx = data.optDouble("baro_idx", current.atmospheric.baroIdx)
            }

            integrity.apply {
                battery = data.optInt("battery", current.integrity.battery)
                currentMa = data.optInt("current_ma", current.integrity.currentMa)
                isCharging = data.optBoolean("is_charging", current.integrity.isCharging)
                satsView = data.optInt("sats_view", current.integrity.satsView)
                satsUsed = data.optInt("sats_used", current.integrity.satsUsed)
                isTamperDetected = data.optBoolean("is_tamper_detected", current.integrity.isTamperDetected)
                isPowerTamper = data.optBoolean("is_power_tamper", current.integrity.isPowerTamper)
                isLocationPending = data.optBoolean("is_location_pending", false)
                locationPendingReason = try { LocationPendingReason.valueOf(data.optString("location_pending_reason", "NONE")) } catch(e: Exception) { LocationPendingReason.NONE }
                isBatterySteepDischarge = data.optBoolean("is_battery_steep_discharge", false)
                isCoolingModeActive = data.optBoolean("is_cooling_mode_active", false)
                isBatteryLow = data.optBoolean("is_battery_low", false)
                isBatteryCritical = data.optBoolean("is_battery_critical", false)
                isPowerSaveMode = data.optBoolean("is_power_save_mode", current.integrity.isPowerSaveMode)
                standbyBucket = data.optInt("standby_bucket", current.integrity.standbyBucket)
                netInterface = data.optString("net_interface", current.integrity.netInterface)
                isStorageLow = data.optBoolean("is_storage_low", current.integrity.isStorageLow)
                isStorageCritical = data.optBoolean("is_storage_critical", current.integrity.isStorageCritical)
                this.gnssDetail = gnssDetail
                uptimeMs = data.optLong("uptime_ms", current.integrity.uptimeMs)
                totalDropMs = data.optLong("total_drop_ms", current.integrity.totalDropMs)
                maxDropMs = data.optLong("max_drop_ms", current.integrity.maxDropMs)
                maxDropTs = data.optLong("max_drop_ts", current.integrity.maxDropTs)
                totalConnectedMs = data.optLong("total_connected_ms", current.integrity.totalConnectedMs)
                sessionConnectedMs = data.optLong("session_connected_ms", current.integrity.sessionConnectedMs)
                lastConnTs = data.optLong("last_conn_ts", current.integrity.lastConnTs)
                lastDiscTs = data.optLong("last_disc_ts", current.integrity.lastDiscTs)
                snrIdx = data.optDouble("snr_idx", current.integrity.snrIdx)
                isSitDetected = data.optBoolean("is_sit_detected", current.integrity.isSitDetected)
                lastSitTs = data.optLong("last_sit_ts", current.integrity.lastSitTs)
                isSuspicious = data.optBoolean("is_suspicious", current.integrity.isSuspicious)
                isAnchorLocked = data.optBoolean("is_anchor_locked", current.integrity.isAnchorLocked)
                sitVz = data.optDouble("sit_vz", current.integrity.sitVz)
                sitDz = data.optDouble("sit_dz", current.integrity.sitDz)
                sitBaro = data.optDouble("sit_baro", current.integrity.sitBaro)
                sitTilt = data.optDouble("sit_tilt", current.integrity.sitTilt)
                sitShock = data.optDouble("sit_shock", current.integrity.sitShock)
                isSitActive = data.optBoolean("is_sit_active", current.integrity.isSitActive)
                violationUptimeMs = data.optLong("violation_uptime_ms", current.integrity.violationUptimeMs)
                isUltraLongStationary = data.optBoolean("is_ultra_long_stationary", current.integrity.isUltraLongStationary)
                gpsHardwareLock = data.optBoolean("gps_hw_lock", current.integrity.gpsHardwareLock)
                isGnssThrottled = data.optBoolean("is_gnss_throttled", current.integrity.isGnssThrottled)
                tamperNote = if (data.has("tamper_note")) data.getString("tamper_note") else null
                thermalHeadroom = data.optDouble("thermal_headroom", 0.0)
                heapAllocatedMb = data.optDouble("heap_allocated_mb", 0.0)

                // Oct.7.11 behavioral health priority resolution
                if (processed.locationPendingReason != LocationPendingReason.NONE) {
                    locationPendingReason = SentinelValidator.getHigherPriorityReason(
                        locationPendingReason,
                        processed.locationPendingReason
                    )
                    isLocationPending = true
                }
            }

            this.thermalSnapshot = if (data.has("thermal_snapshot")) data.optDouble("thermal_snapshot") else null
            this.heapSnapshot = if (data.has("heap_snapshot")) data.optDouble("heap_snapshot") else null
            
            // Oct.7.7 Anomaly Flags
            this.isSuspiciousNoise = data.optBoolean("is_suspicious_noise", current.isSuspiciousNoise)
            this.isMemoryPressureThrottled = data.optBoolean("is_memory_pressure_throttled", current.isMemoryPressureThrottled)

            this.status = statusVar
            this.ts = now
            this.trackerState = try { TrackerState.valueOf(data.optString("tracker_state", current.trackerState.name)) } catch(e: Exception) { current.trackerState }
            this.isClockRegression = processed.isClockRegression
            this.lastValidFixRt = lastFixRt
            lastAlarmAckTs = data.optLong("last_alarm_ack_ts", 0L)
            violationStartTs = data.optLong("violation_start_ts", 0L)
        }
    }

    /**
     * mapProtoToSnapshot: Converts an incoming Proto DTO into an engine snapshot.
     */
    fun mapProtoToSnapshot(proto: RealtimeStatus, now: Long, nowRt: Long, out: LocationUpdate): LocationUpdate {
        return out.apply {
            reset()
            deviceId = proto.id
            viewerId = proto.viewerId
            
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
                currentMa = proto.currentMa
                thermalHeadroom = proto.thermalHeadroom
                heapAllocatedMb = proto.heapAllocatedMb
            }
            
            this.thermalSnapshot = if (proto.hasThermalSnapshot()) proto.thermalSnapshot else null
            this.heapSnapshot = if (proto.hasHeapSnapshot()) proto.heapSnapshot else null
            
            // Oct.7.7 Anomaly Flags
            this.isSuspiciousNoise = proto.isSuspiciousNoise
            this.isMemoryPressureThrottled = proto.isMemoryPressureThrottled

            snrSnapshot = proto.snrIdx * 5.0
            kinetic.jumpTier = proto.jumpTier
            integrity.isJammer = proto.isJammer
            integrity.isStalled = proto.isStalled
            integrity.isTamperDetected = proto.isTamperDetected || proto.isLocationPending
            nowTs = now; this.nowRt = nowRt
            trackerState = try { TrackerState.valueOf(proto.state.name.removePrefix("TS_")) } catch(e: Exception) { TrackerState.UNKNOWN }
            
            kinetic.activityType = try { 
                ActivityType.valueOf(proto.activityType) 
            } catch (e: Exception) { ActivityType.UNKNOWN }
            
            lastAlarmAckTs = proto.lastAlarmAckTs
            violationStartTs = proto.violationStartTs
        }
    }

    /**
     * mapJsonToSnapshot: Converts an incoming JSON payload into an engine snapshot.
     */
    fun mapJsonToSnapshot(data: JSONObject, current: LocationUpdate, now: Long, nowRt: Long, out: LocationUpdate): LocationUpdate {
        val incomingGpsTs = data.optLong("gps_ts", 0L)
        val gpsAgeMs = if (data.has("gps_age_ms")) data.optLong("gps_age_ms") else (if (incomingGpsTs > 0) maxOf(0L, now - incomingGpsTs) else 0L)
        val candidateTs = if (gpsAgeMs > 0 || incomingGpsTs > 0) now - gpsAgeMs else 0L
        
        val statusStr = data.optString("status", current.status.name)
        val statusVar = try { SentinelStatus.valueOf(statusStr) } catch(e: Exception) { current.status }

        return out.apply {
            reset()
            deviceId = data.optString("id", current.deviceId)
            viewerId = data.optString("viewer_id", current.viewerId)

            kinetic.apply {
                lat = data.optDouble("lat", 0.0); lng = data.optDouble("lng", 0.0); alt = data.optDouble("alt", 0.0)
                speed = data.optDouble("speed", 0.0).coerceAtLeast(0.0)
                gpsTs = candidateTs; accuracy = data.optDouble("accuracy", 0.0).coerceAtLeast(0.0)
                bearing = data.optDouble("bearing", 0.0)
                maxAccuracy = data.optDouble("max_accuracy", 0.0)
                kineticEnergy = data.optDouble("kinetic_energy", current.kinetic.kineticEnergy)
            }
            integrity.apply {
                satsUsed = data.optInt("sats_used", -1)
                satsView = data.optInt("sats_view", -1)
                snrIdx = data.optDouble("snr_idx", current.integrity.snrIdx)
                thermalHeadroom = data.optDouble("thermal_headroom", 0.0)
                heapAllocatedMb = data.optDouble("heap_allocated_mb", 0.0)
            }
            atmospheric.apply {
                noiseIdx = data.optDouble("noise_idx", current.atmospheric.noiseIdx)
                luxIdx = data.optDouble("lux_idx", current.atmospheric.luxIdx)
                vibeIdx = data.optDouble("vibe_idx", current.atmospheric.vibeIdx)
                liftIdx = data.optDouble("lift_idx", current.atmospheric.liftIdx)
                tiltIdx = data.optDouble("tilt_idx", current.atmospheric.tiltIdx)
                baroIdx = data.optDouble("baro_idx", current.atmospheric.baroIdx)
            }
            kinetic.jumpTier = data.optInt("jump_tier", 0)
            integrity.isJammer = data.optBoolean("is_jammer", false)
            integrity.isStalled = data.optDouble("is_stalled", 0.0) != 0.0 || data.optBoolean("is_stalled", false)
            integrity.isTamperDetected = data.optBoolean("is_tamper_detected", current.integrity.isTamperDetected) || 
                             data.optBoolean("is_location_pending", false) || 
                             statusVar == SentinelStatus.TAMPER
            nowTs = now; this.nowRt = nowRt
            
            this.thermalSnapshot = if (data.has("thermal_snapshot")) data.optDouble("thermal_snapshot") else null
            this.heapSnapshot = if (data.has("heap_snapshot")) data.optDouble("heap_snapshot") else null
            
            // Oct.7.7 Anomaly Flags
            this.isSuspiciousNoise = data.optBoolean("is_suspicious_noise", false)
            this.isMemoryPressureThrottled = data.optBoolean("is_memory_pressure_throttled", false)

            trackerState = try { TrackerState.valueOf(data.optString("tracker_state", current.trackerState.name)) } catch(e: Exception) { current.trackerState }
            
            kinetic.activityType = try { 
                ActivityType.valueOf(data.optString("activity_type", current.kinetic.activityType.name)) 
            } catch (e: Exception) { ActivityType.UNKNOWN }
            
            lastAlarmAckTs = data.optLong("last_alarm_ack_ts", 0L)
            violationStartTs = data.optLong("violation_start_ts", 0L)
        }
    }

    /**
     * mapSnapshotToHealth: Synchronizes evaluation health state from engine snapshot.
     */
    fun mapSnapshotToHealth(snapshot: LocationUpdate, health: SystemHealthState) {
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
            isTamperDetected = snapshot.integrity.isTamperDetected,
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
            vibration = snapshot.snrSnapshot ?: snapshot.atmospheric.vibration, 
            cpuLoad = snapshot.integrity.cpuLoad, 
            ioWait = snapshot.integrity.ioWait, 
            maxIoLatency = snapshot.integrity.maxIoLatency, 
            isSilentFailure = snapshot.integrity.isSilentFailure, 
            isMaliAnomaly = snapshot.integrity.isMaliAnomaly, 
            isUltraLongStationary = snapshot.integrity.isUltraLongStationary,
            isBatteryLow = snapshot.integrity.isBatteryLow, 
            isBatteryCritical = snapshot.integrity.isBatteryCritical,
            tamperNote = snapshot.suppressionNote,
            isPowerTamper = snapshot.integrity.isPowerTamper,
            isLocationPending = snapshot.integrity.isLocationPending,
            locationPendingReason = snapshot.integrity.locationPendingReason,
            coolingEnteredRt = snapshot.nowRt,
            thermalHeadroom = snapshot.integrity.thermalHeadroom,
            heapAllocatedMb = snapshot.integrity.heapAllocatedMb,
            thermalSnapshot = snapshot.thermalSnapshot,
            heapSnapshot = snapshot.heapSnapshot,
            isSuspiciousNoise = snapshot.isSuspiciousNoise,
            isMemoryPressureThrottled = snapshot.isMemoryPressureThrottled,
            activityType = snapshot.activityType
        )
    }

    /**
     * mapProcessedToSnapshot: Refines engine snapshot with local processor results.
     */
    fun mapProcessedToSnapshot(
        snapshot: LocationUpdate,
        processed: ProcessedLocation,
        rawGpsTs: Long,
        lastValidFixRt: Long,
        snrSnapshot: Double?,
        out: LocationUpdate
    ): LocationUpdate {
        out.copyFrom(snapshot)
        return out.apply {
            this.status = processed.status
            this.integrity.isJammer = processed.jammerDetected
            this.kinetic.jumpTier = processed.jumpTier
            this.kinetic.isAdaptiveJump = processed.isAdaptiveJump
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
            this.integrity.isTamperDetected = processed.tamperDetected
            suppressionNote = processed.suppressionNote
            this.snrSnapshot = snrSnapshot

            // Oct.7.11 behavioral health priority resolution
            if (processed.locationPendingReason != LocationPendingReason.NONE) {
                this.integrity.locationPendingReason = SentinelValidator.getHigherPriorityReason(
                    this.integrity.locationPendingReason,
                    processed.locationPendingReason
                )
                this.integrity.isLocationPending = true
            }
        }
    }

    /**
     * mapStatusToSnapshot: Authority for converting remote tracker update into 
     * a local evaluation snapshot.
     */
    fun mapStatusToSnapshot(
        s: LocationUpdate,
        base: LocationUpdate,
        nowRt: Long,
        out: LocationUpdate
    ): LocationUpdate {
        out.copyFrom(base)
        return out.apply {
            this.status = s.status
            this.integrity.isJammer = s.integrity.isJammer
            this.kinetic.jumpTier = s.kinetic.jumpTier
            this.kinetic.isAdaptiveJump = s.kinetic.isAdaptiveJump
            kinetic.copyFrom(s.kinetic)
            integrity.copyFrom(s.integrity)
            atmospheric.copyFrom(s.atmospheric)
            this.lastValidFixRt = s.lastValidFixRt
            this.integrity.isTamperDetected = s.integrity.isTamperDetected
            suppressionNote = s.integrity.tamperNote
            this.integrity.isStalled = s.integrity.isStalled
            this.isClockRegression = s.isClockRegression || (nowRt - s.lastValidFixRt > 30000L)
            snrSnapshot = s.integrity.snrIdx * 5.0
            this.integrity.thermalHeadroom = s.integrity.thermalHeadroom
            this.integrity.heapAllocatedMb = s.integrity.heapAllocatedMb
            this.thermalSnapshot = s.thermalSnapshot
            this.heapSnapshot = s.heapSnapshot
            this.isSuspiciousNoise = s.isSuspiciousNoise
            this.isMemoryPressureThrottled = s.isMemoryPressureThrottled
            this.kinetic.activityType = s.activityType
            this.trackerState = s.trackerState
            this.lastAlarmAckTs = s.lastAlarmAckTs
            this.violationStartTs = s.violationStartTs
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

            snrIdx = PhysicsUtils.safeDouble(p.snrIdx)
            noiseIdx = PhysicsUtils.safeDouble(p.noiseIdx)
            luxIdx = PhysicsUtils.safeDouble(p.luxIdx)
            vibeIdx = PhysicsUtils.safeDouble(p.vibeIdx)
            proxIdx = PhysicsUtils.safeDouble(p.proxIdx)
            liftIdx = PhysicsUtils.safeDouble(p.liftIdx)
            tiltIdx = PhysicsUtils.safeDouble(p.tiltIdx)
            baroIdx = PhysicsUtils.safeDouble(p.baroIdx)
            
            isSitDetected = p.isSitDetected; isSitActive = p.isSitActive; verticalVelocity = p.verticalVelocity
            sitVz = p.sitVz; sitVzTs = p.sitVzTs; sitVzRt = p.sitVzRt; sitDz = p.sitDz
            sitBaro = p.sitBaro; sitTilt = p.sitTilt; sitShock = p.sitShock; kineticEnergy = p.kineticEnergy
            isBatterySteepDischarge = p.isBatterySteepDischarge; isCoolingModeActive = p.isCoolingModeActive
            isBatteryLow = p.isBatteryLow; isBatteryCritical = p.isBatteryCritical; cpuLoad = p.cpuLoad
            ioWait = p.ioWait; maxIoLatency = p.maxIoLatency; isSilentFailure = p.isSilentFailure
            thermalHeadroom = p.thermalHeadroom; heapAllocatedMb = p.heapAllocatedMb
            activityType = p.activityType
            thermalSnapshot = p.thermalSnapshot
            heapSnapshot = p.heapSnapshot
            
            // Oct.7.7 Anomaly Flags
            isSuspiciousNoise = p.forensic.isSuspiciousNoise
            isMemoryPressureThrottled = p.forensic.isMemoryPressureThrottled
        }
    }

    /**
     * mapProtoToApp: Authority for converting a TrackerStatusProto into a 
     * UI-ready ConnectionPoint.
     */
    fun mapProtoToApp(proto: TrackerStatusProto, out: ConnectionPoint) {
        out.apply {
            ts = proto.ts; rt = proto.rt; rtt = proto.rtt; isConnected = true
            isGap = false; isRecoveryEvent = false; hasGps = proto.accuracy > 0
            isTick = false; gpsAccuracy = proto.accuracy; maxAccuracy = proto.maxAccuracy
            speed = proto.speed; bearing = proto.bearing; currentMa = proto.currentMa
            locationPendingReason = try { LocationPendingReason.valueOf(proto.locationPendingReason.name.removePrefix("LPR_")) } catch(e: Exception) { LocationPendingReason.NONE }

            snrIdx = PhysicsUtils.safeDouble(proto.snrIdx)
            noiseIdx = PhysicsUtils.safeDouble(proto.noiseIdx)
            luxIdx = PhysicsUtils.safeDouble(proto.luxIdx)
            vibeIdx = PhysicsUtils.safeDouble(proto.vibeIdx)
            proxIdx = PhysicsUtils.safeDouble(proto.proxIdx)
            liftIdx = PhysicsUtils.safeDouble(proto.liftIdx)
            tiltIdx = PhysicsUtils.safeDouble(proto.tiltIdx)
            baroIdx = PhysicsUtils.safeDouble(proto.baroIdx)

            isSitDetected = proto.isSitDetected; isSitActive = proto.isSitActive
            verticalVelocity = proto.verticalVelocity; sitVz = proto.sitVz; sitVzTs = proto.sitVzTs
            sitVzRt = proto.sitVzRt; sitDz = proto.sitDz; sitBaro = proto.sitBaro
            sitTilt = proto.sitTilt; sitShock = proto.sitShock
            isBatterySteepDischarge = proto.isBatterySteepDischarge; isCoolingModeActive = proto.isCoolingModeActive
            isBatteryLow = proto.isBatteryLow; isBatteryCritical = proto.isBatteryCritical
            violationUptimeMs = proto.violationUptimeMs; isUltraLongStationary = proto.isUltraLongStationary
            gpsHardwareLock = proto.gpsHardwareLock; isAnchorLocked = proto.isAnchorLocked
            thermalHeadroom = proto.thermalHeadroom; heapAllocatedMb = proto.heapAllocatedMb
            activityType = try { ActivityType.valueOf(proto.activityType) } catch (e: Exception) { ActivityType.UNKNOWN }
            thermalSnapshot = if (proto.hasThermalSnapshot()) proto.thermalSnapshot else null
            heapSnapshot = if (proto.hasHeapSnapshot()) proto.heapSnapshot else null
            
            // Oct.7.7 Anomaly Flags
            isSuspiciousNoise = proto.isSuspiciousNoise
            isMemoryPressureThrottled = proto.isMemoryPressureThrottled
        }
    }

    /**
     * mapEntityToApp: Authority for converting a HistoryEntity into a 
     * UI-ready ConnectionPoint.
     */
    fun mapEntityToApp(entity: HistoryEntity, out: ConnectionPoint) {
        if (entity.payload.isNotEmpty()) {
            try {
                val proto = TrackerStatusProto.parseFrom(entity.payload)
                mapProtoToApp(proto, out)
                return
            } catch (e: Exception) {
                Timber.e(e, "Binary history restoration failed")
            }
        }

        out.apply {
            ts = entity.ts; rt = entity.rt; rtt = entity.rtt; isConnected = entity.isConnected
            isGap = entity.isGap; isRecoveryEvent = entity.isRecoveryEvent; hasGps = entity.hasGps
            isTick = entity.isTick; gpsAccuracy = entity.accuracy; maxAccuracy = entity.maxAccuracy
            speed = entity.speed; bearing = entity.bearing; currentMa = entity.currentMa
            locationPendingReason = try { LocationPendingReason.valueOf(entity.locationPendingReason) } catch(e: Exception) { LocationPendingReason.NONE }

            gpsIndex = entity.gpsIndex
            snrIdx = PhysicsUtils.safeDouble(entity.snrIdx)
            noiseIdx = PhysicsUtils.safeDouble(entity.noiseIdx)
            luxIdx = PhysicsUtils.safeDouble(entity.luxIdx)
            vibeIdx = PhysicsUtils.safeDouble(entity.vibeIdx)
            proxIdx = PhysicsUtils.safeDouble(entity.proxIdx)
            liftIdx = PhysicsUtils.safeDouble(entity.liftIdx)
            tiltIdx = PhysicsUtils.safeDouble(entity.tiltIdx)
            baroIdx = PhysicsUtils.safeDouble(entity.baroIdx)

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
            thermalSnapshot = null
            heapSnapshot = null
            isSuspiciousNoise = false
            isMemoryPressureThrottled = false
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
            gpsIndex = PhysicsUtils.safeDouble(p.gpsIndex), 
            noiseIdx = PhysicsUtils.safeDouble(p.noiseIdx), 
            luxIdx = PhysicsUtils.safeDouble(p.luxIdx), 
            vibeIdx = PhysicsUtils.safeDouble(p.vibeIdx),
            proxIdx = PhysicsUtils.safeDouble(p.proxIdx), 
            liftIdx = PhysicsUtils.safeDouble(p.liftIdx), 
            snrIdx = PhysicsUtils.safeDouble(p.snrIdx), 
            tiltIdx = PhysicsUtils.safeDouble(p.tiltIdx),
            baroIdx = PhysicsUtils.safeDouble(p.baroIdx), 
            verticalVelocity = PhysicsUtils.safeDouble(p.verticalVelocity), 
            sitVz = PhysicsUtils.safeDouble(p.sitVz),
            sitVzTs = p.sitVzTs, sitVzRt = p.sitVzRt, sitDz = PhysicsUtils.safeDouble(p.sitDz),
            isBatterySteepDischarge = p.isBatterySteepDischarge, remoteSig = p.remoteSig,
            isCoolingModeActive = p.isCoolingModeActive, speed = PhysicsUtils.safeDouble(p.speed), 
            bearing = PhysicsUtils.safeDouble(p.bearing),
            isSitDetected = p.isSitDetected, isSitActive = p.isSitActive, 
            sitBaro = PhysicsUtils.safeDouble(p.sitBaro),
            sitTilt = PhysicsUtils.safeDouble(p.sitTilt), 
            sitShock = PhysicsUtils.safeDouble(p.sitShock), 
            currentMa = p.currentMa,
            locationPendingReason = p.locationPendingReason.name, accuracy = PhysicsUtils.safeDouble(p.gpsAccuracy),
            maxAccuracy = PhysicsUtils.safeDouble(p.maxAccuracy), isAnchorLocked = p.isAnchorLocked, isBatteryLow = p.isBatteryLow,
            isBatteryCritical = p.isBatteryCritical, violationUptimeMs = p.violationUptimeMs,
            isUltraLongStationary = p.isUltraLongStationary, gpsHardwareLock = p.gpsHardwareLock,
            thermalHeadroom = PhysicsUtils.safeDouble(p.thermalHeadroom), 
            heapAllocatedMb = PhysicsUtils.safeDouble(p.heapAllocatedMb),
            activityType = p.activityType.name,
            payload = TelemetryProtobufMapper.mapAppToBinary(p)
        )
    }

    /**
     * mapStatusToPending: Authority for converting a LocationUpdate into a 
     * persistence-ready PendingStatusEntity.
     */
    fun mapStatusToPending(status: LocationUpdate): PendingStatusEntity {
        return PendingStatusEntity(
            lat = status.lat, lng = status.lng, speed = PhysicsUtils.safeDouble(status.speed), 
            accuracy = PhysicsUtils.safeDouble(status.accuracy),
            bearing = PhysicsUtils.safeDouble(status.bearing), battery = status.battery, 
            temp = PhysicsUtils.safeDouble(status.temp),
            isCharging = status.isCharging, currentMa = status.currentMa, timestamp = status.ts,
            gpsTs = status.gpsTs, satsView = status.satsView, satsUsed = status.satsUsed,
            maxAccuracy = PhysicsUtils.safeDouble(status.maxAccuracy), 
            snrIdx = PhysicsUtils.safeDouble(status.snrIdx), 
            noiseIdx = PhysicsUtils.safeDouble(status.noiseIdx),
            luxIdx = PhysicsUtils.safeDouble(status.luxIdx), 
            vibeIdx = PhysicsUtils.safeDouble(status.vibeIdx), 
            proxIdx = PhysicsUtils.safeDouble(status.proxIdx),
            liftIdx = PhysicsUtils.safeDouble(status.atmospheric.liftIdx), 
            tiltIdx = PhysicsUtils.safeDouble(status.tiltIdx), 
            baroIdx = PhysicsUtils.safeDouble(status.baroIdx),
            isBatterySteepDischarge = status.isBatterySteepDischarge, isCoolingModeActive = status.isCoolingModeActive,
            isSitDetected = status.isSitDetected, isSitActive = status.isSitActive, sitVz = PhysicsUtils.safeDouble(status.sitVz),
            sitVzTs = status.integrity.sitVzTs, sitVzRt = status.integrity.sitVzRt, sitDz = PhysicsUtils.safeDouble(status.sitDz),
            verticalVelocity = PhysicsUtils.safeDouble(status.verticalVelocity), 
            sitBaro = PhysicsUtils.safeDouble(status.sitBaro), 
            sitTilt = PhysicsUtils.safeDouble(status.sitTilt),
            sitShock = PhysicsUtils.safeDouble(status.sitShock), isStorageLow = status.isStorageLow,
            isStorageCritical = status.isStorageCritical, isPowerSaveMode = status.isPowerSaveMode,
            standbyBucket = status.standbyBucket, netInterface = status.netInterface,
            lastValidFixRt = status.lastValidFixRt, locationPendingReason = status.locationPendingReason.name,
            isAnchorLocked = status.integrity.isAnchorLocked, trackerState = status.trackerState.name,
            status = status.status.name, isBatteryLow = status.isBatteryLow,
            isBatteryCritical = status.isBatteryCritical, isUltraLongStationary = status.isUltraLongStationary,
            violationUptimeMs = status.violationUptimeMs, gpsHardwareLock = status.gpsHardwareLock,
            isGnssThrottled = status.isGnssThrottled, thermalHeadroom = PhysicsUtils.safeDouble(status.integrity.thermalHeadroom),
            heapAllocatedMb = PhysicsUtils.safeDouble(status.integrity.heapAllocatedMb), activityType = status.activityType.name,
            payload = TelemetryProtobufMapper.mapStatusToBinary(status)
        )
    }

    /**
     * mapPendingToStatus: Authority for converting a PendingStatusEntity back 
     * into a domain LocationUpdate.
     */
    fun mapPendingToStatus(entity: PendingStatusEntity, deviceId: String, viewerId: String, out: LocationUpdate): LocationUpdate {
        if (entity.payload.isNotEmpty()) {
            try {
                val proto = RealtimeStatus.parseFrom(entity.payload)
                mapProtoToSnapshot(proto, entity.timestamp, proto.rt, out)
                out.status = try { SentinelStatus.valueOf(proto.sentinelStatus) } catch(e: Exception) { SentinelStatus.VALID }
                return out
            } catch (e: Exception) {
                Timber.e(e, "Binary pending status restoration failed")
            }
        }

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
                thermalSnapshot = null
                heapSnapshot = null
                isSuspiciousNoise = false
                isMemoryPressureThrottled = false
            }

            ts = entity.timestamp
            status = try { SentinelStatus.valueOf(entity.status) } catch(e: Exception) { SentinelStatus.VALID }
            trackerState = try { TrackerState.valueOf(entity.trackerState) } catch(e: Exception) { TrackerState.UNKNOWN }
            lastValidFixRt = entity.lastValidFixRt
        }
        return out
    }
}
