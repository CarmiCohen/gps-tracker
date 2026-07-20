# Project Issues & Hardening Tracking (July.20.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 3 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 308 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #114: Monotonic/Wall-Clock Desync**: While the bridge logic is robust, extreme system clock manipulation while the app is killed (e.g., changing the year) could still create massive "virtual" gaps that might hit the `MAX_BACKFILL_POINTS` limit (1000 points). This is a safe failure mode (preserving the last 1000 seconds), but worth noting.

---

## 🔴 Open Issues

### Issue #107: Step Detector Hardware Registration Failure (R405c Fallback)
*   **Description**: Hardware `TYPE_STEP_DETECTOR` is detected by the OS but `sensorManager.registerListener` returns `false`.
*   **Impact**: The system engages the R405c Accelerometer fallback. While functional for "stay-alive" pulses, it is less power-efficient than the hardware step detector.
*   **Device Noted**: Samsung SM-A155F.

### Issue #113: R405c Fallback Efficacy Verification (Samsung A15)
*   **Description**: Perform long-term field testing on SM-A155F to confirm that the Accelerometer-based stay-alive pulse (R405c) is sufficient to prevent OS-level service eviction when the hardware Step Detector fails to register.

### Issue #114: Monotonic Timeline Boundary Overflow
*   **Description**: Investigate impact of extreme (multi-year) system clock desync on the 1000-point backfill limit.

---

## 🟢 Recently Resolved Issues (July.20.07)
*   **Issue #115: Startup Scope Hardening (GlobalScope Removal)**.
    *   **Root Cause**: Use of unmanaged `GlobalScope` in `GpsApplication` created non-deterministic initialization and defied maintainability objectives.
    *   **Resolution**: Migrated `osmdroid` and `WorkManager` setup to a managed `@ApplicationScope` provided by Hilt, ensuring lifecycle-safe execution.
*   **Issue #109 & #111: Startup Performance Hardening**.
    *   **Resolution**: Offloaded I/O intensive operations to `Dispatchers.IO` to prevent Main-thread blocking.
*   **Issue #110 & #112: mbrainSDK Log Suppression**.
    *   **Resolution**: Filtered vendor-specific `libmbrainSDK` noise in the global `Timber` tree.
*   **Issue #108: MaintenanceWorker Startup Recovery Race**.
    *   **Resolution**: Implemented immediate timestamp refresh in service `onCreate()` methods.

## 🟢 Recently Resolved Issues (July.20.06)
*   **Release Finalization & Version Synchronization**.
*   **Issue #106: Unified Forensic Ribbon Continuity (R106)**.
