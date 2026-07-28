# Project Issues & Hardening Tracking (July.28.21)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 451 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (July.28.21)
*   **[Issue #615] [Severity: Low] [Category: Forensic] Stability Audit Metric Expansion**.
    - **Resolution**: Extended `StabilityAudit` in `TrackerService` and `ViewerService` to track and report GNSS callback jitter. Added `maxGnssJitterMs` tracking to `GpsManager` to detect hardware-level timing inconsistencies. Jitter exceeding `GNSS_JITTER_THRESHOLD_MS` (500ms) is now flagged as hardware instability in forensic logs.
    - **Validation**: Verified build success and requirement alignment (**R615**).

## 🟢 Recently Resolved Issues (July.28.20)
*   **[Issue #614] [Severity: Low] [Category: Structural] GNSS Callback Overhead Monitoring**.
    - **Resolution**: Implemented callback conflation in `GpsManager`. While scalar metrics (sat counts, SNR) remain real-time for health monitoring, detailed satellite list emissions to the `gnssDetailFlow` are now sampled at `GNSS_SAMPLING_INTERVAL_MS` (2000ms) to prevent Main Thread starvation on budget hardware.
    - **Validation**: Verified build success and requirement alignment (**R614**).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
