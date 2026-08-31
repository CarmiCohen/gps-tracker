# Project Issues & Hardening Tracking (Sep.01.12)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 21 |
| **Validation Tasks** | 🟢 Validated | 217 |
| **Resolved (Total)** | 🟢 Progress | 804 |

---

## ⚠️ Newly Identified Risks & Concerns
*   (None)

---

## 🔴 Open Issues
*   *(See Dashboard for total count)*

---

## 🟢 Recently Resolved Issues (Sep.01.11)
*   **Issue #884 RESOLVED: Monitor::Inflate initialization failure regression (R884)**. Hardened `JdHardwareManager` initialization by switching to sequential invocation in `TrackerService` and `ViewerService` on A15 devices. This prevents race conditions where GNSS registration attempted to install the hardware monitor before the native library was fully initialized. (Sep.01.11).
*   **Issue #883 RESOLVED: Persistent 1074ms Davey Remediation (R883)**. Refactored `StatusRowData` in `SharedUiComponents.kt` to use a stable `StatusRowState` data class. This reduced the function parameter count from 22 to 1, significantly lowering the JIT compilation overhead identified by logcat during the final hydration level transition. (Sep.01.11).
*   **Issue #882 RESOLVED: Composition Segmentation & Davey Remediation (R882)**. Remediated 1074ms Davey identified in vSep.01.09. Implemented "Granular Composition Hydration" in `ViewerScreen`. (Sep.01.10).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.01.12)*
*Simplification Ideas: 227*
