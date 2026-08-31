# Project Issues & Hardening Tracking (Aug.31.05)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 25 |
| **Validation Tasks** | 🟢 Validated | 211 |
| **Resolved (Total)** | 🟢 Progress | 788 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(None identified in current audit cycle)*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.31.05)
*   **Issue #810-M Validated: Acoustic Floor Calibration Audit**. Verified adaptive floor recovery logic via `AcousticCalibrationTest`.
    *   **Recovery Verification**: Confirmed that the floor correctly recovers from 90dB saturation to the 50dB baseline within expected forensic timeframes.
    *   **Contraction Logic**: Validated that the time-based contraction factor (`ACOUSTIC_FLOOR_CONTRACTION_EMA`) ensures recovery even during duty-cycle off-periods.
*   **Issue #779 Validated: Forensic Replay & Metadata Hardening**. Extended the `ForensicSanitizer` policy to the telemetry mapping and historical audit layers. (Aug.31.04).
*   **Issue #762 Validated: Acoustic Duty-Cycle & [ULTRA] Badge Correlation**. Hardened end-to-end propagation of the `isUltraLongStationary` state (Aug.31.03).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.31.05)*
*Simplification Ideas: 217*
