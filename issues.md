# Project Issues & Hardening Tracking (Aug.22.05)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 CRITICAL | 50 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 707 |

---

## ⚠️ Newly Identified Risks & Concerns
*   (None)

---

## 🔴 Open Issues
*   **Issue #251**: **Integration Failure (mbrainSDK)**: Logcat reported `Can't load libmbrainSDK`.
*   **Issue #255**: **Compose Lock Verification Failure**: `SnapshotStateList` performance impact.
*   **Issue #307**: **Inconsistent Maintenance Uptime Logging**: Requires verification of monotonic authority.

---

## 🟢 Recently Resolved Issues (Aug.22.05)
*   **Audit Chapter 12.3**: **Sustained Storage Pressure**. Implemented simulation hooks and verified `PersistencePolicy` prioritization. Confirmed that normal logs/trails are gated while `isSpecial` forensic data persists under 99% fill levels (R197).
*   **Issue #280**: **Shadow-Cache LRU Race Condition**. Hardened `ShadowCache` using `ReentrantLock` and optimized initial capacity to prevent structural re-hashing and eviction race conditions during 100Hz simulation bursts (R280).
*   **Issue #140/12.2**: **Database Stress Audit (100Hz)**. Restored stress hooks and verified `ForensicSpillBuffer` stability. Resolved build blockers in `Models.kt` and `ViewerScreen.kt` related to unified telemetry naming.
*   **Issue #266**: **Mali Driver Audit**. Implemented detection hooks in `IntegrityMonitor.kt` to catch graphics layer stalls during high-frequency DB writes on A15 hardware.
*   **Issue #197**: **Database Pruning Standardization**. Aligned `ViolationDao`, `TrailDao`, and `HistoryDao` with R197 chunked pruning standards. All high-frequency tables now support staggered deletion to prevent I/O stalls.
*   **Issue #308**: **Restored Core Engine Definitions**. Re-implemented `AlarmEvaluationState`, `ProcessedLocation`, `SpatialAnchor`, and `RejectedPoint` in `EngineModels.kt`, unblocking the build and verifying Chapter 11.2 tests.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.22.05)
