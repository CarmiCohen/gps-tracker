# Project Issues & Hardening Tracking (Sep.14.43)

## 🎯 Current Resumption Focus: Forensic Integrity & A15 Compliance
Monitoring signaling pipeline stability and sensor-to-relay latency on budget Samsung hardware.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *(No high-priority open issues)*

## 🟢 Recently Resolved Issues (Sep.14.43)
*   **Build Fragility: Version Type Safety (#1035)**:
    *   **Root-Cause Remediation**: Implemented defensive checks in `app/build.gradle` using `rootProject.hasProperty` and `instanceof Integer` validation with explicit string-to-int conversion fallbacks. This prevents `NumberFormatException` and build failures if Git-derived properties are missing or malformed, providing safe fallbacks (R-ID 329).

## 🟢 Recently Resolved Issues (Sep.14.42)
*   **Version Documentation Consistency (#1034)**:
    *   **Root-Cause Remediation**: Implemented the `syncDocsVersion` Gradle task in the root `build.gradle` to automatically synchronize version headers across forensic documentation (`Handover.md`, `issues.md`, etc.). This ensures documentation matches the dynamic build version and eliminates manual synchronization risks (R-ID 328).

## 🟢 Recently Resolved Issues (Sep.14.41)
*   **Version Automation (#1033)**:
    *   **Root-Cause Remediation**: Implemented dynamic versioning in root `build.gradle`. `versionCode` is now derived from `git rev-list --count HEAD` and `versionName` is generated from a UTC timestamp. This eliminates manual versioning errors and satisfies Requirement 6.2.8 (Idea #18, R-ID 327).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 329 (Rules: 64, IDs: 329), Resolved: 1035, Open: 0, Testing: 0, Ideas: 19, QA: 277]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.14.43)*
