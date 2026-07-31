# Project Issues & Hardening Tracking (July.31.37)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 2 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 499 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #663] [Severity: Medium] [Category: Performance] SnapshotStateList Lock Verification Failure**.
    *   **Concern**: Regression or incomplete fix for #657. JIT/Dex verification warnings for `SnapshotStateList.conditionalUpdate` are appearing in Logcat, potentially impacting telemetry processing throughput.
*   **[Issue #664] [Severity: Medium] [Category: Performance] Startup Davey Stalls (Regression)**.
    *   **Concern**: 1.7s+ Davey stalls observed during startup (PID 27707), indicating that the `STARTUP_SETTLING_DELAY_MS` (3000ms) might be deferring the symptom rather than resolving the root cause of main-thread contention.

---

## 🔴 Open Issues
*   **[Issue #660] Forensic Audit: Log Buffer Pressure**.
    *   **Description**: High-frequency telemetry logging causing occasional I/O spikes in `LogManager`.
    *   **Objective**: Implement non-blocking circular log buffer and optimize SQLite batch inserts.

---

## 🟢 Recently Resolved Issues (July.31.37)
*   **[Issue #662] [Severity: High] [Category: Hardware] libmbrainSDK Loading Failure**.
    *   **Resolution**: Hardened JNI loading for Samsung A15/Android 15+. Fixed ProGuard rules to preserve `MbrainHardwareManager` and native methods. Corrected `app/build.gradle` by setting `useLegacyPackaging = false` to support 16KB page alignment and uncompressed library loading. Added explicit `abiFilters` for ARM architectures.
*   **[Issue #661] [Severity: Critical] [Category: Stability] ForegroundServiceStartNotAllowedException during Restoration**.
    *   **Resolution**: Hardened `onStartService` in `MainActivity.kt` by wrapping the FGS start attempt and the lifecycle check in a comprehensive `try-catch` block.
*   **[Issue #657] [Severity: Low] [Category: Performance] Compose Snapshot Lock Verification Failure**.
*   **[Issue #656] [Severity: Medium] [Category: Stability] userfaultfd: MOVE ioctl unsupported**.
*   **[Issue #642] Map Settings Icon Contrast**.
*   **[Issue #653] [Severity: High] [Category: Performance] Excessive Garbage Collection**.
*   **[Issue #658] [Severity: High] [Category: Performance] Persistent Startup Davey Stalls**.
*   **[Issue #659] [Severity: Medium] [Category: Stability] libmbrainSDK Initialization Instability**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.31.37-G)
