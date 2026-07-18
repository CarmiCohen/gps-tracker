# Project Issues & Hardening Tracking (July.18.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 291 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **samsung-specific ANR on Migration**: Even with IO offloading, extremely large log tables (1000+ entries) might cause slow startup during the table recreation on lower-end devices like the A15.
*   **Anchor Sensitivity**: The new `ANCHOR_DISPLACEMENT_WEIGHT` for Issue #062 might require hardware-specific tuning if urban canyon jitter triggers false breakouts on older devices.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (July.18.01)
*   **Issue #097: Room Database Identity Hash Mismatch (IllegalStateException)**.
    *   **Root Cause**: Discrepancy between manual SQL in `MIGRATION_55_56` and Room's expected schema (identity hash) for version 56.
    *   **Resolution**: Bumped version to 57. Added `MIGRATION_56_57` which performs a robust "create-new-copy-old-rename" sequence for all tables to strictly align with Entity definitions.
    *   **Verification**: App initializes database successfully and resumes tracking without integrity errors.

*   **Issue #096 Hardening: Room Database Migration Crash**. (July.18.00)
    *   **Root Cause**: Inconsistent floating-point default values.
    *   **Resolution**: Harmonized all `Double` column defaults to `"0"` in `@Entity` definitions.
