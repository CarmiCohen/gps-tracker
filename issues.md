# Project Issues & Hardening Tracking (Aug.04.116)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 540 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None identified in Aug.04.116.*

---

## 🔴 Open Issues
*   *No high-priority open issues.*

---

## 🟢 Recently Resolved Issues (Aug.04.116)
*   **[Issue #731] [Severity: High] [Category: Persistence] Forensic Bloat: Important/Special Logs Exempt from Pruning**.
    *   **Resolution**: Implemented a secondary safety tier in `LogRepository.proactivePruning`. Introduced `LogDao.pruneSpecialLogsChunk` to allow chunked deletion of `isSpecial` (Forensic Trace) logs once the database exceeds `LOG_LIMIT_STRICT` (5000). This ensures system stability on budget hardware while preserving maximum history. (R731).

---

## 🟢 Recently Resolved Issues (Aug.04.115)
*   **[Issue #729] [Severity: Low] [Category: Maintenance] Forensic Audit: Automated Database Integrity Validation**.
    *   **Resolution**: Implemented `PRAGMA integrity_check` validation within the `AppDatabase`. Integrated the audit into `MaintenanceWorker` with charging-aware frequency (24h/12h). (R729).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.04.116)
