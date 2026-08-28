# Project Issues & Hardening Tracking (Aug.27.05)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 46 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 748 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.27.05)
*   **Concern #748: CallbackFlow BaseEventQueue Leak (GpsManager)**. Identified that `hardwareObservationFlow` in `GpsManager.kt` was performing asynchronous unregistration in its `awaitClose` block. Hardened the flow to synchronously await the `removeLocationUpdates` task, ensuring native disposal completes before the callback object is reclaimed (R748).
*   **Concern #747: Persistent BaseEventQueue Leak (GpsManager Task Race)**. (Aug.27.04) Hardened `GpsManager.stop()` to synchronously await unregistration tasks using `Tasks.await()`, ensuring native disposal finishes before the hardware thread is terminated (R747).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.27.05)
