# Project Issues & Hardening Tracking (Sep.01.22)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 20 |
| **Validation Tasks** | 🟢 Validated | 218 |
| **Resolved (Total)** | 🟢 Progress | 811 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(None currently identified for the new version).*

---

## 🔴 Open Issues
*   **Issue #891: Persistent `BaseEventQueue.dispose` warning during teardown.** (Validating fix in vSep.01.22).

---

## 🟢 Recently Resolved Issues (Sep.01.22)
*   **Issue #891 RESOLVED: Strict Teardown Sequencing & Settling Expansion (R891)**. Implemented deterministic unregistration sequencing in `HardwareProvider.stop()`, closing Location and GNSS pipes before sensors and display. Increased the teardown settling window to 800ms for OS-level native cleanup. Repaired `ForensicSnapshot` syntax and hardened logging to isolate specific component disposal failures on SM-A155F. (Sep.01.22).
*   **Issue #890 RESOLVED: Persistent native leak & Teardown Hardening (R890)**. Hardened `ManagedLocationCallback` by implementing the unified `ManagedUnregistrationHelper` pattern (4000ms latch + fallback). Introduced a settling delay in `HardwareProvider.stop()` before terminating the `HandlerThread`. (Sep.01.17).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.01.22)*
*Simplification Ideas: 229*
