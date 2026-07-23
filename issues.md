# Project Issues & Hardening Tracking (July.23.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 364 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Anchor Jitter Sensitivity (Issue #530)**: The reduction of `PARKING_ANCHOR_MIN_DIST` to 6m (to meet the 5m breakout requirement) makes the engine more sensitive to extreme multipath bursts. The 8-sample averaging window is now the primary defense against static "spaghetti" trails.

---

## 🔴 Open Issues
*   (None currently identified)

---

## 🟢 Recently Resolved Issues (July.23.07)
*   **Issue #113: Samsung A15 Fallback Hardening - Hardware Poke**.
    *   **Resolution**: Implemented a 10-second hardware "poke" in `TrackerService` (WakeLock renewal + sensor request) to prevent aggressive OS-level background eviction on budget hardware. Promoted Foreground Service to `specialUse` for this hardware profile.
    *   **Verification**: Service stability verified during extended stationary periods on A15 hardware.
*   **Issue #120b: I/O Stabilization - Startup Pruning Delay**.
    *   **Resolution**: Implemented a 2000ms delay for `proactivePruning` in `BaseMonitorService` to eliminate I/O contention during cold starts.
    *   **Verification**: Zero "UI ERROR" logs or stuttering observed during first 10 seconds of service initialization.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
