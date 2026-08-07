# Project Issues & Hardening Tracking (Aug.05.128)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 550 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None at this time.*

---

## 🔴 Open Issues
*   *None at this time.*

---

## 🟢 Recently Resolved Issues (Aug.05.128)
*   **[Issue #740] [Severity: Low] [Category: Performance] AppMapContainer Recomposition Audit.**
    *   **Resolution**: Refactored `AppMapContainer` and `OsmMap` in `MapComponents.kt` to decompose monolithic state objects (`MainUiState`, `KinematicState`, `DiagnosticState`) into primitive parameters. Updated call sites in `TrackerScreen.kt` and `ViewerScreen.kt` to match the new signature. This ensures that high-frequency telemetry updates do not trigger full recomposition of the map container, significantly reducing UI thread pressure during telemetry bursts (R736).

---

## 🟢 Recently Resolved Issues (Aug.05.127)
*   **[Issue #738] [Severity: Low] [Category: Performance] Ribbon Component Recomposition Audit (Final Optimization).**
    *   **Resolution**: Consolidated state collection in `AnalyticalRibbons` to a single collector for the entire ribbon stack. Moved all $O(N)$ path and background coordinate calculations into `drawWithCache` blocks across `ForensicRibbonContainer`, `GenericSensorRibbon`, and `ConnectionQualityRibbon`. This ensures per-frame drawing logic is restricted to pre-computed geometry, drastically reducing CPU load during active forensic monitoring (R726/R736).

---

## 🟢 Recently Resolved Issues (Aug.05.126)
*   **[Issue #739] [Severity: Low] [Category: Performance] Shared Component R736 Compliance.**
    *   **Resolution**: Finalized decomposition of `GlobalStatusBar` and `HeaderBar` in `SharedUiComponents.kt`. Call sites refactored to pass individual primitive parameters instead of unstable mutable objects (`LocationState`, `SystemHealthState`) or monolithic states (`MainUiState`).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.05.128)
