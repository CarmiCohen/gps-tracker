# Project Issues & Hardening Tracking (Sep.03.120)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **SOT Master Rules** | 🏗️ Standards | 41 |
| **Functional R-IDs** | 🧩 Requirements | 215 |
| **Resolved Issues** | 🟢 Progress | 866 |
| **Open Technical Issues** | 🔴 Priority | 0 |
| **Testing Chapters** | 🧪 Protocol | 100 |
| **Testing Sub-items** | 🔍 Granularity | 124 |
| **Simplification Ideas** | 💡 Future | 244 |
| **QA Validation Tasks** | 🟢 Validated | 234 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **None**. All high-priority Samsung A15 background suppression issues have been mitigated.

---

## 🔴 Open Issues (Prioritized)
*   **None**.

---

## 🟢 Recently Resolved Issues (Sep.03.120)
*   **Issue #899 RESOLVED: Basic Field Test Preparation (S21FE -> A15)**. Prepared the app for a coordinated field test. Verified `HardwareProvider` uses real high-accuracy GPS and aligned `SignalingConstants` for default identity pairing ("T" and "V"). Ready for deployment (R899).

---

## 🟢 Recently Resolved Issues (Sep.04.01)
*   **Issue #898 RESOLVED: A15 Connectivity & GPS Hardening**. Budget hardware (A15) showed intermittent signaling loss and GPS staleness due to aggressive OS background suppression. Implemented a multi-tier hardening strategy: Reduced A15 radio poke interval to 30s, tightened heuristic connection recovery to 10s, and forced a 10s GPS polling baseline when the screen is off to stay within UI freshness windows (R898).

---

## 🟢 Recently Resolved Issues (Sep.03.110)
*   **Issue #897 RESOLVED: Target SDK 35 FGS Compatibility**. Fixed `InvalidForegroundServiceTypeException` in `MaintenanceWorker` and `BootServiceStartWorker` by explicitly declaring and passing `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`. Standardized `getForegroundInfo()` across all work artifacts to ensure compliance with Android 15's stricter service type enforcement (R897).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.03.120)*
Current Audit Baseline: [SOT: 256 (Rules: 41, IDs: 215), Resolved: 866, Open: 0, Testing: 100 (Sub-items: 124), Ideas: 244, QA: 234]
