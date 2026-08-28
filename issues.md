# Project Issues & Hardening Tracking (Aug.28.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 45 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 749 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.28.00)
*   **Concern #749: Persistent BaseEventQueue Leak (SystemStatusProvider)**. Deployment testing of Aug.27.05 revealed that `BaseEventQueue` disposal warnings persisted after `TrackerService` termination. Identified that `SystemStatusProviderImpl` was running multiple hardware-bound `callbackFlow` implementations (Internet, Battery, Power) in the application scope without deterministic unregistration. Hardened all flows to follow SOT 1.8, ensuring `ConnectivityManager` callbacks and `BroadcastReceivers` are explicitly unregistered in `awaitClose` (R749).
*   **Concern #748: CallbackFlow BaseEventQueue Leak (GpsManager)**. (Aug.27.05) Identified that `hardwareObservationFlow` in `GpsManager.kt` was performing asynchronous unregistration in its `awaitClose` block. Hardened the flow to synchronously await the `removeLocationUpdates` task, ensuring native disposal completes before the callback object is reclaimed (R748).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.28.00)
