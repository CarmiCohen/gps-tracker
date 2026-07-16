# Project Handover: Issue #512 (Status Consolidation) & #515 (Anchor Removal)

## Current Status: IN PROGRESS (Build Stable, Cleanup WIP)
The project has successfully consolidated its core system statuses and removed the high-complexity "Stationary Anchor" logic. The build should be stable, but legacy "Muzzle" code remains in the service layer as part of the ongoing Issue #508.

## Forensic Status Information

### 1. Completed Actions (Issue #512: Status Consolidation)
- **Engine Layer**:
    - **`EngineModels.kt`**: `SentinelStatus` reduced to `VALID`, `JUMP`, `TAMPER`. `AlarmEvaluationState` now uses `status: SentinelStatus` and `isJammer` instead of overlapping booleans.
    - **`LocationSentinel.kt`**: Removed `isSuspicious` flag. Logic now branches on `SentinelStatus.TAMPER`.
    - **`LocationProcessor.kt`**: `processGpsPoint` signature updated. EMA alpha selection now uses the consolidated status.
    - **`MainAlarmLogic.kt`**: Detection logic rewritten to use `SentinelStatus` as the primary source of truth for hardware and trajectory integrity.
- **Application & Persistence Layer**:
    - **`Database.kt`**: Incremented to **version 56**. Added `status` column to `PendingStatusEntity` via `MIGRATION_55_56`.
    - **`Models.kt`**: `TrackerStatus`, `LocationState`, `IntegrityState`, and `IntegrityStateUi` now use `status: SentinelStatus`. Legacy flags (`isSuspicious`, `isJump`, `isJammer`, `isStalled`) have been removed from the primary data structures.
    - **`TelemetryUseCase.kt`**: Mappings updated to propagate `SentinelStatus`.
    - **`SyncManager.kt`**: Telemetry push and offline buffering now include the consolidated status.
    - **`RemoteHandler.kt`**: Introduced `trackerStatus` for remote state tracking.
- **UI Layer**:
    - **`DashboardUseCase.kt`**: Badges and indicators are now derived from the consolidated `status` field.
    - **`SharedUiComponents.kt`**: Ribbon logic remains intact but `isAnchorLocked` indicators are being phased out.

### 2. Completed Actions (Issue #515: Stationary Anchor Removal)
- **`LocationProcessor.kt`**: Deleted `parkingAnchorPoint`, `anchorEscapeScore`, and all displacement trend logic.
- **Replacement**: Implemented a simple speed gate in `processGpsPoint`. If `estimatedSpeed < STATIONARY_SPEED_THRESHOLD_MPS` (0.5 m/s), coordinates are retained from the last valid fix to prevent drift.
- **Cleanup**: `isAnchorLocked` flag removed from `Models.kt`, `LocationUpdate.kt`, and telemetry pipeline. (Note: Column still exists in DB for migration safety but is unused).

### 3. Work-in-Progress (Issue #508: Optimization Removal)
- **Objective**: Strip out "Muzzle" logic and `AdaptiveJumpConfidence`.
- **Status**: 
    - `EngineConstants.kt`: Muzzle and Adaptive Jump constants removed.
    - `LocationSentinel.kt`: `isMuzzled` parameter removed from sensor checks.
    - **REMAINING**: `TrackerService.kt` and `ViewerService.kt` still contain `isMuzzled` state, `muzzleReleaseJob`, and associated lifecycle logic. These must be deleted.
    - **REMAINING**: `PhysicsUtils.kt` still contains adaptive scaling logic in `isVisualJump`.

## Immediate Resumption Steps
1.  **Finalize Issue #508**: 
    - Remove `isMuzzled` and `isAdaptationMuzzled` from `TrackerService.kt`.
    - Delete `muzzleReleaseJob` and `adaptationMuzzleJob` logic.
    - Clean up `PhysicsUtils.isVisualJump` to remove `isAdaptiveJump` and SNR-based scaling.
2.  **Verify Telemetry Parity**: Ensure `SyncManager` and `RemoteHandler` no longer pass unused legacy booleans in their internal JSON/Proto builders.
3.  **Run Gradle Build**: Confirm that the removal of `isAnchorLocked` and `isSuspicious` hasn't left dead references in `MapComponents.kt` or `OverlayComponents.kt`.

## Environment Info
- **Project Root**: `C:/CCwork/Android Projects/gps-tracker`
- **Database Version**: 56
- **Simplification Authority**: `SIMPLIFICATION_PLAN.md`
- **Consolidated States**: `VALID` (0), `JUMP` (1), `TAMPER` (2)
