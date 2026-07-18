# Project Issues & Hardening Tracking (July.18.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 290 |

---

## ⚠️ Newly Identified Risks & Concerns
*   ** samsung-specific ANR on Migration**: Even with IO offloading, extremely large log tables (1000+ entries) might cause slow startup during the `logs` table recreation on lower-end devices like the A15. Added pruning logic to mitigate.
*   **Anchor Sensitivity**: The new `ANCHOR_DISPLACEMENT_WEIGHT` for Issue #062 might require hardware-specific tuning if urban canyon jitter triggers false breakouts on older devices.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (July.18.00)
*   **Issue #096 Hardening: Room Database Migration Crash (IllegalStateException)**.
    *   **Root Cause**: Inconsistent floating-point default value representation between Room's expected schema ("0.0") and SQLite's normalized format ("0").
    *   **Resolution**: Harmonized all `Double` column default values to `"0"` (integer string) across all `@Entity` definitions and within the `MIGRATION_54_55` and `MIGRATION_55_56` SQL recreation scripts in `Database.kt`.
    *   **Verification**: App successfully performs migration and starts without verification errors.

*   **Issue #062: Dynamic Anchor Breakout (R990)**. (v9.3.56 / July17.08)
    *   **Root Cause**: Anchors were "sticky" because they only broke out on absolute distance thresholds.
    *   **Resolution**: Implemented a displacement-weighted `anchorEscapeScore` monitor in `LocationProcessor`.
