# Project Issues & Hardening Tracking (Sep.01.18)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 21 |
| **Validation Tasks** | 🟢 Validated | 218 |
| **Resolved (Total)** | 🟢 Progress | 810 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #891: Persistent `BaseEventQueue.dispose` warning during teardown.** Despite Issue #890 hardening, Logcat still reports native disposal failures on SM-A155F. Suspected leak in specific sensor registration paths or third-party components (e.g., Maps/OSMDroid) during rapid lifecycle transitions. (Sep.01.18).

---

## 🔴 Open Issues
*   **Issue #891: Persistent `BaseEventQueue.dispose` warning during teardown.** (See above).

---

## 🟢 Recently Resolved Issues (Sep.01.17)
*   **Issue #890 RESOLVED: Persistent native leak & Teardown Hardening (R890)**. Hardened `ManagedLocationCallback` by implementing the unified `ManagedUnregistrationHelper` pattern (4000ms latch + fallback). Introduced a 500ms settling delay in `HardwareProvider.stop()` before terminating the `HandlerThread`, ensuring native `BaseEventQueue` disposal is completed by the OS, resolving persistent Logcat warnings on SM-A155F. (Sep.01.17).
*   **Issue #889 RESOLVED: ManagedHardware Boilerplate Reduction (R889)**. Extracted shared unregistration logic (4000ms latch + fallback) into `ManagedUnregistrationHelper`. Refactored `ManagedNetworkCallback`, `ManagedGnssStatusCallback`, `ManagedSensorListener`, and `ManagedDisplayListener` to use the helper, reducing `ManagedHardware.kt` by ~195 lines of redundant code while ensuring consistent hardening. (Sep.01.16).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.01.18)*
*Simplification Ideas: 229*
