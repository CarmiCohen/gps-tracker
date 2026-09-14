# Project Issues & Hardening Tracking (Sep.14.42)

## 🎯 Current Resumption Focus: Forensic Integrity & A15 Compliance
Monitoring signaling pipeline stability and sensor-to-relay latency on budget Samsung hardware.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Build Fragility: Version Type Safety (#1035)**:
    *   **Concern**: `app/build.gradle` uses `.toInteger()` on a catalog string without validation.
    *   **Risk**: `NumberFormatException` during build if the `.toml` entry is accidentally corrupted or set to a non-numeric string.

## 🟢 Recently Resolved Issues (Sep.14.42)
*   **Version Documentation Consistency (#1034)**:
    *   **Root-Cause Remediation**: Implemented a `syncDocsVersion` Gradle task in the root `build.gradle` that uses regex to automatically update version headers in `Handover.md`, `issues.md`, and other status reports. This ensures documentation matches the dynamic build version and eliminates manual synchronization risks (R-ID 328).

## 🟢 Recently Resolved Issues (Sep.14.41)
*   **Version Automation (#1033)**:
    *   **Root-Cause Remediation**: Implemented dynamic versioning in root `build.gradle`. `versionCode` is now derived from `git rev-list --count HEAD` and `versionName` is generated from a UTC timestamp. This eliminates manual versioning errors and satisfies Requirement 6.2.8 (Idea #18, R-ID 327).

## 🟢 Recently Resolved Issues (Sep.14.30)
*   **Version Management Centralization (#1026)**:
    *   **Root-Cause Remediation**: Migrated `versionCode` and `versionName` to `libs.versions.toml`. (Obsoleted by #1033 dynamic logic).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 328 (Rules: 64, IDs: 328), Resolved: 1034, Open: 1, Testing: 0, Ideas: 19, QA: 277]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.14.42)*
