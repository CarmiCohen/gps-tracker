# Resolution Archive (Sep.09.16)

## 🟢 Resolved Issues (Sep.09.16)
*   **Identity Color Role Confusion RESOLVED (#942)**: 
    *   **Map Icons**: Remediated `MapOverlayManager.kt` where both icons used Cyan. Fixed `createTrackerBitmap` to use `BrandJd` Green with `Style.FILL` for the inner circle.
    *   **HUD StatusBar**: Fixed `SharedUiComponents.kt` to ensure peer badges (TRK/VWR) use role-appropriate colors (`BrandJd`/`ViewerCyan`). Local status indicators and progress bars now also follow role identity.
    *   **Telemetry Dashboard**: Refined `OverlayComponents.kt` to enforce identity colors for metrics (Tracker=Green, Viewer=Cyan) regardless of active mode.

## 🟢 Resolved Issues (Sep.09.15)
*   **Watchdog Precision Audit RESOLVED (R-ID 302)**: Remediated cumulative scheduling drift in `SystemMonitor.kt` by implementing **Fixed Grid Scheduling**. Watchdog pulses are now anchored to the service start monotonic time (`elapsedRealtime`) and aligned to a strict 90s grid. Updated `TrackerService.kt` and `ViewerService.kt` to set the session anchor at initialization.
*   **A15 Hysteresis Audit RESOLVED (R-ID 274)**: Verified that the 10s cooling window (`GNSS_THROTTLING_HYSTERESIS_MS`) in `HardwareProvider.kt` correctly suppresses HUD speed jitter during rapid thermal transitions on Samsung A15 hardware. Confirmed that UI sampling in `MainViewModel.kt` is throttled to 5s for A15 devices to maintain HUD stability.

## 🟢 Resolved Issues (Sep.09.11)
*   **Issue #939 RESOLVED: Build Restoration (Part B)**. Fixed unresolved references in `BehaviorUseCase.kt` and `MainViewModel.kt` following the telemetry partitioning (R-ID 284). Updated `effectiveLocation.speed` to `effectiveLocation.kinetic.speed` and mapped `maxTemp`/`currentMa` to their respective partitioned states (`atmospheric.maxTemp` and `integrity.currentMa`).
*   **Hardening Audit RESOLVED (Forensic Integrity)**: 
    *   **Mapping Fix**: Identified and resolved gaps in `ConnectivitySuite.kt` where `integrity.signal`, `atmospheric.maxTemp`, `integrity.isGnssThrottled`, and `kinetic.maxAccuracy` were not propagated from remote telemetry (Protobuf/JSON).
    *   **HUD Restoration**: Fixed regressions where remote trackers incorrectly reported "Hardware Offline" due to missing signal strength mapping.
*   **Siren Resumption Hardening RESOLVED (R-ID 301)**: 
    *   **Cooldown Protection**: Refactored `AppAlarmManager.resetEvaluation()` to preserve `lastSirenStopTs` during role transitions (Tracker <-> Viewer).

*(Total: 971 Issues Resolved since inception)*
