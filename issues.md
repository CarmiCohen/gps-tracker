# Project Issues & Hardening Tracking (Aug.15.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 0 | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 619 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **None**

---

## 🔴 Open Issues
*   **None**

---

## 🟢 Recently Resolved Issues (Aug.15.01)
*   **[Issue #178] [Severity: Critical] [Category: Performance] Sustained 100Hz Heap Exhaustion & ANR.**
    *   **Resolution**: Reduced forensic signature lookback to 10 minutes and gated `eventLogsFlow` mapping by UI visibility to resolve OOM/ANR under high-frequency flow. (R178)
*   **[Issue #177] [Severity: Critical] [Category: Performance] Startup ANR & Heap Exhaustion.**
    *   **Resolution**: Hardened `LogRepository` by reducing reactive log limits (2k/5k) and implementing pruning for "Important" logs. (R177)
*   **[Issue #176] [Severity: Critical] [Category: Performance] Proactive Pruning ANR.**
    *   **Resolution**: Optimized database schema and refactored pruning to use chunked transactions. (R176)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.15.01)
