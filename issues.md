# Project Issues & Hardening Tracking (v9.3.16)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 276 |

---

## ⚠️ Newly Identified Risks & Concerns
*No newly identified risks.*

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (v9.3.16)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#079** | **TrackerService API Synchronization** | **Resolved**. Remediated compilation errors by aligning service telemetry logic with current engine component signatures (`processGpsPoint`, `evaluateAlarms`, `pushCurrentStatus`). Implemented missing `BaseMonitorService` members. |
| **#078** | **Map Centering Follow Conflict** | **Resolved**. Implemented `MapFollowMode` (TRACKER, VIEWER, AUTO) in `MainUiState`. Updated `OsmMap` lock logic to respect follow intent, preventing the map from snapping back to the tracker after user centers on the viewer. |

---

## 🟢 Recently Resolved Issues (v9.3.15)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#077** | **Type Safety Hardening** | **Resolved**. Systematic audit and elimination of redundant `toDouble()`/`toFloat()` conversions across core engine and app modules. Implemented `Double` pre-buffering in `AppSensorManager` and captured boundary conversions at the source in `TrackerService`. Standardized all persistence to `Double` in `SettingsRepository`. |
