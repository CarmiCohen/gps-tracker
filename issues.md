# Project Issues & Hardening Tracking (Sep.14.41)

## 🎯 Current Resumption Focus: Forensic Integrity & A15 Compliance
Monitoring signaling pipeline stability and sensor-to-relay latency on budget Samsung hardware.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Version Documentation Consistency (#1034)**:
    *   **Concern**: Documentation headers (e.g., in `Handover.md`) are not synchronized with the build version.
    *   **Risk**: Manual versioning errors and "documentation drift" between code and status reports (violates Requirement 7.2.2).
*   **Build Fragility: Version Type Safety (#1035)**:
    *   **Concern**: `app/build.gradle` uses `.toInteger()` on a catalog string without validation.
    *   **Risk**: `NumberFormatException` during build if the `.toml` entry is accidentally corrupted or set to a non-numeric string.

## 🟢 Recently Resolved Issues (Sep.14.41)
*   **Version Automation (#1033)**:
    *   **Root-Cause Remediation**: Implemented dynamic versioning in root `build.gradle`. `versionCode` is now derived from `git rev-list --count HEAD` and `versionName` is generated from a UTC timestamp. This eliminates manual versioning errors and satisfies Requirement 6.2.8 (Idea #18, R-ID 327).

## 🟢 Recently Resolved Issues (Sep.14.30)
*   **Version Management Centralization (#1026)**:
    *   **Root-Cause Remediation**: Migrated `versionCode` and `versionName` to `libs.versions.toml`. (Obsoleted by #1033 dynamic logic).

## 🟢 Recently Resolved Issues (Sep.14.20)
*   **Notification IPC Optimization (#1025)**:
    *   **Root-Cause Remediation**: Implemented state-change suppression in `AppNotificationManager.kt` to eliminate redundant `notify()` calls when the pulse status text is identical (R-ID 325).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 327 (Rules: 64, IDs: 327), Resolved: 1033, Open: 2, Testing: 0, Ideas: 18, QA: 277]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.14.41)*
