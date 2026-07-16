# Project Handover: Issues #508, #510, #512, #515 (Consolidation & Simplification)

## Current Status: IN PROGRESS (Build Broken)
The project is mid-refactor. We have successfully stripped the core engine and protocol buffers of decommissioned features ("Chair Sit Detection", "Stationary Anchor", "Adaptive Jump", "Muzzle Logic") and consolidated system statuses into `SentinelStatus`. However, the application layer (`app` module) is failing to compile due to lingering references to these removed fields and methods.

## Forensic Status Information

### 1. Engine Hardening (core:engine) - Completed
- **`EngineModels.kt`**: Removed all sit-related fields from `AlarmEvaluationState`. Consolidated `SentinelStatus` (VALID, JUMP, TAMPER).
- **`LocationUpdate.kt`**: Purged of `isSitDetected`, `isSitActive`, `verticalVelocity`, `isAnchorLocked`, and `isJump`/`isJammer`.
- **`LocationProcessor.kt`**: Removed Stationary Anchor logic; replaced with a simple speed gate. Fixed `nowWall` reference error.
- **`LocationSentinel.kt`**: Stripped of IMU-based sit detection state machine and vertical displacement tracking.
- **`MainAlarmLogic.kt`**: Removed `ALERT_ID_TRACKER_CHAIR` and associated evaluation logic.
- **`TelemetryAggregator.kt` / `TelemetryMerger.kt`**: Aligned with the simplified `LocationUpdate` model.
- **Decommissioned**: `ImmFilter.kt` and `GtoEngine.kt` are now empty placeholders. `AdaptationMuzzleTest.kt` is decommissioned.

### 2. Application Layer (app) - Partial
- **Protos**: Both `app_settings.proto` files are synchronized and cleaned of decommissioned fields.
- **`SettingsMapper.kt` / `SyncManager.kt`**: Aligned with the new schema and status model.
- **`BehaviorUseCase.kt` / `TrackerStateManager.kt`**: Successfully migrated from `isVisualJump` boolean to `SentinelStatus`.
- **`DashboardUseCase.kt`**: Purged of sit/velocity computations.
- **UI Components**: `MapComponents.kt` and `OverlayComponents.kt` cleaned of "ANCHOR LOCKED" and "SITTING" badges.
- **`AppAlarmManager.kt`**: Signature of `evaluateAlarms` aligned; sit alert logic removed.

## Immediate Resumption Steps (Build Blockers)
The build is currently blocked by the following unresolved references in the `app` module:

1.  **`MainViewModel.kt`**:
    - Remove `UiEvent.CalibrateChair` and references in `onEvent`.
    - Fix `permissions.copy` parameter mismatch for `isXiaomiManualOverride`.
    - Resolve `Unresolved reference: SetJammerSuspicion` and `SetSignalLoss` (likely missing from `UiEvent`).
    - Fix `Unresolved reference: mapStatsFromStatus` (missing from `TelemetryUseCase`).
2.  **`RemoteHandler.kt`**:
    - Remove `verticalVelocity` mapping.
    - Fix `Unresolved reference: updateSensorData` (parameter mismatch with `LocationSentinel`).
3.  **`SharedUiComponents.kt`**:
    - Remove the Analytical Ribbons for SVZ, SDZ, BAR, and TLT as their data sources are gone.
4.  **`LogManager.kt`**:
    - Fix `Unresolved reference: snrIdx` (removed from `LocationState`).
    - Resolve missing `LOG_MUZZLE_STARTUP_MS` constant.
5.  **`SystemMonitor.kt`**:
    - Restore missing constants: `SYSTEM_WATCHDOG_INTERVAL_MS`, `SYSTEM_WATCHDOG_THROTTLE_MS`, `WATCH_DOG_DANGER_WINDOW_MS`.
6.  **`SettingsComponents.kt`**:
    - Remove the `chairOccupied` alert toggle.
7.  **`TrackerScreen.kt` / `ViewerScreen.kt`**:
    - Remove UI buttons triggering `CalibrateChair`.

## Environment Info
- **Database Version**: 57
- **Project Root**: `C:/CCwork/Android Projects/gps-tracker`
- **Source of Truth**: `SIMPLIFICATION_PLAN.md`
