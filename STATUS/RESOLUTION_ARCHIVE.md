# 🏛️ Resolution Archive - Sep.25.00

## 🏁 Issue #1326: Telemetry Data Corruption in LocationProcessor
*   **Resolved**: Sep.25.00
*   **Root Cause**: The `satsUsed` field in `processGpsPoint` was mapped to a zero-placeholder, which was inadvertently propagated into the tracking engine's sentinel logic. This caused incorrect signal quality scoring and corrupted satellite count history logs.
*   **Remediation**:
    *   **LocationProcessor.kt**: Updated `processGpsPoint` to source `satsUsed` directly from the unified `SystemEvaluationSnapshot`.
    *   **Data Integrity**: Eliminated the legacy placeholder, ensuring that the tracking sentinel receives actual hardware satellite counts for validation.
*   **R-ID**: 479

## 🏁 Issue #1325: Unified Snapshot Metadata Gaps
*   **Resolved**: Sep.25.00
*   **Root Cause**: Following the unification of evaluation logic in Sep.24.96, the `SystemEvaluationSnapshot` lacked several metadata fields (satellite counts, proximity indices, vibration rolling sums, and violation uptime statistics) required for downstream signaling and persistence. This forced components to observe fragmented side-channel flows.
*   **Remediation**:
    *   **EngineModels.kt**: Expanded `SystemEvaluationSnapshot` to include all metadata fields required for a full `LocationUpdate`.
    *   **MonitorService.kt**: Refactored the core evaluation loop to populate these fields from the `HardwareSuite` and `SessionManager` state.
    *   **AppEventCoordinator.kt**: Updated mapping logic to utilize the snapshot as the authoritative source for persistence and peer signaling, removing reliance on redundant event metadata.
*   **R-ID**: 478

# 🏛️ Resolution Archive - Sep.24.96

## 🏁 Issue #1312: Unified Evaluation Logic Snapshot
*   **Resolved**: Sep.24.96
*   **Root Cause**: The background evaluation pipeline utilized two distinct and partially redundant data structures (`AlarmTelemetrySnapshot` and `SensorStateSnapshot`) to pass data to the alarm and location engines. This created a temporal decoupling risk where sensor data and GPS coordinates could originate from slightly different internal states, and increased boilerplate in `MonitorService`.
*   **Remediation**:
    *   **EngineModels.kt**: Consolidated all telemetry, environmental sensors, health metrics, and behavioral flags into a single, unified `SystemEvaluationSnapshot` DTO.
    *   **LocationProcessor.kt / LocationSentinel.kt**: Refactored `processGpsPoint` and `updateSensorData` to consume the unified snapshot, ensuring absolute data parity across kinematic evaluation.
    *   **AppAlarmManager.kt**: Updated `evaluateAlarms` and `syncEvaluationState` to utilize the unified snapshot, simplifying the data synchronization logic.
    *   **MonitorService.kt**: Refactored the core `processTick` loop to construct the unified snapshot once and propagate it as the single source of truth for the entire background cycle (R-ID 475).
*   **R-ID**: 475

## 🏁 Issue #1313: Role-Switching Atomic State Reset
*   **Resolved**: Sep.24.96
*   **Root Cause**: Runtime role transitions (Tracker <-> Viewer) did not trigger a comprehensive reset of the `LocationProcessingState` within the unified `MonitorService`. This created a risk of stale accuracy windows, motion probablities, or sensor baselines leaking from the previous role into the new one.
*   **Remediation**:
    *   **MonitorService.kt**: Implemented a reactive observer for `repository.appModeFlow`. Introduced `handleRoleTransition` which executes an atomic session reset via `SessionLifecycleCoordinator`.
    *   **State Management**: Made `remoteProcessor` non-nullable and ensured both primary and remote processors are cleared, jobs are cancelled, and new logic anchors are reloaded from the repository upon transition.
    *   **Hardening**: Established absolute architectural isolation between roles within the unified service lifecycle (R-ID 476).
*   **R-ID**: 476
