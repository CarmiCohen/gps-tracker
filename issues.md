# Project Issues & Hardening Tracking (Aug.26.19)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 STABLE | 47 |
| **Validation Tasks** | 🟢 PASSED | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 743 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(No new concerns identified in this subversion)*

---

## 🔴 Open Issues
*   *(No high-priority open issues remaining for this subversion)*

---

## 🟢 Recently Resolved Issues (Aug.26.19)
*   **Concern #742 Hardening**: **Managed Hardware Callbacks**. Identified that anonymous `LocationCallback` in `GpsManager.restartLocationUpdates()` and escaped async `stepDetector` registrations in `AppSensorManager` were triggering `BaseEventQueue` leaks. Implemented explicit lifecycle tracking and cancellation for these transient registrations. Centralized native hardware bridge release in `BaseMonitorService` to ensure deterministic disposal during role-swaps (R742).

## 🟢 Recently Resolved Issues (Aug.26.18)
*   **Concern #742 Resolved**: **Recurrent EventQueue Leak**. Identified that `GpsManager` was redundantly registering `GnssStatus.Callback` within its `callbackFlow`, leading to overlapping native event queues during polling interval changes. Decoupled the callback lifecycle from the flow and tied it strictly to the synchronized `start()`/`stop()` lifecycle of the singleton. Hardened permission checks to ensure safety during registration (R742).
*   **Concern #738 Resolved**: **EventQueue Resource Leak**. Hardened lifecycle management in `AppSensorManager` and `GpsManager`. Synchronized `start()` and `stop()` methods and implemented atomic state re-checks within asynchronous registration blocks to prevent `BaseEventQueue disposal failures (R738).
*   **Concern #739 Resolved**: **Hydration Performance Stall (A15)**. Decomposed Map Hydration into Levels 4-7. Spreads Map Engine, Trails, Markers, and Final Overlays over multiple frames using IdleHandler and staggered delays to eliminate Davey stalls (R739).
*   **Concern #740 Resolved**: **System Issue Counter Mismatch**. Synchronized `PhoneSetupOverlay` items with `MainUiState.systemIssuesCount` (Aug.26.15).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.26.19)
