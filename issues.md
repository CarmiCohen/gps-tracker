# Project Issues & Hardening Tracking (July.31.38)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 2 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 502 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues
*   **[Issue #663] [Severity: Medium] [Category: Performance] SnapshotStateList Lock Verification Failure**.
    *   **Concern**: Regression or incomplete fix for #657. JIT/Dex verification warnings for `SnapshotStateList.conditionalUpdate` are appearing in Logcat, potentially impacting telemetry processing throughput.
*   **[Issue #664] [Severity: Medium] [Category: Performance] Startup Davey Stalls (Regression)**.
    *   **Concern**: 1.7s+ Davey stalls observed during startup (PID 27707), indicating that the `STARTUP_SETTLING_DELAY_MS` (3000ms) might be deferring the symptom rather than resolving the root cause of main-thread contention.

---

## 🟢 Recently Resolved Issues (July.31.38)
*   **[Issue #660] Forensic Audit: Log Buffer Pressure**.
    *   **Resolution**: Implemented non-blocking circular log buffer using Kotlin Channels and optimized SQLite batch inserts. This eliminates I/O spikes by decoupling log submission from persistence and processing logs in batches within transactions.
*   **[Issue #666] [Severity: Critical] [Category: Stability] Phone Setup ANR (Main-Thread Contention)**.
    *   **Resolution**: Hardened Samsung A15 stability by relaxing permission polling intervals to 5s in `MainViewModel`.
*   **[Issue #665] [Severity: High] [Category: Hardware] 16KB Page Size Alignment Regression**.
    *   **Resolution**: Restored full compliance with 16KB page alignment by adding `android:extractNativeLibs="false"` to `AndroidManifest.xml`.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.31.38)
