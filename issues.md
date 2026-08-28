# Project Issues & Hardening Tracking (Aug.28.10)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 43 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 759 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #757: Persistent BaseEventQueue Leak**. Verification of #756 showed that the `BaseEventQueue.dispose` warning persists in logs during service teardown. The leak is likely deeper in the native location listener handshake than previously mitigated.

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.28.10)
*   **Concern #758: UI Thread Congestion (Frame Skipping)**. Resolved "Davey" warnings (>1800ms) and 310+ skipped frames during Map Hydration (Levels 4-7). Implemented IO-thread pre-warming for the OSMDroid `SqlTileWriter` and integrated an `isOsmReady` gate into the `LifecycleHydrationManager` to ensure heavy engine initialization doesn't block the Main thread during startup (R758).
*   **Concern #757: Persistent BaseEventQueue Leak (Lifecycle Sync)**. Resolved native resource leak by refactoring `GpsManager.kt` to perform unconditional cleanup of location callbacks and hardware threads during `stop()`. (R757).
*   **Concern #759: Excessive Logcat Spam**. Soak testing on Samsung A15 devices revealed high-frequency diagnostic log spam (`getPackageName: com.gps19.app`) triggered by system-level calls. Migrated all lookups in `MainActivity` and `BaseMonitorService` to the `GpsApplication.PACKAGE_NAME` shadow-cache (R759).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.28.10)
