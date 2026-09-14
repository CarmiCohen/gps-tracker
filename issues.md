git add .
git commit -m "Forensic: Signaling State Reduction & Redundant Event Pruning (Sep.14.52) [R-ID 335]"
git tag -a Sep.14.52 -m "Release Sep.14.52: Simplified reactive signaling hierarchy and pruned redundant pulse events."
git push origin main --tags
# Project Issues & Hardening Tracking (Sep.14.52)

## 🎯 Current Resumption Focus: Forensic Integrity & A15 Compliance
Monitoring signaling pipeline stability and sensor-to-relay latency on budget Samsung hardware.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *(No high-priority open issues)*

## 🟢 Recently Resolved Issues (Sep.14.52)
*   **Signaling State Reduction & Redundant Event Pruning (#1041)**:
    *   **Root-Cause Remediation**: Simplified the sealed class hierarchy in `CommandRouter` by removing redundant `ViewerPulse` and `TransientDrop` events. Unified peer vitality detection around `ConnectivityEvent.PeerPulse` and removed legacy `ACTION_RELAY_STATUS` broadcast leftovers to reduce reactive path overhead. (R-ID 335).

## 🟢 Recently Resolved Issues (Sep.14.50)
*   **Redundant Logic Pruning & Legacy Backfill Triggers Removal (#1040)**:
    *   **Root-Cause Remediation**: Conducted a deep audit of `ConnectivitySuite` to remove legacy backfill triggers now fully handled by the 60s periodic identity sync loop. Pruned all associated leftover variables (`lastForceJoinTs`) to simplify the application architecture. (R-ID 334).

## 🟢 Recently Resolved Issues (Sep.14.47)
*   **Signaling Forensic Decoupling (#1039)**:
    *   **Root-Cause Remediation**: Migrated signaling drop and high-latency formatting and throttling from `ConnectivitySuite` to `SignalingForensicLogger`. (R-ID 333).
*   **A15 Battery Compliance & Signaling Log Throttling (#1038)**:
    *   **Root-Cause Remediation**: Implemented 10s throttling for forensic signaling drop logs. Protects the Android 15 battery discharge curve. (R-ID 332).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 335 (Rules: 64, IDs: 335), Resolved: 1041, Open: 0, Testing: 0, Ideas: 19, QA: 277]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (Sep.14.52)*
