# Project Issues & Hardening Tracking (Sep.09.15)

## 🎯 Current Resumption Focus: Background Service Stability & Grid Precision
Watchdog precision gaps and GNSS hysteresis for Android 15 have been fully remediated. Focus shifts to long-term lifecycle stability and potential simplification of the service orchestration layer.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *None at this time. Dashboard metrics reflect full remediation of current priority batch.*

## 🟢 Recently Resolved Issues (Sep.09.15)
*   **Watchdog Precision Audit RESOLVED (R-ID 302)**:
    *   **Fixed Grid Scheduling**: Remediated cumulative scheduling drift in `SystemMonitor.kt` by implementing grid-aligned watchdog pulses.
    *   **Anchor Integration**: Updated `TrackerService.kt` and `ViewerService.kt` to anchor the watchdog grid to the service start time, ensuring strict 90s intervals regardless of individual tick execution delays.
*   **A15 Hysteresis Audit RESOLVED (R-ID 274)**:
    *   **Suppression Verification**: Verified that the 10s cooling window in `HardwareProvider.kt` correctly suppresses GNSS jitter during rapid thermal transitions on Samsung A15.
    *   **UI Sampling Alignment**: Confirmed that `MainViewModel.kt` uses a 5s sampling rate for A15 devices to maintain HUD stability during high-load scenarios.

## 🟢 Recently Resolved Issues (Sep.09.11)
*   **Issue #939 RESOLVED: Build Restoration (Part B)**: Fixed unresolved references in `BehaviorUseCase.kt` and `MainViewModel.kt` following the telemetry partitioning (R-ID 284).
*   **Hardening Audit RESOLVED (Forensic Integrity)**: Fixed mapping gaps in `ConnectivitySuite.kt` for signal, maxTemp, and isGnssThrottled.
*   **Siren Resumption Hardening RESOLVED (R-ID 301)**: Cooldown protection preserved across role transitions.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 303 (Rules: 53, IDs: 250), Resolved: 970, Open: 0, Testing: 100% (Sub-items: 50), Ideas: 5, QA: 267]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.09.15)*
