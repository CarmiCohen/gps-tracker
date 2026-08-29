# Project Issues & Hardening Tracking (Aug.29.12)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 33 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 772 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None identified in this session.

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.29.12)
*   **Concern #762: Acoustic Refinement (R762b)**. Encapsulated the adaptive acoustic duty-cycle calculation into a standalone pure function in `SentinelValidator.kt`. Refactored `HardwareProvider.kt` to utilize this function, reducing complexity in the acoustic monitoring loop and improving testability (R762b).
*   **Concern #765: Ultra-Long Stationary State UI Refinement**. Resolved in Aug.29.11. Added visual indicators to HUD and Dashboard.
*   **Concern #765: Ultra-Long Stationary State Exposure**. Resolved in Aug.29.10. Centralized detection in HardwareProvider.
*   **Concern #764: Shared Engine Configuration Refinement**. Resolved in Aug.29.09. Consolidated device-specific flags into `HardwareCapabilities`.
*   **Concern #763: Ultra-Long Stationary GNSS Relaxation**. Resolved in Aug.29.08. Implemented 5min relaxation after 4h immobility.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.29.12)
