# Project Issues & Hardening Tracking (Aug.13.13)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 CLEAN | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 604 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(None)*

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.13.13)
*   **[Issue #164] [Severity: Medium] [Category: Maintenance] Forensic Log Buffer Audit.**
    *   **Resolution**: Hardened the 100Hz logging path. Implemented deterministic composite IDs (`F-timestamp-idx`) to eliminate UUID churn. Added raw telemetry snapshots (`tempSnapshot`, `battSnapshot`, `chargingSnapshot`) to `LogEntry` to defer string formatting. Expanded `FORENSIC_SPILL_CAPACITY` to 10,000 and `LOG_BUFFER_CAPACITY` to 2,000. Removed nondeterministic `AtomicBoolean` guards in `LogManager` to prevent event drops. (R164)

---

## 🟢 Recently Resolved Issues (Aug.13.12)
*   **[Issue #163] [Severity: Medium] [Category: Performance] 1Hz Telemetry Path Optimization.**
    *   **Resolution**: Refactored `DashboardState` to use primitive types instead of pre-formatted strings. Moved formatting logic into Composable components (`MainDashboardGrid`, `TelemetryBox`, etc.) using `remember` blocks to eliminate object churn during the 1Hz heartbeat. (R163)

---

## 🟢 Recently Resolved Issues (Aug.13.11)
*   **[Issue #162] [Severity: High] [Category: UI/UX] Phone Setup ANR Stall.**
    *   **Resolution**: Hardened hydration gate (150ms) and increased staggered rendering offsets (80ms) in `PhoneSetupOverlay`. Memoized static build properties and hardware-specific descriptions. Optimized `HeaderBar` to hide alert animations while the setup overlay is active (R162).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.13.13)
