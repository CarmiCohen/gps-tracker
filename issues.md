# Project Issues & Hardening Tracking (Aug.04.114)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 537 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None identified in Aug.04.114.*

---

## 🔴 Open Issues
*   *No high-priority open issues.*

---

## 🟢 Recently Resolved Issues (Aug.04.114)
*   **[Issue #728] [Severity: Medium] [Category: Performance] Forensic Audit: Storage-Aware Adaptive Pruning**.
    *   **Resolution**: Refined `LogRepository.proactivePruning` to utilize `StorageStatsManager` for granular pressure detection. Implemented "Fragmentation-Aware" deletion cycles using chunked deletes (`PRUNE_CHUNK_SIZE = 100`) and adaptive thresholds (300 to 3000 entries) based on storage state (Critical, Low, Normal, Charging). This prevents SQLite database fragmentation and I/O stalls on budget eMMC storage while maintaining a lean persistence footprint (R728).

---

## 🟢 Recently Resolved Issues (Aug.04.113)
*   **[Issue #727] [Severity: Medium] [Category: Performance] Forensic Trace Persistence: Batch-Write Optimization**.
    *   **Resolution**: Implemented Dynamic Batch Sizing in `LogRepository.performForensicDrain` (R727).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.04.114)
