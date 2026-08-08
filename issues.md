# Project Issues & Hardening Tracking (Aug.07.06)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Action Required | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 563 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(None)*

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.07.06)
*   **[Issue #753] [Severity: High] [Category: Documentation] Restoration of Resolution Archive Integrity.**
    *   **Resolution**: Restored truncated historical records in `RESOLUTION_ARCHIVE.md` to satisfy R752 documentation integrity requirements.
*   **[Issue #752] [Severity: Low] [Category: Documentation] Status Tracking Synchronization.**
    *   **Resolution**: Synchronized `issues.md` and `RESOLUTION_ARCHIVE.md` baselines. Formalized Status Tracking Integrity requirement (R752).
*   **[Issue #751] [Severity: Low] [Category: Documentation] Final R747 Terminology Alignment.**
    *   **Resolution**: Performed a final sweep of `event-tables.md` and `EVENTS_DOC.md` to remove all remaining "Tracker:" prefixes and ensure "Device" is used consistently for remote status reporting (R751).
*   **[Issue #750] [Severity: Low] [Category: Documentation] Documentation Locality Synchronization.**
    *   **Resolution**: Synchronized formal documentation (specifically `ALARM_AND_SIREN_MECHANISM.md`, `SETTINGS_PAGE_DETAIL.md`, `EVENTS_AND_LOGGING_MECHANISM.md`, and `GUIDE_AND_SETTINGS.md`) with the R747 authority (R750).
*   **[Issue #749] [Severity: Low] [Category: Documentation] Documentation & Shard Synchronization.**
    *   **Resolution**: Synchronized the historical record to reflect the 558 resolution baseline. Formalized documentation integrity rules in SOT (R749).
*   **[Issue #748] [Severity: Low] [Category: UI/UX] Log Message Prefix Cleanup.**
    *   **Resolution**: Hardened log message consistency by removing legacy "Tracker:" prefixes and standardizing "Device" terminology in `IntegrityMonitor.kt` and `ViewerService.kt`. Ensures full compliance with R747 locality rules (R748).
*   **[Issue #747] [Severity: Medium] [Category: UI/UX] Event & Alert Text Unification.**
    *   **Resolution**: Synchronized all system event and alert text with the authoritative mapping (R747). Viewer-local events now use the "This device:" prefix, and tracker-remote events use "Device" in subtitles to ensure professional consistency and locality clarity. Updated `MainAlarmLogic.kt` and `EngineConstants.kt`.
*   **[Issue #746] [Severity: Low] [Category: Infrastructure] Missing libmbrainSDK.**
    *   **Resolution**: Fully transitioned the JNI bridge to the `jdMbrain` namespace to eliminate legacy log noise (`Can't load libmbrainSDK`) and avoid collisions with Samsung system libraries (R746).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.07.06)
