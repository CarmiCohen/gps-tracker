# Project Issues & Hardening Tracking (Aug.21.06)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 STABLE | 0 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 686 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #246-C3**: **Samsung OS Log Noise**: High-frequency OS-level package auditing (Kumiho/CFMS) persists on the SM-A155F. While UI stalls are mitigated by hydration consolidation (R246), background auditing overhead remains a systemic risk for budget hardware.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.21.06)
*   **Issue #196**: **Forensic Pipeline Hardening**: Implemented range-based signature deduplication (R197) and overflow hysteresis (R196) to handle 100Hz pressure on budget hardware. Added `SetForensicSimulation` hook for validation (R196-V).
*   **Issue #570**: **TrackerStatus Typo**: Fixed `violationUptimeMs` mapping in `toMap()` to ensure correct telemetry parity.
*   **Issue #246**: **UI Thread Optimization**: Mitigated Davey stalls (>700ms) on Samsung A15 by consolidating UI hydration to 3 phases and refactoring sensitivity sliders into a reusable component (R246).
*   **Issue #244**: **Native Library Audit**: Verified successful runtime loading and initialization of `libjdHardware.so` on target hardware (R244).
*   **Issue #247**: **UI Component Regression**: Restored sensitivity sliders for Vibration and Tilt in `AlertManagementOverlay` (R247).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.21.06)
