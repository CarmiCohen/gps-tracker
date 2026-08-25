# Project Issues & Hardening Tracking (Aug.24.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 CRITICAL | 49 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 710 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Samsung A15 Setup Blocker**: Initial deployment on SM-A155F identifies "Unrestricted" battery mode and "Appear on Top" permissions as hard blockers for system readiness. The `PhoneSetupOverlay` correctly intercepts mode entry, but automated recovery may be needed if users bypass these settings.
*   **Persistent Compose Lock Verification**: Logcat identifies `SnapshotStateList.conditionalUpdate` failures on A15 hardware despite Issue #255 refactoring. This suggests further optimization of high-frequency telemetry observers is required to eliminate main-thread frame skips (39+ frames).

---

## 🔴 Open Issues
*   **Issue #309**: **Compose Lock Verification Persistent Warnings**. Investigate why `SnapshotStateList` continues to trigger lock verification failures on non-generational GCs (Samsung A15). Potential need for `mutative` state aggregation or moving more UI logic to `Default` dispatcher.
*   **Issue #310**: **libmbrainSDK Ghost Load Persistence**. Even after Issue #251 "Identity Swap", the logcat continues to report `Can't load libmbrainSDK` on boot. Investigate deeper into CFMS (Custom Frequency Manager Service) triggers on Samsung devices to fully silence this forensic noise.

---

## 🟢 Recently Resolved Issues (Aug.24.01)
*   **Issue #307**: **Inconsistent Maintenance Uptime Logging**. Standardized Monotonic Authority by migrating `MaintenanceWorker` silence detection to `elapsedRealtime()`. Implemented `LAST_SERVICE_TICK_REALTIME_KEY` persistence in `TrackerService` and `ViewerService` to ensure duration-check integrity across service restarts and system time jumps.
*   **Issue #255**: **Compose Lock Verification Failure**. Refactored `MapOverlayManager` pools and icon caches to `SnapshotStateList` and `SnapshotStateMap`. This ensures proper snapshot isolation during high-frequency telemetry updates, eliminating `conditionalUpdate` lock verification failures within the `AndroidView.update` block.
*   **Issue #251**: **Integration Failure (mbrainSDK)**. Identified the `libmbrainSDK` Logcat error as a "Ghost Load" triggered by Samsung's CFMS detecting JNI patterns formerly associated with the legacy name. Documented the R212 Identity Swap in `JdHardwareManager` and `DEVICE_SPECIFIC_ADAPTATIONS.md` to silence forensic false positives.
*   **Audit Chapter 12.3**: **Sustained Storage Pressure**. Implemented simulation hooks and verified `PersistencePolicy` prioritization. Confirmed that normal logs/trails are gated while `isSpecial` forensic data persists under 99% fill levels (R197).
*   **Issue #280**: **Shadow-Cache LRU Race Condition**. Hardened `ShadowCache` using `ReentrantLock` and optimized initial capacity to prevent structural re-hashing and eviction race conditions during 100Hz simulation bursts (R280).
*   **Issue #140/12.2**: **Database Stress Audit (100Hz)**. Restored stress hooks and verified `ForensicSpillBuffer` stability. Resolved build blockers in `Models.kt` and `ViewerScreen.kt` related to unified telemetry naming.
*   **Issue #266**: **Mali Driver Audit**. Implemented detection hooks in `IntegrityMonitor.kt` to catch graphics layer stalls during high-frequency DB writes on A15 hardware.
*   **Issue #197**: **Database Pruning Standardization**. Aligned `ViolationDao`, `TrailDao`, and `HistoryDao` with R197 chunked pruning standards. All high-frequency tables now support staggered deletion to prevent I/O stalls.
*   **Issue #308**: **Restored Core Engine Definitions**. Re-implemented `AlarmEvaluationState`, `ProcessedLocation`, `SpatialAnchor`, and `RejectedPoint` in `EngineModels.kt`, unblocking the build and verifying Chapter 11.2 tests.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.24.01)
