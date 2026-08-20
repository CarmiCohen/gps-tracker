# Project Issues & Hardening Tracking (Aug.20.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 ACTIVE | 1 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 666 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #224-C1**: Coordinate "Snap" Artifacts. Reported edge cases where visual coordinate smoothing is bypassed during GPS revival, causing perceived jumps. (Issue #224)

---

## 🔴 Open Issues
*   **Issue #224: Field Monitoring & Maintenance**: Monitor production telemetry and address coordinate stabilization edge cases. Audit `HistoryDao` restoration integrity.

---

## 🟢 Recently Resolved Issues (Aug.20.03)
*   **Issue #223: Production Release Packaging**: Performed final audit and removal of debug instrumentation (`SimulateThermalEvent`, `TriggerForensicTest`) from UI overlays, ViewModels, and background services (R223).
*   **Issue #221: PhoneSetup UI Clipping**: Remediated layout compression on SM-A155F by removing redundant inset paddings and increasing scrollable bottom clearance (R221).
*   **Issue #222: Permission Refresh Performance**: Eliminated redundant state triggers during lifecycle transitions and optimized hydration timings to resolve 800ms+ startup jank (R222).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.20.04)
