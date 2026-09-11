# Project Issues & Hardening Tracking (Sep.11.40)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated and audited. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence in the Sep.11.40 release candidate.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Reactive Flow Stalls (#915)**: Integrity warnings indicate power-related stalls compromising monitoring vitality on A15 hardware.
*   **GNSS Jitter & Stability Gaps (#916)**: Stability audit reports ~10s gaps during logic pulses and 9000ms GNSS jitter, suggesting hardware-level contention or scheduling interference.

## 🟢 Recently Resolved Issues (Sep.11.40)
*   **Viewer ID Adoption Failure RESOLVED (#912)**:
    *   **Root-Cause Remediation**: Corrected a logic error in `TrackerService.handleViewerPulse` where `configManager.viewerId` was compared against the Tracker default ("T") instead of the Viewer default ("V"). Tracker now correctly adopts custom Viewer IDs, enabling green TRK/DAT status on the Viewer.
*   **A15 Deployment Failure RESOLVED (#908)**: 
    *   **Root-Cause Remediation**: SM-A155F device (`R58X40GV2AR`) successfully detected, deployed, and verified. Connectivity and hardware initialization confirmed via logcat and UI audit.
*   **GNSS Zombie Recovery Logic Verified (#905/R252)**:
    *   **Root-Cause Remediation**: Verified `HardwareProvider` revival pulse logic (30s intervals) and hardware-level reset triggers.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 314 (Rules: 58, IDs: 256), Resolved: 994, Open: 2, Testing: 100% (Sub-items: 51), Ideas: 7, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.40)*
