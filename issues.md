# Project Issues & Hardening Tracking (v9.3.20)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 280 |

---

## ⚠️ Newly Identified Risks & Concerns
*No newly identified risks.*

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (v9.3.20)
*   **Requirement R405**: Samsung A15 Power & Logic Hardening.
    *   Unified system heartbeat to **2000ms (2s)** globally to improve power resilience.
    *   Implemented proactive `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for A15 devices.
    *   Eliminated `isA15` logic branching from the core engine and service layers.
    *   Added `TYPE_STEP_DETECTOR` "Stay-Alive" sensor subscription to maintain process priority on Samsung devices.

---

## 🟢 Recently Resolved Issues (v9.3.18)
*Moved to [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md)*
