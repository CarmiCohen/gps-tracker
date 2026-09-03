# Project Issues & Hardening Tracking (Sep.03.110)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **SOT Master Rules** | 🏗️ Standards | 41 |
| **Functional R-IDs** | 🧩 Requirements | 213 |
| **Resolved Issues** | 🟢 Progress | 864 |
| **Open Technical Issues** | 🔴 Priority | 1 |
| **Testing Chapters** | 🧪 Protocol | 100 |
| **Testing Sub-items** | 🔍 Granularity | 124 |
| **Simplification Ideas** | 💡 Future | 243 |
| **QA Validation Tasks** | 🟢 Validated | 232 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #898: A15 Tracker Connectivity (Intermittent SRV/GPS red)**. Budget hardware (SM-A155F) shows intermittent loss of relay connection (SRV) and GPS freshness (GPS) indicators in the UI. Forensic audit suggests potential background throttling of the Socket.io thread or GNSS polling despite existing A15 "poke" mechanisms.

---

## 🔴 Open Issues (Prioritized)

### High Priority (Stability & Compliance)
*   **Issue #898**: Investigating socket flapping and GPS staleness on Samsung A15. Traced logic to `CommunicationManager` events and `HudTelemetryState` aggregation thresholds.

---

## 🟢 Recently Resolved Issues (Sep.03.110)
*   **Issue #897 RESOLVED: Target SDK 35 FGS Compatibility**. Fixed `InvalidForegroundServiceTypeException` in `MaintenanceWorker` and `BootServiceStartWorker` by explicitly declaring and passing `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`. Standardized `getForegroundInfo()` across all work artifacts to ensure compliance with Android 15's stricter service type enforcement (R897).

---

## 🟢 Recently Resolved Issues (Sep.03.100)
*   **Issue #247 RESOLVED: Signal Loss False Positives**. Implemented a 5s forensic grace period for budget hardware (A15) and correlated Signal Loss triggers with relay recovery states to eliminate false alarms during network handovers (R248).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.03.110)*
Current Audit Baseline: [SOT: 254 (Rules: 41, IDs: 213), Resolved: 864, Open: 1, Testing: 100 (Sub-items: 124), Ideas: 243, QA: 232]
