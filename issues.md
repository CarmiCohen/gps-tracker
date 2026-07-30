# Project Issues & Hardening Tracking (July.30.46)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 2 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 491 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #656] [Severity: Medium] [Category: Stability] userfaultfd: MOVE ioctl unsupported**. Kernel-level timeout detected on Samsung A15; may impact ART memory compaction efficiency.
*   **[Issue #657] [Severity: Low] [Category: Performance] Compose Snapshot Lock Verification Failure**. `SnapshotStateList` methods failing verification, leading to sub-optimal UI performance.

---

## 🔴 Open Issues
*   **[Issue #653] Excessive Garbage Collection**. Confirmed ~34MB churn every 120ms.
*   **[Issue #642] Map Settings Icon Contrast**. Verified low contrast on Mapnik tiles.

---

## 🟢 Recently Resolved Issues (July.30.46)
*   **[Issue #655] [Severity: High] [Category: Performance] Regression: Unthrottled IPC Bursts**.
    *   **Resolution**: Implemented `FORCED_REFRESH_COOLDOWN_MS` (2s) in `SystemStatusProvider` to prevent reactive UI/Service cycles from flooding the system with unthrottled IPC audits.
    *   **Impact**: Eliminated `getPackageName` logcat spam and associated 1.1s Davey stalls on Samsung A15 hardware.
*   **[Issue #654] UI Jank & Main Thread Stalls during IPC bursts**.
    *   **Resolution**: Centralized IPC calls into `SystemStatusProvider`.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.46-G)
