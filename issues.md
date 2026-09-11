# Project Issues & Hardening Tracking (Sep.11.41)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated and audited. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence in the Sep.11.41 release candidate.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **GNSS Jitter & Stability Gaps (#916)**: Stability audit reports ~10s gaps during logic pulses and 9000ms GNSS jitter, suggesting hardware-level contention or scheduling interference.

## 🟢 Recently Resolved Issues (Sep.11.41)
*   **Reactive Flow Stalls RESOLVED (#915)**:
    *   **Root-Cause Remediation**: Identified that `distinctUntilChanged()` in `SystemStatusProviderImpl` prevented "vitality pulses" for stable hardware states (e.g., constant power/storage), causing false-positive stall warnings in `IntegrityMonitor`. 
    *   **Fix**: Removed source-level `distinctUntilChanged()`, added periodic polling (60s) for Internet/Battery status, and updated `HardwareProvider` status flows to emit pulses. `IntegrityMonitor` now applies local deduplication while using raw pulses for heartbeat validation.
*   **Viewer ID Adoption Failure RESOLVED (#912)**:
    *   **Root-Cause Remediation**: Corrected a logic error in `TrackerService.handleViewerPulse` where `configManager.viewerId` was compared against the Tracker default ("T") instead of the Viewer default ("V").
*   **A15 Deployment Failure RESOLVED (#908)**: 
    *   **Root-Cause Remediation**: SM-A155F device (`R58X40GV2AR`) successfully detected, deployed, and verified.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 314 (Rules: 58, IDs: 256), Resolved: 995, Open: 1, Testing: 100% (Sub-items: 51), Ideas: 7, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.41)*
