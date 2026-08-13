# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 592**

## 31. System Log Hardening (Aug.13.08)
*   **Issue #156: WakeLock Log Saturation**.
    *   **Resolution**: Implemented **WakeLock Log Throttling (R156)** in `SystemMonitor`. Introduced a minimum 60-second logging interval for WakeLock acquisitions using `WAKELOCK_LOG_THROTTLE_MS`. This prevents high-frequency background pulses (such as the 10s stay-alive checks in `AppSensorManager`) from flooding the logcat while ensuring that resource state changes remain visible for forensic analysis. (R156)

## 30. Phone Setup UI Refinement (Aug.13.07)
*   **Issue #155: Phone Setup UI Clutter**.
    *   **Resolution**: Refined the `GuideSection` component in `PhoneSetupOverlay` to hide completion-dependent action buttons once steps are verified (`isCompleted == true`). This improvement clarifies the remaining setup tasks and reduces visual noise, enhancing the "out-of-box" experience. (R155)

## 29. Telemetry GC Pressure Mitigation (Aug.13.06)
*   **Issue #152: Excessive GC Pressure**.
    *   **Resolution**: Implemented **Telemetry Flyweight Pooling (R152)**. Refactored `ConnectionPoint` into a mutable class and eliminated expensive default `UUID.randomUUID()` generation. Introduced a managed pool of `ConnectionPoint` instances in `HistoryManager` and optimized `MainRepository` to support zero-allocation buffering for history persistence. This eliminates 1Hz object churn during steady-state tracking, significantly reducing GC spikes on budget hardware. (R152)

## 28. Startup Davey Stall Mitigation (Aug.13.05)
*   **Issue #153: Startup Davey Stalls**.
    *   **Resolution**: Implemented **Staggered UI Hydration (R153)**. Introduced a multi-stage `hydrationLevel` in `MainUiState` to decouple the theme scaffold from complex Navigation and Screen content. Refactored `MainViewModel` to progressively increment hydration (Stages 0-3) with intentional delays (150ms-300ms) during cold-boot initialization. This spreads the massive initial composition pass across multiple frames, eliminating the 1600ms Davey stalls previously observed on budget hardware like the Samsung A15. (R153)

## 27. Samsung A15 Detection Hardening (Aug.13.04)
*   **Issue #150: Samsung A15 Phone Setup Bypass**.
    *   **Resolution**: Hardened the **Samsung A15 Detection Logic (R405)**. Broadened the `isA15Device` detection in `SystemStatusProvider` to inspect `Build.DEVICE` and `Build.PRODUCT` strings, capturing variants like SM-A155F. Relocated the automated setup prompt trigger from `MainActivity` to the `MainViewModel` permission monitoring loop. This eliminates race conditions during startup and ensures the battery exemption prompt triggers reliably even if the OS-level state is acquired after the first frame render. (R405)

## 26. Build Stability & Forensic Deduplication (Aug.13.02)
*   **Issue #154: Type Inference Failures**.
    *   **Resolution**: Hardened the **Latency Monitoring Framework (R154)** by explicitly typing measurement calls and refactoring `LogRepository` to fix a deduplication bug (Issue #705) where forensic signatures were compared incorrectly.

## 25. Forensic Performance Hardening (Aug.13.00)
*   **Issue #146: Optimize Forensic Drainer**.
    *   **Resolution**: Hardened the **Forensic Drainer (R146)** to eliminate 200ms latency spikes and high GC pressure. Refactored `ForensicSpillBuffer` to utilize zero-allocation paths for `peek()` and `writeTrace()` by implementing pre-allocated processing buffers. Streamlined `LogRepository.performForensicDrain()` using a single-pass filtering/mapping loop and optimized signature deduplication.

---
*For historical resolutions #1 through #24, please refer to the Git history or individual backlog shards in `STATUS/backlog_shards/`. (vAug.13.08)
