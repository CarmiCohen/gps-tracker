# Project Issues & Hardening Tracking (Aug.07.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 552 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None at this time.*

---

## 🔴 Open Issues
*   *None at this time.*

---

## 🟢 Recently Resolved Issues (Aug.07.01)
*   **[Issue #742] [Severity: Medium] [Category: Infrastructure] Proximity Index Sensitivity Refinement.**
    *   **Resolution**: Eliminated binary behavior in proximity sensing by implementing a debounced linear transition. Added `PROXIMITY_EMA_ALPHA` (0.15) to `EngineConstants.kt`. Updated `AppSensorManager` to apply EMA to raw proximity values and changed the forensic buffer to use average-based aggregation. Refactored `TelemetryAggregator` to use average-based merging for `proxIdx` across all time scales. This allows `RibbonsOverlay` to accurately visualize proximity density rather than instantaneous state flips (R742).

---

## 🟢 Recently Resolved Issues (Aug.07.00)
*   **[Issue #741] [Severity: Low] [Category: Performance] Dashboard & TelemetryBox Recomposition Audit.**
    *   **Resolution**: Refactored `TrackerDashboard`, `ViewerDashboard`, and `TelemetryBox` to eliminate monolithic state dependencies (`MainUiState`, `KinematicState`, `DiagnosticState`). Decomposed all parameters into primitives or stable objects and hoisted Flow collectors to the screen level. This ensures that high-frequency telemetry updates only trigger localized recompositions of specific telemetry fields rather than the entire dashboard grid (R736).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.07.01)
