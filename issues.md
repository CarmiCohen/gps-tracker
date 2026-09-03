# Project Issues & Hardening Tracking (Sep.03.100)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Healthy | 0 |
| **Validation Tasks** | 🟢 Validated | 229 |
| **Resolved (Total)** | 🟢 Progress | 860 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues (Prioritized)

### High Priority (Stability & Compliance)
*   None.

---

## 🟢 Recently Resolved Issues (Sep.03.100)
*   **Issue #247 RESOLVED: Signal Loss False Positives**. Implemented a 5s forensic grace period for budget hardware (A15) and correlated Signal Loss triggers with relay recovery states to eliminate false alarms during network handovers (R248).

---

## 🟢 Recently Resolved Issues (Sep.03.50)
*   **Issue #897 RESOLVED: Target SDK 35 FGS Compatibility**. Fixed `InvalidForegroundServiceTypeException` in `MaintenanceWorker` by explicitly declaring and passing `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` (R897).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.03.100)*
*Simplification Ideas: 242 Active*
