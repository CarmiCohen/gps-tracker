# Project Issues & Hardening Tracking (Aug.03.37)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 508 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (Aug.03.37)
*   **[Issue #669] [Severity: High] [Category: Performance] Forensic Audit: Database I/O Contention under High-Frequency Trace Bursts**.
    *   **Resolution**: Implemented `ForensicSpillBuffer` using `MappedByteBuffer` to decouple trace capture from SQLite persistence. High-frequency telemetry is now serialized into off-heap memory at 100Hz and drained to the database in efficient batches by a background worker, eliminating "Davey" stalls and WAL pressure on budget hardware (R-HARDWARE-01).
*   **[Issue #668] [Severity: Medium] [Category: Performance] Forensic Audit: Object Churn in Telemetry Pipelines**.
    *   **Resolution**: Implemented zero-allocation telemetry path using mutable flyweight patterns for `SystemHealthState`, `LocationState`, and `ViolationReport`. Introduced pulse-triggered StateFlow emissions in `MainViewModel` to eliminate `copy()` churn while maintaining UI reactivity (R668).
*   **[Issue #667] [Severity: Medium] [Category: Performance] Forensic Audit: Memory Pressure under High-Frequency JNI Traffic**.
    *   **Resolution**: Implemented zero-copy state synchronization using `DirectByteBuffer` in `MbrainHardwareManager`.
*   **[Issue #664] [Severity: Medium] [Category: Performance] Startup Davey Stalls (Regression)**.
    *   **Resolution**: Eliminated 1.7s+ startup Davey stalls by deferring `osmdroid` initialization in `GpsApplication`.
*   **[Issue #663] Forensic Audit: SnapshotStateList Lock Verification Failure**.
*   **[Issue #660] Forensic Audit: Log Buffer Pressure**.
*   **[Issue #666] Phone Setup ANR (Main-Thread Contention)**.
*   **[Issue #665] 16KB Page Size Alignment Regression**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.03.37)
