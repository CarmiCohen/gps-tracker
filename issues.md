# Project Issues & Hardening Tracking (Sep.01.16)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 20 |
| **Validation Tasks** | 🟢 Validated | 218 |
| **Resolved (Total)** | 🟢 Progress | 809 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None identified in this session.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (Sep.01.16)
*   **Issue #889 RESOLVED: ManagedHardware Boilerplate Reduction (R889)**. Extracted shared unregistration logic (4000ms latch + fallback) into `ManagedUnregistrationHelper`. Refactored `ManagedNetworkCallback`, `ManagedGnssStatusCallback`, `ManagedSensorListener`, and `ManagedDisplayListener` to use the helper, reducing `ManagedHardware.kt` by ~195 lines of redundant code while ensuring consistent hardening. (Sep.01.16).
*   **Issue #888 RESOLVED: Specific sensor unregistration hardening (R888)**. Refactored `ManagedSensorListener` to support hardened specific sensor unregistration. Replaced direct `unregisterListener` calls in `HardwareProvider.kt` (step detector recovery) with the managed implementation to prevent `BaseEventQueue` leaks during individual sensor cycling on SM-A155F. (Sep.01.15).
*   **Issue #887 RESOLVED: Native BaseEventQueue leak remediation (R887)**. Hardened `ManagedHardware.kt` by increasing unregistration timeouts to 4000ms and implementing a final direct fallback unregistration on the current thread if the asynchronous attempt times out. This prevents native `BaseEventQueue` leaks during high-load stalls on SM-A155F. (Sep.01.14).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.01.16)*
*Simplification Ideas: 228*
