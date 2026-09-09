# Resolution Archive (Sep.09.11)

## 🟢 Resolved Issues (Sep.09.11)
*   **Issue #939 RESOLVED: Build Restoration (Part B)**. Fixed unresolved references in `BehaviorUseCase.kt` and `MainViewModel.kt` following the telemetry partitioning (R-ID 284). Updated `effectiveLocation.speed` to `effectiveLocation.kinetic.speed` and mapped `maxTemp`/`currentMa` to their respective partitioned states (`atmospheric.maxTemp` and `integrity.currentMa`).
*   **Hardening Audit RESOLVED (Forensic Integrity)**: 
    *   **Mapping Fix**: Identified and resolved gaps in `ConnectivitySuite.kt` where `integrity.signal`, `atmospheric.maxTemp`, `integrity.isGnssThrottled`, and `kinetic.maxAccuracy` were not propagated from remote telemetry (Protobuf/JSON).
    *   **HUD Restoration**: Fixed regressions where remote trackers incorrectly reported "Hardware Offline" due to missing signal strength mapping.
*   **Siren Resumption Hardening RESOLVED (R-ID 301)**: 
    *   **Cooldown Protection**: Refactored `AppAlarmManager.resetEvaluation()` to preserve `lastSirenStopTs` during role transitions (Tracker <-> Viewer).

## 🟢 Resolved Issues (Sep.09.10)
*   **Legacy Field Cleanup RESOLVED (Part C - R-ID 284)**. Successfully migrated all consumers in the `:app` module to use partitioned state objects directly. Sanitized `LocationUpdate.kt` by removing 26+ legacy bridge properties.
*   **Issue #935 RESOLVED: Monotonic Propagation (R-ID 279)**. Restored monotonic `rt` field propagation in `TelemetryUseCase`.

*(Total: 968 Issues Resolved since inception)*
