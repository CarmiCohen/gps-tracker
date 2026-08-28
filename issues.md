# Project Issues & Hardening Tracking (Aug.28.06)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 43 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 755 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.28.06)
*   **Concern #755: GNSS & Network Unregistration Hardening**. Standardized GNSS unregistration by implementing `ManagedGnssStatusCallback` in `ManagedHardware.kt` and refactoring `GpsManager.kt`. Increased unregistration timeouts to 2000ms to tolerate high Main Looper congestion during teardown, effectively silencing `BaseEventQueue` disposal warnings (R755).
*   **Concern #754: Managed Sensor Abstraction (Leak Suppression)**. Introduced `ManagedSensorListener` and `ManagedDisplayListener` in `ManagedHardware.kt` to standardize synchronous hardware unregistration. Refactored `AppSensorManager` to use these abstractions (R754).
*   **Concern #753: Broadcast Hardware Abstraction (Leak Suppression)**. Implemented `ManagedBroadcastReceiver` and refactored `SystemStatusProvider` and `CommandRouter` to ensure deterministic cleanup (R753).
*   **Concern #752: Persistent BaseEventQueue Leak (Post-Abstraction)**. Resolved unregister deadlocks in `ManagedHardware.unregister` (R752).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.28.06)
