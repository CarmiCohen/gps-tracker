# Project Issues & Hardening Tracking (Aug.16.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 0 | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 624 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None at this time.*

---

## 🔴 Open Issues
*   *None at this time.*

---

## 🟢 Recently Resolved Issues (Aug.16.00)
*   **[Issue #182] [Severity: Critical] [Category: Environment] Startup ANR & GC Thrashing.**
    *   **Resolution**: Optimized `MapOverlayManager` to reuse cached `GeoPoint` objects within `TrailPoint` and `ViolationPoint`, eliminating massive allocation churn during map updates. Increased `STARTUP_SETTLING_DELAY_MS` to 10s and deferred `GpsApplication` osmdroid setup to ensure main-thread clarity during first-frame rendering. (R182)
*   **[Issue #181] [Severity: High] [Category: Performance] DeadSystemException on Startup.**
    *   **Resolution**: Addressed via the increased `STARTUP_SETTLING_DELAY_MS` (10000ms) which prevents Binder exhaustion and system-server stalls during heavy database and UI initialization on resource-constrained environments. (R181)
*   **[Issue #180] [Severity: Critical] [Category: Performance] SQLite UNIQUE constraint failure regression.**
    *   **Resolution**: Transitioned the `UNIQUE` constraint to `localId` (Migration 70/71) and updated `LogDao` to use `OnConflictStrategy.IGNORE`. Wired all missing migrations in `AppModule`. (R180)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.16.00)
