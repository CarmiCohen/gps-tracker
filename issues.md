# Project Issues & Hardening Tracking (Aug.27.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 47 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 745 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.27.02)
*   **Concern #745: Persistent BaseEventQueue Leak (AppSensorManager)**. Deployment on A15 hardware confirmed that the "BaseEventQueue.dispose" warning persisted despite GpsManager hardening. Identified that `AppSensorManager` was quitting its `sensorThread` before the system could finalize listener unregistration. Hardened `stop()` to queue unregistration on the `sensorHandler` and wait for the thread to join, ensuring deterministic disposal of the native event queue (R745).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.27.02)
