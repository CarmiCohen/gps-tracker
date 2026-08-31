package com.gps19.app

import com.gps19.core.engine.*

/**
 * TelemetryMapper: Centralized authority for telemetry data transformation.
 * Aug.31.00:
 * - Issue #782: Protocol Audit - Binary Schema Expansion. Added 
 *   violationUptimeMs mapping for full parity in binary telemetry (R782).
 * - Forensic Audit: Hardened history mappings for violationUptimeMs and 
 *   isUltraLongStationary to ensure replay parity.
 * Aug.29.10:
 * - Concern #765: Added isUltraLongStationary mapping for full state parity.
 */
object TelemetryMapper {

    /**
     * Maps core and forensic fields from EngineConnectionPoint to ConnectionPoint.
     */
    fun mapEngineToApp(p: EngineConnectionPoint, out: ConnectionPoint) {
        out.apply {
            ts = p.ts; rt = p.rt; rtt = p.rtt; localSig = 10; remoteSig = p.remoteSig; isConnected = p.isConnected
            isGap = p.isGap; isRecoveryEvent = p.isRecoveryEvent; hasGps = p.hasGps; isTick = p.isTick
            gpsAccuracy = p.accuracy; maxAccuracy = p.maxAccuracy
            speed = p.speed; bearing = p.bearing; currentMa = p.currentMa
            locationPendingReason = p.locationPendingReason
            isUltraLongStationary = p.isUltraLongStationary
            violationUptimeMs = p.violationUptimeMs

            // Forensic Parity
            snrIdx = p.snrIdx
            noiseIdx = p.noiseIdx
            luxIdx = p.luxIdx
            vibeIdx = p.vibeIdx
            proxIdx = p.proxIdx
            liftIdx = p.liftIdx
            tiltIdx = p.tiltIdx
            baroIdx = p.baroIdx
            isSitDetected = p.isSitDetected
            isSitActive = p.isSitActive
            verticalVelocity = p.verticalVelocity
            sitVz = p.sitVz
            sitVzTs = p.sitVzTs
            sitVzRt = p.sitVzRt
            sitDz = p.sitDz
            sitBaro = p.sitBaro
            sitTilt = p.sitTilt
            sitShock = p.sitShock
            kineticEnergy = p.kineticEnergy
            isBatterySteepDischarge = p.isBatterySteepDischarge
            isCoolingModeActive = p.isCoolingModeActive
            isBatteryLow = p.isBatteryLow
            isBatteryCritical = p.isBatteryCritical
            cpuLoad = p.cpuLoad
            ioWait = p.ioWait
            maxIoLatency = p.maxIoLatency
            isSilentFailure = p.isSilentFailure
        }
    }

    /**
     * Maps forensic fields from HistoryEntity to ConnectionPoint.
     */
    fun mapEntityToApp(entity: HistoryEntity, out: ConnectionPoint) {
        out.apply {
            gpsIndex = entity.gpsIndex
            noiseIdx = entity.noiseIdx
            luxIdx = entity.luxIdx
            vibeIdx = entity.vibeIdx
            proxIdx = entity.proxIdx
            liftIdx = entity.liftIdx
            snrIdx = entity.snrIdx
            tiltIdx = entity.tiltIdx
            baroIdx = entity.baroIdx
            isSitDetected = entity.isSitDetected
            isSitActive = entity.isSitActive
            verticalVelocity = entity.verticalVelocity
            sitVz = entity.sitVz
            sitVzTs = entity.sitVzTs
            sitVzRt = entity.sitVzRt
            sitDz = entity.sitDz
            sitBaro = entity.sitBaro
            sitTilt = entity.sitTilt
            sitShock = entity.sitShock
            isBatterySteepDischarge = entity.isBatterySteepDischarge
            isCoolingModeActive = entity.isCoolingModeActive
            isBatteryLow = entity.isBatteryLow
            isBatteryCritical = entity.isBatteryCritical
            isUltraLongStationary = entity.isUltraLongStationary
            violationUptimeMs = entity.violationUptimeMs
        }
    }

    /**
     * Maps forensic fields from ConnectionPoint to HistoryEntity.
     */
    fun mapAppToEntity(point: ConnectionPoint, ribbonKey: String): HistoryEntity {
        return HistoryEntity(
            ts = point.ts, rt = point.rt, rtt = point.rtt, isConnected = point.isConnected, 
            isGap = point.isGap, isRecoveryEvent = point.isRecoveryEvent,
            hasGps = point.hasGps, isTick = point.isTick, ribbonKey = ribbonKey,
            isBatterySteepDischarge = point.isBatterySteepDischarge,
            remoteSig = point.remoteSig,
            isCoolingModeActive = point.isCoolingModeActive,
            isBatteryLow = point.isBatteryLow,
            isBatteryCritical = point.isBatteryCritical,
            speed = point.speed, bearing = point.bearing,
            currentMa = point.currentMa,
            locationPendingReason = point.locationPendingReason.name,
            accuracy = point.gpsAccuracy,
            maxAccuracy = point.maxAccuracy,
            gpsIndex = point.gpsIndex,
            noiseIdx = point.noiseIdx,
            luxIdx = point.luxIdx,
            vibeIdx = point.vibeIdx,
            proxIdx = point.proxIdx,
            liftIdx = point.liftIdx,
            snrIdx = point.snrIdx,
            tiltIdx = point.tiltIdx,
            baroIdx = point.baroIdx,
            isSitDetected = point.isSitDetected,
            isSitActive = point.isSitActive,
            verticalVelocity = point.verticalVelocity,
            sitVz = point.sitVz,
            sitVzTs = point.sitVzTs,
            sitVzRt = point.sitVzRt,
            sitDz = point.sitDz,
            sitBaro = point.sitBaro,
            sitTilt = point.sitTilt,
            sitShock = point.sitShock,
            isUltraLongStationary = point.isUltraLongStationary,
            violationUptimeMs = point.violationUptimeMs
        )
    }

    /**
     * Maps forensic fields from TrackerStatus to PendingStatusEntity.
     */
    fun mapStatusToPending(status: TrackerStatus, out: PendingStatusEntity): PendingStatusEntity {
        return out.copy(
            snrIdx = status.snrIdx,
            noiseIdx = status.noiseIdx,
            luxIdx = status.luxIdx,
            vibeIdx = status.vibeIdx,
            liftIdx = status.liftIdx,
            tiltIdx = status.tiltIdx,
            baroIdx = status.baroIdx,
            isSitDetected = status.isSitDetected,
            isSitActive = status.isSitActive,
            sitVz = status.sitVz,
            sitVzTs = status.sitVzTs,
            sitVzRt = status.sitVzRt,
            sitDz = status.sitDz,
            sitBaro = status.sitBaro,
            sitTilt = status.sitTilt,
            sitShock = status.sitShock,
            verticalVelocity = status.verticalVelocity,
            isBatterySteepDischarge = status.isBatterySteepDischarge,
            isCoolingModeActive = status.isCoolingModeActive,
            isBatteryLow = status.isBatteryLow,
            isBatteryCritical = status.isBatteryCritical,
            isUltraLongStationary = status.isUltraLongStationary,
            violationUptimeMs = status.violationUptimeMs
        )
    }

    /**
     * Maps forensic fields from PendingStatusEntity back to TrackerStatus.
     */
    fun mapPendingToStatus(entity: PendingStatusEntity, out: TrackerStatus): TrackerStatus {
        return out.copy(
            snrIdx = entity.snrIdx,
            noiseIdx = entity.noiseIdx,
            luxIdx = entity.luxIdx,
            vibeIdx = entity.vibeIdx,
            liftIdx = entity.liftIdx,
            tiltIdx = entity.tiltIdx,
            baroIdx = entity.baroIdx,
            isSitDetected = entity.isSitDetected,
            isSitActive = entity.isSitActive,
            sitVz = entity.sitVz,
            sitVzTs = entity.sitVzTs,
            sitVzRt = entity.sitVzRt,
            sitDz = entity.sitDz,
            sitBaro = entity.sitBaro,
            sitTilt = entity.sitTilt,
            sitShock = entity.sitShock,
            verticalVelocity = entity.verticalVelocity,
            isBatterySteepDischarge = entity.isBatterySteepDischarge,
            isCoolingModeActive = entity.isCoolingModeActive,
            isBatteryLow = entity.isBatteryLow,
            isBatteryCritical = entity.isBatteryCritical,
            isUltraLongStationary = entity.isUltraLongStationary,
            violationUptimeMs = entity.violationUptimeMs
        )
    }
}
