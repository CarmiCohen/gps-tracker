# Project Issues & Hardening Tracking (Aug.14.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 0 | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 618 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No critical risks identified. System stable under 100Hz load.*

---

## 🔴 Open Issues
*   *No active open issues.*

---

## 🟢 Recently Resolved Issues (Aug.14.07)
*   **[Issue #177] [Severity: Critical] [Category: Performance] Startup ANR & Heap Exhaustion.**
    *   **Resolution**: Hardened `LogRepository` by reducing reactive log limits (2k/5k), implementing pruning for "Important" logs (preventing 100k+ row bloat), and tightening forensic signature lookback to 1 hour to prevent OOM during recovery. (R177)
*   **[Issue #176] [Severity: Critical] [Category: Performance] Proactive Pruning ANR.**
    *   **Resolution**: Optimized database schema with composite indices and refactored `LogRepository` to use `withTransaction` for batch operations. Eliminated 2.2s I/O stalls. (R176)
*   **[Issue #172] [Severity: High] [Category: Data Integrity] Viewer-Side LocationProcessor State Audit.**
    *   **Resolution**: Finalized full forensic SIT state parity in the viewer-side mirrored state. (R172)
*   **[Issue #174] [Severity: Medium] [Category: Performance] Forensic Replay Latency Audit.**
    *   **Resolution**: Optimized replay scrubbing performance for high-frequency (100Hz) telemetry sets. (R174)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.14.07)
