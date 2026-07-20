# Project Issues & Hardening Tracking (July.20.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 300 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Monotonic/Wall-Clock Desync**: While the bridge logic is robust, extreme system clock manipulation while the app is killed (e.g., changing the year) could still create massive "virtual" gaps that might hit the `MAX_BACKFILL_POINTS` limit (1000 points). This is a safe failure mode (preserving the last 1000 seconds), but worth noting.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (July.20.01)
*   **Issue #105: Forensic Ribbon Continuity Verification**.
    *   **Root Cause**: Process death caused a reset of the monotonic timeline, masking "Stability Gaps" during startup. Race conditions in `HistoryManager` initialization allowed first ticks to process before drift references were restored.
    *   **Resolution**: Implemented monotonic timeline reconstruction in `TrackerService` and `ViewerService` using persisted drift references. Hardened `HistoryManager` initialization into a synchronous-like suspend sequence to ensure forensic state is ready before the first telemetry pulse.

## 🟢 Recently Resolved Issues (July.20.00)
*   **Issue #104: Startup ANR Hardening (Proactive Log Pruning)**.
    *   **Resolution**: Implemented `proactivePruning()` in `LogDao` and integrated it into `MainViewModel.loadInitialData`.

## 🟢 Recently Resolved Issues (July.19.04)
*   **Issue #103: Drift Reference Loss in `HistoryManager`**.
    *   **Resolution**: Persisted `clockDriftRef` in `DataStore` (v58).
