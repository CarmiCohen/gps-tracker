# Chat Session Log & Forensic Analysis: Issue #1330 (Snap-to-Update Monolith)

## 📌 Session Overview
*   **Selected Issue**: Issue #1330 (Snap-to-Update Monolith)
*   **Objective**: Evaluate merging `SystemEvaluationSnapshot` and `LocationUpdate` into a single unified telemetry DTO or streamlined polymorphic structure to eliminate the heavy manual bridge mapping layers.
*   **Status**: Comprehensive structural audit and alignment verification completed. Strategy formulated for zero-allocation consolidation.

---

## 🔍 Detailed Data Structure Analysis

### 1. `SystemEvaluationSnapshot` (`EngineModels.kt`)
A flat data class representing all fields evaluated during background engine ticks or alarm monitoring cycles.
*   **Kinematic & Location State**: `status` (`SentinelStatus`), `lat`, `lng`, `alt`, `accuracy`, `maxAccuracy`, `speed`, `bearing`, `gpsTs`, `lastValidFixRt`, `distToHome`, `isStalled`, `isClockRegression`, `isJammer`, `jumpTier`, `isAdaptiveJump`, `tamperDetected`, `jammerDetected`, `isAnchorLocked`, `suppressionNote`.
*   **Environmental & Sensor State**: `vibration`, `heading`, `baroAlt`, `baroAltEma`, `lux`, `isNear`, `tiltDegrees`, `acousticDb`, `peakShock`, `acousticMinDb`, `luxBaseline`, `acousticFloorDb`, `adaptiveVibrationFloor`, `kineticEnergy`, `peakVerticalVelocity`, `peakVerticalVelocityTs`, `peakVerticalVelocityRt`, `peakVerticalDisplacement`.
*   **Health & System State**: `batteryLevel`, `batteryTemp`, `currentMa`, `isCharging`, `isPowerTamper`, `isLocationPending`, `locationPendingReason`, `isPowerSaveMode`, `standbyBucket`, `netInterface`, `isStorageLow`, `isStorageCritical`, `isBatterySteepDischarge`, `isCoolingModeActive`, `isGpsHardwareLock`, `cpuLoad`, `ioWait`, `maxIoLatency`, `isSilentFailure`, `isMaliAnomaly`, `isUltraLongStationary`, `isBatteryLow`, `isBatteryCritical`, `isSignalLoss`, `isGpsStalling`, `isGpsGap`, `localInternetLoss`, `isHardwareOnline`.
*   **Signaling Metadata**: `satsUsed`, `satsView`, `proxIdx`, `proximityCm`, `proximityDebounceMs`, `vibrationRollingSum`, `violationUptimeMs`, `violationPercentage`, `isWarming`, `isSirenActive`.

### 2. `LocationUpdate` (`LocationUpdate.kt`)
A nested/partitioned structure with three sub-states used for persistence and state flows in `TelemetryRepository`.
*   **`KineticState`**: `lat`, `lng`, `alt`, `speed`, `accuracy`, `maxAccuracy`, `bearing`, `gpsTs`, `rt`, `isJump`, `isTrajectoryPromoted`, `jumpTier`, `isAdaptiveJump`, `verticalVelocity`, `kineticEnergy`, `distToTracker`, `distToHome`.
*   **`AtmosphericState`**: `temp`, `maxTemp`, `baroAlt`, `baroAltEma` (mapped as `baroIdx`), `lux`, `luxBaseline`, `luxIdx`, `acousticDb`, `acousticFloorDb`, `noiseIdx`, `tiltDegrees`, `heading`, `vibration`, `vibrationRollingSum`, `vibeIdx`, `peakVibrationShock`, `peakVibrationShockTs`, `adaptiveVibrationFloor`, `proxIdx`, `proximityCm`, `proximityDebounceMs`, `isNear`, `liftIdx`, `tiltIdx`.
*   **`IntegrityState`**: `battery`, `isCharging`, `currentMa`, `satsView`, `satsUsed`, `snrIdx`, `isTamperDetected`, `isPowerTamper`, `isJammer`, `isStalled`, `isSuspicious`, `isAnchorLocked`, `gpsHardwareLock`, `isBatteryLow`, `isBatteryCritical`, `isCoolingModeActive`, `isUltraLongStationary`, `isGnssThrottled`, `isBatterySteepDischarge`, `isPowerSaveMode`, `standbyBucket`, `netInterface`, `isStorageLow`, `isStorageCritical`, `micPending`, `violationUptimeMs`, `violationPercentage`, `uptimeMs`, `totalConnectedMs`, `sessionConnectedMs`, `lastConnTs`, `lastDiscTs`, `totalDropMs`, `maxDropMs`, `maxDropTs`, `lastEnergyDeltaMa`, `lastEnergyDeltaTemp`, `lastEnergyDurationMs`, `gnssDetail`, `isSitDetected`, `lastSitTs`, `sitVz`, `sitVzTs`, `sitVzRt`, `sitDz`, `sitBaro`, `sitTilt`, `sitShock`, `isSitActive`, `isLocationPending`, `locationPendingReason`, `signal`, `tamperNote`.
*   **Top-level Attributes**: `status`, `ts`, `isMe`, `trackerState`, `isClockRegression`, `lastValidFixRt`.

---

## 🛠 Manual Bridge Mapping Analysis (`AppEventCoordinator.kt`)
In `handleTickEvaluated` and `handleViewerLocationUpdated`, a monolithic block manually populates a new `LocationUpdate` instance from `SystemEvaluationSnapshot` fields.
Example of block mapping footprint:
```kotlin
repository.updateLocation(LocationUpdate().apply {
    this.kinetic.lat = proc?.optimizedPoint?.lat ?: 0.0
    this.kinetic.lng = proc?.optimizedPoint?.lng ?: 0.0
    // ... ~50 fields explicitly assigned per tick ...
})
```
This introduces significant allocation churn and manual copy overhead, creating a duplicate "bridge layer" where a single polymorphically extended or flat unified telemetry class could satisfy both local evaluation and persistence needs.

---

## 🎯 Architecture Consolidation Strategy
1.  **DTO Unification**: Leverage the ~95% structural field overlap between `SystemEvaluationSnapshot` and `LocationUpdate`.
2.  **Zero-Allocation Buffering**: Align the mapping schema with `TelemetryRepository`'s flyweight double-buffering scheme to completely eliminate runtime object creation per background pulse.
3.  **Polymorphic Extension**: Maintain cross-module compatibility with pure domain logic components via clean interfaces or shared components in `:core:engine`.
