# Project Issues & Hardening Tracking (Sep.14.00)

## 🎯 Current Resumption Focus: Signaling Handshake Recovery
Investigating and resolving the connection failure between Viewer and Tracker roles using descriptive forensic drop logs.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
1.  **Forensic Drop Analysis (#1020)**: Monitor logcat for `Forensic drop` warnings. Use descriptive reasons from `SignalingValidator.getDropReason` to confirm if legitimate packets are being filtered during role transitions.
2.  **Identity Sync Loop Verification (#1021)**: Verify `ConnectivitySuite.startIdentitySyncLoop` successfully refreshes relay state after network handovers and role switches.
3.  **Relay Connectivity & Stale Socket Audit (#1022)**: Evaluate Simplification Idea #17 to ensure stale socket instances are not blocking new connection attempts during mode transitions.
4.  **IPC Noise Suppression (#1019)**: Continue monitoring `getPackageName` spam. Consider deeper shadowing if framework-level diagnostic logs persist despite cache hits.

## 🟢 Recently Resolved Issues (Sep.14.00)
*   **Build Restoration (#1024)**:
    *   **Root-Cause Remediation**: Resolved `Unresolved reference` errors in `TrackerService` and `ViewerService` by re-consolidating `ConnectivityEvent` as a top-level sealed class in `ConnectivitySuite.kt`. Restored project to a compiling state. (R-ID 321).
*   **Forensic Visibility Enhancement (#1019)**:
    *   **Root-Cause Remediation**: Integrated `SignalingValidator.getDropReason` into `ConnectivitySuite` logs. Rejection warnings now include descriptive reasons (e.g., "Unauthorized Viewer", "Echo suppression") instead of just raw IDs, enabling faster triage of signaling stalls. (R-ID 320).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 293 (Rules: 63, IDs: 321), Resolved: 1024, Open: 4, Testing: 0, Ideas: 18, QA: 277]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.14.00)*
