# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 423**

## 4. Stability & Budget Baseline (July.30.35)
*   **Issue #640: Tracker Mode ANR (Regression)**.
    *   **Root Cause**: Main-thread contention on budget hardware (Samsung A15) caused by high-frequency UI pulses triggering $O(N)$ map overlay reconstructions (trails and accuracy circles).
    *   **Resolution**: Implemented aggressive 1000ms throttling for heavy overlay updates and decoupled tracker/viewer trail processing in `MapOverlayManager.kt`. Enforced 1000ms gating and 2.0m threshold for accuracy circle recalculations.
    - **Impact**: Eliminated system-level unresponsiveness post-relay connection on baseline devices.
*   **Issue #637: Log Spam: getPackageName()**.
    *   **Resolution**: Implemented 2000ms short-term status cache for `isLocalOnline()`.
*   **Issue #639: Tracker Mode ANR on Startup**.
    *   **Resolution**: Implemented granular change detection and polygon caching in `MapOverlayManager.kt`.
*   **Issue #638: Incorrect Permission Defaults**.
    *   **Resolution**: Corrected `PermissionState` data class in `MainUiState.kt`.
*   **Issue #634: ForegroundServiceStartNotAllowedException Crash**.
    *   **Resolution**: Implemented Foreground Service Start Hardening in `MainActivity`.

## 1. Kernel & OS Performance Hardening (July.25.13)
*   **Issue #547: Kernel Performance Warning (`userfaultfd`)**. 
    *   Finalized verification stack for Zero-Churn performance. 
    *   Integrated `LatencyMonitor` into `dashboardState` computation in `MainViewModel`. 
    *   Added forensic jitter logging for A15 hardware to detect ART compaction stalls on kernels lacking `userfaultfd` support.
*   **Issue #555: Forensic Snapshot Integrity**.
    *   Audited flyweight lifecycles in the telemetry pipeline.
    *   Ensured immutable boundaries between background aggregation and reactive UI consumers.

## 2. Network Lifecycle Hardening (July.25.12)
*   **Issue #545: Production Logging Leak (`StackLog`)**. 
    *   Implemented idempotent lifecycle management in `ConnectivitySuite`. 
    *   Added `isStarted` state guarding to prevent redundant platform-level network callback registrations.

## 3. Generic Latency Monitoring (July.25.11)
*   **Issue #590: Latency Monitoring Framework**. 
    *   Implemented unified `LatencyMonitor` in `:core:engine`. 
    *   Integrated monitoring into JNI (Mbrain), DB (Repository), and Log paths.

... [See historical logs for older resolutions]
