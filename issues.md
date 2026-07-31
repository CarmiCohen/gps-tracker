# Project Issues & Hardening Tracking (July.31.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 3 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 498 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #662] [Severity: High] [Category: Hardware] libmbrainSDK Loading Failure**.
    *   **Concern**: `libmbrainSDK` fails to load on the target device, disabling hardware watchdog pokes and stabilization features for budget hardware (Samsung A15).
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

## 🟢 Recently Resolved Issues (July.31.01)
*   **[Issue #661] [Severity: Critical] [Category: Stability] ForegroundServiceStartNotAllowedException during Restoration**.
    *   **Resolution**: Hardened `onStartService` in `MainActivity.kt` by wrapping the FGS start attempt and the lifecycle check in a comprehensive `try-catch` block. This ensures that any OS-level denials (even while the Activity is technically RESUMED) are caught, preventing fatal crashes and correctly triggering the `SetRecoveryPending` state.
*   **[Issue #657] [Severity: Low] [Category: Performance] Compose Snapshot Lock Verification Failure**.
    *   **Resolution**: Hardened the `AndroidView` update cycle in `MapComponents.kt` by wrapping imperative overlay updates in `Snapshot.withoutReadObservation`.
*   **[Issue #656] [Severity: Medium] [Category: Stability] userfaultfd: MOVE ioctl unsupported**.
*   **[Issue #642] Map Settings Icon Contrast**.
*   **[Issue #653] [Severity: High] [Category: Performance] Excessive Garbage Collection**.
*   **[Issue #658] [Severity: High] [Category: Performance] Persistent Startup Davey Stalls**.
*   **[Issue #659] [Severity: Medium] [Category: Stability] libmbrainSDK Initialization Instability**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.31.01-G)
