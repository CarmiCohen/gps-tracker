# Project Issues & Hardening Tracking (Aug.29.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 38 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 767 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None identified in this session.

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.29.07)
*   **Concern #762: Acoustic Duty-Cycle Optimization**. Identified excessive battery drain during long stationary periods due to fixed microphone duty-cycling. Remediated by implementing adaptive off-cycle scaling in `HardwareProvider`, increasing the interval from 8s up to 30s based on stationary duration (R762).
*   **Concern #761: Telemetry Mapping Decomposition**. Resolved in Aug.29.05. Centralized transformations in `TelemetryMapper.kt`.
*   **Legacy Purge**: Resolved in Aug.29.05. Emptied legacy manager files.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.29.07)
