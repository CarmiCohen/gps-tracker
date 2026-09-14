# Project Issues & Hardening Tracking (Sep.14.30)

## 🎯 Current Resumption Focus: Forensic Integrity & A15 Compliance
Monitoring signaling pipeline stability and sensor-to-relay latency on budget Samsung hardware.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*No open issues for this release cycle.*

## 🟢 Recently Resolved Issues (Sep.14.30)
*   **Version Management Centralization (#1026)**:
    *   **Root-Cause Remediation**: Migrated `versionCode` and `versionName` to `libs.versions.toml`. This ensures single-source-of-truth for project versioning, eliminating manual synchronization overhead and risk of version mismatch across documentation and build artifacts (Idea #18, R-ID 326).

## 🟢 Recently Resolved Issues (Sep.14.20)
*   **Notification IPC Optimization (#1025)**:
    *   **Root-Cause Remediation**: Implemented state-change suppression in `AppNotificationManager.kt` to eliminate redundant `notify()` calls when the pulse status text is identical. This reduces unnecessary IPC overhead and framework diagnostic noise on budget A15 hardware (R-ID 325).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 326 (Rules: 64, IDs: 326), Resolved: 1032, Open: 0, Testing: 0, Ideas: 18, QA: 277]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.14.30)*
