# Project Issues & Hardening Tracking (July.28.20)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 450 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (July.28.20)
*   **[Issue #614] [Severity: Low] [Category: Structural] GNSS Callback Overhead Monitoring**.
    - **Resolution**: Implemented callback conflation in `GpsManager`. While scalar metrics (sat counts, SNR) remain real-time for health monitoring, detailed satellite list emissions to the `gnssDetailFlow` are now sampled at `GNSS_SAMPLING_INTERVAL_MS` (2000ms) to prevent Main Thread starvation on budget hardware.
    - **Validation**: Verified build success and requirement alignment (**R614**).

## 🟢 Recently Resolved Issues (July.28.18)
*   **[Issue #613] [Severity: Med] [Category: Forensic] Location Refresh Reactivity**.
    - **Resolution**: Migrated manual location-pending and stall re-checks to a reactive `locationStatusFlow` in `GpsManager`. `IntegrityMonitor` now observes this flow to maintain `SystemHealthState`. `TrackerService` refactored to broadcast status during pending intervals.
    - **Validation**: Verified build success and requirement alignment (**R613**).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
