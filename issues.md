# Project Issues & Hardening Tracking (v9.3.55)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 288 |

---

## ⚠️ Newly Identified Risks & Concerns
*   ** samsung-specific ANR on Migration**: Even with IO offloading, extremely large log tables (1000+ entries) might cause slow startup during the `logs` table recreation on lower-end devices like the A15. Added pruning logic to mitigate.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (v9.3.55 / July17.07)
*   **Issue #096: Room Database Migration Crash (IllegalStateException)**.
    *   **Root Cause**: Schema drift in the `logs` table where the persistent database lacked fields defined in the `LogEntity` Kotlin class. Missing registration of version 54 migrations.
    *   **Resolution**: Bumped database version to 55. Implemented `MIGRATION_54_55` to explicitly recreate the `logs` table with correct columns, types, and defaults. Registered all pending migrations in `AppModule`.
    *   **Startup ANR Hardening**: Offloaded `loadInitialData` to `Dispatchers.IO` to ensure Room migrations do not block the UI thread during cold start.
*   **Issue #092: Landing Page ANR Hardening**.
    *   **Root Cause**: Main thread starvation due to heavy entity-to-model mapping (Logs, Trails, History) during flow collection.
    *   **Resolution**: Implemented **R953 (Data Flow Offloading)**. All mapping operations are now offloaded to `Dispatchers.Default` in Repositories and UseCases.
