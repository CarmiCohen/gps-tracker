# Project Issues & Hardening Tracking (Sep.02.76)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Healthy | 0 |
| **Validation Tasks** | 🟢 Validated | 227 |
| **Resolved (Total)** | 🟢 Progress | 858 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues (Prioritized)

### High Priority (Stability & Compliance)
*   None.

---

## 🟢 Recently Resolved Issues (Sep.02.76)
*   **Issue #246 RESOLVED: Map Settings in Viewer Mode**. Restored functionality to the map tools overlay in viewer mode by integrating `MapUseCase` and `HomePointUseCase` into the `MainViewModel` event pipeline. This ensures that toggles for fences, violations, and geofence editing are correctly handled when the app is in viewer role (R-ID 247).

---
## 🟢 Recently Resolved Issues (Sep.02.70)
*   **Idea #241 RESOLVED: Protobuf Mapping Unification**. Consolidated mapping logic for `RealtimeStatus` (Signaling) and `TrackerStatusProto` (Persistence) into `TelemetryProtobufMapper` (R-ID 245).
*   **Idea #240 RESOLVED: ContextShadow Automation**. Integrated Hilt-managed `@ShadowContext` injection across all singleton services and suites (R-ID 244).
*   **Issue #245 RESOLVED: "SYS" Badge Deactivation lifecycle**. Added handlers for `ConfirmStopTracking` and `ManualExit` in `MainViewModel` to ensure `isSystemActive` is toggled false upon session termination (R-ID 246).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.02.76)*
*Simplification Ideas: 241 Active*
