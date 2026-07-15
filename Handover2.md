# Project Handover: Issue #509 (Abandon GtoEngine / Hindsight Removal)

## Current Status: IN PROGRESS (PARTIAL)
Issue #509 (R406h) aims to remove the complexity of Graph Trajectory Optimization and hindsight-based location promotion. The transition to versioning scheme `July.1.13` has started.

## Forensic Status Information

### 1. Completed Actions
- **`EngineModels.kt`**: 
    - Updated version header to `July.1.13`.
    - Deleted `RejectedPoint` data class.
    - Removed `promotedPoints: List<EngineGeoPoint>?` from `SentinelResult` data class.
    - Note: `isTrajectoryPromoted` remains in `AlarmEvaluationState` and `EngineConnectionPoint` (needs removal in services/persistence layers if applicable).

### 2. Immediate Next Steps (Pending Refactoring)
- **`GtoEngine.kt`**: This file must be deleted. It is the core of the complexity being removed.
- **`LocationSentinel.kt`**:
    - Remove `private val gtoEngine = GtoEngine()`.
    - In `processLocation`, delete the block that calls `gtoEngine.evaluateTrajectory(...)` and handles trajectory promotion.
    - Remove `getHindsightBuffer()` function.
    - Ensure all remaining logic uses ` July.1.13` versioning.
- **`LocationProcessor.kt`**:
    - Remove `isTrajectoryPromoted` from `ProcessedLocation` data class.
    - In `processGpsPoint`, remove logic that handles `SentinelStatus.TRAJECTORY_PROMOTED`, specifically the call to `PhysicsUtils.interpolateSegment`.
    - Ensure all remaining logic uses `July.1.13` versioning.
- **`EngineConstants.kt`**:
    - Remove constants: `HINDSIGHT_BUFFER_SIZE`, `HINDSIGHT_MAX_AGE_MS`, `PROMOTION_ANGLE_TOLERANCE`, `GTO_TOW_SPEED_THRESHOLD`, `GTO_KINEMATIC_SPEED_DELTA`, `GTO_WORK_SPEED_THRESHOLD`, `TRAJECTORY_PROMOTION_WINDOW_MS`.
    - Ensure version header is `July.1.13`.

### 3. Dependencies to Clean
- **`BaseMonitorService.kt` / `TrackerService.kt` / `ViewerService.kt`**: Check if `forensicUseCase` or `syncManager` still expect trajectory promotion flags in their status payloads.
- **`TelemetryProcessor.kt`**: Verify if `isTrajectoryPromoted` needs to be stripped from incoming telemetry mapping.

## Environment Info
- **Project Root:** `C:/CCwork/Android Projects/gps-tracker`
- **Requirement Authority:** **R406h** (Formalized in `STATUS/SOT_MASTER_REQUIREMENTS.md`).
- **Current Version Context:** `July.1.13`
