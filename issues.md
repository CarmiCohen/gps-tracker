# Project Issues & Hardening Tracking (Aug.28.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 43 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 756 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(No new concerns identified in this cycle)*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.28.07)
*   **Concern #756: Persistent BaseEventQueue Leak (GNSS/Network)**. Resolved remaining native leaks by hardening the `GpsManager` unregistration sequence with explicit trace logging and implementing deterministic listener clearing in `CommunicationManager` (Socket.io). Added fallback unregistration paths in `ManagedHardware.kt` to handle scenarios where the hardware thread is disposed before unregistration completes (R756).
*   **Concern #755: GNSS & Network Unregistration Hardening**. Standardized GNSS unregistration by implementing `ManagedGnssStatusCallback` in `ManagedHardware.kt` and refactoring `GpsManager.kt`. Increased unregistration timeouts to 2000ms.
*   **Concern #754: Managed Sensor Abstraction (Leak Suppression)**. Introduced `ManagedSensorListener` and `ManagedDisplayListener` in `ManagedHardware.kt`.
*   **Concern #753: Broadcast Hardware Abstraction (Leak Suppression)**. Implemented `ManagedBroadcastReceiver` for deterministic cleanup.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.28.07)
