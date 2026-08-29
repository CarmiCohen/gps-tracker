# Project Issues & Hardening Tracking (Aug.29.10)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 35 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 770 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None identified in this session.

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.29.10)
*   **Concern #765: Ultra-Long Stationary State Exposure**. Centralized the "Ultra-Long Stationary" detection in `HardwareProvider` and exposed it via Flow. Integrated this state into `TrackerService`, `NotificationManager`, and the UI aggregation pipeline. This ensures users (locally) and viewers (remotely) have full transparency when the system enters GNSS relaxation mode (R765).
*   **Concern #764: Shared Engine Configuration Refinement**. Resolved in Aug.29.09. Consolidated device-specific flags into `HardwareCapabilities`.
*   **Concern #763: Ultra-Long Stationary GNSS Relaxation**. Resolved in Aug.29.08. Implemented 5min relaxation after 4h immobility.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.29.10)
