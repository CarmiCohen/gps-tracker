# Project Issues & Hardening Tracking (Aug.27.03)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 46 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 746 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.27.03)
*   **Concern #746: Multi-Source BaseEventQueue Leak**. Deployment logs confirmed that hardening `AppSensorManager` alone was insufficient due to lingering `GnssStatus` callbacks in `GpsManager` and async `StepDetector` registration races. Standardized the "Unregister-on-Thread" pattern and implemented synchronous latching in `stop()` to guarantee native disposal before thread termination (R746).
*   **Concern #745: Persistent BaseEventQueue Leak (AppSensorManager)**. (Aug.27.02) Hardened `stop()` to queue unregistration on the `sensorHandler` and wait for the thread to join (R745).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.27.03)
