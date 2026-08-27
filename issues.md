# Project Issues & Hardening Tracking (Aug.27.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 ATTENTION | 47 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 744 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.27.01)
*   **Concern #744: Persistent EventQueue Leak**. Identified that the `LocationCallback` in `GpsManager.hardwareObservationFlow` was escaping the disposal sequence due to the 5-second lingering subscription of `WhileSubscribed(5000)`. Hardened `GpsManager` to explicitly track and synchronously unregister the `activeLocationCallback` during `stop()`, ensuring native resources are released before the hardware thread is quit (R744).
*   **Concern #742 Hardening**: **Managed Hardware Callbacks**. (Aug.26.19) Implemented explicit lifecycle tracking and cancellation for transient `LocationCallback` and `stepDetector` registrations. Centralized native hardware bridge release in `BaseMonitorService` (R742).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.27.01)
