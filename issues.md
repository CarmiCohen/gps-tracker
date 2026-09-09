# Project Issues & Hardening Tracking (Sep.09.11)

## 🎯 Current Resumption Focus: Forensic Integrity & Background Hardening
Telemetry integrity gaps identified during the post-partition audit have been remediated. Siren cooldown logic is now robust across role transitions. Focus shifts to background service lifecycle stability on Android 15.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **A15 Hysteresis Audit**: Verify that GNSS throttling hysteresis (R-ID 274) correctly suppresses HUD jitter during rapid thermal transitions on Samsung A15.
*   **Watchdog Precision**: Audit `SystemWatchdog` for potential drift during long-running background sessions (>12h).

## 🟢 Recently Resolved Issues (Sep.09.11)
*   **Issue #939 RESOLVED: Build Restoration (Part B)**: Fixed unresolved references in `BehaviorUseCase.kt` and `MainViewModel.kt` following the telemetry partitioning (R-ID 284). Updated mappings for `speed`, `maxTemp`, and `currentMa` to their respective partitioned sub-states.
*   **Hardening Audit RESOLVED (Forensic Integrity)**: 
    *   **Mapping Fix**: Identified and resolved gaps in `ConnectivitySuite.kt` where `integrity.signal`, `atmospheric.maxTemp`, `integrity.isGnssThrottled`, and `kinetic.maxAccuracy` were not propagated from remote telemetry (Protobuf/JSON).
    *   **HUD Restoration**: Fixed regressions where remote trackers incorrectly reported "Hardware Offline" due to missing signal strength mapping.
*   **Siren Resumption Hardening RESOLVED (R-ID 301)**: 
    *   **Cooldown Protection**: Refactored `AppAlarmManager.resetEvaluation()` to preserve `lastSirenStopTs` during role transitions (Tracker <-> Viewer).

## 🟢 Recently Resolved Issues (Sep.09.10)
*   **Legacy Field Cleanup RESOLVED (Part C)**: 
    *   Migrated `TrackerService.kt`, `ViewerService.kt`, `LogManager.kt`, `GpsStatusManager.kt`, `DashboardStateProvider.kt`, `ConnectivitySuite.kt`, and `TelemetryUseCase.kt` to use partitioned states (`.kinetic`, `.atmospheric`, `.integrity`) directly.
    *   **Removed 26+ Legacy Bridges**: Sanitized `LocationUpdate.kt` by removing all aggregate property accessors. (R-ID 284).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 302 (Rules: 53, IDs: 249), Resolved: 968, Open: 2, Testing: 98% (Sub-items: 50), Ideas: 2, QA: 269]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.09.11)*
