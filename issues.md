# Project Issues & Hardening Tracking (Aug.18.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Pending | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 640 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No new risks identified.*

---

## 🔴 Open Issues
*   *No open issues.*

---

## 🟢 Recently Resolved Issues (Aug.18.02)
*   **Issue #197: Forensic Storage-Aware Adaptive Pruning Refinement**:
    *   Hardened storage management for 100Hz sampling by implementing forensic-specific retention limits in `EngineConstants.kt`.
    *   Optimized `LogDao` with chunk-based pruning for `FORENSIC_TRACE` entries to minimize transaction lock duration during heavy I/O.
    *   Refined `proactivePruning` in `LogRepository.kt` to adaptively throttle pruning intensity based on `SystemHealthState` (Storage Critical/Low, Battery Charging/Low).
*   **Issue #196: Forensic Log Buffer Pressure Audit**:
    *   Hardened log buffer resilience for 100Hz sampling by increasing `LOG_BUFFER_CAPACITY` to 5000 and `LOG_BATCH_SIZE` to 100.
    *   Refined `LogRepository.kt` drainer logic: lowered `FORENSIC_FILL_THRESHOLD` to 25% to trigger earlier flushing.
    *   Optimized `startForensicDrainer()` to prioritize buffer relief during high-pressure events, even when CPU load is high, reducing `FORENSIC_OVERFLOW` risk.
*   **Issue #194: Battery Steep Discharge Logic Hardening**:
    *   Refined `checkBatteryDischarge()` to use load-aware thresholds (`NORMAL` vs `HIGH_LOAD`).
    *   Sensitivity is now automatically adjusted (reduced) when thermal throttling or CPU load > 70% is detected.
*   **Issue #195: Database Migration Crash Loop**: 
    *   Hardened `AppDatabase` migrations (`68` through `72`) to explicitly drop legacy indices before creation.
    *   Resolved `connection_history` schema mismatch by forcing the addition of `sitVzRt` in a hardened recovery migration (v72).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.18.02)
