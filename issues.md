# Project Issues & Hardening Tracking (v9.3.52)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 287 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None at this time.*

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (v9.3.52 / July17.06)
*   **Issue #092: Landing Page ANR Hardening**.
    *   **Root Cause**: Main thread starvation due to heavy entity-to-model mapping (Logs, Trails, History) during flow collection.
    *   **Resolution**: Implemented **R953 (Data Flow Offloading)**. All mapping operations are now offloaded to `Dispatchers.Default` in Repositories and UseCases.
*   **Issue #095: Setup Flow Deadlock & ANR Hardening**.
    *   **Root Cause**: Main thread starvation due to aggressive IPC polling (2s) and concurrent Map rendering.
    *   **Resolution**: Implemented **R950 (Setup Resource Isolation)** and **R951 (Throttled Polling)**.
