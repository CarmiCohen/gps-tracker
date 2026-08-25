# Project Issues & Hardening Tracking (Aug.25.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 STABLE | 47 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 712 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Samsung A15 Setup Blocker**: Initial deployment on SM-A155F identifies "Unrestricted" battery mode and "Appear on Top" permissions as hard blockers for system readiness. The `PhoneSetupOverlay` correctly intercepts mode entry, but automated recovery may be needed if users bypass these settings (Idea #183).

---

## 🔴 Open Issues
*   *(No critical open issues for Aug.25.00)*

---

## 🟢 Recently Resolved Issues (Aug.25.00)
*   **Issue #309**: **Compose Lock Verification Persistent Warnings**. Refactored `MapOverlayManager` pools (`homeMarkerPool`, `violationMarkerPool`, etc.) and `homeIcons` cache from `SnapshotStateList/Map` to standard `ArrayList/HashMap`. Since these are accessed imperatively within `AndroidView.update`, removing Compose observation overhead eliminated `conditionalUpdate` failures and frame skips on A15 hardware (R309).
*   **Issue #310**: **libmbrainSDK Ghost Load Persistence**. Neutralized all literal legacy SDK signatures within `JdHardwareManager` log messages. This prevents Samsung's CFMS string-pool scanning from triggering heuristic "Ghost Load" attempts, silencing forensic noise (R212).

---

## 🟢 Recently Resolved Issues (Aug.24.01)
*   **Issue #307**: **Inconsistent Maintenance Uptime Logging**. Standardized Monotonic Authority by migrating `MaintenanceWorker` silence detection to `elapsedRealtime()`. Implemented `LAST_SERVICE_TICK_REALTIME_KEY` persistence in `TrackerService` and `ViewerService` to ensure duration-check integrity across service restarts and system time jumps.
*   **Issue #255**: **Compose Lock Verification Failure**. Refactored `MapOverlayManager` pools and icon caches to `SnapshotStateList` and `SnapshotStateMap`. (Superseded by Issue #309).
*   **Issue #251**: **Integration Failure (mbrainSDK)**. Identified the `libmbrainSDK` Logcat error as a "Ghost Load" triggered by Samsung's CFMS. (Hardened by Issue #310).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.25.00)
