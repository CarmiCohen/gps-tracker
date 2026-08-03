# Project Issues & Hardening Tracking (Aug.03.45)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 509 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (Aug.03.45)
*   **[Issue #700] [Severity: Medium] [Category: Performance] Forensic Audit: Power-Aware Sampling Scaling**.
    *   **Resolution**: Implemented dynamic forensic sampling rates (10Hz - 100Hz) based on `isCharging` and `isCoolingModeActive`. Integrated zero-allocation logging path (`logForensicTraceOptimized`) to ensure R668 compliance during 100Hz capture, optimizing battery consumption while maintaining forensic integrity (R700).
*   **[Issue #669] [Severity: High] [Category: Performance] Forensic Audit: Database I/O Contention under High-Frequency Trace Bursts**.
    *   **Resolution**: Implemented `ForensicSpillBuffer` using `MappedByteBuffer` to decouple trace capture from SQLite persistence (R-HARDWARE-01).
*   **[Issue #668] [Severity: Medium] [Category: Performance] Forensic Audit: Object Churn in Telemetry Pipelines**.
    *   **Resolution**: Implemented zero-allocation telemetry path using mutable flyweight patterns (R668).
*   **[Issue #667] [Severity: Medium] [Category: Performance] Forensic Audit: Memory Pressure under High-Frequency JNI Traffic**.
*   **[Issue #664] [Severity: Medium] [Category: Performance] Startup Davey Stalls (Regression)**.
*   **[Issue #663] Forensic Audit: SnapshotStateList Lock Verification Failure**.
*   **[Issue #660] Forensic Audit: Log Buffer Pressure**.
*   **[Issue #666] Phone Setup ANR (Main-Thread Contention)**.
*   **[Issue #665] 16KB Page Size Alignment Regression**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.03.45)
