# Project Issues & Hardening Tracking (Sep.01.23)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 19 |
| **Validation Tasks** | 🟢 Validated | 219 |
| **Resolved (Total)** | 🟢 Progress | 812 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(None currently identified for the new version).*

---

## 🔴 Open Issues
*   *(No high-priority blockers remaining for this subversion).*

---

## 🟢 Recently Resolved Issues (Sep.01.23)
*   **Issue #891 RESOLVED: Strict Teardown Sequencing & Settling Expansion (R891)**. Verified on SM-A155F hardware. Implemented deterministic unregistration sequencing in `HardwareProvider.stop()`, closing Location and GNSS pipes before sensors and display. Increased the teardown settling window to 800ms for OS-level native cleanup. Repaired `ForensicSnapshot` syntax and hardened logging. (Sep.01.23).
*   **Issue #890 RESOLVED: Persistent native leak & Teardown Hardening (R890)**. Hardened `ManagedLocationCallback` by implementing the unified `ManagedUnregistrationHelper` pattern. (Sep.01.17).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.01.23)*
*Simplification Ideas: 229*
