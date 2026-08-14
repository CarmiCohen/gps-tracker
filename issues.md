# Project Issues & Hardening Tracking (Aug.14.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 0 | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 612 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None at this time.*

---

## 🔴 Open Issues
*   *No critical open issues.*

---

## 🟢 Recently Resolved Issues (Aug.14.02)
*   **[Issue #170] [Severity: Medium] [Category: UI/UX] Forensic Replay UI Audit.**
    *   **Resolution**: Restored coordinate-aware scrubbing in `AnalyticalRibbons`. Implemented `replayCursorTs` synchronization between ribbons and map. Utilized binary search for frame-perfect coordinate matching during high-frequency (100Hz) replay simulation. Verified zero-drift alignment between `vibeIdx` spikes and map marker positioning. (R170)

---

## 🟢 Recently Resolved Issues (Aug.14.01)
*   **[Issue #169] [Severity: High] [Category: Performance] Geofence Accuracy vs. Battery Audit.**
    *   **Resolution**: Resolved a "false-secure" risk where moving devices with screen-off dropped to 45s GPS polling. Updated `ServiceBehaviorUseCase` to maintain a safe 5s/2s polling interval whenever a geofence is active (R406a). Verified integrity via `GeofenceBatteryAuditTest` and `ServiceBehaviorAuditTest`. (R169)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.14.02)
