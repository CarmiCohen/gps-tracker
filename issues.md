# Project Issues & Hardening Tracking (Aug.27.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 46 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 747 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.27.04)
*   **Concern #747: Persistent BaseEventQueue Leak (GpsManager Task Race)**. Deployment regression confirmed that `BaseEventQueue.dispose` warnings persisted because `fusedLocationClient.removeLocationUpdates()` returns an asynchronous Task. Hardened `GpsManager.stop()` to synchronously await these tasks using `Tasks.await()`, ensuring native disposal finishes before the hardware thread is terminated (R747).
*   **Concern #746: Multi-Source BaseEventQueue Leak**. (Aug.27.03) Standardized the "Unregister-on-Thread" pattern and implemented synchronous latching in `stop()` to guarantee native disposal before thread termination (R746).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.27.04)
