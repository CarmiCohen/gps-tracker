# Project Issues & Hardening Tracking (Sep.03.50)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Healthy | 0 |
| **Validation Tasks** | 🟢 Validated | 228 |
| **Resolved (Total)** | 🟢 Progress | 859 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues (Prioritized)

### High Priority (Stability & Compliance)
*   None.

---

## 🟢 Recently Resolved Issues (Sep.03.50)
*   **Issue #897 RESOLVED: Target SDK 35 FGS Compatibility**. Fixed `InvalidForegroundServiceTypeException` in `MaintenanceWorker` by explicitly declaring and passing `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`. This ensures background recovery remains functional on Android 15 (Target SDK 35) (R897).

---

## 🟢 Recently Resolved Issues (Sep.02.76)
*   **Issue #246 RESOLVED: Map Settings in Viewer Mode**. Restored functionality to the map tools overlay in viewer mode by integrating `MapUseCase` and `HomePointUseCase` into the `MainViewModel` event pipeline (R-ID 247).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.03.50)*
*Simplification Ideas: 241 Active*
