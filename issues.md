# Project Issues & Hardening Tracking (Aug.17.11)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Pending | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 638 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No new risks identified.*

---

## 🔴 Open Issues
*   *No open issues.*

---

## 🟢 Recently Resolved Issues (Aug.17.11)
*   **Issue #194: Battery Steep Discharge Logic Hardening**:
    *   Refined `checkBatteryDischarge()` to use load-aware thresholds (`NORMAL` vs `HIGH_LOAD`).
    *   Sensitivity is now automatically adjusted (reduced) when thermal throttling or CPU load > 70% is detected, accounting for 100Hz forensic sampling and stress test saturation.
    *   Improved log diagnostic details to include the load context when a steep discharge is detected.
*   **Issue #195: Database Migration Crash Loop**: 
    *   Hardened `AppDatabase` migrations (`68` through `72`) to explicitly drop legacy indices before creation, resolving `UNIQUE constraint` violations.
    *   Resolved `connection_history` schema mismatch by forcing the addition of `sitVzRt` in a hardened recovery migration (v72).
    *   Verified successful app startup and UI rendering on physical device.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.17.11)
