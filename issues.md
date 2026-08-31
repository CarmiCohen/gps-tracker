# Project Issues & Hardening Tracking (Aug.31.03)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 26 |
| **Validation Tasks** | 🟢 Validated | 210 |
| **Resolved (Total)** | 🟢 Progress | 786 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(None identified in current audit cycle)*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.31.03)
*   **Issue #762 Validated: Acoustic Duty-Cycle & [ULTRA] Badge Correlation**. Hardened end-to-end propagation of the `isUltraLongStationary` state.
    *   **IntegrityMonitor.kt**: Now observes hardware relaxation state to update local health.
    *   **TelemetryUseCase.kt**: Restored mapping parity for status ingestion and updates (R765, R778).
    *   **HistoryManager.kt**: Integrated flag into analytical ribbons to ensure historical replay parity.
    *   **TrackerService.kt**: Propagates hardware relaxation to both signaling and history streams.
*   **Issue #782 Validated: UI Performance Hardening (History Sampling)**. Hardened the forensic ribbon pipeline in `MainViewModel` by integrating the `sample()` operator (3000ms for A15 hardware). This ensures Davey immunity (R312, R650) during high-frequency database writes triggered by the `ExecuteStressTest` routine, protecting the UI frame budget.
*   **Issue #782 Resolved: Protocol Audit - Binary Schema Expansion & Forensic Audit**. Expanded `RealtimeStatus` Protobuf schema to carry `violationUptimeMs` and `isUltraLongStationary`. Hardened `TelemetryMapper` and `ConnectionPoint` to ensure full forensic parity in history persistence and UI Replay.
*   **Issue #779: Forensic Metadata Leak Cleanup**. Implemented `ForensicSanitizer` to scrub absolute internal paths and normalize hardware identifiers in all exported logs and telemetry.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.31.03)*
*Simplification Ideas: 216*
