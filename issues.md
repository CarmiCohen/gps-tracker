# Project Issues & Hardening Tracking (July.28.22)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 452 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Concern #616-C1] Target Discrepancy in Objective Description**: The objective for Issue #616 specified auditing `SettingsRepository`, which utilizes `DataStore` flows and lacks `MutableSharedFlow`. The hardening was instead applied to `MainRepository.kt` (the central repository pipeline) where `_uiCommands` and `_liveHistoryFlow` were identified as using the default `SUSPEND` strategy.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (July.28.22)
*   **[Issue #616] [Severity: Med] [Category: Structural] Repository Event Pipeline Hardening**.
    - **Resolution**: Hardened the core repository event pipelines by configuring `_uiCommands` and `_liveHistoryFlow` in `MainRepository.kt` with `onBufferOverflow = BufferOverflow.DROP_OLDEST`. This prevents collector-side suspension during high-load signaling or UI contention, ensuring non-blocking telemetry and command routing.
    - **Validation**: Verified requirement alignment (**R616**).

## 🟢 Recently Resolved Issues (July.28.21)
*   **[Issue #615] [Severity: Low] [Category: Forensic] Stability Audit Metric Expansion**.
    - **Resolution**: Extended `StabilityAudit` in `TrackerService` and `ViewerService` to track and report GNSS callback jitter. Added `maxGnssJitterMs` tracking to `GpsManager` to detect hardware-level timing inconsistencies. Jitter exceeding `GNSS_JITTER_THRESHOLD_MS` (500ms) is now flagged as hardware instability in forensic logs.
    - **Validation**: Verified build success and requirement alignment (**R615**).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
