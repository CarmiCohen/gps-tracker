# Project Issues & Hardening Tracking (v9.3.56)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 289 |

---

## ⚠️ Newly Identified Risks & Concerns
*   ** samsung-specific ANR on Migration**: Even with IO offloading, extremely large log tables (1000+ entries) might cause slow startup during the `logs` table recreation on lower-end devices like the A15. Added pruning logic to mitigate.
*   **Anchor Sensitivity**: The new `ANCHOR_DISPLACEMENT_WEIGHT` for Issue #062 might require hardware-specific tuning if urban canyon jitter triggers false breakouts on older devices.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (v9.3.56 / July17.08)
*   **Issue #062: Dynamic Anchor Breakout (R990)**.
    *   **Root Cause**: Anchors were "sticky" because they only broke out on absolute distance thresholds, ignoring sustained physical effort and gradual displacement.
    *   **Resolution**: Implemented a displacement-weighted `anchorEscapeScore` monitor in `LocationProcessor`. Breaking out now considers physical motion, velocity, and displacement trends in a transition zone (70% of threshold).
*   **Issue #096: Room Database Migration Crash (IllegalStateException)**.
    *   **Root Cause**: Schema drift in the `logs` table where the persistent database lacked fields defined in the `LogEntity` Kotlin class. Missing registration of version 54 migrations.
    *   **Resolution**: Bumped database version to 55. Implemented `MIGRATION_54_55` to explicitly recreate the `logs` table with correct columns, types, and defaults. Registered all pending migrations in `AppModule`.
*   **Issue #092: Landing Page ANR Hardening**.
    *   **Root Cause**: Main thread starvation due to heavy entity-to-model mapping (Logs, Trails, History) during flow collection.
    *   **Resolution**: Implemented **R953 (Data Flow Offloading)**. All mapping operations are now offloaded to `Dispatchers.Default` in Repositories and UseCases.
