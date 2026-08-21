# Project Issues & Hardening Tracking (Aug.21.08)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 STABLE | 0 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 687 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #246-C3**: **Samsung OS Log Noise**: High-frequency OS-level package auditing (Kumiho/CFMS) persists on the SM-A155F. While UI stalls are mitigated by hydration consolidation (R246), background auditing overhead remains a systemic risk for budget hardware.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.21.08)
*   **Issue #196-V**: **Forensic Validation Hook UI**: Integrated the `SetForensicSimulation` toggle into the `DiagnosticsScreen`. This allows for manual verification of EMA reliability degradation and `ALERT_ID_PERFORMANCE_SPIKE` alarm triggers (R196-V).
*   **Issue #196**: **Forensic Pipeline Hardening**: Implemented range-based signature deduplication (R197) and overflow hysteresis (R196).
*   **Issue #570**: **TrackerStatus Typo**: Fixed `violationUptimeMs` mapping in `toMap()`.
*   **Issue #246**: **UI Thread Optimization**: Mitigated Davey stalls on Samsung A15 via 3-phase hydration (R246).
*   **Issue #244**: **Native Library Audit**: Verified `libjdHardware.so` runtime initialization on SM-A155F.
*   **Issue #247**: **UI Component Regression**: Restored sensitivity sliders (R247).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.21.08)
