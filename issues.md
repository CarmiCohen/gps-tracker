# Project Issues & Hardening Tracking (July.28.18)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 449 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #614] [Risk: Low] GNSS Callback Frequency**: With reactive monitoring in `GpsManager`, extremely frequent hardware callbacks on certain devices might increase flow processing overhead. Monitor `IntegrityMonitor` execution time in `LatencyMonitor`.

---

## 🔴 Open Issues
*   **[Issue #614] GNSS Callback Overhead Monitoring**: Investigate if sampling of GNSS status updates is required for low-end hardware.

---

## 🟢 Recently Resolved Issues (July.28.18)
*   **[Issue #613] [Severity: Med] [Category: Forensic] Location Refresh Reactivity**.
    - **Resolution**: Migrated manual location-pending and stall re-checks to a reactive `locationStatusFlow` in `GpsManager`. `IntegrityMonitor` now observes this flow to maintain `SystemHealthState`. `TrackerService` refactored to broadcast status during pending intervals.
    - **Validation**: Verified build success and requirement alignment (**R613**).

## 🟢 Recently Resolved Issues (July.28.17)
*   **[Issue #612] [Severity: Med] [Category: Structural] Standby & Power-Save Reactivity**.
    - **Resolution**: Migrated remaining OS polling (Power Save Mode and App Standby Buckets) from the `IntegrityMonitor` logic loop to reactive flows in `SystemStatusProvider`. Introduced `PowerStatus` flow which uses a `BroadcastReceiver` for immediate Power Save updates and a 60s background poll for Standby Buckets.
    - **Validation**: Verified build success and requirement alignment (**R612**).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
