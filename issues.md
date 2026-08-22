# Project Issues & Hardening Tracking (Aug.22.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 CRITICAL | 53 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 704 |

---

## ⚠️ Newly Identified Risks & Concerns
*   (None)

---

## 🔴 Open Issues
*   **Issue #251**: **Integration Failure (mbrainSDK)**: Logcat reported `Can't load libmbrainSDK`.
*   **Issue #255**: **Compose Lock Verification Failure**: `SnapshotStateList` performance impact.
*   **Issue #266**: **Mali Driver "Meow" Configuration Failures**: Graphics layer config failures on A15.
*   **Issue #280**: **Shadow-Cache LRU Race Condition**: Unsynchronized eviction during 100Hz simulation.
*   **Issue #307**: **Inconsistent Maintenance Uptime Logging**: Requires verification of monotonic authority.

---

## 🟢 Recently Resolved Issues (Aug.22.04)
*   **Issue #197**: **Database Pruning Standardization**. Aligned `ViolationDao`, `TrailDao`, and `HistoryDao` with R197 chunked pruning standards. All high-frequency tables now support staggered deletion to prevent I/O stalls.
*   **Issue #308**: **Restored Core Engine Definitions**. Re-implemented `AlarmEvaluationState`, `ProcessedLocation`, `SpatialAnchor`, and `RejectedPoint` in `EngineModels.kt`, unblocking the build and verifying Chapter 11.2 tests.
*   **Issue #250**: **Navigation Backstack Inconsistency**. Hardened backstack logic using explicit `popUpTo` and `launchSingleTop`.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.22.04)
