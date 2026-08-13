# Project Issues & Hardening Tracking (Aug.13.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 AT RISK | 2 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 594 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #156] WakeLock Log Saturation**: Frequent `acquireWakeLock(force=true)` calls from `AppSensorManager` saturate logcat, masking other forensic events.
*   **[Issue #157] Violation Path Allocations**: `ViolationPoint` and `ViolationEntity` still use `UUID.randomUUID()` and `GeoPoint` allocations in the detection hot-path, which may cause secondary GC spikes during high-activity scenarios.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.13.07)
*   **[Issue #155] [Severity: Low] [Category: UI/UX] Phone Setup UI Clutter.**
    *   **Resolution**: Refined `GuideSection` in `PhoneSetupOverlay` to hide completion-dependent action buttons once steps are verified (`isCompleted == true`). This reduces visual noise and clarifies the remaining setup tasks for the user.

---

## 🟢 Recently Resolved Issues (Aug.13.06)
*   **[Issue #152] [Severity: Medium] [Category: Performance] Excessive GC Pressure.**
    *   **Resolution**: Refactored the telemetry hot-path to use **Flyweight Pooling (R152)**. Converted `ConnectionPoint` to a mutable class and removed automatic `UUID` generation. Implemented a pooling strategy in `HistoryManager` and `MainRepository` to eliminate object churn during 1Hz steady-state tracking. UI flows now receive stable copies while the persistence layer utilizes zero-allocation mapping to Room entities.

---

## 🟢 Recently Resolved Issues (Aug.13.05)
*   **[Issue #153] [Severity: High] [Category: Performance] Startup Davey Stalls.**
    *   **Resolution**: Implemented **Staggered UI Hydration (R153)**. Introduced a multi-stage boot sequence to spread composition load across multiple frames, eliminating 1600ms stalls on Samsung A15.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.13.07)
