# Project Handover: Issue #511 (Ribbon Simplification) & #512 (Status Consolidation)

## Current Status: IN PROGRESS (Build Breakage Warning)
The project has successfully simplified its telemetry pipeline but is currently mid-transition for the `SentinelStatus` consolidation. 

**CRITICAL**: The build is likely broken. `EngineModels.kt`, `LocationSentinel.kt`, and `LocationProcessor.kt` have been updated to use the consolidated `SentinelStatus` (VALID, JUMP, TAMPER), but `AppAlarmManager.kt` and several UI/View components still reference legacy status values (e.g., `VISUAL_JUMP`, `SENSOR_SUSPICIOUS`).

## Forensic Status Information

### 1. Completed Actions (Issue #511: Ribbon Simplification)
- **Engine Layer**:
    - **`EngineModels.kt`**: Stripped `EngineConnectionPoint` and `EngineSensorSnapshot` of high-frequency indexes (`luxIdx`, `vibeIdx`, `snrIdx`, `tiltIdx`, `baroIdx`, `noiseIdx`, `gpsIndex`, `verticalVelocity`).
    - **`TelemetryAggregator.kt`**: Removed all index-based merging and backfilling logic.
- **Application & Persistence Layer**:
    - **`Database.kt`**: Incremented to version 55. Added `MIGRATION_54_55` to drop index columns via table recreation.
    - **`Models.kt`**: Aligned `ConnectionPoint`, `IntegrityState`, and `TrackerStatus` with simplified metrics.
    - **`HistoryManager.kt` & `SyncManager.kt`**: Removed mapping/syncing logic for obsolete indexes.
    - **Services**: `TrackerService.kt` and `ViewerService.kt` updated to match new telemetry signatures.
- **UI Layer**:
    - **`SharedUiComponents.kt`**: Removed obsolete sensor ribbons. Introduced core **SPD** (Speed) and **ACC** (Accuracy) ribbons.
    - **`DashboardUseCase.kt`**: Updated to format labels based on the simplified status/sensor data.
- **Preservation**: As per explicit instruction, all **Chair Sit Detection** metrics and logic remain fully intact and prioritized.

### 2. Current Work-in-Progress (Issue #512: Status Consolidation)
- **Objective**: Reduce 9+ statuses to `VALID`, `JUMP`, and `TAMPER`.
- **Files Modified**: 
    - `EngineModels.kt`: Statuses consolidated.
    - `LocationSentinel.kt`: Mapping logic simplified.
    - `LocationProcessor.kt`: `ProcessedLocation` aligned.
- **Remaining Task**: Refactor `AppAlarmManager.kt` and UI labels to handle the consolidated status mapping.

### 3. Cleanup Items
- **Deleted**: `core/engine/src/main/java/com/gps19/core/engine/GtoEngine.kt`.
- **Deleted**: `core/engine/src/test/java/com/gps19/core/engine/LocationSentinelHindsightTest.kt` (Actioned during handover).

## Immediate Resumption Steps
1.  **Resolve `AppAlarmManager.kt`**: Refactor the `evaluateAlarms` loop. It currently expects legacy `SentinelStatus` values to branch into specific alarm triggers. These must be mapped to the new `VALID/JUMP/TAMPER` logic.
2.  **Verify UI Freshness**: Ensure `DashboardUseCase.kt` correctly interprets the new status for Ghost Mode / Staleness badges.
3.  **Run Gradle Build**: Verify that all symbol references to `SentinelStatus` are resolved.

## Environment Info
- **Project Root**: `C:/CCwork/Android Projects/gps-tracker`
- **Current Version Context**: `July.15.2013`
- **Database Version**: 55
- **Simplification Authority**: `SIMPLIFICATION_PLAN.md`
