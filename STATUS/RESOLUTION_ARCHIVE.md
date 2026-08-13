# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 599**

## 38. 1Hz Telemetry Path Optimization (Aug.13.12)
*   **Issue #163: Telemetry Path Churn**.
    *   **Resolution**: Eliminated object churn in the 1Hz telemetry path by refactoring `DashboardState` to use primitive types instead of pre-formatted strings. Moved formatting logic into the UI layer (`MainDashboardGrid`, `TelemetryBox`, `ForensicSection`) using `remember` blocks to ensure string allocations only occur when the underlying data actually changes. This significantly reduces GC pressure and improves UI fluidity on budget hardware (R163).

## 37. Phone Setup ANR Remediation (Aug.13.11)
*   **Issue #162: Phone Setup ANR Stall**.
    *   **Resolution**: Resolved Main-thread ANR stalls during phone setup by implementing a 150ms hydration gate and 80ms sequential rendering offsets in `PhoneSetupOverlay`. Memoized static build properties and hardware strings to minimize resource lookup overhead. Optimized `HeaderBar` to suppress pulse animations while the setup overlay is visible (R162).

## 36. Version Synchronization (Aug.13.10)
*   **Issue #160: Version Mismatch**.
    *   **Resolution**: Synchronized `app/build.gradle` and UI to `Aug.13.10`. Resolved the inconsistency where the previous deployment was using an outdated `Aug.13.09` build despite documentation claims. (R160)

## 35. False Positive Remediation (Aug.13.10)
*   **Issue #161: Persistent Denials (False Positive)**.
    *   **Resolution**: Confirmed that SELinux denials observed after the initial fix were due to the version mismatch (#160). Verified that the SDK-aware branching in `SystemStatusProviderImpl.kt` (R159) is fully effective in the synchronized build. (R161)

## 34. SELinux Telemetry Remediation (Aug.13.10)
*   **Issue #159: SELinux LoadAvg Denials**.
    *   **Resolution**: Remediated `/proc/loadavg` and `/proc/stat` access denials by implementing SDK-aware branching in `SystemStatusProviderImpl.kt`. On Android 10+ (SDK 29+), the system now bypasses restricted `/proc` reads to eliminate SELinux audit noise. Stress detection and silent failure correlation now correctly rely on fallback proxies, including I/O latency and thermal throttling states. (R159)

## 33. Forensic Validation & QA Audit (Aug.13.09)
*   **Issue #158: Forensic Validation & QA Audit**.
    *   **Resolution**: Conducted an end-to-end audit of the performance hardening cycle (R152-R157). Verified that:
        1. **WakeLock Throttling (R156)** successfully reduces logcat noise.
        2. **Staggered Hydration (R153)** eliminates Main-thread Davey stalls during composition.
        3. **Flyweight Pooling (R152, R157)** effectively stabilizes memory usage and reduces GC frequency during active tracking.
    *   Synchronized `versionName` to `Aug.13.09` across the build system and UI. Identified **Issue #159** (SELinux LoadAvg denials) as a follow-up hardening task. (R158)

## 32. Violation Path Optimization (Aug.13.09)
*   **Issue #157: Violation Path Allocations**.
    *   **Resolution**: Eliminated object churn in the violation detection and mapping hot-paths by refactoring `ViolationPoint` into a mutable class with primitive coordinates (`lat`, `lng`) and internal `GeoPoint` caching (R157). Removed automatic `UUID.randomUUID()` generation and transient `GeoPoint` allocations during database-to-UI mapping, significantly reducing GC pressure during high-activity scenarios. (R157)

## 31. System Log Hardening (Aug.13.08)
*   **Issue #156: WakeLock Log Saturation**.
    *   **Resolution**: Implemented **WakeLock Log Throttling (R156)** in `SystemMonitor`. Acquisition logs are now throttled to 1/min using `WAKELOCK_LOG_THROTTLE_MS`. This prevents high-frequency background pulses from flooding the logcat while ensuring that resource state changes remain visible for forensic analysis. (R156)

## 30. Phone Setup UI Refinement (Aug.13.07)
*   **Issue #155: Phone Setup UI Clutter**.
    *   **Resolution**: Refined the `GuideSection` component in `PhoneSetupOverlay` to hide completion-dependent action buttons once steps are verified (`isCompleted == true`). This improvement clarifies the remaining setup tasks and reduces visual noise. (R155)

## 29. Telemetry GC Pressure Mitigation (Aug.13.06)
*   **Issue #152: Excessive GC Pressure**.
    *   **Resolution**: Implemented **Telemetry Flyweight Pooling (R152)**. Refactored `ConnectionPoint` into a mutable class and eliminated expensive default `UUID.randomUUID()` generation. Introduced instance pooling in `HistoryManager` and `MainRepository` to eliminate 1Hz object churn during steady-state tracking. (R152)

## 28. Startup Davey Stall Mitigation (Aug.13.05)
*   **Issue #153: Startup Davey Stalls**.
    *   **Resolution**: Implemented **Staggered UI Hydration (R153)**. Introduced a multi-stage `hydrationLevel` in `MainUiState` to decouple theme scaffold from complex Navigation and Screen content. This spreads the massive initial composition pass across multiple frames, eliminating the 1600ms Davey stalls previously observed. (R153)

## 27. Samsung A15 Detection Hardening (Aug.13.04)
*   **Issue #150: Samsung A15 Phone Setup Bypass**.
    *   **Resolution**: Hardened the **Samsung A15 Detection Logic (R405)** by inspecting `Build.DEVICE` and `Build.PRODUCT` strings (e.g., SM-A155F). Relocated trigger logic to the permission loop to eliminate race conditions. (R405)

---
*For historical resolutions #1 through #24, please refer to the Git history or individual backlog shards in `STATUS/backlog_shards/`. (vAug.13.12)
