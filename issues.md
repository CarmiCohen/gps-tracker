# Project Issues & Hardening Tracking (July.16.22)

This document tracks active issues, technical debt, and pending implementation tasks.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 301 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **ConnectivitySuite Scope**: Consolidates network, sync, and remote handling. Maintain internal modularity to prevent bloat.
*   **AppContainer Circularity**: Resolved via lambda/lazy. Must be preserved in future DI changes.
*   **AlarmHistory Persistence**: `AlarmHistory` is currently held in memory. Transient states like `firstViolationTs` are reset if the process is killed.
*   **Main Thread Sensitivity**: Budget devices (A15/etc) are extremely sensitive to initialization spikes. Any new service-level component MUST offload I/O and state hydration by default. Even `getSystemService` calls should be considered potentially blocking on low-end hardware.
*   **Lazy Initialization Side-Effects**: Errors during component creation (e.g., DB migration failures) will now manifest later during first-use rather than at app launch. Robust logging is required to catch these deferred failures.

---

## 🔴 Open Issues
*   (None)

---

## 🟢 Recently Resolved Issues (July.16.22)
*   **Issue #526: A15 Landing Page Hang (Deep Fix)**: 
    *   Transitioned `AppContainer` to full lazy initialization to prevent Main thread spikes.
    *   Offloaded `osmdroid` and `WorkManager` setup in `GpsApplication` to background dispatchers.
    *   Verified smooth performance on S21FE and resolved startup unresponsiveness on Samsung A15.
*   **Issue #525**: Acoustic Fast-Path lockout timestamp not propagated to engine.
*   **Issue #524**: Stale `powerAlarmPending` flag in `AlarmHistory` on power restoration.
*   **Issue #523**: `IntegrityMonitor` Thread Safety (Concurrent access to `currentHealth`).
*   **Issue #522**: `LocationProcessor` Stationary Jitter (Anchor logic implemented for coordinate clamping).
*   **Issue #521**: `LocationSentinel` Passive Zeroing (Baseline capture logic implemented for relative tilt).
*   **Issue #520**: `ViewerService` Infrastructure Alert Logic Error (Fixed `isHardwareOnline` mapping).
*   **Issue #519**: `ViewerService` Health Mapping Regression (Viewers now monitor remote tracker health).
*   **Issue #518**: `ViewerService` Identity Bug (Fixed tracker ID persistence key).
*   **Issue #517 (R406k)**: Refactor AppAlarmManager (De-duplicate local state flags).
*   **Issue #516 (R406j)**: De-duplicate "Status" Logic.
*   **Issue #514 (R406i)**: Simplify GpsManager.
*   **Issue #513 (R406h)**: Flatten Service Architecture (ConnectivitySuite).
