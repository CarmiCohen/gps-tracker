# Project Issues & Hardening Tracking (July.19.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 297 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Samsung-specific ANR on Migration**: Even with IO offloading, extremely large log tables (1000+ entries) might cause slow startup during the table recreation on lower-end devices like the A15.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (July.19.04)
*   **Issue #103: Drift Reference Loss in `HistoryManager`**.
    *   **Root Cause**: `clockDriftRef` was only held in-memory, causing a loss of forensic continuity if the process was killed and the system clock was changed before restart.
    *   **Resolution**: Persisted `clockDriftRef` in `DataStore` (v58). `HistoryManager` now restores the drift reference during initialization, ensuring gap-filling logic correctly accounts for clock jumps occurring during app downtime.

## 🟢 Recently Resolved Issues (July.19.03)
*   **Issue #102: Temporal Forensic Integrity (Monotonic Time Strategy)**.
    *   **Root Cause**: Engine logic historically used Wall-clock time, which is subject to jumps and regressions, causing potential corruption in trajectory optimization (GTO) and alarm debouncing.
    *   **Resolution**: Hardened the `:core:engine` and app services to use a dual-time approach. All logic calculations (aging, debouncing, breakout) now use monotonic `rt` (realtime) timestamps. Human-readable logging persists using `ts` (UTC) for forensic correlation.

## 🟢 Recently Resolved Issues (July.19.02)
*   **Issue #100: Relay Wake-up Timeout Hardening**.
    *   **Root Cause**: Insufficient 30s timeout for relay infrastructure cold-starts.
    *   **Resolution**: Increased `NETWORK_TIMEOUT_MS` to 60s in `EngineConstants.kt`.
