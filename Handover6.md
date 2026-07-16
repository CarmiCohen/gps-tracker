# Project Handover: Issue #508, #510, #512, #515 (Simplification & Cleanup)

## Current Status: IN PROGRESS (Build Broken)
The project is mid-refactor. We are stripping out high-complexity features ("Chair Sit Detection", "Stationary Anchor", "Muzzle Optimizations") and consolidating system statuses into a unified `SentinelStatus` model. 

**CRITICAL**: The build is currently failing. `SettingsMapper.kt` has unresolved references, and the telemetry pipeline in the service layer likely has parameter mismatches following the signature changes in `SyncManager`.

## Forensic Status Information

### 1. Issue #510: Abandon Chair Sit Detection (Logic Stripped)
- **Core Engine**: `EngineModels.kt` has been cleared of sit-related metrics in `EngineConnectionPoint` and `AlarmEvaluationState`. `LocationSentinel.kt` no longer contains vertical velocity or plunge detection logic.
- **App Sensors**: `AppSensorManager.kt` has been stripped of its IMU-based sit detection state machine and vertical displacement tracking.
- **Data Layer**: `MainRepository.kt` and `TelemetryUseCase.kt` no longer map or persist sit events.
- **Database**: Version 57 migration (in `Database.kt`) recreates tables to drop all sit-related columns (`sitVz`, `sitDz`, `sitBaro`, `sitTilt`, `sitShock`).

### 2. Issue #512: Consolidate Sentinel Statuses (Refactor Incomplete)
- **Model**: `SentinelStatus` (`VALID`, `JUMP`, `TAMPER`) is now the source of truth. Redundant booleans like `isSuspicious` and `isJump` have been removed from `Models.kt` and `LocationUpdate.kt`.
- **Telemetry**: `SyncManager.pushCurrentStatus` signature was simplified, removing legacy booleans.
- **Proto**: `app_settings.proto` was updated with `optional string status`.
- **BLOCKER**: `SettingsMapper.kt` is failing to compile. It cannot find `setStatus` on `TrackerStatusProto.Builder`. Additionally, it still tries to map `sitTilt` which was removed from the domain model.

### 3. Issue #515: Stationary Anchor Removal (Completed)
- **Logic**: Replaced the complex anchor logic in `LocationProcessor.kt` with a simple speed gate (< 0.5m/s).
- **Cleanliness**: `isAnchorLocked` has been removed from `Models.kt`, `DashboardState`, and the database schema.

### 4. Issue #508: Optimization Removal (Completed)
- **Muzzle**: All "Muzzle" logic (startup/sync suppression) and `muzzleReleaseJob` have been deleted from `TrackerService.kt`, `ViewerService.kt`, and `LocationSentinel.kt`.
- **Adaptive Jump**: SNR-based scaling and `isAdaptiveJump` flags have been removed from `PhysicsUtils.kt` and associated engine models.

## Immediate Resumption Steps
1.  **Fix `SettingsMapper.kt`**:
    - Update the mapping for `TrackerStatusProto`. Ensure the builder uses the correct setter for `status`.
    - Remove the mapping for `sitTilt` (and any other sit-related fields) as they are gone from the `TrackerStatus` domain model.
2.  **Fix Service Telemetry**:
    - Align `TrackerService.kt` and `ViewerService.kt` calls to `syncManager.pushCurrentStatus` with the new signature.
3.  **UI Verification**:
    - Verify `MapComponents.kt` and `OverlayComponents.kt` no longer attempt to render `LOCKED` or `SITTING` badges.
4.  **Tests**:
    - Refactor or delete `AdaptationMuzzleTest.kt` as it tests decommissioned logic.

## Environment Info
- **Project Root**: `C:/CCwork/Android Projects/gps-tracker`
- **Database Version**: 57
- **Simplified States**: `VALID` (0), `JUMP` (1), `TAMPER` (2)
- **Authority**: `SIMPLIFICATION_PLAN.md`
