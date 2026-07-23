# Project Issues & Hardening Tracking (July.23.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 365 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Stationary Sensitivity (Issue #530 Refinement)**: The new IMU-damping factor (`0.5`) on anchor breakout scoring relies heavily on the `isPhysicallyStationary` flag. If a device has a faulty accelerometer or extremely high vibration floor, it might lead to "sticky" anchors during real movement.

---

## 🔴 Open Issues
*   (None currently identified)

---

## 🟢 Recently Resolved Issues (July.23.07)
*   **Issue #530: Urban Multipath Suppression - Accuracy-Weighted Anchor**.
    *   **Resolution**: Refined the stationary anchor breakout logic in `LocationProcessor.kt`. Displacement toward breakout is now penalized by fix accuracy (high uncertainty = lower breakout vote) and damped by IMU stationary confirmation. Added suppression for "Accuracy Snaps" to prevent false breakouts during accuracy recovery.
    *   **Verification**: Backlog validation #530 requirements met for urban canyon stability and 5m breakout.
*   **Issue #113: Samsung A15 Fallback Hardening - Hardware Poke**.
    *   **Resolution**: Implemented a 10-second hardware "poke" in `TrackerService` (WakeLock renewal + sensor request) to prevent aggressive OS-level background eviction on budget hardware. Promoted Foreground Service to `specialUse` for this hardware profile.
    *   **Verification**: Service stability verified during extended stationary periods on A15 hardware.
*   **Issue #120b: I/O Stabilization - Startup Pruning Delay**.
    *   **Resolution**: Implemented a 2000ms delay for `proactivePruning` in `BaseMonitorService` to eliminate I/O contention during cold starts.
    *   **Verification**: Zero "UI ERROR" logs or stuttering observed during first 10 seconds of service initialization.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
