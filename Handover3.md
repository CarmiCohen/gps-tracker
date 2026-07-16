# Project Handover: Issue #509 (Abandon GtoEngine / Hindsight Removal)

## Current Status: COMPLETED (Phase 3)
The project has successfully transitioned to versioning context `July.15.2013`. The complex Graph Trajectory Optimization (GTO) and hindsight-based location promotion have been entirely stripped from the core engine and application layers.

## Forensic Status Information

### 1. Completed Actions (Issue #509 / R406h)
- **Engine Layer**:
    - **`GtoEngine.kt`**: Logic abandoned; file remains as a commented-out placeholder (pending manual deletion).
    - **`LocationSentinel.kt`**: Removed all hindsight buffer management and `evaluateTrajectory` calls.
    - **`LocationProcessor.kt`**: Removed `isTrajectoryPromoted` flag from `ProcessedLocation`. Deleted interpolated segment logic. Updated `LocationProcessorListener` to remove the hindsight parameter.
    - **`EngineConstants.kt`**: Deleted constants: `HINDSIGHT_BUFFER_SIZE`, `HINDSIGHT_MAX_AGE_MS`, `PROMOTION_ANGLE_TOLERANCE`, `GTO_TOW_SPEED_THRESHOLD`, `GTO_KINEMATIC_SPEED_DELTA`, `GTO_WORK_SPEED_THRESHOLD`, `TRAJECTORY_PROMOTION_WINDOW_MS`.
    - **`EngineModels.kt`**: Deleted `RejectedPoint`. Removed `promotedPoints` from `SentinelResult`. Stripped `isTrajectoryPromoted` from `AlarmEvaluationState` and `EngineConnectionPoint`.

- **Application & Persistence Layer**:
    - **`Database.kt` & `Models.kt`**: Removed `isHindsightCorrected` from `TrailEntity` and `TrailPoint`.
    - **`LocationUpdate.kt` & `TrackerStatus`**: Removed `isTrajectoryPromoted` from telemetry payloads.
    - **`MainRepository.kt`**: Updated `saveTrailPoint` to ignore hindsight flags.
    - **`MapComponents.kt`**: Simplified trail rendering to use a single color, removing logic that branched on hindsight status.
    - **Services**: `TrackerService`, `ViewerService`, `SyncManager`, and `AppAlarmManager` were all updated to remove trajectory promotion handling.
    - **Use Cases**: `TelemetryUseCase`, `BehaviorUseCase`, `TrackerStateManager`, and `MainAlarmLogic` were stripped of GTO-specific state transitions.

- **Tests**:
    - `LocationSentinelHindsightTest.kt`: Cleared (obsolete).
    - `AdaptationMuzzleTest.kt` & `ForensicIdentityTest.kt`: Refactored to match the new `LocationProcessor` API.

### 2. Immediate Next Step (Issue #510 / R406h)
- **Abandon Chair Sit Detection (R832)**:
    - **Objective**: Simplify the sensor pipeline by removing the high-overhead Sit Detection logic (Barometer/Accel/Tilt fusion).
    - **Files to Modify**: 
        - `LocationSentinel.kt`: Remove `isSitDetected`, `lastSitTs`, `baselineSitTilt`, and the sit detection block in `updateSensorState`.
        - `EngineModels.kt`: Remove sit-related fields from `EngineConnectionPoint` and `AlarmEvaluationState`.
        - `EngineConstants.kt`: Delete all `CHAIR_SIT_*` and `CHAIR_PLUNGE_*` constants.
        - `LocationUpdate.kt` & `Models.kt`: Strip sit-related telemetry fields.
        - `MainAlarmLogic.kt`: Remove `ALERT_ID_TRACKER_CHAIR` and associated violation detection.

### 3. Cleanup Items
- Manually delete `core/engine/src/main/java/com/gps19/core/engine/GtoEngine.kt`.
- Manually delete `core/engine/src/test/java/com/gps19/core/engine/LocationSentinelHindsightTest.kt`.

## Environment Info
- **Project Root**: `C:/CCwork/Android Projects/gps-tracker`
- **Simplification Authority**: `SIMPLIFICATION_PLAN.md`
- **Current Version Context**: `July.15.2013`
- **Build Status**: Verified API alignment across all modified files.
