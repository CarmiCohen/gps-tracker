# Project Issues & Hardening Tracking (Sep.03.121)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **SOT Master Rules** | 🏗️ Standards | 41 |
| **Functional R-IDs** | 🧩 Requirements | 215 |
| **Resolved Issues** | 🟢 Progress | 867 |
| **Open Technical Issues** | 🔴 Priority | 0 |
| **Testing Chapters** | 🧪 Protocol | 100 |
| **Testing Sub-items** | 🔍 Granularity | 124 |
| **Simplification Ideas** | 💡 Future | 244 |
| **QA Validation Tasks** | 🟢 Validated | 234 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #900: Background Service Restriction (A15)**: `android.app.BackgroundServiceStartNotAllowedException` observed on SM-A155F (A15). The OS is blocking `SystemForegroundService` start from the background. This is a critical risk for Tracker reliability on Android 14/15 (Target SDK 35) (R897/R898).
*   **Issue #901: Log Spam Regression**: Persistent `getPackageName: com.gps19.app` spam observed on both SM-G990E and SM-A155F despite Issue #894 remediation. Shadow-caching in `MainActivity` and `ConnectivitySuite` appears bypassed by system-level calls.
*   **Issue #902: Tracker Signal Loss**: A15 (Tracker) UI displays "SIGNAL LOSS" and "UNCERTAINTY: SIGNAL LOSS" during active session. While GPS loss is expected indoors, the lack of a clear "Relay Connected" confirmation in the logs suggests potential socket instability on budget hardware.
*   **Issue #903: Teardown-Loop Anomaly**: Logcat shows multiple "Starting connection" followed immediately by "Starting teardown sequence" on A15. This suggests a potential lifecycle crash or immediate service restart loop during the hydration phase.

---

## 🔴 Open Issues (Prioritized)
*   **None**.

---

## 🟢 Recently Resolved Issues (Sep.03.121)
*   **Issue #899 RESOLVED: Multi-Device Field Test (S21FE -> A15)**. Deployed version `Sep.03.120` to both devices. Verified Viewer readiness and identified critical A15-specific background regressions for next-phase remediation. Readiness prep complete (R899).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.03.121)*
Current Audit Baseline: [SOT: 256 (Rules: 41, IDs: 215), Resolved: 867, Open: 0, Testing: 100 (Sub-items: 124), Ideas: 244, QA: 234]
