# Project Issues & Hardening Tracking (July.23.06)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 362 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Anchor Jitter Sensitivity (Issue #530)**: The reduction of `PARKING_ANCHOR_MIN_DIST` to 6m (to meet the 5m breakout requirement) makes the engine more sensitive to extreme multipath bursts. The 8-sample averaging window is now the primary defense against static "spaghetti" trails.

---

## 🔴 Open Issues
*   (None currently identified)

---

## 🟢 Recently Resolved Issues (July.23.06)
*   **Issue #072: Map Stabilization - Temporal Smoothing**.
    *   **Resolution**: Implemented EMA-based smoothing for tracker and viewer markers in `OsmMap`. Added a 30m snap threshold to ensure visual continuity during large movements while suppressing high-zoom jitter.
    *   **Verification**: Markers remain stable when stationary or moving slowly, but snap instantly to new positions upon significant displacement.
*   **Issue #530: Validation - Urban Multipath Stress Testing**.
    *   **Resolution**: Hardened "Accuracy Recovery" logic by explicitly suppressing jump flags during snaps. Refined "Stationary Anchor" sensitivity by reducing the minimum breakout distance to 6m and increasing the displacement score weight (8.0).
    *   **Verification**: Breakout sensitivity now meets the < 5m requirement for real movement while suppressing visual jumps during accuracy recovery.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
