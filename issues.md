# Project Issues & Hardening Tracking (v9.3.36)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 283 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Stray Proto File**: A stray file was accidentally created at `app/src/proto/app_settings.proto` during optimization. It should be manually deleted to avoid build confusion, as automated deletion is restricted.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (v9.3.36)
*   **Issue #092**: Landing Page Responsiveness & Redundant Service Startup.
    *   Eliminated mandatory 2s delay for manual role selection in `MainAppContent.kt`.
    *   Optimized `LaunchedEffect` to prevent redundant service calls during manual selection.
    *   Formalized requirement **R925** in `SOT_MASTER_REQUIREMENTS.md`.

---

## 🟢 Recently Resolved Issues (v9.3.25)
*   **Requirement R988**: Binary Telemetry Activation & Optimization.
    *   Activated Protobuf-based binary channel for tracker-to-relay communication.
    *   Optimized `RealtimeStatus` schema by migrating `TrackerState` and `LocationPendingReason` to Enums, reducing payload size.
    *   Implemented server-side binary routing in `relay-server/index.js` using explicit routing IDs to avoid server-side decoding.
*   **Issue #088**: Refined Identity Validation Feedback.
    *   Updated `SettingsRepository` to provide clear error messages when ID collisions occur with reserved legacy aliases (`T`, `V`, `Trk`, `viewer`).

---

## 🟢 Recently Resolved Issues (v9.3.20)
*   **Requirement R405**: Samsung A15 Power & Logic Hardening.
    *   Unified system heartbeat to **2000ms (2s)** globally to improve power resilience.
    *   Implemented proactive `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for A15 devices.
    *   Eliminated `isA15` logic branching from the core engine and service layers.
    *   Added `TYPE_STEP_DETECTOR` "Stay-Alive" sensor subscription to maintain process priority on Samsung devices.
