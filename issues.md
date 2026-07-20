# Project Issues & Hardening Tracking (July.20.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 298 |

---

## ⚠️ Newly Identified Risks & Concerns
*   (None)

---

## 🔴 Open Issues
*   (None)

---

## 🟢 Recently Resolved Issues (July.20.00)
*   **Issue #104: Startup ANR Hardening (Proactive Log Pruning)**.
    *   **Root Cause**: Database initialization or schema migration triggered ANRs on lower-end hardware (Samsung A15) due to excessively large log tables. Pruning was reactive, leaving cold-starts vulnerable.
    *   **Resolution**: Implemented `proactivePruning()` in `LogDao` and integrated it into `MainViewModel.loadInitialData`. The pruning logic sheds routine heartbeats (`watchdog_stats`, `viewer_pulse`, etc.) first to maintain forensic integrity while ensuring UI responsiveness during startup.

## 🟢 Recently Resolved Issues (July.19.04)
*   **Issue #103: Drift Reference Loss in `HistoryManager`**.
    *   **Root Cause**: `clockDriftRef` was only held in-memory, causing a loss of forensic continuity if the process was killed and the system clock was changed before restart.
    *   **Resolution**: Persisted `clockDriftRef` in `DataStore` (v58). `HistoryManager` now restores the drift reference during initialization, ensuring gap-filling logic correctly accounts for clock jumps occurring during app downtime.

## 🟢 Recently Resolved Issues (July.19.03)
*   **Issue #102: Temporal Forensic Integrity (Monotonic Time Strategy)**.
    *   **Root Cause**: Engine logic historically used Wall-clock time, which is subject to jumps and regressions, causing potential corruption in trajectory optimization (GTO) and alarm debouncing.
    *   **Resolution**: Hardened the `:core:engine` and app services to use a dual-time approach. All logic calculations (aging, debouncing, breakout) now use monotonic `rt` (realtime) timestamps. Human-readable logging persists using `ts` (UTC) for forensic correlation.
