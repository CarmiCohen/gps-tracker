# Project Issues & Hardening Tracking (Aug.28.05)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 43 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 754 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.28.05)
*   **Concern #754: Managed Sensor Abstraction (Leak Suppression)**. Introduced `ManagedSensorListener` and `ManagedDisplayListener` in `ManagedHardware.kt` to standardize synchronous hardware unregistration. Refactored `AppSensorManager` to use these abstractions, replacing manual `CountDownLatch` logic and ensuring deterministic native resource cleanup (R754).
*   **Concern #753: Broadcast Hardware Abstraction (Leak Suppression)**. Implemented `ManagedBroadcastReceiver` to standardize and harden unregistration of system receivers. Refactored `SystemStatusProvider` and `CommandRouter` to use this abstraction, ensuring deterministic native resource cleanup and silencing persistent `BaseEventQueue` warnings (R753).
*   **Concern #752: Persistent BaseEventQueue Leak (Post-Abstraction)**. Resolved the persistent native leak warning by remediating a deadlock in `ManagedHardware.unregister`. The utility now detects if it's already on the Main Looper and executes immediately (R752).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.28.05)
