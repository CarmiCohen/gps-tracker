# Project Issues & Hardening Tracking (Sep.11.42)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence in the Sep.11.42 release candidate.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *(None)*

## 🟢 Recently Resolved Issues (Sep.11.42)
*   **GNSS Jitter & Stability Gaps RESOLVED (#916)**:
    *   **Root-Cause Remediation (Gaps)**: Identified that `TrackerService` and `ViewerService` hardcoded a 2s interval for stability auditing, causing false-positives when dynamic polling scaled to 60s. Updated to use `currentIntervalMs`.
    *   **Root-Cause Remediation (Jitter)**: Decoupled GNSS Status callbacks in `HardwareProvider` into a dedicated `HandlerThread` (`GNSSThread`) with background priority. This eliminates 9000ms jitter caused by scheduling contention with high-frequency sensor data (200Hz) on budget hardware (A15).
*   **Reactive Flow Stalls RESOLVED (#915)**: Decoupled vitality monitoring from state-change detection.
*   **Viewer ID Adoption Failure RESOLVED (#912)**: Corrected logic error in `TrackerService.handleViewerPulse`.
*   **A15 Deployment Failure RESOLVED (#908)**: SM-A155F device successfully verified.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 314 (Rules: 58, IDs: 256), Resolved: 996, Open: 0, Testing: 100% (Sub-items: 51), Ideas: 9, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.42)*
