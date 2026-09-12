# Project Issues & Hardening Tracking (Sep.12.45)

## 🎯 Current Resumption Focus: Signaling Integrity & Multi-Device Sync
Verifying connection handshake reliability and session initialization across roles.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*None.*

## 🟢 Recently Resolved Issues (Sep.12.45)
*   **Signaling Resumption Hardening (#1015)**:
    *   **Root-Cause Remediation**: Hardened `ViewerService` and `TrackerService` to ensure the operational tick loop is initiated on *every* peer pulse if the job is not active. This prevents session stalls that occurred when a service loop was terminated (e.g., via exception or lifecycle) but the peer remained in the `SessionManager` pulse map, causing subsequent pulses to be ignored. (R-ID 314).
*   **Dual-Device Deployment & Baseline Sync (#1014)**:
    *   **Root-Cause Remediation**: Synchronized project baseline to version Sep.12.45. Prepared for dual-device deployment to S21FE and A15 to verify signaling fixes from Sep.12.31.

## 🟢 Recently Resolved Issues (Sep.12.31)
*   **Viewer Connection Handshake Bug (#1013)**:
    *   **Root-Cause Remediation**: Fixed a logic error in `ConnectivitySuite.handleJsonUpdate` where `viewer_pulse` and `tracker_pulse` packets were being consumed without emitting a `PeerPulse` event. (R-ID 314).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 288 (Rules: 62, IDs: 288), Resolved: 1015, Open: 0, Testing: 1 (Sub-items: 271), Ideas: 18, QA: 271]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.12.45)*
