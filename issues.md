# Project Issues & Hardening Tracking (v9.3.17)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 278 |

---

## ⚠️ Newly Identified Risks & Concerns
*No newly identified risks.*

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (v9.3.17)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **R404** | **Legacy Relay URL Fallback** | **Resolved**. Removed hardcoded legacy URL (`relay.gps19.com`) from `TrackerService`. Synchronized all service fallbacks to use `MainRepository.DEFAULT_RELAY_URL` pointing to the Render server. |
| **R403** | **Startup ANR Remediation** | **Resolved**. Increased global heartbeat (`TICK_INTERVAL_MS`) from 1s to 2s to skip startup frames and reduce main thread pressure during initialization. Standardized `TrackerService` to use the global constant. |

---

## 🟢 Recently Resolved Issues (v9.3.16)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#079** | **TrackerService API Synchronization** | **Resolved**. Remediated compilation errors by aligning service telemetry logic with current engine component signatures (`processGpsPoint`, `evaluateAlarms`, `pushCurrentStatus`). Implemented missing `BaseMonitorService` members. |
| **#078** | **Map Centering Follow Conflict** | **Resolved**. Implemented `MapFollowMode` (TRACKER, VIEWER, AUTO) in `MainUiState`. Updated `OsmMap` lock logic to respect follow intent, preventing the map from snapping back to the tracker after user centers on the viewer. |
