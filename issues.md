# Project Issues & Hardening Tracking (July.20.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 6 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 303 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Monotonic/Wall-Clock Desync**: While the bridge logic is robust, extreme system clock manipulation while the app is killed (e.g., changing the year) could still create massive "virtual" gaps that might hit the `MAX_BACKFILL_POINTS` limit (1000 points). This is a safe failure mode (preserving the last 1000 seconds), but worth noting.
*   **Startup Frame Drop (Davey)**: Observed >700ms jank during `MainActivity` initialization. Likely caused by heavy I/O or repository initialization on the main thread during `MainViewModel.loadInitialData`.
*   **mbrainSDK Integration Missing**: Logcat reports `Can't load libmbrainSDK`. This library seems to be a missing dependency for advanced system monitoring.

---

## 🔴 Open Issues

### Issue #107: Step Detector Hardware Registration Failure (R405c Fallback)
*   **Description**: Hardware `TYPE_STEP_DETECTOR` is detected by the OS but `sensorManager.registerListener` returns `false`.
*   **Impact**: The system engages the R405c Accelerometer fallback. While functional for "stay-alive" pulses, it is less power-efficient than the hardware step detector.
*   **Device Noted**: Samsung SM-A155F.

### Issue #109: Startup Performance Bottleneck
*   **Description**: `Skipped 66 frames!` during startup.
*   **Action**: Investigate `MainViewModel` and `GpsApplication` initialization for blocking calls.

### Issue #110: Missing libmbrainSDK
*   **Description**: Repeated `Can't load libmbrainSDK` errors in logs.
*   **Action**: Verify if this is an optional vendor SDK or a missing project component.

### Issue #111: Performance Profiling for Startup Jank
*   **Description**: Use the Android Studio Profiler to identify exactly which part of `MainActivity.onCreate` or `MainViewModel.init` is blocking the UI thread. 
*   **Objective**: Reduce startup "Davey" duration to under 100ms.

### Issue #112: mbrainSDK Log Suppression
*   **Description**: Investigate if the `libmbrainSDK` load attempts can be suppressed or handled gracefully on devices where the library is absent to clean up system logs and prevent initialization noise.

### Issue #113: R405c Fallback Efficacy Verification (Samsung A15)
*   **Description**: Perform long-term field testing on SM-A155F to confirm that the Accelerometer-based stay-alive pulse (R405c) is sufficient to prevent OS-level service eviction when the hardware Step Detector fails to register.

---

## 🟢 Recently Resolved Issues (July.20.07)
*   **Issue #108: MaintenanceWorker Startup Recovery Race**.
    *   **Root Cause**: `LAST_SERVICE_TICK_TS_KEY` persisted from previous sessions without being cleared, causing the worker to trigger recovery before the new service could perform its first tick.
    *   **Resolution**: Implemented immediate timestamp refresh in `TrackerService.onCreate()` and `ViewerService.onCreate()` to "claim" the service as active before the staggered initialization completes.

## 🟢 Recently Resolved Issues (July.20.06)
*   **Release Finalization & Version Synchronization**.
    *   **Resolution**: Synchronized project versioning to `July.20.06` across `build.gradle`, source headers, and documentation. Performed final hardening of forensic ribbon drawing logic.

## 🟢 Recently Resolved Issues (July.20.05)
*   **Issue #106: Unified Forensic Ribbon Continuity (R106)**.
    *   **Root Cause**: Analytical ribbons (sensors and connection) were independently scaled and rendered, leading to visual misalignment and lack of synchronization during forensic review.
    *   **Resolution**: Consolidated `AnalyticalRibbons` to a single scale-aware `activeHistoryFlow`. Refactored rendering to use `ForensicRibbonContainer`, a unified baseline logic that enforces timeline continuity. Implemented explicit "Black Gap" visualization for data loss segments.
