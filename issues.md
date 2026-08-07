# Project Issues & Hardening Tracking (Aug.07.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 551 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None at this time.*

---

## 🔴 Open Issues
*   *None at this time.*

---

## 🟢 Recently Resolved Issues (Aug.07.00)
*   **[Issue #741] [Severity: Low] [Category: Performance] Dashboard & TelemetryBox Recomposition Audit.**
    *   **Resolution**: Refactored `TrackerDashboard`, `ViewerDashboard`, and `TelemetryBox` to eliminate monolithic state dependencies (`MainUiState`, `KinematicState`, `DiagnosticState`). Decomposed all parameters into primitives or stable objects and hoisted Flow collectors to the screen level. This ensures that high-frequency telemetry updates only trigger localized recompositions of specific telemetry fields rather than the entire dashboard grid (R736).

---

## 🟢 Recently Resolved Issues (Aug.05.128)
*   **[Issue #740] [Severity: Low] [Category: Performance] AppMapContainer Recomposition Audit.**
    *   **Resolution**: Refactored `AppMapContainer` and `OsmMap` in `MapComponents.kt` to decompose monolithic state objects into primitive parameters. This prevents UI thread stalls during telemetry bursts by isolating map rendering from unrelated state changes.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.07.00)
