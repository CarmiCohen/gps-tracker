# Project Issues & Hardening Tracking (Aug.28.11)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 43 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 762 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #758b: Residual UI Thread Congestion**. Despite engine pre-warming, log analysis on SM-A155F still shows "Davey" warnings (>1000ms) during Map Hydration (Levels 4-7). This indicates that the imperative overlay addition (Trails/Markers) in `MapOverlayManager` is still too heavy for single-frame execution.

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.28.11)
*   **Concern #757: Persistent BaseEventQueue Leak (Hardening)**. Audit of deployment logs showed native disposal failures during service teardown. Refactored `GpsManager` and `AppSensorManager` to make listener unregistration unconditional, ensuring that background revival callbacks and secondary hardware listeners are cleared even if primary flow state-tracking was out of sync (R757).
*   **Concern #759: Excessive Logcat Spam (Samsung A15)**. Hardened `SystemStatusProvider` to utilize `GpsApplication.PACKAGE_NAME` shadow-cache for high-frequency permission and capability checks, eliminating repetitive system-level diagnostic logs (R759).
*   **Concern #758: UI Thread Congestion (Frame Skipping)**. Implemented IO-thread pre-warming for OSM engine and integrated an `isOsmReady` gate to ensure heavy engine initialization occurs off the UI thread (R758).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.28.11)
