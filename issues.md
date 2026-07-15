# Project Issues & Hardening Tracking (July.11.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 284 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Reduced Telemetry Density (Issue #501)**: Standardizing to a 2s heartbeat removes the 200ms "High Frequency" mode. This improves battery life and stability but may result in slightly less granular trail paths during high-speed movement (e.g., >100km/h).
*   **Stray Proto File**: A stray file was accidentally created at `app/src/proto/app_settings.proto` during optimization. It should be manually deleted to avoid build confusion.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (July.11.01)
*   **Issue #501 (R406a)**: Unified Heartbeat (2s Standard).
    *   Standardized all periodic tasks and hardware polling to 2000ms (`TICK_INTERVAL_MS`).
    *   Removed `MOVING_GPS_POLLING_MS`, `STATIONARY_GPS_POLLING_MS`, `HIGH_FREQUENCY_GPS_POLLING_MS`, and `VIEWER_GPS_POLLING_MS`.
    *   Simplified `ServiceBehaviorUseCase` and `GpsManager` by removing dynamic interval logic.
    *   Formalized requirement **R406a** in `SOT_MASTER_REQUIREMENTS.md`.

---

## 🟢 Recently Resolved Issues (v9.3.36)
*   **Issue #092**: Landing Page Responsiveness & Redundant Service Startup.
    *   Eliminated mandatory 2s delay for manual role selection in `MainAppContent.kt`.
    *   Optimized `LaunchedEffect` to prevent redundant service calls during manual selection.
    *   Formalized requirement **R925** in `SOT_MASTER_REQUIREMENTS.md`.
